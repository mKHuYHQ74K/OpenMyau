package myau.command.commands;

import com.google.common.collect.Iterables;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import myau.Myau;
import myau.command.Command;
import myau.enums.ChatColors;
import myau.util.ChatUtil;
import myau.util.TeamUtil;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.event.HoverEvent;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;
import java.util.stream.Collectors;

public class DumpArmorCommand extends Command {
    public DumpArmorCommand() {
        super(new ArrayList<>(Arrays.asList("dump", "dumparmor")));
    }
    @Override
    public void runCommand(ArrayList<String> args) {
        String target_name;
        if (args.size() < 2) {
            target_name = null;
        } else {
            target_name = args.get(1);
        }
        for (EntityPlayer player : TeamUtil.getLoadedEntitiesSorted().stream().filter(entity -> entity instanceof EntityPlayer).map(EntityPlayer.class::cast).collect(Collectors.toList())) {
            if (target_name == null || target_name.equals(player.getName())) {
                IChatComponent info1 = new ChatComponentText(ChatColors.formatColor(String.format(
                        "&7DumpArmor: [&r%s&7] is holding",
                        player.getDisplayName().getFormattedText()
                )));
                for (int i = 4; i >= 0; i--) {
                    ItemStack itemStack;
                    if (i == 0) {
                        itemStack = player.getHeldItem();
                    } else {
                        itemStack = player.inventory.armorInventory[i - 1];
                    }
                    if (itemStack != null) {
                        IChatComponent info2 = new ChatComponentText(ChatColors.formatColor(String.format(
                                " [&r%s&7]",
                                itemStack.getDisplayName())));
                        info2.getChatStyle().setChatHoverEvent(
                                new HoverEvent(
                                        HoverEvent.Action.SHOW_ITEM,
                                        new ChatComponentText(itemStack.serializeNBT().toString())
                                )
                        );
                        info1.appendSibling(info2);
                    }
                }
                ChatUtil.send(info1);
            }
        }
    }
}
