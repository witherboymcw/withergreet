
package me.minh.withergreet;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandHandler {

    private final WitherGreet plugin;
    private final Helper helper;
    private final MessageService messageService;

    CommandHandler(WitherGreet plugin, Helper helper, MessageService messageService) {
        this.plugin = plugin;
        this.helper = helper;
        this.messageService = messageService;
    }

    public boolean handle(CommandSender sender, Command command, String label, String[] args) {
        String cmdName = command.getName().toLowerCase(Locale.ROOT);

        if (!(sender instanceof Player player)) {
            return handleNonPlayer(sender, cmdName, args);
        }

        return handlePlayer(player, cmdName, args);
    }

    // =========================
    // Non-player
    // =========================

    private boolean handleNonPlayer(CommandSender sender, String cmdName, String[] args) {
        if (helper.isGreetCommand(cmdName)) {
            return handleConsoleGreetCommand(sender);
        }

        if (cmdName.equals("withergreet") && isReloadArg(args)) {
            plugin.performReload(sender);
            return true;
        }

        sender.sendMessage(helper.getMessage(
                helper.getDefaultLanguage(),
                "systemMessages.playersOnly",
                "This command is only for players."
        ));
        return true;
    }

    // =========================
    // Player
    // =========================

    private boolean handlePlayer(Player player, String cmdName, String[] args) {
        return switch (cmdName) {
            case "tpspawn" -> handleTpSpawn(player);
            case "withergreet" -> handleWitherGreet(player, args);
            case "language" -> handleLanguage(player, args);
            default -> {
                if (helper.isGreetCommand(cmdName)) {
                    plugin.broadcastRandomLocalizedMessage(player, "greetings");
                    yield true;
                }
                yield false;
            }
        };
    }

    // =========================
    // WitherGreet command
    // =========================

    private boolean handleWitherGreet(Player player, String[] args) {
        String language = helper.getPlayerLanguage(player);

        if (isReloadArg(args)) {
            if (!helper.canRunReload(player)) {
                helper.sendNonOpMessage(player);
                return true;
            }
            plugin.performReload(player);
            return true;
        }

        if (isHelpArg(args)) {
            helper.sendRandomLocalizedMessage(player, "help");
            helper.sendMessage(player, language, "systemMessages.helpList",
                    String.join("\n", helper.getHelpList()), Map.of());
            return true;
        }

        plugin.broadcastRandomLocalizedMessage(player, "greetings");
        return true;
    }

    // =========================
    // Language command
    // =========================

    private boolean handleLanguage(Player player, String[] args) {
        switch (args.length) {
            case 1 -> {
                String selected = args[0].toLowerCase(Locale.ROOT);
                return handleLanguageSelection(player, player, selected);
            }
            case 2 -> {
                if (!helper.validateNonOpLanguageArgs(player, args)) return true;

                String selector = args[0];
                String selected = args[1].toLowerCase(Locale.ROOT);

                if (helper.isLanguageCopySource(args[1])) {
                    return handleLanguageCopy(player, selector, args[1]);
                }

                if (selector.equalsIgnoreCase("@a")) {
                    return handleSetLanguageForAll(player, selected);
                }

                Player target = helper.resolveTargetPlayer(player, selector);
                if (target == null) return true;

                return handleLanguageSelection(player, target, selected);
            }
            default -> {
                messageService.sendFormattedMessage(player, helper.getLanguageUsage());
                return true;
            }
        }
    }

    private boolean handleLanguageSelection(Player actor, Player target, String selected) {
        if (helper.isLanguageInfoQuery(selected)) {
            String current = helper.getPlayerLanguage(target);

            if (target.equals(actor)) {
                helper.sendMessage(actor, "systemMessages.languageCurrentSelf",
                        "Your language is %language%.",
                        Map.of("%language%", current));
            } else {
                helper.sendMessage(actor, "systemMessages.languageCurrentOther",
                        "%target%'s language is %language%.",
                        Map.of("%target%", target.getName(), "%language%", current));
            }
            return true;
        }

        String resolved = helper.resolveSelectedLanguage(selected);
        if (resolved == null) {
            helper.sendMessage(actor, "systemMessages.languageUnavailable", "Language not available.");
            return true;
        }

        helper.applyLanguageChange(actor, target, resolved);
        return true;
    }

    private boolean handleLanguageCopy(Player player, String selector, String sourceSelector) {
        Player source = helper.resolvePlayerSelector(player, sourceSelector);
        if (source == null) {
            helper.sendMessage(player, "systemMessages.sourcePlayerNotFound", "Source player not found.");
            return true;
        }

        String copied = helper.getPlayerLanguage(source);

        if (selector.equalsIgnoreCase("@a")) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                helper.setPlayerLanguage(online, copied);
            }

            helper.sendMessage(player, "systemMessages.languageCopiedAll",
                    "All players now use %source%'s language (%language%).",
                    Map.of("%source%", source.getName(), "%language%", copied));
            return true;
        }

        Player target = helper.resolveTargetPlayer(player, selector);
        if (target == null) return true;

        helper.setPlayerLanguage(target, copied);
        helper.notifyLanguageCopied(player, target, source, copied);
        return true;
    }

    private boolean handleSetLanguageForAll(Player player, String selected) {
        String resolved = helper.resolveSelectedLanguage(selected);
        if (resolved == null) {
            helper.sendMessage(player, "systemMessages.languageUnavailable", "Language not available.");
            return true;
        }

        for (Player online : Bukkit.getOnlinePlayers()) {
            helper.setPlayerLanguage(online, resolved);
            helper.sendMessage(online, "systemMessages.languageChangedSelf",
                    "Your language was changed to %language%.",
                    Map.of("%language%", resolved));
        }

        helper.sendMessage(player, "systemMessages.languageChangedAll",
                "Changed language of all players to %language%.",
                Map.of("%language%", resolved));
        return true;
    }

    // =========================
    // Other
    // =========================

    private boolean handleConsoleGreetCommand(CommandSender sender) {
        List<String> commands = plugin.getConfig().getStringList("consoleGreetCommands");
        for (String cmd : commands) {
            if (cmd == null || cmd.isBlank()) continue;
            Bukkit.dispatchCommand(sender, cmd);
        }
        return true;
    }

    private boolean handleTpSpawn(Player player) {
        double x = plugin.getConfig().getDouble("coordinateSpawnX", 0.0);
        double y = plugin.getConfig().getDouble("coordinateSpawnY", 64.0);
        double z = plugin.getConfig().getDouble("coordinateSpawnZ", 0.0);
        String worldName = Objects.requireNonNullElse(plugin.getConfig().getString("coordinateSpawnWorld"), "world");

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            helper.sendMessage(player, "systemMessages.worldMissing",
                    "World '%world%' does not exist!",
                    Map.of("%world%", worldName));
            return true;
        }

        player.teleport(new Location(world, x, y, z));
        return true;
    }

    private boolean isReloadArg(String[] args) {
        return args.length > 0 && args[0].equalsIgnoreCase("reload");
    }

    private boolean isHelpArg(String[] args) {
        return args.length > 0 && args[0].equalsIgnoreCase("help");
    }
}
