package me.minh.withergreet;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

public final class LanguageService {

    private final WitherGreet plugin;
    private final NamespacedKey languageKey;
    private final Map<String, FileConfiguration> languages = new HashMap<>();

    LanguageService(WitherGreet plugin) {
        this.plugin = plugin;
        this.languageKey = new NamespacedKey(plugin, "language");
    }

    void reloadLanguages(String defaultLanguage) {
        copyDefaultLanguages();
        languages.clear();
        loadLanguages();

        if (!languages.containsKey(defaultLanguage)) {
            throw new IllegalStateException("Default language not loaded: " + defaultLanguage);
        }
    }

    boolean hasLanguage(String language) {
        return languages.containsKey(language);
    }

    Collection<String> getLanguageNames() {
        return languages.keySet();
    }

    String getPlayerLanguage(Player player, String defaultLanguage) {
        String language = player.getPersistentDataContainer()
                .get(languageKey, PersistentDataType.STRING);

        if (language != null && languages.containsKey(language)) {
            return language;
        }

        return defaultLanguage;
    }

    void setPlayerLanguage(Player player, String language) {
        player.getPersistentDataContainer().set(languageKey, PersistentDataType.STRING, language);
    }

    FileConfiguration getLanguageConfig(String language, String defaultLanguage) {
        FileConfiguration config = languages.get(language);
        if (config != null) {
            return config;
        }

        FileConfiguration fallback = languages.get(defaultLanguage);
        return Objects.requireNonNull(fallback, "Default language not loaded: " + defaultLanguage);
    }

    private void copyDefaultLanguages() {
        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }

        try (java.util.jar.JarFile jarFile = new java.util.jar.JarFile(plugin.getPluginJar())) {
            var entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                String name = entry.getName();

                if (!name.startsWith("lang/") || !name.endsWith(".yml")) {
                    continue;
                }

                File file = new File(plugin.getDataFolder(), name);
                if (!file.exists()) {
                    plugin.saveResource(name, false);
                }
            }
        } catch (IOException | SecurityException ignored) {
        }
    }

    private void loadLanguages() {
        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }

        File[] files = langFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return;
        }

        for (File file : files) {
            String name = file.getName().replace(".yml", "").toLowerCase(Locale.ROOT);
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            languages.put(name, config);
        }
    }
}
