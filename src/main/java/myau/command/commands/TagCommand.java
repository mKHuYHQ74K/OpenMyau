package myau.command.commands;

import myau.Myau;
import myau.command.Command;
import myau.util.ChatUtil;

import java.util.*;

public class TagCommand extends Command {
    private static final Map<String, String> COLOR_ALIASES = new LinkedHashMap<>();

    static {
        COLOR_ALIASES.put("r", "c");
        COLOR_ALIASES.put("g", "a");
        COLOR_ALIASES.put("0", "0");
        COLOR_ALIASES.put("1", "1");
        COLOR_ALIASES.put("2", "2");
        COLOR_ALIASES.put("3", "3");
        COLOR_ALIASES.put("4", "4");
        COLOR_ALIASES.put("5", "5");
        COLOR_ALIASES.put("6", "6");
        COLOR_ALIASES.put("7", "7");
        COLOR_ALIASES.put("8", "8");
        COLOR_ALIASES.put("9", "9");
        COLOR_ALIASES.put("a", "a");
        COLOR_ALIASES.put("b", "b");
        COLOR_ALIASES.put("c", "c");
        COLOR_ALIASES.put("d", "d");
        COLOR_ALIASES.put("e", "e");
        COLOR_ALIASES.put("f", "f");
    }

    public TagCommand() {
        super(new ArrayList<>(Arrays.asList("tag", "untag")));
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        if (args.size() < 2) {
            ChatUtil.sendFormatted(
                    String.format("%sUsage: .%s <&oplayer&r> [color] / .%s list&r / .%s remove <&oplayer&r>",
                            Myau.clientName, args.get(0).toLowerCase(Locale.ROOT),
                            args.get(0).toLowerCase(Locale.ROOT), args.get(0).toLowerCase(Locale.ROOT))
            );
            return;
        }

        String commandName = args.get(0).toLowerCase(Locale.ROOT);

        // list
        if (args.get(1).equalsIgnoreCase("list")) {
            Map<String, String> all = Myau.tagManager.getAll();
            if (all.isEmpty()) {
                ChatUtil.sendFormatted(String.format("%sNo tagged players&r", Myau.clientName));
                return;
            }
            ChatUtil.sendFormatted(String.format("%sTagged players:&r", Myau.clientName));
            for (Map.Entry<String, String> entry : all.entrySet()) {
                ChatUtil.sendRaw(String.format("   §%s%s§r", entry.getValue(), entry.getKey()));
            }
            return;
        }

        // .untag <player> or .tag remove <player>
        if (commandName.equals("untag") || args.get(1).equalsIgnoreCase("remove")) {
            String target = commandName.equals("untag") ? args.get(1) : args.get(2);
            boolean removed = Myau.tagManager.remove(target);
            if (removed) {
                ChatUtil.sendFormatted(String.format("%sRemoved tag for &o%s&r", Myau.clientName, target));
            } else {
                ChatUtil.sendFormatted(String.format("%sNo tag found for &o%s&r", Myau.clientName, target));
            }
            return;
        }

        // .tag <player> [color]
        String player = args.get(1);
        if (args.size() >= 3) {
            String raw = args.get(2).toLowerCase(Locale.ROOT);
            String mapped = COLOR_ALIASES.get(raw);
            if (mapped == null) {
                ChatUtil.sendFormatted(String.format("%sInvalid color: %s&r. Use: 0-9, a-f, r(red), g(green)", Myau.clientName, raw));
                return;
            }
            boolean nowTagged = Myau.tagManager.toggle(player, mapped);
            if (nowTagged) {
                ChatUtil.sendFormatted(String.format("%sTagged &o%s&r with color §%s%s&r", Myau.clientName, player, mapped, mapped));
            } else {
                ChatUtil.sendFormatted(String.format("%sUntagged &o%s&r", Myau.clientName, player));
            }
        } else {
            boolean nowTagged = Myau.tagManager.toggle(player);
            if (nowTagged) {
                ChatUtil.sendFormatted(String.format("%sTagged &o%s&r", Myau.clientName, player));
            } else {
                ChatUtil.sendFormatted(String.format("%sUntagged &o%s&r", Myau.clientName, player));
            }
        }
    }
}
