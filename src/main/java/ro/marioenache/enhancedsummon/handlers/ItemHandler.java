package ro.marioenache.enhancedsummon.handlers;

import com.google.gson.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import ro.marioenache.enhancedsummon.utils.EnchantmentMapper;
import ro.marioenache.enhancedsummon.utils.JsonProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ItemHandler {

    private final JavaPlugin plugin;
    private final JsonProcessor jsonProcessor;
    private final EnchantmentMapper enchantmentMapper;

    public ItemHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        this.jsonProcessor = new JsonProcessor();
        this.enchantmentMapper = new EnchantmentMapper();
    }

    /**
     * Handles spawning an item with JSON data
     */
    public void handleItemSpawn(Location location, String jsonData, CommandSender sender) {
        // Create the ItemStack async
        createItemFromJsonAsync(jsonData, sender)
                .thenAcceptAsync(itemStack -> {
                    if (itemStack == null) {
                        return;
                    }

                    // Then spawn the item on the main thread
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        try {
                            Item item = location.getWorld().dropItem(location, itemStack);
                            item.setVelocity(new Vector(0, 0, 0)); // Prevent the item from flying away

                            sender.sendMessage("§aSuccessfully spawned item in " +
                                    location.getWorld().getName() +
                                    " at X:" + location.getX() +
                                    " Y:" + location.getY() +
                                    " Z:" + location.getZ());
                        } catch (Exception e) {
                            sender.sendMessage("§cFailed to spawn item: " + e.getMessage());
                            plugin.getLogger().warning("Error spawning item: " + e.getMessage());
                            e.printStackTrace();
                        }
                    });
                }, task -> Bukkit.getScheduler().runTask(plugin, task));
    }

    /**
     * Creates an ItemStack from JSON data asynchronously
     */
    private CompletableFuture<ItemStack> createItemFromJsonAsync(String jsonData, CommandSender sender) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Default to stone if no JSON provided
                if (jsonData == null || jsonData.isEmpty()) {
                    return new ItemStack(Material.STONE, 1);
                }

                // Parse JSON object from string
                JsonObject jsonObject = JsonParser.parseString(jsonData).getAsJsonObject();

                // Check for Item wrapper at the root level (vanilla format)
                JsonObject itemObject;
                if (jsonObject.has("Item")) {
                    itemObject = jsonObject.getAsJsonObject("Item");
                } else {
                    itemObject = jsonObject;
                }

                // Extract material (id) - required
                Material material = Material.STONE; // Default
                if (itemObject.has("id")) {
                    String itemId = itemObject.get("id").getAsString();
                    try {
                        // Support both minecraft:item_id and direct material names
                        if (itemId.contains(":")) {
                            itemId = itemId.substring(itemId.indexOf(":") + 1);
                        }
                        itemId = itemId.toUpperCase().replace(" ", "_");
                        material = Material.valueOf(itemId);
                    } catch (IllegalArgumentException e) {
                        sender.sendMessage("§cInvalid material: " + itemId);
                        return null;
                    }
                }

                // Extract count - optional, defaults to 1
                int amount = 1;
                if (itemObject.has("Count")) {
                    amount = itemObject.get("Count").getAsInt();
                } else if (itemObject.has("count")) {
                    amount = itemObject.get("count").getAsInt();
                }

                // Create the base item
                ItemStack itemStack = new ItemStack(material, amount);
                ItemMeta meta = itemStack.getItemMeta();
                if (meta == null) {
                    return itemStack; // Some materials don't have meta
                }

                // Process components section (Minecraft 1.20+ format)
                if (itemObject.has("components")) {
                    JsonObject componentsObject = itemObject.getAsJsonObject("components");

                    // Process custom_name
                    if (componentsObject.has("custom_name")) {
                        JsonElement nameElement = componentsObject.get("custom_name");

                        // Handle different formats of custom_name
                        if (nameElement.isJsonPrimitive() && nameElement.getAsJsonPrimitive().isString()) {
                            String nameString = nameElement.getAsString();

                            // Check if the string is a JSON array string that needs parsing
                            if (nameString.startsWith("[")) {
                                try {
                                    JsonArray nameArray = JsonParser.parseString(nameString).getAsJsonArray();
                                    StringBuilder displayName = new StringBuilder();

                                    for (JsonElement element : nameArray) {
                                        if (element.isJsonObject()) {
                                            JsonObject textObj = element.getAsJsonObject();
                                            if (textObj.has("text")) {
                                                String text = textObj.get("text").getAsString();

                                                // Apply color if present
                                                if (textObj.has("color")) {
                                                    String color = textObj.get("color").getAsString();
                                                    text = applyColor(text, color);
                                                }

                                                // Apply formatting
                                                if (textObj.has("italic") && !textObj.get("italic").getAsBoolean()) {
                                                    text = ChatColor.RESET + text;
                                                }

                                                displayName.append(text);
                                            }
                                        } else if (element.isJsonPrimitive()) {
                                            displayName.append(element.getAsString());
                                        }
                                    }

                                    meta.setDisplayName(displayName.toString());
                                } catch (JsonSyntaxException e) {
                                    // Not valid JSON, just use as is
                                    meta.setDisplayName(nameString);
                                }
                            } else {
                                // Regular string
                                meta.setDisplayName(nameString);
                            }
                        } else {
                            // Direct JSON element
                            meta.setDisplayName(processCustomName(nameElement));
                        }
                    }

                    // Process enchantments
                    if (componentsObject.has("enchantments")) {
                        JsonObject enchantmentsObj = componentsObject.getAsJsonObject("enchantments");

                        // Process levels of enchantments
                        if (enchantmentsObj.has("levels")) {
                            JsonObject levelsObj = enchantmentsObj.getAsJsonObject("levels");

                            for (Map.Entry<String, JsonElement> entry : levelsObj.entrySet()) {
                                String enchName = entry.getKey();
                                int level = entry.getValue().getAsInt();

                                // Strip minecraft: prefix if present
                                if (enchName.contains(":")) {
                                    enchName = enchName.substring(enchName.indexOf(":") + 1);
                                }

                                Enchantment enchantment = enchantmentMapper.getEnchantmentByName(enchName);
                                if (enchantment != null) {
                                    meta.addEnchant(enchantment, level, true);
                                }
                            }
                        }

                        // Handle show_in_tooltip property
                        if (enchantmentsObj.has("show_in_tooltip")) {
                            JsonElement tooltipElement = enchantmentsObj.get("show_in_tooltip");
                            boolean showInTooltip = false;

                            // Parse all possible boolean formats including Minecraft's "0b"
                            if (tooltipElement.isJsonPrimitive()) {
                                JsonPrimitive primitive = tooltipElement.getAsJsonPrimitive();
                                if (primitive.isBoolean()) {
                                    showInTooltip = primitive.getAsBoolean();
                                } else if (primitive.isNumber()) {
                                    showInTooltip = primitive.getAsInt() != 0;
                                } else if (primitive.isString()) {
                                    String value = primitive.getAsString();
                                    if (value.equals("0b") || value.equals("false")) {
                                        showInTooltip = false;
                                    } else {
                                        showInTooltip = true;
                                    }
                                }
                            }

                            if (!showInTooltip) {
                                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                            }
                        }
                    }
                }

                // Process legacy tag data if present and components not present
                if (itemObject.has("tag") && !itemObject.has("components")) {
                    JsonObject tagObject = itemObject.getAsJsonObject("tag");
                    processTagData(tagObject, meta, sender);
                }

                // Apply metadata to the item
                itemStack.setItemMeta(meta);

                return itemStack;

            } catch (JsonSyntaxException e) {
                sender.sendMessage("§cInvalid JSON format: " + e.getMessage());
                plugin.getLogger().warning("JSON syntax error: " + e.getMessage());
                return null;
            } catch (Exception e) {
                sender.sendMessage("§cError creating item: " + e.getMessage());
                plugin.getLogger().warning("Error creating item: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        });
    }

    /**
     * Process NBT tag data (legacy Minecraft format)
     */
    private void processTagData(JsonObject tagObject, ItemMeta meta, CommandSender sender) {
        try {
            // Handle custom name and display properties
            if (tagObject.has("display")) {
                JsonObject displayObject = tagObject.getAsJsonObject("display");

                // Set custom name
                if (displayObject.has("Name")) {
                    JsonElement nameElement = displayObject.get("Name");
                    String name = processCustomName(nameElement);
                    meta.setDisplayName(name);
                }

                // Set lore
                if (displayObject.has("Lore")) {
                    JsonArray loreArray = displayObject.getAsJsonArray("Lore");
                    List<String> lore = new ArrayList<>();
                    for (JsonElement element : loreArray) {
                        lore.add(processCustomName(element));
                    }
                    meta.setLore(lore);
                }
            }

            // Handle enchantments
            if (tagObject.has("Enchantments") || tagObject.has("ench")) {
                JsonArray enchArray = tagObject.has("Enchantments") ?
                        tagObject.getAsJsonArray("Enchantments") :
                        tagObject.getAsJsonArray("ench");

                for (JsonElement element : enchArray) {
                    if (element.isJsonObject()) {
                        JsonObject enchObj = element.getAsJsonObject();
                        String enchId = enchObj.has("id") ?
                                enchObj.get("id").getAsString() :
                                "minecraft:protection";

                        // Extract the actual enchantment name
                        if (enchId.contains(":")) {
                            enchId = enchId.substring(enchId.indexOf(":") + 1);
                        }

                        int level = enchObj.has("lvl") ?
                                enchObj.get("lvl").getAsInt() : 1;

                        // Convert to Bukkit enchantment and apply
                        Enchantment enchantment = enchantmentMapper.getEnchantmentByName(enchId);
                        if (enchantment != null) {
                            meta.addEnchant(enchantment, level, true);
                        }
                    }
                }
            }

            // Handle item flags
            if (tagObject.has("HideFlags")) {
                int hideFlags = tagObject.get("HideFlags").getAsInt();

                // Check each bit flag and apply corresponding ItemFlag
                if ((hideFlags & 1) != 0) meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                if ((hideFlags & 2) != 0) meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                if ((hideFlags & 4) != 0) meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
                if ((hideFlags & 8) != 0) meta.addItemFlags(ItemFlag.HIDE_DESTROYS);
                if ((hideFlags & 16) != 0) meta.addItemFlags(ItemFlag.HIDE_PLACED_ON);
                if ((hideFlags & 32) != 0) meta.addItemFlags(ItemFlag.HIDE_DYE);
            }

            // Handle unbreakable
            if (tagObject.has("Unbreakable")) {
                boolean unbreakable = tagObject.get("Unbreakable").getAsBoolean();
                meta.setUnbreakable(unbreakable);
            }

            // Handle custom model data
            if (tagObject.has("CustomModelData")) {
                int customModelData = tagObject.get("CustomModelData").getAsInt();
                meta.setCustomModelData(customModelData);
            }
        } catch (Exception e) {
            sender.sendMessage("§cError processing tag data: " + e.getMessage());
            plugin.getLogger().warning("Error processing tag data: " + e.getMessage());
        }
    }

    /**
     * Process custom name element, handling JSON text components
     */
    private String processCustomName(JsonElement element) {
        if (element == null) {
            return "";
        }

        // If it's a simple string, just return it
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            String rawString = element.getAsString();

            // Check if this might be a JSON string that needs to be parsed
            if (rawString.startsWith("[") || rawString.startsWith("{")) {
                try {
                    // Try to parse it as JSON
                    JsonElement parsedElement = JsonParser.parseString(rawString);
                    return processCustomName(parsedElement);
                } catch (JsonSyntaxException e) {
                    // Not valid JSON, just return as-is
                    return rawString;
                }
            }

            return rawString;
        }

        // Handle JSON array format (for text components)
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            StringBuilder result = new StringBuilder();

            for (JsonElement componentElement : array) {
                if (componentElement.isJsonObject()) {
                    JsonObject component = componentElement.getAsJsonObject();
                    String textValue = component.has("text") ? component.get("text").getAsString() : "";

                    // Apply color if present
                    if (component.has("color")) {
                        String color = component.get("color").getAsString();
                        textValue = applyColor(textValue, color);
                    }

                    // Apply formatting
                    if (component.has("bold") && component.get("bold").getAsBoolean()) {
                        textValue = ChatColor.BOLD + textValue;
                    }
                    if (component.has("italic") && component.get("italic").getAsBoolean()) {
                        textValue = ChatColor.ITALIC + textValue;
                    } else if (component.has("italic") && !component.get("italic").getAsBoolean()) {
                        textValue = ChatColor.RESET + textValue;
                    }
                    if (component.has("underlined") && component.get("underlined").getAsBoolean()) {
                        textValue = ChatColor.UNDERLINE + textValue;
                    }
                    if (component.has("strikethrough") && component.get("strikethrough").getAsBoolean()) {
                        textValue = ChatColor.STRIKETHROUGH + textValue;
                    }
                    if (component.has("obfuscated") && component.get("obfuscated").getAsBoolean()) {
                        textValue = ChatColor.MAGIC + textValue;
                    }

                    result.append(textValue);
                } else if (componentElement.isJsonPrimitive()) {
                    result.append(componentElement.getAsString());
                }
            }

            return result.toString();
        }

        // Handle single text component object
        if (element.isJsonObject()) {
            JsonObject jsonObject = element.getAsJsonObject();

            // Check for "extra" array format
            if (jsonObject.has("extra") && jsonObject.has("text")) {
                StringBuilder result = new StringBuilder(jsonObject.get("text").getAsString());
                JsonArray extraArray = jsonObject.getAsJsonArray("extra");

                for (JsonElement extraElement : extraArray) {
                    if (extraElement.isJsonObject()) {
                        JsonObject extraObj = extraElement.getAsJsonObject();
                        String text = extraObj.has("text") ? extraObj.get("text").getAsString() : "";

                        // Apply color if present
                        if (extraObj.has("color")) {
                            String color = extraObj.get("color").getAsString();
                            text = applyColor(text, color);
                        }

                        // Apply formatting
                        if (extraObj.has("bold") && extraObj.get("bold").getAsBoolean()) {
                            text = ChatColor.BOLD + text;
                        }
                        if (extraObj.has("italic") && extraObj.get("italic").getAsBoolean()) {
                            text = ChatColor.ITALIC + text;
                        } else if (extraObj.has("italic") && !extraObj.get("italic").getAsBoolean()) {
                            text = ChatColor.RESET + text;
                        }
                        if (extraObj.has("underlined") && extraObj.get("underlined").getAsBoolean()) {
                            text = ChatColor.UNDERLINE + text;
                        }
                        if (extraObj.has("strikethrough") && extraObj.get("strikethrough").getAsBoolean()) {
                            text = ChatColor.STRIKETHROUGH + text;
                        }
                        if (extraObj.has("obfuscated") && extraObj.get("obfuscated").getAsBoolean()) {
                            text = ChatColor.MAGIC + text;
                        }

                        result.append(text);
                    } else if (extraElement.isJsonPrimitive()) {
                        result.append(extraElement.getAsString());
                    }
                }

                return result.toString();
            }

            // Regular text component
            if (jsonObject.has("text")) {
                String textValue = jsonObject.get("text").getAsString();

                // Apply color if present
                if (jsonObject.has("color")) {
                    String color = jsonObject.get("color").getAsString();
                    textValue = applyColor(textValue, color);
                }

                // Apply formatting
                if (jsonObject.has("bold") && jsonObject.get("bold").getAsBoolean()) {
                    textValue = ChatColor.BOLD + textValue;
                }
                if (jsonObject.has("italic") && jsonObject.get("italic").getAsBoolean()) {
                    textValue = ChatColor.ITALIC + textValue;
                } else if (jsonObject.has("italic") && !jsonObject.get("italic").getAsBoolean()) {
                    textValue = ChatColor.RESET + textValue;
                }
                if (jsonObject.has("underlined") && jsonObject.get("underlined").getAsBoolean()) {
                    textValue = ChatColor.UNDERLINE + textValue;
                }
                if (jsonObject.has("strikethrough") && jsonObject.get("strikethrough").getAsBoolean()) {
                    textValue = ChatColor.STRIKETHROUGH + textValue;
                }
                if (jsonObject.has("obfuscated") && jsonObject.get("obfuscated").getAsBoolean()) {
                    textValue = ChatColor.MAGIC + textValue;
                }

                return textValue;
            }
        }

        // Use the JsonProcessor as a fallback
        return jsonProcessor.extractTextFromJson(element);
    }

    /**
     * Apply color code to a text string
     */
    private String applyColor(String text, String color) {
        ChatColor chatColor = getChatColorFromName(color);
        if (chatColor != null) {
            return chatColor + text;
        }
        return text;
    }

    /**
     * Convert a color name to its corresponding ChatColor
     */
    private ChatColor getChatColorFromName(String colorName) {
        switch (colorName.toLowerCase()) {
            case "black": return ChatColor.BLACK;
            case "dark_blue": return ChatColor.DARK_BLUE;
            case "dark_green": return ChatColor.DARK_GREEN;
            case "dark_aqua": return ChatColor.DARK_AQUA;
            case "dark_red": return ChatColor.DARK_RED;
            case "dark_purple": return ChatColor.DARK_PURPLE;
            case "gold": return ChatColor.GOLD;
            case "gray": return ChatColor.GRAY;
            case "dark_gray": return ChatColor.DARK_GRAY;
            case "blue": return ChatColor.BLUE;
            case "green": return ChatColor.GREEN;
            case "aqua": return ChatColor.AQUA;
            case "red": return ChatColor.RED;
            case "light_purple": return ChatColor.LIGHT_PURPLE;
            case "yellow": return ChatColor.YELLOW;
            case "white": return ChatColor.WHITE;
            default: return null;
        }
    }
}