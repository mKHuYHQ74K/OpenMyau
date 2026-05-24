package myau.module.modules;

import myau.enums.ChatColors;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.Render3DEvent;
import myau.events.TickEvent;
import myau.mixin.IAccessorRenderManager;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import myau.util.ChatUtil;
import myau.util.RenderUtil;
import myau.util.SoundUtil;
import myau.util.TeamUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.EntityList;
import org.lwjgl.opengl.GL11;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.event.HoverEvent;
import net.minecraft.init.Items;
import net.minecraft.item.*;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ItemAlarm extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final BooleanProperty players = new BooleanProperty("players", true);
    public final BooleanProperty friends = new BooleanProperty("friends", false);
    public final BooleanProperty teams = new BooleanProperty("teams", false);
    public final BooleanProperty bots = new BooleanProperty("bots", false);
    public final BooleanProperty fireball = new BooleanProperty("fireball", true);
    public final BooleanProperty pearl = new BooleanProperty("pearl", true);
    public final BooleanProperty potion = new BooleanProperty("potion", true);
    public final BooleanProperty bow = new BooleanProperty("bow", false);
    public final BooleanProperty egg = new BooleanProperty("egg", false);
    public final BooleanProperty snowball = new BooleanProperty("snowball", false);
    public final BooleanProperty spawn_egg = new BooleanProperty("spawn_egg", false);
    public final BooleanProperty fishing_rod = new BooleanProperty("fishing_rod", false);
    public final BooleanProperty stick = new BooleanProperty("stick", false);
    public final BooleanProperty diamond_pickaxe = new BooleanProperty("diamond_pickaxe", false);
    public final BooleanProperty sound = new BooleanProperty("sound", true);
    public final IntProperty cooldown = new IntProperty("cooldown", 30, 1, 300);

    // 3D marker display
    public final BooleanProperty marker = new BooleanProperty("marker", true);
    public final BooleanProperty iconMode = new BooleanProperty("icon", false);
    public final ModeProperty markerPos = new ModeProperty("marker-position", 0, new String[]{"LEFT", "RIGHT"});
    public final IntProperty maxMarkers = new IntProperty("max-markers", 5, 1, 20);
    public final IntProperty markerDuration = new IntProperty("marker-duration", 30, 1, 300);

    private final Map<String, TrackedPlayer> tracked = new ConcurrentHashMap<>();

    public ItemAlarm() {
        super("ItemAlarm", false);
    }

    private static String trackKey(UUID uuid, String itemType) {
        return uuid.toString() + "|" + itemType;
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled()) return;
        if (event.getType() != EventType.POST) return;

        long now = System.nanoTime() / 1000000;
        tracked.entrySet().removeIf(e -> e.getValue().expiry <= now);

        boolean playSound = false;
        for (EntityPlayer player : TeamUtil.getLoadedEntitiesSorted().stream().filter(entity -> entity instanceof EntityPlayer).map(EntityPlayer.class::cast).collect(Collectors.toList())) {
            if (player == mc.thePlayer) continue;

            if (TeamUtil.isBot(player)) {
                if (!this.bots.getValue()) continue;
            } else if (TeamUtil.isSameTeam(player)) {
                if (!this.teams.getValue()) continue;
            } else if (TeamUtil.isFriend(player)) {
                if (!this.friends.getValue()) continue;
            } else {
                if (!this.players.getValue()) continue;
            }

            ItemStack itemStack = player.getHeldItem();
            for (HeldItems heldItems : HeldItems.values()) {
                if (heldItems.check(this, itemStack, player.getPersistentID(), now)) {
                    playSound = true;

                    if (this.marker.getValue() && itemStack != null) {
                        tracked.put(trackKey(player.getPersistentID(), heldItems.name()),
                                new TrackedPlayer(itemStack.copy(), now + markerDuration.getValue() * 1000L));
                    }

                    IChatComponent info1 = new ChatComponentText(ChatColors.formatColor(String.format(
                            "&7ItemAlarm: [&r%s&7] is holding [&r",
                            player.getDisplayName().getFormattedText()
                    )));
                    IChatComponent info2 = new ChatComponentText(itemStack.getDisplayName());
                    info2.getChatStyle().setChatHoverEvent(
                            new HoverEvent(
                                    HoverEvent.Action.SHOW_ITEM,
                                    new ChatComponentText(itemStack.serializeNBT().toString())
                            )
                    );
                    IChatComponent info3 = new ChatComponentText(ChatColors.formatColor(String.format(
                            "&r&7] &a%dm",
                            (int) Math.sqrt(mc.getRenderManager().getDistanceToCamera(player.posX, player.posY, player.posZ))
                    )));
                    info1.appendSibling(info2);
                    info1.appendSibling(info3);
                    ChatUtil.send(info1);
                    break;
                }
            }
        }

        if (playSound && this.sound.getValue()) {
            SoundUtil.playSound("random.orb");
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (!this.isEnabled() || !this.marker.getValue() || mc.theWorld == null) return;

        long now = System.nanoTime() / 1000000;
        int max = this.maxMarkers.getValue();

        Map<UUID, List<Map.Entry<String, TrackedPlayer>>> byPlayer = new HashMap<>();
        Iterator<Map.Entry<String, TrackedPlayer>> it = tracked.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, TrackedPlayer> entry = it.next();
            if (entry.getValue().expiry <= now) {
                it.remove();
                continue;
            }
            String[] parts = entry.getKey().split("\\|");
            if (parts.length < 2) { it.remove(); continue; }
            UUID uuid = UUID.fromString(parts[0]);
            byPlayer.computeIfAbsent(uuid, k -> new ArrayList<>()).add(entry);
        }

        for (Map.Entry<UUID, List<Map.Entry<String, TrackedPlayer>>> playerEntry : byPlayer.entrySet()) {
            EntityPlayer player = mc.theWorld.getPlayerEntityByUUID(playerEntry.getKey());
            if (player == null) continue;

            if (player.deathTime > 0) continue;
            if (mc.getRenderViewEntity().getDistanceToEntity(player) > 512.0F) continue;

            List<Map.Entry<String, TrackedPlayer>> items = playerEntry.getValue();
            int itemCount = items.size();
            int renderCount = Math.min(itemCount, max);

            for (int i = 0; i < renderCount; i++) {
                boolean isLast = (i == renderCount - 1);
                boolean overflow = isLast && itemCount > max;
                renderMarker(player, items.get(i).getValue().itemStack,
                        event.getPartialTicks(), i, overflow, itemCount - max);
            }
        }
    }

    private void renderMarker(EntityPlayer player, ItemStack itemStack, float partialTicks,
                              int stackIndex, boolean overflow, int extraCount) {
        if (itemStack == null) return;

        double x = RenderUtil.lerpDouble(player.posX, player.lastTickPosX, partialTicks)
                - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX();
        double y = RenderUtil.lerpDouble(player.posY, player.lastTickPosY, partialTicks)
                - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY()
                + player.getEyeHeight();
        double z = RenderUtil.lerpDouble(player.posZ, player.lastTickPosZ, partialTicks)
                - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ();

        double distance = mc.getRenderViewEntity().getDistanceToEntity(player);
        double scale = Math.pow(Math.min(Math.max(distance, 4.0), 64.0), 0.75) * 0.0075;

        boolean useIcon = iconMode.getValue() && !overflow;
        String label = overflow ? "... (+" + extraCount + " more)" : itemStack.getDisplayName();
        int elemW = mc.fontRendererObj.getStringWidth(label);

        // Billboard transform
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y + (player.isSneaking() ? 0.5 : 0.9), z);
        GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
        float view = mc.gameSettings.thirdPersonView == 2 ? -1.0F : 1.0F;
        GlStateManager.rotate(mc.getRenderManager().playerViewX, view, 0.0F, 0.0F);
        GlStateManager.scale(-scale, -scale, scale);

        boolean leftSide = markerPos.getValue() == 0;
        float margin = 4.0F;
        float xOffset, textX, bgLeft, bgRight;
        if (leftSide) {
            xOffset = -(elemW + margin);
            textX = 0;
            bgLeft = -2;
            bgRight = elemW + 2;
        } else {
            xOffset = elemW + margin;
            textX = -elemW;
            bgLeft = -elemW - 2;
            bgRight = 2;
        }

        float lineH = useIcon ? 22 : mc.fontRendererObj.FONT_HEIGHT + 4;
        float yOffset = -stackIndex * lineH;

        GlStateManager.translate(xOffset, yOffset, 0.0F);

        // Background
        float bgTop = useIcon ? -12 : -mc.fontRendererObj.FONT_HEIGHT - 2;
        float bgBottom = useIcon ? 12 : 1;
        RenderUtil.enableRenderState();
        RenderUtil.drawRect(bgLeft, bgTop, bgRight, bgBottom, 0x90000000);
        RenderUtil.disableRenderState();

        if (useIcon) {
            int iconSize = 16;
            int iconX = (int) (textX + (elemW - iconSize) / 2.0F);
            int iconY = -iconSize / 2;
            GlStateManager.pushMatrix();
            GlStateManager.translate(iconX, iconY, 0.0F);
            GlStateManager.enableDepth();
            RenderHelper.enableGUIStandardItemLighting();
            GlStateManager.disableLighting();
            GlStateManager.enableRescaleNormal();
            float curZ = mc.getRenderItem().zLevel;
            mc.getRenderItem().zLevel = -100.0F;
            mc.getRenderItem().renderItemIntoGUI(itemStack, 0, 0);
            mc.getRenderItem().renderItemOverlays(mc.fontRendererObj, itemStack, 0, 0);
            mc.getRenderItem().zLevel = curZ;
            RenderHelper.disableStandardItemLighting();
            GlStateManager.popMatrix();
            // Restore GL state to billboard-safe defaults
            GlStateManager.enableDepth();
            GlStateManager.depthFunc(GL11.GL_LEQUAL);
            GlStateManager.enableAlpha();
            GlStateManager.alphaFunc(GL11.GL_GREATER, 0.5F);
            GlStateManager.enableCull();
            GlStateManager.enableTexture2D();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        } else {
            GlStateManager.disableDepth();
            mc.fontRendererObj.drawStringWithShadow(
                    label,
                    textX,
                    (float) (-mc.fontRendererObj.FONT_HEIGHT) + 1.0F,
                    overflow ? 0xFFAAAAAA : 0xFFFFFFFF
            );
            GlStateManager.enableDepth();
        }

        GlStateManager.popMatrix();
    }

    @Override
    public void onDisabled() {
        for (HeldItems heldItems : HeldItems.values()) {
            heldItems.clean();
        }
        tracked.clear();
    }

    private enum HeldItems {
        fireball {
            @Override
            protected boolean contains(ItemAlarm itemAlarm, Item item) {
                return itemAlarm.fireball.getValue() && item instanceof ItemFireball;
            }
        },
        pearl {
            @Override
            protected boolean contains(ItemAlarm itemAlarm, Item item) {
                return itemAlarm.pearl.getValue() && item instanceof ItemEnderPearl;
            }
        },
        potion {
            @Override
            protected boolean contains(ItemAlarm itemAlarm, Item item) {
                return itemAlarm.potion.getValue() && item instanceof ItemPotion;
            }
            @Override
            protected long variation(ItemStack itemStack) {
                long id = 0;
                for (PotionEffect effect : ((ItemPotion) itemStack.getItem()).getEffects(itemStack)) {
                    id |= 1L << effect.getPotionID();
                }
                return id;
            }
        },
        bow {
            @Override
            protected boolean contains(ItemAlarm itemAlarm, Item item) {
                return itemAlarm.bow.getValue() && item instanceof ItemBow;
            }
        },
        egg {
            @Override
            protected boolean contains(ItemAlarm itemAlarm, Item item) {
                return itemAlarm.egg.getValue() && item instanceof ItemEgg;
            }
        },
        snowball {
            @Override
            protected boolean contains(ItemAlarm itemAlarm, Item item) {
                return itemAlarm.snowball.getValue() && item instanceof ItemSnowball;
            }
        },
        spawn_egg {
            @Override
            protected boolean contains(ItemAlarm itemAlarm, Item item) {
                return itemAlarm.spawn_egg.getValue() && item == Items.spawn_egg;
            }
            @Override
            protected long variation(ItemStack itemStack) {
                return EntityList.getIDFromString(ItemMonsterPlacer.getEntityName(itemStack));
            }
        },
        fishing_rod {
            @Override
            protected boolean contains(ItemAlarm itemAlarm, Item item) {
                return itemAlarm.fishing_rod.getValue() && item instanceof ItemFishingRod;
            }
        },
        stick {
            @Override
            protected boolean contains(ItemAlarm itemAlarm, Item item) {
                return itemAlarm.stick.getValue() && item == Items.stick;
            }
        },
        diamond_pickaxe {
            @Override
            protected boolean contains(ItemAlarm itemAlarm, Item item) {
                return itemAlarm.diamond_pickaxe.getValue() && item == Items.diamond_pickaxe;
            }
        };

        abstract protected boolean contains(ItemAlarm itemAlarm, Item item);

        protected boolean contains(ItemAlarm itemAlarm, ItemStack itemStack) {
            return this.contains(itemAlarm, itemStack.getItem());
        }

        protected final Map<Pair<UUID, Long>, Long> data = new HashMap<>();

        public boolean check(ItemAlarm itemAlarm, ItemStack itemStack, UUID uuid, long currentTimeMillis) {
            if (itemStack == null) return false;
            if (!this.contains(itemAlarm, itemStack)) return false;
            ImmutablePair<UUID, Long> id = new ImmutablePair<>(uuid, this.variation(itemStack));
            Long expired = this.data.get(id);
            if (expired != null && expired > currentTimeMillis) return false;
            this.data.put(id, currentTimeMillis + itemAlarm.cooldown.getValue() * 1000L);
            return true;
        }

        public void clean() {
            this.data.clear();
        }

        protected long variation(ItemStack itemStack) {
            return 0L;
        }
    }

    private static class TrackedPlayer {
        final ItemStack itemStack;
        final long expiry;

        TrackedPlayer(ItemStack itemStack, long expiry) {
            this.itemStack = itemStack;
            this.expiry = expiry;
        }
    }
}
