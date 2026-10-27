package dev.deluxeg4.antiwinter.commands;

import dev.deluxeg4.antiwinter.detection.ChannelFingerprints;
import dev.deluxeg4.antiwinter.detection.DetectionService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

public final class WinterChannelsCommand implements CommandExecutor {
    private final ChannelFingerprints fingerprints;

    public WinterChannelsCommand(DetectionService detections) {
        this.fingerprints = detections.channelFingerprints();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("antiwinter.admin")) {
            sender.sendMessage(Component.text("You do not have permission to use this command."));
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(Component.text("/winterchannels list | show <player> | capture <name> <player> | compare <name> <player>"));
            return true;
        }

        switch (args[0].toLowerCase(java.util.Locale.ROOT)) {
            case "list" -> sender.sendMessage(Component.text("Channel baselines: " + String.join(", ", fingerprints.baselineNames())));
            case "show" -> {
                if (args.length != 2) return usage(sender);
                Player player = onlinePlayer(args[1]);
                if (player == null) return notOnline(sender);
                Set<String> channels = new TreeSet<>(fingerprints.channels(player.getUniqueId()));
                sender.sendMessage(Component.text(player.getName() + " announced " + channels.size() + " channel(s): " + String.join(", ", channels)));
            }
            case "capture" -> {
                if (args.length != 3) return usage(sender);
                Player player = onlinePlayer(args[2]);
                if (player == null) return notOnline(sender);
                try {
                    fingerprints.capture(args[1], player.getUniqueId());
                    sender.sendMessage(Component.text("Saved baseline '" + args[1] + "' from " + player.getName() + "."));
                } catch (IOException | IllegalArgumentException exception) {
                    sender.sendMessage(Component.text("Could not save baseline: " + exception.getMessage()));
                }
            }
            case "compare" -> {
                if (args.length != 3) return usage(sender);
                Player player = onlinePlayer(args[2]);
                if (player == null) return notOnline(sender);
                try {
                    ChannelFingerprints.Comparison comparison = fingerprints.compare(args[1], player.getUniqueId());
                    if (comparison == null) {
                        sender.sendMessage(Component.text("No baseline named '" + args[1] + "'."));
                        return true;
                    }
                    sender.sendMessage(Component.text("'" + player.getName() + "' vs '" + args[1] + "': "
                            + (comparison.identical() ? "exact match" : "different")
                            + "; missing=" + comparison.missing() + "; extra=" + comparison.extra()));
                } catch (IllegalArgumentException exception) {
                    sender.sendMessage(Component.text(exception.getMessage()));
                }
            }
            default -> usage(sender);
        }
        return true;
    }

    private static Player onlinePlayer(String name) {
        return Bukkit.getPlayerExact(name);
    }

    private static boolean notOnline(CommandSender sender) {
        sender.sendMessage(Component.text("That player is not online."));
        return true;
    }

    private static boolean usage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage: /winterchannels list | show <player> | capture <name> <player> | compare <name> <player>"));
        return true;
    }
}
