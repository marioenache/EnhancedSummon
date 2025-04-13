package ro.marioenache.enhancedsummon.commands;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import ro.marioenache.enhancedsummon.handlers.EntityHandler;
import ro.marioenache.enhancedsummon.handlers.ItemHandler;

import java.util.concurrent.CompletableFuture;

public class SummonCommandExecutor implements CommandExecutor {
    
    private final JavaPlugin plugin;
    private final EntityHandler entityHandler;
    private final ItemHandler itemHandler;
    
    public SummonCommandExecutor(JavaPlugin plugin) {
        this.plugin = plugin;
        this.entityHandler = new EntityHandler(plugin);
        this.itemHandler = new ItemHandler(plugin);
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        // Run the command processing asynchronously
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> processCommand(sender, args));
        return true;
    }
    
    private void processCommand(CommandSender sender, String[] args) {
        // Check if there are enough arguments (at least entity type)
        if (args.length < 1) {
            sender.sendMessage("§cUsage: /esummon <entity> [world] [x] [y] [z] [json]");
            return;
        }
        
        // Try to parse the entity type
        EntityType entityType;
        try {
            entityType = EntityType.valueOf(args[0].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage("§cInvalid entity type: " + args[0]);
            return;
        }
        
        // Parse location information
        Location location;
        int jsonArgIndex = -1;
        
        if (args.length >= 5) {
            // Full location specified (world, x, y, z)
            location = parseLocation(sender, args);
            if (location == null) {
                return; // Error already sent to sender
            }
            jsonArgIndex = 5; // JSON starts at index 5 if location is specified
        } else if (sender instanceof Player) {
            // Use player's location
            location = ((Player) sender).getLocation();
            jsonArgIndex = args.length >= 2 ? 1 : -1; // JSON starts at index 1 if only entity is specified
        } else {
            // Console without full location specified
            sender.sendMessage("§cConsole must specify world and coordinates: /esummon <entity> <world> <x> <y> <z> [json]");
            return;
        }
        
        // Parse JSON argument if present
        String jsonData = null;
        if (jsonArgIndex != -1 && args.length > jsonArgIndex) {
            StringBuilder jsonBuilder = new StringBuilder();
            for (int i = jsonArgIndex; i < args.length; i++) {
                jsonBuilder.append(args[i]).append(" ");
            }
            jsonData = jsonBuilder.toString().trim();
        }
        
        // Handle entity spawning based on type
        final String finalJsonData = jsonData;
        final Location finalLocation = location;
        final EntityType finalEntityType = entityType;
        
        if (entityType == EntityType.ITEM) {
            // Handle item entity spawning
            itemHandler.handleItemSpawn(finalLocation, finalJsonData, sender);
        } else {
            // Handle regular entity spawning
            entityHandler.handleEntitySpawn(finalLocation, finalEntityType, finalJsonData, sender);
        }
    }
    
    /**
     * Parse a location from command arguments
     */
    private Location parseLocation(CommandSender sender, String[] args) {
        String worldName = args[1];
        World world = Bukkit.getWorld(worldName);
        
        if (world == null) {
            sender.sendMessage("§cWorld not found: " + worldName);
            return null;
        }
        
        try {
            double x = parseCoordinate(args[2], sender instanceof Player ? ((Player) sender).getLocation().getX() : 0);
            double y = parseCoordinate(args[3], sender instanceof Player ? ((Player) sender).getLocation().getY() : 0);
            double z = parseCoordinate(args[4], sender instanceof Player ? ((Player) sender).getLocation().getZ() : 0);
            return new Location(world, x, y, z);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid coordinates. Please use numbers for x, y, and z.");
            return null;
        }
    }
    
    /**
     * Parse a coordinate string which might be relative (with ~)
     */
    private double parseCoordinate(String coord, double reference) {
        if (coord.startsWith("~")) {
            if (coord.length() > 1) {
                return reference + Double.parseDouble(coord.substring(1));
            } else {
                return reference;
            }
        }
        return Double.parseDouble(coord);
    }
}