package me.minh.withergreet;

import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

final class MessageService {

    private final WitherGreet plugin;
    private final Random random;

    MessageService(WitherGreet plugin, Random random) {
        this.plugin = plugin;
        this.random = random;
    }

    void sendRandomLocalizedMessage(Player player, String language, String path) {
        List<String> messages = plugin.getLanguageConfig(language).getStringList(path);
        if (messages.isEmpty()) {
            return;
        }

        player.sendMessage(formatComponent(randomMessage(messages), player));
    }

    void sendFormattedMessage(Player player, String raw) {
        player.sendMessage(formatComponent(raw, player));
    }

    void broadcastRandomLocalizedMessage(Player player, String language, String path) {
        List<String> messages = plugin.getLanguageConfig(language).getStringList(path);
        if (messages.isEmpty()) {
            return;
        }

        Component component = formatComponent(randomMessage(messages), player);
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage(component);
        }
    }

    private Component formatComponent(String raw, Player player) {
        String formatted = raw
                .replace("%player%", player.getName())
                .replace("\\n", "\n");

        return LegacyComponentSerializer
                .legacyAmpersand()
                .deserialize(formatted);
    }

    private String randomMessage(List<String> messages) {
        return messages.get(random.nextInt(messages.size()));
    }
}
