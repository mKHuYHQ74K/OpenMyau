package myau.command.commands;

import myau.Myau;
import myau.command.Command;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class RemoveConfigCommand extends Command {
    private static final File CONFIG_ROOT_DIR;

    static {
        File mcDir = Minecraft.getMinecraft().mcDataDir;
        CONFIG_ROOT_DIR = new File(new File(mcDir, "config"), "myau");

        if (!CONFIG_ROOT_DIR.exists()) {
            CONFIG_ROOT_DIR.mkdirs();
        }
    }

    public RemoveConfigCommand() {
        super(new ArrayList<>(Arrays.asList("rm", "remove")));
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        ArrayList<String> realArgs = new ArrayList<>();
        if (args.size() > 1) {
            realArgs = new ArrayList<>(args.subList(1, args.size()));
        }

        if (realArgs.isEmpty()) {
            sendError("Usage error! Correct format: .rm <config name> (e.g. .rm 123)");
            listAllConfigs();
            return;
        }

        String configName = realArgs.get(0);
        File configFile = null;

        try {
            String configFileName = configName.endsWith(".json") ? configName : configName + ".json";
            configFile = new File(CONFIG_ROOT_DIR, configFileName);

            if (configFile == null || !configFile.exists()) {
                sendError("Config file not found: " + configName);
                listAllConfigs();
                return;
            }

            boolean deleted = deleteFileOrDir(configFile);
            if (deleted) {
                sendSuccess("Successfully deleted config: " + configName);
            } else {
                sendError("Failed to delete config: " + configName + " (file in use/permission denied)");
            }
        } catch (Exception e) {
            sendError("Delete failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean deleteFileOrDir(File file) {
        if (file == null) return false;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteFileOrDir(child);
                }
            }
        }
        return file.delete();
    }

    private void listAllConfigs() {
        sendChatMessage(Myau.clientName + "§7Configs:§r");

        File[] configFiles = CONFIG_ROOT_DIR.listFiles((dir, name) -> name != null && name.endsWith(".json"));

        if (configFiles == null || configFiles.length == 0) {
            sendChatMessage("§7»§r §oNo config files§r");
            System.out.println("Current config directory: " + CONFIG_ROOT_DIR.getAbsolutePath());
            return;
        }

        for (File file : configFiles) {
            sendChatMessage("§7»§r §o" + file.getName() + "§r");
        }
    }

    private void sendSuccess(String msg) {
        sendChatMessage(Myau.clientName + "§a " + msg);
    }

    private void sendError(String msg) {
        sendChatMessage(Myau.clientName + "§c " + msg);
    }

    private void sendChatMessage(String message) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) {
            String formattedMsg = message.replace("&", "§");
            mc.thePlayer.addChatMessage(new ChatComponentText(formattedMsg));
        }
    }
}