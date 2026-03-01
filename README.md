# SimpleLaunch - Paper 1.21 Plugin

A simple Minecraft plugin for Paper 1.21 that launches players when they step on Gold Pressure Plates.

## Features

- Players are launched 2 blocks high and 5 blocks forward when stepping on Gold Pressure Plates
- Plays "Firework Blast" sound effect when launched
- Spawns "Cloud" particles at player's feet during launch
- Prevents fall damage for 5 seconds after being launched
- Uses modern Paper API 1.21 and Java 21

## Installation

1. Download the latest JAR file from the [Releases](https://github.com/yourusername/SimpleLaunch/releases) page
2. Place the JAR file in your server's `plugins` folder
3. Restart your Paper server
4. The plugin will automatically enable

## Usage

Simply place Gold Pressure Plates on your server. When players step on them, they will be launched according to the configured parameters.

## Configuration

This plugin doesn't require any configuration files. All settings are hardcoded in the plugin.

## Permissions

No special permissions are required. All players can use the launch feature.

## Building from Source

1. Install Java 21 JDK
2. Install Maven
3. Clone this repository
4. Run `mvn clean package`
5. The JAR file will be created in the `target` directory

## API Usage

Other plugins can interact with SimpleLaunch using the following methods: