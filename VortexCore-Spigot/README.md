# VortexCore-Spigot

Spigot-oriented **distribution** of VortexCore. This module shades the compiled **`VortexCore-API`** JAR and adds Spigot-only classes (for example `SpigotChatPromptManager` and `SpigotCommandMapBridge`, which registers dynamic commands via reflection because Spigot does not expose `Server#getCommandMap()` like Paper).

## Maven

- **Paper servers**: depend on `net.vortexdevelopment:VortexCore-Paper` only (not this module).
- **Spigot-oriented servers**: depend on `net.vortexdevelopment:VortexCore-Spigot` only. Do not add `VortexCore-Paper` or duplicate `VortexCore-API` as a second runtime dependency for the same plugin.

Shared code in `VortexCore-API` is compiled against **`paper-api`** so Adventure-backed Bukkit APIs match the Paper line; the Spigot artifact reuses that bytecode and supplies Spigot-specific bridges where needed.

`SpigotBukkitAdventureBridge` registers on startup (Vinject) and drives `AdventureUtils` paths that touch Bukkit: **item display name and lore**, **inventory titles**, **`Component` messages to `CommandSender`s**, **hologram armor stand names**, **entity teleport** (sync `teleport` vs Paper `teleportAsync`), and **shutdown detection** for scheduler guards (`Bukkit.isStopping()` when present). Legacy **section** serialization is used where Spigot only accepts strings.

**Skulls:** `HeadUtils` / `SkullProfiles` use `SpigotSkullProfileService`, which applies custom skin URLs via Mojang **GameProfile** and the CraftBukkit `profile` field on `SkullMeta` (classic Spigot approach). Paper builds use `PaperSkullProfileService` instead.

## `plugin.yml`

Merge the `libraries` list from this module’s `src/main/resources/plugin.yml` into your plugin so Spigot loads Kyori at runtime. Keep versions aligned with the parent POM property `adventure.version`.

## API

Use `net.vortexdevelopment.vortexcore.spi.ChatPrompts.promptPlayer(...)` instead of calling `PaperPromptManager` directly.
