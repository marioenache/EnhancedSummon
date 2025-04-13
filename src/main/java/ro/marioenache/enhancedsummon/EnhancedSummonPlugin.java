package ro.marioenache.enhancedsummon;

import org.bukkit.plugin.java.JavaPlugin;
import ro.marioenache.enhancedsummon.commands.SummonCommandExecutor;
import ro.marioenache.enhancedsummon.commands.SummonTabCompleter;

public class EnhancedSummonPlugin extends JavaPlugin {
    
    @Override
    public void onEnable() {
        getLogger().info("EnhancedSummon has been enabled!");
        
        // Register command executor
        getCommand("esummon").setExecutor(new SummonCommandExecutor(this));
        
        // Optionally register command tab completer for better user experience
        getCommand("esummon").setTabCompleter(new SummonTabCompleter());
    }
    
    @Override
    public void onDisable() {
        getLogger().info("EnhancedSummon has been disabled!");
    }
}