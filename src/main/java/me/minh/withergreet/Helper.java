package me.minh.withergreet;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public final class Helper {

    private final WitherGreet plugin;
    private final Map<String, String> commandAliases;
    private LanguageService languageService;
    private MessageService messageService;
    private String defaultLanguage;
    private FileConfiguration defaultLangConfig;
    private String reloadMessage;
    private String enableMessage;
    private String disableMessage;
    private java.util.List<String> helpList;
    private String languageUsage;
    private boolean autoReload;
    private boolean joinGreetingEnabled;
    private boolean deathGreetingEnabled;
    private boolean suppressVanillaDeathMessage;
    private boolean commandReactionsEnabled;
    private List<String> greetCommandNames;
    private boolean allowNonOpSelfSelector;
    private boolean allowNonOpNearestSelector;
    private boolean allowNonOpOwnNameSelector;
    private boolean allowNonOpLanguageCopy;
    private boolean debug;

    Helper(WitherGreet plugin, Map<String, String> commandAliases) {
        this.plugin = plugin;
        this.commandAliases = commandAliases;
    }

    void setServices(LanguageService languageService, MessageService messageService) {
        this.languageService = java.util.Objects.requireNonNull(languageService);
        this.messageService = java.util.Objects.requireNonNull(messageService);
    }

    void loadSettingsFromConfig() {
        FileConfiguration cfg = plugin.getConfig();
        this.defaultLanguage = Objects.requireNonNullElse(cfg.getString("defaultLanguage"), "english");
        this.defaultLangConfig = null; // reset cache
        this.reloadMessage = Objects.requireNonNullElse(cfg.getString("reloadMessage"), "WitherGreet reloaded.");
        this.enableMessage = Objects.requireNonNullElse(cfg.getString("enableMessage"), "WitherGreet enabled.");
        this.disableMessage = Objects.requireNonNullElse(cfg.getString("disableMessage"), "WitherGreet disabled.");
        this.helpList = cfg.getStringList("helpList");
        if (this.helpList == null || this.helpList.isEmpty()) {
            this.helpList = java.util.List.of(
                "/hi, /hello, /greet, /withergreet",
                "/tpspawn",
                "/language <language | default | what | current | info>",
                "/withergreet <reload | help>"
            );
        }
        this.languageUsage = Objects.requireNonNullElse(
                cfg.getString("languageUsage"),
                "Usage: /language <language>\n/language <player> <language>"
        );
        this.autoReload = cfg.getBoolean                    ("autoReload", true);
        this.joinGreetingEnabled = cfg.getBoolean           ("joinGreetingEnabled", true);
        this.deathGreetingEnabled = cfg.getBoolean          ("deathGreetingEnabled", true);
        this.suppressVanillaDeathMessage = cfg.getBoolean   ("suppressVanillaDeathMessage", true);
        this.commandReactionsEnabled = cfg.getBoolean       ("commandReactionsEnabled", true);
        this.greetCommandNames = cfg.getStringList          ("greetCommandNames").stream()
                .map(name -> name.toLowerCase(Locale.ROOT))
                .filter(name -> !name.isBlank())
                .toList();
        if (this.greetCommandNames.isEmpty()) {
            this.greetCommandNames = List.of("greet", "hi", "hello");
        }
        this.allowNonOpSelfSelector = cfg.getBoolean        ("allowNonOpSelfSelector", true);
        this.allowNonOpNearestSelector = cfg.getBoolean     ("allowNonOpNearestSelector", true);
        this.allowNonOpOwnNameSelector = cfg.getBoolean     ("allowNonOpOwnNameSelector", true);
        this.allowNonOpLanguageCopy = cfg.getBoolean        ("allowNonOpLanguageCopy", true);
        this.debug = cfg.getBoolean("debug", false);
    }

    boolean canRunReload(CommandSender sender) {
        return sender.isOp()
                || sender.hasPermission("bukkit.command.reload")
                || sender.hasPermission("minecraft.command.reload");
    }

    void reloadRuntimeData() {
        languageService.reloadLanguages(defaultLanguage);
        this.defaultLangConfig = null;
        loadCommandAliases();
    }

    void sendRandomLocalizedMessage(Player player, String path) {
        messageService.sendRandomLocalizedMessage(player, getPlayerLanguage(player), path);
    }


    void sendMessage(Player player, String path, String fallback) {
        sendMessage(player, getPlayerLanguage(player), path, fallback, Map.of());
    }

    void sendMessage(Player player, String path, String fallback, Map<String, String> replacements) {
        sendMessage(player, getPlayerLanguage(player), path, fallback, replacements);
    }

    void sendMessage(Player player, String language, String path, String fallback, Map<String, String> replacements) {
        String message = applyReplacements(getMessage(language, path, fallback), replacements);
        messageService.sendFormattedMessage(player, message);
    }

    String getMessage(String language, String path, String fallback) {
        FileConfiguration langCfg = getLanguageConfig(language);
        String message = langCfg.getString(path);
        if (message != null && !message.isBlank()) {
            return message;
        }

        if (!language.equalsIgnoreCase(defaultLanguage)) {
            FileConfiguration defCfg = getLanguageConfig(defaultLanguage);
            String def = defCfg.getString(path);
            if (def != null && !def.isBlank()) {
                return def;
            }
        }

        return fallback;
    }

    void broadcastRandomLocalizedMessage(Player player, String path) {
        messageService.broadcastRandomLocalizedMessage(player, getPlayerLanguage(player), path);
    }

    String getPlayerLanguage(Player player) {
        return languageService.getPlayerLanguage(player, defaultLanguage);
    }

    FileConfiguration getLanguageConfig(String language) {
        if (language.equalsIgnoreCase(defaultLanguage)) {
            if (defaultLangConfig == null) {
                defaultLangConfig = languageService.getLanguageConfig(defaultLanguage, defaultLanguage);
            }
            return defaultLangConfig;
        }
        return languageService.getLanguageConfig(language, defaultLanguage);
    }

    String getDefaultLanguage() {
        return defaultLanguage;
    }

    String getReloadMessage() {
        return reloadMessage;
    }

    String getEnableMessage() {
        return enableMessage;
    }

    String getDisableMessage() {
        return disableMessage;
    }

    java.util.List<String> getHelpList() {
        return helpList;
    }

    String getLanguageUsage() {
        return languageUsage;
    }

    boolean isAutoReload() {
        return autoReload;
    }

    boolean isJoinGreetingEnabled() {
        return joinGreetingEnabled;
    }

    boolean isDeathGreetingEnabled() {
        return deathGreetingEnabled;
    }

    boolean isSuppressVanillaDeathMessage() {
        return suppressVanillaDeathMessage;
    }

    boolean isCommandReactionsEnabled() {
        return commandReactionsEnabled;
    }

    boolean isAllowNonOpSelfSelector() {
        return allowNonOpSelfSelector;
    }

    boolean isAllowNonOpNearestSelector() {
        return allowNonOpNearestSelector;
    }

    boolean isAllowNonOpOwnNameSelector() {
        return allowNonOpOwnNameSelector;
    }

    boolean isAllowNonOpLanguageCopy() {
        return allowNonOpLanguageCopy;
    }

    boolean isGreetCommand(String cmdName) {
        return greetCommandNames.contains(cmdName.toLowerCase(Locale.ROOT));
    }

    boolean isAllowedNonOpSelector(Player player, String selector) {
        return (allowNonOpSelfSelector && selector.equalsIgnoreCase("@s"))
                || (allowNonOpNearestSelector && selector.equalsIgnoreCase("@p"))
                || (allowNonOpOwnNameSelector && selector.equalsIgnoreCase(player.getName()));
    }

    void sendNonOpMessage(Player player) {
        sendRandomLocalizedMessage(player, "nonop");
    }

    boolean validateNonOpLanguageArgs(Player player, String[] args) {
        if (player.isOp()) {
            return true;
        }

        String first = args[0];
        String second = args[1].toLowerCase(Locale.ROOT);

        boolean validFirst = isAllowedNonOpSelector(player, first);
        boolean validSecond = isDefaultLanguageKeyword(second)
                || languageService.hasLanguage(second)
                || (allowNonOpLanguageCopy && isLanguageCopySource(args[1]));

        if (!validFirst || !validSecond) {
            sendNonOpMessage(player);
            return false;
        }

        return true;
    }

    boolean isLanguageInfoQuery(String selected) {
        return selected.equals("what") || selected.equals("current") || selected.equals("info");
    }

    boolean isLanguageCopySource(String arg) {
        return arg.equalsIgnoreCase("@s")
                || arg.equalsIgnoreCase("@p")
                || plugin.getServer().getPlayer(arg) != null;
    }

    private boolean isDefaultLanguageKeyword(String selected) {
        return selected.equals("default");
    }

    String resolveSelectedLanguage(String selected) {
        if (isDefaultLanguageKeyword(selected)) {
            return defaultLanguage;
        }
        if (languageService.hasLanguage(selected)) {
            return selected;
        }
        return null;
    }

    Player resolvePlayerSelector(Player player, String selector) {
        if (selector.equalsIgnoreCase("@s")) {
            return player;
        }
        if (selector.equalsIgnoreCase("@p")) {
            return findNearestPlayer(player);
        }
        return plugin.getServer().getPlayer(selector);
    }

    Player resolveTargetPlayer(Player player, String selector) {
        Player target = resolvePlayerSelector(player, selector);
        if (target != null) {
            return target;
        }

        if (selector.equalsIgnoreCase("@p")) {
            sendMessage(player, "systemMessages.noPlayerFound", "No player found.");
        } else {
            sendMessage(player, "systemMessages.playerNotFound", "Player not found.");
        }
        return null;
    }

    void setPlayerLanguage(Player target, String language) {
        languageService.setPlayerLanguage(target, language);
    }

    void applyLanguageChange(Player actor, Player target, String language) {
        setPlayerLanguage(target, language);

        if (target.equals(actor)) {
            sendMessage(actor, "systemMessages.languageChangedSelf", "Language changed to %language%.",
                    Map.of("%language%", language));
            return;
        }

        sendMessage(actor, "systemMessages.languageChangedOther",
                "Changed language of %target% to %language%.",
                Map.of("%target%", target.getName(), "%language%", language));
        sendMessage(target, "systemMessages.languageChangedByOther",
                "Your language was changed to %language%.",
                Map.of("%language%", language));
    }

    void notifyLanguageCopied(Player actor, Player target, Player source, String language) {
        sendMessage(actor, "systemMessages.languageCopied",
                "Copied language from %source% (%language%) to %target%.",
                Map.of("%source%", source.getName(), "%language%", language, "%target%", target.getName()));

        if (!target.equals(actor)) {
            sendMessage(target, "systemMessages.languageCopiedToYou",
                    "Your language was set to %language% (copied from %source%).",
                    Map.of("%language%", language, "%source%", source.getName()));
        }
    }

    private Player findNearestPlayer(Player player) {
        var playerLocation = player.getLocation();
        if (playerLocation == null) {
            return null;
        }

        Player nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Player online : plugin.getServer().getOnlinePlayers()) {
            var onlineLocation = online.getLocation();
            if (onlineLocation == null) {
                continue;
            }

            double distance = onlineLocation.distanceSquared(playerLocation);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = online;
            }
        }

        return nearest;
    }

    private String applyReplacements(String message, Map<String, String> replacements) {
        if (message == null || message.isEmpty() || replacements.isEmpty()) {
            return message;
        }
        String resolved = message;
        for (var entry : replacements.entrySet()) {
            String k = entry.getKey();
            String v = entry.getValue();
            if (k == null || k.isEmpty()) continue;
            if (v == null) v = "";
            resolved = resolved.replace(k, v);
        }
        return resolved;
    }

    private void loadCommandAliases() {
        File file = new File(plugin.getDataFolder(), "hashmap.yml");
        if (!file.exists()) {
            plugin.saveResource("hashmap.yml", false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        commandAliases.clear();

        var section = config.getConfigurationSection("aliases");
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            String value = section.getString(key);
            if (value == null) {
                continue;
            }

            commandAliases.put(
                    key.toLowerCase(Locale.ROOT),
                    value.toLowerCase(Locale.ROOT)
            );
        }

        if (debug) {
            plugin.getLogger().info(() -> "Loaded " + commandAliases.size() + " command aliases.");
        }
    }
}
