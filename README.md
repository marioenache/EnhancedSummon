# Enhanced Summon Plugin

A Bukkit/Spigot plugin that enhances the summon command to support world specification, which is particularly useful when executing from the console. Now with support for item summoning and JSON component arguments!

## Features

- Adds a new command `/esummon` that works like the vanilla summon command but with world support
- Allows console to specify which world to spawn entities in
- Maintains normal functionality for players (uses current location if not specified)
- Supports summoning items with specific properties
- Supports JSON component arguments for customizing entities

## Usage

- For players: `/esummon <entity>` - Spawns entity at player's location
- For players/console: `/esummon <entity> <world> <x> <y> <z>` - Spawns entity at specified location in specified world
- For players/console with JSON: `/esummon <entity> <world> <x> <y> <z> <json>` - Spawns entity with custom properties

## Permissions

- `enhancedsummon.use` - Permission to use the `/esummon` command

## Installation

1. Place the plugin jar file in your server's `plugins` folder
2. Restart your server or load the plugin with a plugin manager
3. Configure permissions as needed

## Building from Source

1. Clone the repository
2. Run `mvn clean package`
3. The compiled jar will be in the `target` folder

## Author

Created by [Mario Enache](https://marioenache.ro)