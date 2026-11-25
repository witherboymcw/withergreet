package me.minh.withergreet;

import java.util.Locale;

import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerCommandEvent;

public class ServerCommand implements Listener {

    private final WitherGreet plugin;

    public ServerCommand(WitherGreet plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onServerCommand(ServerCommandEvent event) {

        String rawCommand = event.getCommand();

        String commandName = rawCommand.split(" ")[0].toLowerCase(Locale.ROOT);
        commandName = plugin.getCommandAliases().getOrDefault(commandName, commandName);

        CommandSender sender = event.getSender();

        if (plugin.getHelper().isAutoReload()
                && commandName.equals("reload")
                && plugin.getHelper().canRunReload(sender)) {

            plugin.performReload(sender);
        }
    }
}