package myau.management;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NickManager {
    private final File file = new File(Minecraft.getMinecraft().mcDataDir, "config/Myau/nicknames.json");
    private final Map<String, String> nickMap = new ConcurrentHashMap<>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private volatile List<Map.Entry<String, String>> sortedCache = Collections.emptyList();
    private volatile Map<String, Pattern> patternCache = Collections.emptyMap();
    private volatile boolean dirty = false;

    public void setNick(String original, String nick) {
        nickMap.put(original, nick);
        dirty = true;
        save();
    }

    public String removeByNick(String nick) {
        Iterator<Map.Entry<String, String>> it = nickMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> entry = it.next();
            if (entry.getValue().equalsIgnoreCase(nick)) {
                it.remove();
                dirty = true;
                save();
                return entry.getKey();
            }
        }
        return null;
    }

    public String removeByOriginal(String original) {
        String removed = nickMap.remove(original);
        if (removed != null) {
            dirty = true;
            save();
        }
        return removed;
    }

    public String getNick(String original) {
        return nickMap.get(original);
    }

    public String getOriginal(String nick) {
        for (Map.Entry<String, String> entry : nickMap.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(nick)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public Map<String, String> getAll() {
        return new LinkedHashMap<>(nickMap);
    }

    /**
     * Replaces all nicknamed player names in the given text with their nicknames.
     * Player names are matched as whole words (not substrings) and are handled
     * regardless of surrounding Minecraft color codes.
     */
    public String replaceAll(String text) {
        if (text == null || text.isEmpty() || nickMap.isEmpty()) return text;

        if (dirty) {
            rebuildCache();
        }

        String result = text;
        for (Map.Entry<String, String> entry : sortedCache) {
            result = patternCache.get(entry.getKey())
                    .matcher(result)
                    .replaceAll(Matcher.quoteReplacement(entry.getValue()));
        }
        return result;
    }

    private void rebuildCache() {
        List<Map.Entry<String, String>> sorted = new ArrayList<>(nickMap.entrySet());
        sorted.sort((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()));

        Map<String, Pattern> patterns = new HashMap<>();
        for (Map.Entry<String, String> entry : sorted) {
            // Match the name as a whole word, handling Minecraft color codes:
            // (?<=^)             - start of string
            // (?<=[^a-zA-Z0-9_]) - preceded by non-word char
            // (?<=§[a-zA-Z0-9]) - preceded by a color code (§ + letter/digit)
            // (?=$|[^a-zA-Z0-9_]) - followed by end of string or non-word char
            String regex = "(?i)(?:(?<=^)|(?<=[^a-zA-Z0-9_])|(?<=§[a-zA-Z0-9]))"
                    + Pattern.quote(entry.getKey())
                    + "(?=$|[^a-zA-Z0-9_])";
            patterns.put(entry.getKey(), Pattern.compile(regex));
        }

        sortedCache = sorted;
        patternCache = patterns;
        dirty = false;
    }

    public void save() {
        try {
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            try (Writer writer = new FileWriter(file)) {
                gson.toJson(nickMap, writer);
            }
        } catch (IOException e) {
            System.err.println("Error saving nicknames: " + e.getMessage());
        }
    }

    public void load() {
        if (!file.exists()) return;
        try (Reader reader = new FileReader(file)) {
            Type type = new TypeToken<Map<String, String>>() {}.getType();
            Map<String, String> loaded = gson.fromJson(reader, type);
            if (loaded != null) {
                nickMap.clear();
                nickMap.putAll(loaded);
                dirty = true;
            }
        } catch (IOException e) {
            System.err.println("Error loading nicknames: " + e.getMessage());
        }
    }
}
