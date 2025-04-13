package ro.marioenache.enhancedsummon.utils;

import org.bukkit.enchantments.Enchantment;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for mapping Minecraft enchantment IDs to Bukkit Enchantment enum values
 */
public class EnchantmentMapper {
    
    private final Map<String, Enchantment> enchantmentMap;
    
    public EnchantmentMapper() {
        this.enchantmentMap = createEnchantmentMap();
    }
    
    /**
     * Maps a Minecraft enchantment ID to a Bukkit Enchantment
     */
    public Enchantment getEnchantmentByName(String name) {
        // Try to get enchantment by map
        Enchantment enchantment = enchantmentMap.get(name.toLowerCase());
        if (enchantment != null) {
            return enchantment;
        }
        
        // Fallback: try direct name match
        try {
            String bukkitName = name.toUpperCase().replace(":", "_");
            return Enchantment.getByName(bukkitName);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Creates a map of Minecraft enchantment names to Bukkit Enchantment values
     */
    private Map<String, Enchantment> createEnchantmentMap() {
        Map<String, Enchantment> map = new HashMap<>();
        
        // Protection enchantments
        map.put("protection", Enchantment.PROTECTION);
        map.put("fire_protection", Enchantment.FIRE_PROTECTION);
        map.put("feather_falling", Enchantment.FEATHER_FALLING);
        map.put("blast_protection", Enchantment.BLAST_PROTECTION);
        map.put("projectile_protection", Enchantment.PROJECTILE_PROTECTION);
        map.put("respiration", Enchantment.RESPIRATION);
        map.put("aqua_affinity", Enchantment.AQUA_AFFINITY);
        map.put("thorns", Enchantment.THORNS);
        map.put("depth_strider", Enchantment.DEPTH_STRIDER);
        map.put("frost_walker", Enchantment.FROST_WALKER);
        map.put("binding_curse", Enchantment.BINDING_CURSE);
        
        // Weapon enchantments
        map.put("sharpness", Enchantment.SHARPNESS);
        map.put("smite", Enchantment.SMITE);
        map.put("bane_of_arthropods", Enchantment.BANE_OF_ARTHROPODS);
        map.put("knockback", Enchantment.KNOCKBACK);
        map.put("fire_aspect", Enchantment.FIRE_ASPECT);
        map.put("looting", Enchantment.LOOTING);
        map.put("sweeping", Enchantment.SWEEPING_EDGE);
        
        // Tool enchantments
        map.put("efficiency", Enchantment.EFFICIENCY);
        map.put("silk_touch", Enchantment.SILK_TOUCH);
        map.put("unbreaking", Enchantment.UNBREAKING);
        map.put("fortune", Enchantment.FORTUNE);
        
        // Bow enchantments
        map.put("power", Enchantment.POWER);
        map.put("punch", Enchantment.PUNCH);
        map.put("flame", Enchantment.FLAME);
        map.put("infinity", Enchantment.INFINITY);
        
        // Fishing rod enchantments
        map.put("luck_of_the_sea", Enchantment.LUCK_OF_THE_SEA);
        map.put("lure", Enchantment.LURE);
        
        // Trident enchantments
        map.put("loyalty", Enchantment.LOYALTY);
        map.put("impaling", Enchantment.IMPALING);
        map.put("riptide", Enchantment.RIPTIDE);
        map.put("channeling", Enchantment.CHANNELING);
        
        // Crossbow enchantments
        map.put("multishot", Enchantment.MULTISHOT);
        map.put("quick_charge", Enchantment.QUICK_CHARGE);
        map.put("piercing", Enchantment.PIERCING);
        
        // Other enchantments
        map.put("mending", Enchantment.MENDING);
        map.put("vanishing_curse", Enchantment.VANISHING_CURSE);
        map.put("soul_speed", Enchantment.SOUL_SPEED);
        map.put("swift_sneak", Enchantment.SWIFT_SNEAK);
        
        return map;
    }
}