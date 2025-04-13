package ro.marioenache.enhancedsummon.commands;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SummonTabCompleter implements TabCompleter {

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // First argument - entity type
            String input = args[0].toUpperCase();
            completions = Arrays.stream(EntityType.values())
                    .map(EntityType::name)
                    .filter(name -> name.startsWith(input))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            // Second argument - world name
            String input = args[1].toLowerCase();
            completions = Bukkit.getWorlds().stream()
                    .map(World::getName)
                    .filter(name -> name.toLowerCase().startsWith(input))
                    .collect(Collectors.toList());
        } else if (args.length >= 3 && args.length <= 5) {
            // Coordinate suggestions - offer current position or zero
            completions.add("~");
            completions.add("0");
        }

        return completions;
    }
}