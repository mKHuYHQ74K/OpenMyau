package myau.management;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;

import java.io.*;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TagManager {
    private final File file = new File(Minecraft.getMinecraft().mcDataDir, "config/Myau/tags.json");
    private final Map<String, String> tags = new ConcurrentHashMap<>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public boolean toggle(String player) {
        if (tags.containsKey(player)) {
            tags.remove(player);
            save();
            return false;
        } else {
            tags.put(player, "7");
            save();
            return true;
        }
    }

    public boolean toggle(String player, String color) {
        if (tags.containsKey(player)) {
            tags.remove(player);
            save();
            return false;
        } else {
            tags.put(player, color);
            save();
            return true;
        }
    }

    public boolean remove(String player) {
        boolean removed = tags.remove(player) != null;
        if (removed) save();
        return removed;
    }

    public boolean isTagged(String player) {
        return tags.containsKey(player);
    }

    public String getColor(String player) {
        return tags.get(player);
    }

    public Map<String, String> getAll() {
        return new LinkedHashMap<>(tags);
    }

    public void save() {
        try {
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            try (Writer writer = new FileWriter(file)) {
                gson.toJson(tags, writer);
            }
        } catch (IOException e) {
            System.err.println("Error saving tags: " + e.getMessage());
        }
    }

    public void load() {
        if (!file.exists()) return;
        try (Reader reader = new FileReader(file)) {
            Type type = new TypeToken<Map<String, String>>() {}.getType();
            Map<String, String> loaded = gson.fromJson(reader, type);
            if (loaded != null) {
                tags.clear();
                tags.putAll(loaded);
            }
        } catch (Exception e) {
            System.err.println("Error loading tags: " + e.getMessage());
        }
    }
}
