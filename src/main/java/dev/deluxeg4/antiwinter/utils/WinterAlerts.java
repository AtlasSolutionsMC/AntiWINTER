package dev.deluxeg4.antiwinter.utils;

import dev.deluxeg4.antiwinter.AntiWinterPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class WinterAlerts {
    private final AntiWinterPlugin plugin;

    public WinterAlerts(AntiWinterPlugin plugin) {
        this.plugin = plugin;
    }

    public void send(String message) {
        plugin.getLogger().info(message);
        for (Player staff : Bukkit.getOnlinePlayers()) {
            staff.getScheduler().execute(plugin, () -> {
                if (staff.hasPermission("antiwinter.alert")) staff.sendMessage(Component.text(message));
            }, null, 1L);
        }
    }
}
