package me.minh.withergreet;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class WitherGreet extends JavaPlugin {

    private final Map<String, String> commandAliases = new HashMap<>();
    private LanguageService languageService;
    private MessageService messageService;
    private Helper helper;
    private CommandHandler commandHandler;

    // =========================
    // Plugin Startup
    // =========================

    @Override
    public void onEnable() {

        saveDefaultConfig();
        helper = new Helper(this, commandAliases);
        helper.loadSettingsFromConfig();
        languageService = new LanguageService(this);
        messageService = new MessageService(this, new Random());
        helper.setServices(languageService, messageService);
        commandHandler = new CommandHandler(this, helper, messageService);

        helper.reloadRuntimeData();

        Bukkit.getPluginManager().registerEvents(new PlayerEvents(this), this);
        Bukkit.getPluginManager().registerEvents(new ServerCommand(this), this);
        Bukkit.getPluginManager().registerEvents(new BlockRecycle(this), this);

        TabComplete tabComplete = new TabComplete(this);

        Objects.requireNonNull(getCommand("greet")).setExecutor(this);
        Objects.requireNonNull(getCommand("hi")).setExecutor(this);
        Objects.requireNonNull(getCommand("hello")).setExecutor(this);
        Objects.requireNonNull(getCommand("withergreet")).setExecutor(this);
        Objects.requireNonNull(getCommand("language")).setExecutor(this);
        Objects.requireNonNull(getCommand("tpspawn")).setExecutor(this);

        Objects.requireNonNull(getCommand("withergreet")).setTabCompleter(tabComplete);
        Objects.requireNonNull(getCommand("language")).setTabCompleter(tabComplete);
        Objects.requireNonNull(getCommand("tpspawn")).setTabCompleter(tabComplete);

        getLogger().info(helper.getEnableMessage());
    }

    // =========================
    // Commands
    // =========================

    public void performReload(org.bukkit.command.CommandSender sender) {
        reloadConfig();
        helper.loadSettingsFromConfig();
        helper.reloadRuntimeData();

        sender.sendMessage(helper.getReloadMessage());
    }   

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return commandHandler.handle(sender, command, label, args);
    }

    // =========================
    // Utility Methods
    // =========================

    public LanguageService getLanguageService() {
        return languageService;
    }

    public Map<String, String> getCommandAliases() {
        return commandAliases;
    }

    public Helper getHelper() {
        return helper;
    }

    boolean isAllowedNonOpSelector(Player player, String selector) {
        return helper.isAllowedNonOpSelector(player, selector);
    }

    public void broadcastRandomLocalizedMessage(Player player, String path) {
        helper.broadcastRandomLocalizedMessage(player, path);
    }

    org.bukkit.configuration.file.FileConfiguration getLanguageConfig(String language) {
        return helper.getLanguageConfig(language);
    }

    File getPluginJar() {
        return getFile();
    }

    // =========================
    // Shutdown
    // =========================

    @Override
    public void onDisable() {
        getLogger().info(helper.getDisableMessage());
    }
}
