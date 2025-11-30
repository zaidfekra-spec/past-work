package com.example.welcome;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * WelcomeMessagePlugin
 * - Sends styled welcome messages, first-join broadcasts.
 * - /welcome reload|set <key> <value>
 * - Per-world toggles, permissions, and dynamic placeholders.
 */
public class WelcomeMessagePlugin extends JavaPlugin implements Listener, TabExecutor {

    private final Set<UUID> seenPlayers = new HashSet<>();
    private final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("welcome").setExecutor(this);
        getCommand("welcome").setTabCompleter(this);
        getLogger().info("WelcomeMessagePlugin enabled.");
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll((Listener) this);
        getLogger().info("WelcomeMessagePlugin disabled.");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        FileConfiguration cfg = getConfig();
        String worldName = p.getWorld().getName();

        if (!cfg.getBoolean("worlds." + worldName + ".enabled", true)) {
            return; // Respect per-world toggle
        }

        boolean isFirstJoinSession = seenPlayers.add(p.getUniqueId());
        String baseMsg = cfg.getString("messages.join", "&bWelcome &f{player}&b to {world}!");
        String decorated = decorate(baseMsg, p);

        p.sendMessage(color(decorated));

        // Optional broadcast for first joins
        if (isFirstJoinSession && cfg.getBoolean("broadcast-first-join", true)) {
            String firstMsg = cfg.getString("messages.firstJoin", "&aFirst time here, &f{player}&a — have fun!");
            Bukkit.broadcastMessage(color(decorate(firstMsg, p)));
        }

        // Optional OP greeting
        if (p.isOp() && cfg.getBoolean("messages.opGreeting.enabled", true)) {
            String opMsg = cfg.getString("messages.opGreeting.text", "&6Greetings, OP {player}. Server time {time}");
            p.sendMessage(color(decorate(opMsg, p)));
        }
    }

    /**
     * Commands:
     * /welcome reload          -> reload config
     * /welcome set <path> <v>  -> set a config value live
     * /welcome worlds          -> list worlds and toggle state
     */
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("welcome")) return false;

        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "WelcomeMessagePlugin commands: reload | set <path> <value> | worlds");
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload":
                if (!sender.hasPermission("welcome.admin")) {
                    sender.sendMessage(ChatColor.RED + "You lack permission: welcome.admin");
                    return true;
                }
                reloadConfig();
                sender.sendMessage(ChatColor.GREEN + "Config reloaded.");
                return true;

            case "set":
                if (!sender.hasPermission("welcome.admin")) {
                    sender.sendMessage(ChatColor.RED + "You lack permission: welcome.admin");
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /welcome set <path> <value>");
                    return true;
                }
                String path = args[1];
                String value = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                getConfig().set(path, value);
                saveConfig();
                sender.sendMessage(ChatColor.GREEN + "Set " + path + " = " + value);
                return true;

            case "worlds":
                listWorlds(sender);
                return true;

            default:
                sender.sendMessage(ChatColor.RED + "Unknown subcommand: " + args[0]);
                return true;
        }
    }

    private void listWorlds(CommandSender sender) {
        List<String> lines = Bukkit.getWorlds().stream().map(w -> {
            boolean enabled = getConfig().getBoolean("worlds." + w.getName() + ".enabled", true);
            return ChatColor.AQUA + w.getName() + ChatColor.WHITE + " -> " + (enabled ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled");
        }).collect(Collectors.toList());

        if (lines.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "No worlds found.");
            return;
        }

        sender.sendMessage(ChatColor.YELLOW + "Worlds:");
        lines.forEach(sender::sendMessage);
    }

    private String decorate(String msg, Player p) {
        String worldName = p.getWorld().getName();
        return msg
                .replace("{player}", p.getName())
                .replace("{world}", worldName)
                .replace("{online}", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("{time}", LocalDateTime.now().format(timeFmt));
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("welcome")) return Collections.emptyList();
        if (args.length == 1) {
            return Arrays.asList("reload", "set", "worlds");
        }
        if (args.length == 2 && "set".equalsIgnoreCase(args[0])) {
            return Arrays.asList("messages.join", "messages.firstJoin", "messages.opGreeting.text", "broadcast-first-join");
        }
        return Collections.emptyList();
    }
}
