package myau.command.commands;

import myau.Myau;
import myau.command.Command;
import myau.enums.ChatColors;
import myau.util.ChatUtil;

import java.util.*;

public class NickCommand extends Command {
    public NickCommand() {
        super(new ArrayList<>(Arrays.asList("nick", "unnick")));
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        if (args.size() < 2) {
            ChatUtil.sendFormatted(
                    String.format("%sUsage: .%s <&oplayer&r> <&onick&r> / .%s <&onick&r> / .%s list&r",
                            Myau.clientName, args.get(0).toLowerCase(Locale.ROOT),
                            args.get(0).toLowerCase(Locale.ROOT), args.get(0).toLowerCase(Locale.ROOT))
            );
            return;
        }

        String commandName = args.get(0).toLowerCase(Locale.ROOT);

        // .nick list or .unnick list
        if (args.get(1).equalsIgnoreCase("list")) {
            Map<String, String> all = Myau.nickManager.getAll();
            if (all.isEmpty()) {
                ChatUtil.sendFormatted(String.format("%sNo nicknames set&r", Myau.clientName));
                return;
            }
            ChatUtil.sendFormatted(String.format("%sNicknames:&r", Myau.clientName));
            for (Map.Entry<String, String> entry : all.entrySet()) {
                // Split the original name with a color-code reset+reapply so replaceAll
                // can't match the contiguous literal (avoids nick->nick display loop)
                String name = entry.getKey();
                int mid = Math.max(1, name.length() / 2);
                String displayOriginal = name.substring(0, mid) + "§r§o" + name.substring(mid);
                ChatUtil.sendRaw(String.format(ChatColors.formatColor("   &o%s&r &7->&r &o%s&r"), displayOriginal, entry.getValue()));
            }
            return;
        }

        // .unnick <nick>
        if (commandName.equals("unnick")) {
            String original = Myau.nickManager.removeByNick(args.get(1));
            if (original == null) {
                // Try treating it as original name instead
                String removed = Myau.nickManager.removeByOriginal(args.get(1));
                if (removed == null) {
                    ChatUtil.sendFormatted(String.format("%sNo nickname found for &o%s&r", Myau.clientName, args.get(1)));
                } else {
                    ChatUtil.sendFormatted(String.format("%sRemoved nick for &o%s&r", Myau.clientName, removed));
                }
            } else {
                ChatUtil.sendFormatted(String.format("%sRemoved nick &o%s&r for &o%s&r", Myau.clientName, args.get(1), original));
            }
            return;
        }

        // .nick <player> <nick> ...
        if (args.size() < 3) {
            ChatUtil.sendFormatted(
                    String.format("%sUsage: .%s <&oplayer&r> <&onick&r>&r", Myau.clientName, args.get(0).toLowerCase(Locale.ROOT))
            );
            return;
        }

        String player = args.get(1);
        String nick = String.join(" ", args.subList(2, args.size()));

        Myau.nickManager.setNick(player, nick);
        ChatUtil.sendFormatted(String.format("%sSet nick &o%s&r -> &o%s&r", Myau.clientName, player, nick));
    }
}
