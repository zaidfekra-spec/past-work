package com.example.database;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Statistic;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * DatabaseExamplePlugin
 * - Demonstrates async MySQL I/O with CompletableFuture.
 * - /db set <player> <kills> | /db get <player> | /db synconline
 * - Creates table if missing; caches results; mirrors from vanilla stats.
 */
public class DatabaseExamplePlugin extends JavaPlugin implements TabExecutor {

    private Connection conn;
    private final Map<String, Integer> cache = new HashMap<>();

    @Override
    public void onEnable() {
        getCommand("db").setExecutor(this);
        getCommand("db").setTabCompleter(this);

        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/mcdata?useSSL=false&serverTimezone=UTC", "user", "pass");
            try (PreparedStatement ps = conn.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS kills (player VARCHAR(36) PRIMARY KEY, count INT NOT NULL)")) {
                ps.executeUpdate();
            }
            getLogger().info("MySQL connected.");
        } catch (SQLException e) {
            getLogger().warning("MySQL not available: " + e.getMessage());
            conn = null;
        }
    }

    @Override
    public void onDisable() {
        if (conn != null) {
            try { conn.close(); } catch (SQLException ignored) {}
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("db")) return false;
        if (args.length < 1) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /db set|get|synconline");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "set":
                return handleSet(sender, args);
            case "get":
                return handleGet(sender, args);
            case "synconline":
                return handleSyncOnline(sender);
            default:
                sender.sendMessage(ChatColor.RED + "Unknown subcommand.");
                return true;
        }
    }

    private boolean handleSet(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /db set <player> <kills>");
            return true;
        }
        String player = args[1];
        int kills;
        try {
            kills = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Kills must be a number.");
            return true;
        }
        cache.put(player, kills);
        saveAsync(player, kills).thenRun(() ->
                sender.sendMessage(ChatColor.GREEN + "Set and saved " + player + " = " + kills));
        return true;
    }

    private boolean handleGet(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /db get <player>");
            return true;
        }
        String player = args[1];

        // Prefer cache; fallback to DB
        Integer cached = cache.get(player);
        if (cached != null) {
            sender.sendMessage(ChatColor.YELLOW + "[Cache] " + player + " kills: " + cached);
            return true;
        }

        getAsync(player).thenAccept(k -> {
            sender.sendMessage(ChatColor.AQUA + "[DB] " + player + " kills: " + k);
            cache.put(player, k);
        });
        return true;
    }

    private boolean handleSyncOnline(CommandSender sender) {
        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (online.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "No players online.");
            return true;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : online) {
                    int vanilla = p.getStatistic(Statistic.PLAYER_KILLS);
                    cache.put(p.getName(), vanilla);
                    save(p.getName(), vanilla);
                }
            }
        }.runTaskAsynchronously(this);

        sender.sendMessage(ChatColor.GREEN + "Sync triggered for " + online.size() + " players.");
        return true;
    }

    private CompletableFuture<Void> saveAsync(String player, int kills) {
        return CompletableFuture.runAsync(() -> save(player, kills));
    }

    private void save(String player, int kills) {
        if (conn == null) return;
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO kills (player, count) VALUES (?, ?) ON DUPLICATE KEY UPDATE count=?")) {
            ps.setString(1, player);
            ps.setInt(2, kills);
            ps.setInt(3, kills);
            ps.executeUpdate();
        } catch (SQLException e) {
            getLogger().warning("Save failed for " + player + ": " + e.getMessage());
        }
    }

    private CompletableFuture<Integer> getAsync(String player) {
        return CompletableFuture.supplyAsync(() -> {
            if (conn == null) return 0;
            try (PreparedStatement ps = conn.prepareStatement("SELECT count FROM kills WHERE player=?")) {
                ps.setString(1, player);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt(1);
                }
            } catch (SQLException e) {
                getLogger().warning("Get failed for " + player + ": " + e.getMessage());
            }
            return 0;
        });
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("db")) return Collections.emptyList();
        if (args.length == 1) {
            return Arrays.asList("set", "get", "synconline");
        }
        if (args.length == 2 && ("get".equalsIgnoreCase(args[0]) || "set".equalsIgnoreCase(args[0]))) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
