package com.example.nms;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.craftbukkit.v1_19_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Collectors;

/**
 * NMSExamplePlugin
 * - Demonstrates client-side entity spawning via NMS packets (1.19_R3).
 * - /nmsarmorstand <name> [glow] [invisible]
 * - Keeps a per-player list of "virtual" entities; allows cleanup.
 */
public class NMSExamplePlugin extends JavaPlugin implements TabExecutor {

    private final Map<UUID, List<Integer>> spawnedIds = new HashMap<>();

    @Override
    public void onEnable() {
        getCommand("nmsarmorstand").setExecutor(this);
        getCommand("nmsarmorstand").setTabCompleter(this);
        getLogger().info("NMSExamplePlugin enabled.");
    }

    @Override
    public void onDisable() {
        spawnedIds.clear();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("nmsarmorstand")) return false;
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /nmsarmorstand <name> [glow] [invisible] | /nmsarmorstand clear");
            return true;
        }
        if ("clear".equalsIgnoreCase(args[0])) {
            clearVirtual(player);
            sender.sendMessage(ChatColor.GREEN + "Cleared virtual entities for you.");
            return true;
        }

        String name = args[0];
        boolean glow = args.length > 1 && Boolean.parseBoolean(args[1]);
        boolean invisible = args.length > 2 && Boolean.parseBoolean(args[2]);

        spawnVirtualArmorStand(player, name, glow, invisible);
        sender.sendMessage(ChatColor.GREEN + "Spawned virtual ArmorStand named: " + name);
        return true;
    }

    private void spawnVirtualArmorStand(Player player, String name, boolean glow, boolean invisible) {
        Location loc = player.getLocation();
        Level level = ((CraftWorld) player.getWorld()).getHandle();

        ArmorStand stand = new ArmorStand(EntityType.ARMOR_STAND, level);
        stand.setPos(loc.getX(), loc.getY(), loc.getZ());
        stand.setCustomName(Component.literal(name));
        stand.setCustomNameVisible(true);
        stand.setInvisible(invisible);
        stand.setGlowingTag(glow);

        // Send packets only to the player (client-side entity)
        var addPacket = new ClientboundAddEntityPacket(stand);
        ((CraftPlayer) player).getHandle().connection.send(addPacket);

        SynchedEntityData data = stand.getEntityData();
        var items = data.packDirty();
        var dataPacket = new ClientboundSetEntityDataPacket(stand.getId(), items);
        ((CraftPlayer) player).getHandle().connection.send(dataPacket);

        spawnedIds.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>()).add(stand.getId());
    }

    private void clearVirtual(Player player) {
        // In a full example you’d send clientbound remove packets.
        // Here we just forget IDs so future spawns don’t accumulate.
        spawnedIds.remove(player.getUniqueId());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("nmsarmorstand")) return Collections.emptyList();
        if (args.length == 1) {
            return Arrays.asList("clear", "Guardian", "Knight", "Guide");
        }
        if (args.length == 2) {
            return Arrays.asList("true", "false");
        }
        if (args.length == 3) {
            return Arrays.asList("true", "false");
        }
        return Collections.emptyList();
    }
}
