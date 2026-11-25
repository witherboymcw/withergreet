
package me.minh.withergreet;

import java.util.Locale;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerEvents implements Listener {

    private final WitherGreet plugin;

    public PlayerEvents(WitherGreet plugin) {
        this.plugin = plugin;
    }

    // =========================
    // Player Death Reactions
    // =========================
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {

        var helper = plugin.getHelper();

        if (!helper.isDeathGreetingEnabled()) {
            return;
        }

        Player player = event.getEntity();

        if (helper.isSuppressVanillaDeathMessage()) {
            event.deathMessage(null);
        }

        plugin.broadcastRandomLocalizedMessage(player, "death");
    }

    // =========================
    // Command Reactions
    // =========================

    @EventHandler
    public void onExternalCommand(PlayerCommandPreprocessEvent event) {

        var helper = plugin.getHelper();

        String message = event.getMessage().toLowerCase(Locale.ROOT);

        if (!message.startsWith("/")) return;

        String commandName = message.substring(1).split(" ")[0];
        commandName = plugin.getCommandAliases().getOrDefault(commandName, commandName);

        Player player = event.getPlayer();

        if (helper.isAutoReload()
                && commandName.equals("reload")
                && helper.canRunReload(player)) {

            plugin.performReload(player);
        }

        if (helper.isCommandReactionsEnabled()) {
            plugin.broadcastRandomLocalizedMessage(player, "commands." + commandName);
        }
    }
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {

        var helper = plugin.getHelper();

        if (!helper.isJoinGreetingEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        plugin.broadcastRandomLocalizedMessage(player, "greetings");
    }
}