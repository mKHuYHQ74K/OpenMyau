package myau.module.modules;

import myau.enums.ChatColors;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.TickEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.IntProperty;
import myau.util.ChatUtil;
import myau.util.SoundUtil;
import myau.util.TeamUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.event.HoverEvent;
import net.minecraft.init.Items;
import net.minecraft.item.*;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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
    public ItemAlarm() {
        super("ItemAlarm", false);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled()) return;
        if (event.getType() != EventType.POST) return;
        boolean playSound = false;
        long currentTimeMillis = System.nanoTime() / 1000000;
        for (EntityPlayer player : TeamUtil.getLoadedEntitiesSorted().stream().filter(entity -> entity instanceof EntityPlayer).map(EntityPlayer.class::cast).collect(Collectors.toList())) {
            if (player == mc.thePlayer) {
                continue;
            } else if (TeamUtil.isBot(player)) {
                if (!this.bots.getValue()) {
                    continue;
                }
            } else if (TeamUtil.isSameTeam(player)) {
                if (!this.teams.getValue()) {
                    continue;
                }
            } else if (TeamUtil.isFriend(player)) {
                if (!this.friends.getValue()) {
                    continue;
                }
            } else {
                if (!this.players.getValue()) {
                    continue;
                }
            }
            ItemStack itemStack = player.getHeldItem();
            for (HeldItems heldItems: HeldItems.values()) {
                if (heldItems.check(this, itemStack, player.getPersistentID(), currentTimeMillis)) {
                    playSound = true;
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
                            (int)Math.sqrt(mc.getRenderManager().getDistanceToCamera(player.posX, player.posY, player.posZ))
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

    @Override
    public void onDisabled() {
        for (HeldItems heldItems: HeldItems.values()) {
            heldItems.clean();
        }
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
                for (PotionEffect effect: ((ItemPotion)itemStack.getItem()).getEffects(itemStack)) {
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
            if (itemStack == null) {
                return false;
            }
            if (!this.contains(itemAlarm, itemStack)) {
                return false;
            }
            ImmutablePair<UUID, Long> id = new ImmutablePair<>(uuid, this.variation(itemStack));
            Long expired = this.data.get(id);
            if (expired != null && expired > currentTimeMillis) {
                return false;
            }
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
}
