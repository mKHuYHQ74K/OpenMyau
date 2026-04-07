package myau.module.modules;

import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.TickEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.util.ChatUtil;
import myau.util.SoundUtil;
import myau.util.TeamUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.*;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.PotionEffect;
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
    public final BooleanProperty knockback = new BooleanProperty("knockback", true);
    public final BooleanProperty sound = new BooleanProperty("sound", true);
    public final IntProperty cooldown = new IntProperty("cooldown", 15, 1, 60);
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
                    ChatUtil.sendFormatted(String.format(
                            "&eItemAlarm: &7[&r%s&7] is holding [&r%s&r&7] &a%dm",
                            player.getDisplayName().getFormattedText(),
                            itemStack.getDisplayName(),
                            (int)Math.sqrt(mc.getRenderManager().getDistanceToCamera(player.posX, player.posY, player.posZ))
                    ));
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
            protected final Map<Pair<UUID, Long>, Long> data = new HashMap<>();
            @Override
            protected boolean contains(ItemAlarm itemAlarm, Item item) {
                return itemAlarm.potion.getValue() && item instanceof ItemPotion;
            }
            @Override
            public boolean check(ItemAlarm itemAlarm, ItemStack itemStack, UUID uuid, long currentTimeMillis) {
                if (itemStack == null) {
                    return false;
                }
                if (!this.contains(itemAlarm, itemStack.getItem())) {
                    return false;
                }
                long id = 0;
                for (PotionEffect effect: ((ItemPotion)itemStack.getItem()).getEffects(itemStack)) {
                    id |= 1L << effect.getPotionID();
                }
                Long expired = this.data.get(new ImmutablePair<>(uuid, id));
                if (expired != null && expired > currentTimeMillis) {
                    return false;
                }
                this.data.put(new ImmutablePair<>(uuid, id), currentTimeMillis + itemAlarm.cooldown.getValue() * 1000L);
                return true;
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
        knockback {
            @Override
            protected boolean contains(ItemAlarm itemAlarm, Item item) {
                return itemAlarm.knockback.getValue();
            }
            @Override
            public boolean check(ItemAlarm itemAlarm, ItemStack itemStack, UUID uuid, long currentTimeMillis) {
                if (itemStack == null) {
                    return false;
                }
                if (!this.contains(itemAlarm, itemStack.getItem())) {
                    return false;
                }
                if (!EnchantmentHelper.getEnchantments(itemStack).containsKey(Enchantment.knockback.effectId)) {
                    return false;
                }
                Long expired = this.data.get(uuid);
                if (expired != null && expired > currentTimeMillis) {
                    return false;
                }
                this.data.put(uuid, currentTimeMillis + itemAlarm.cooldown.getValue() * 1000L);
                return true;
            }
        };
        abstract protected boolean contains(ItemAlarm itemAlarm, Item item);
        protected final Map<UUID, Long> data = new HashMap<>();
        public boolean check(ItemAlarm itemAlarm, ItemStack itemStack, UUID uuid, long currentTimeMillis) {
            if (itemStack == null) {
                return false;
            }
            if (!this.contains(itemAlarm, itemStack.getItem())) {
                return false;
            }
            Long expired = this.data.get(uuid);
            if (expired != null && expired > currentTimeMillis) {
                return false;
            }
            this.data.put(uuid, currentTimeMillis + itemAlarm.cooldown.getValue() * 1000L);
            return true;
        }
        public void clean() {
            this.data.clear();
        }
    }
}
