package me.minh.withergreet;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class TabComplete implements TabCompleter {

    private final WitherGreet plugin;

    public TabComplete(WitherGreet plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender,
                                      Command command,
                                      String alias,
                                      String[] args) {

        List<String> suggestions = new ArrayList<>();
        String cmdName = command.getName().toLowerCase(Locale.ROOT);
        boolean isOp = sender.isOp();

        var helper = plugin.getHelper();
        var languageService = plugin.getLanguageService();

        // ---- /withergreet ----
        if (cmdName.equals("withergreet")) {
            if (args.length == 1) {
                suggestions.add("help");
                if (isOp) suggestions.add("reload");
            }
        }

        // ---- /language ----
        if (cmdName.equals("language")) {
            if (args.length == 1) {

                suggestions.addAll(languageService.getLanguageNames());
                suggestions.addAll(List.of("default", "what", "current", "info"));

                if (isOp) {
                    Bukkit.getOnlinePlayers().forEach(p -> suggestions.add(p.getName()));
                    suggestions.addAll(List.of("@p", "@s"));
                    suggestions.add("@a");
                } else {
                    if (helper.isAllowNonOpOwnNameSelector()) {
                        suggestions.add(sender.getName());
                    }
                    if (helper.isAllowNonOpNearestSelector()) {
                        suggestions.add("@p");
                    }
                    if (helper.isAllowNonOpSelfSelector()) {
                        suggestions.add("@s");
                    }
                }

            } else if (args.length == 2) {

                String selector = args[0];

                boolean isSelfSelector = isOp
                        ? selector.equalsIgnoreCase("@s")
                        || selector.equalsIgnoreCase("@p")
                        || selector.equalsIgnoreCase(sender.getName())
                        : plugin.isAllowedNonOpSelector((Player) sender, selector);

                if (isSelfSelector) {
                    suggestions.addAll(languageService.getLanguageNames());

                    if (isOp) {
                        suggestions.addAll(List.of("default", "what", "current", "info", "@s", "@p"));
                        Bukkit.getOnlinePlayers().forEach(p -> suggestions.add(p.getName()));
                        suggestions.add("@a");
                    } else {
                        suggestions.addAll(List.of("default", "what", "current", "info"));

                        if (helper.isAllowNonOpLanguageCopy()) {
                            if (helper.isAllowNonOpSelfSelector()) suggestions.add("@s");
                            if (helper.isAllowNonOpNearestSelector()) suggestions.add("@p");
                            if (helper.isAllowNonOpOwnNameSelector()) suggestions.add(sender.getName());
                        }
                    }

                } else {
                    suggestions.addAll(List.of("what", "current", "info"));
                }
            }
        }

        if (cmdName.equals("tpspawn")) {
            return java.util.Collections.emptyList();
        }

        String lastArg = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);

        return suggestions.stream()
                .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(lastArg))
                .sorted()
                .collect(Collectors.toList());
    }
}