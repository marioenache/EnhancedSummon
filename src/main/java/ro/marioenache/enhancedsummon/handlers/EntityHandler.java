package ro.marioenache.enhancedsummon.handlers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;
import ro.marioenache.enhancedsummon.utils.JsonProcessor;

import java.util.concurrent.CompletableFuture;

public class EntityHandler {

    private final JavaPlugin plugin;
    private final JsonProcessor jsonProcessor;

    public EntityHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        this.jsonProcessor = new JsonProcessor();
    }

    /**
     * Handles spawning an entity with JSON data
     */
    public void handleEntitySpawn(Location location, EntityType entityType, String jsonData, CommandSender sender) {
        // Process JSON async and then spawn on main thread
        processJsonAsync(jsonData)
                .thenAcceptAsync(processedJson -> {
                    // Entity spawning must happen on the main thread
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        try {
                            Entity entity = location.getWorld().spawnEntity(location, entityType);

                            // Apply JSON data to the entity if provided
                            if (processedJson != null) {
                                applyJsonToEntity(entity, processedJson, sender);
                            }

                            sender.sendMessage("§aSuccessfully spawned " + entityType.toString() +
                                    " in " + location.getWorld().getName() +
                                    " at X:" + location.getX() +
                                    " Y:" + location.getY() +
                                    " Z:" + location.getZ());
                        } catch (Exception e) {
                            sender.sendMessage("§cFailed to spawn entity: " + e.getMessage());
                            plugin.getLogger().warning("Error spawning entity: " + e.getMessage());
                            e.printStackTrace();
                        }
                    });
                }, task -> Bukkit.getScheduler().runTask(plugin, task));
    }

    /**
     * Process JSON data asynchronously
     */
    private CompletableFuture<String> processJsonAsync(String jsonData) {
        return CompletableFuture.supplyAsync(() -> jsonData);
    }

    /**
     * Applies JSON data to an entity using NBT tags
     */
    private void applyJsonToEntity(Entity entity, String jsonData, CommandSender sender) {
        try {
            // Parse the JSON data
            JsonObject jsonObject = JsonParser.parseString(jsonData).getAsJsonObject();

            // Apply custom name if present
            if (jsonObject.has("CustomName")) {
                JsonElement nameElement = jsonObject.get("CustomName");
                String customName = jsonProcessor.extractTextFromJson(nameElement);
                entity.setCustomName(customName);
                entity.setCustomNameVisible(true);
            }

            // Apply custom name visibility if present
            if (jsonObject.has("CustomNameVisible")) {
                boolean visible = jsonObject.get("CustomNameVisible").getAsBoolean();
                entity.setCustomNameVisible(visible);
            }

            // Apply glowing effect if present
            if (jsonObject.has("Glowing")) {
                boolean glowing = jsonObject.get("Glowing").getAsBoolean();
                entity.setGlowing(glowing);
            }

            // Apply gravity if present
            if (jsonObject.has("NoGravity")) {
                boolean noGravity = jsonObject.get("NoGravity").getAsBoolean();
                entity.setGravity(!noGravity);
            }

            // Apply silent if present
            if (jsonObject.has("Silent")) {
                boolean silent = jsonObject.get("Silent").getAsBoolean();
                entity.setSilent(silent);
            }

            // Apply invulnerable if present
            if (jsonObject.has("Invulnerable")) {
                boolean invulnerable = jsonObject.get("Invulnerable").getAsBoolean();
                entity.setInvulnerable(invulnerable);
            }

            // Apply fire ticks if present
            if (jsonObject.has("Fire")) {
                int fire = jsonObject.get("Fire").getAsInt();
                entity.setFireTicks(fire);
            }

            // Check for components format (Minecraft 1.20+)
            if (jsonObject.has("components")) {
                JsonObject componentsObject = jsonObject.getAsJsonObject("components");

                // Apply custom name from components if present
                if (componentsObject.has("custom_name")) {
                    JsonElement nameElement = componentsObject.get("custom_name");
                    String customName = jsonProcessor.extractTextFromJson(nameElement);
                    entity.setCustomName(customName);
                    entity.setCustomNameVisible(true);
                }

                // Apply custom name visibility from components if present
                if (componentsObject.has("custom_name_visible")) {
                    boolean visible = componentsObject.get("custom_name_visible").getAsBoolean();
                    entity.setCustomNameVisible(visible);
                }

                // Apply glowing effect from components if present
                if (componentsObject.has("glowing")) {
                    boolean glowing = componentsObject.get("glowing").getAsBoolean();
                    entity.setGlowing(glowing);
                }

                // Apply gravity from components if present
                if (componentsObject.has("no_gravity")) {
                    boolean noGravity = componentsObject.get("no_gravity").getAsBoolean();
                    entity.setGravity(!noGravity);
                }

                // Apply silent from components if present
                if (componentsObject.has("silent")) {
                    boolean silent = componentsObject.get("silent").getAsBoolean();
                    entity.setSilent(silent);
                }

                // Apply invulnerable from components if present
                if (componentsObject.has("invulnerable")) {
                    boolean invulnerable = componentsObject.get("invulnerable").getAsBoolean();
                    entity.setInvulnerable(invulnerable);
                }

                // Apply fire ticks from components if present
                if (componentsObject.has("fire")) {
                    int fire = componentsObject.get("fire").getAsInt();
                    entity.setFireTicks(fire);
                }
            }

        } catch (Exception e) {
            sender.sendMessage("§cError applying JSON data: " + e.getMessage());
            plugin.getLogger().warning("Error applying JSON data: " + e.getMessage());
        }
    }
}