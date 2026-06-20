# Code Style

## Imports

Prefer top-level imports. No inline FQCN unless two classes share the same simple name.

```java
// WRONG
net.vortexdevelopment.vortexcore.text.lang.Lang.send(p, "key");

// RIGHT
import net.vortexdevelopment.vortexcore.text.lang.Lang;
Lang.send(p, "key");
```

## Messaging

- `Lang.send` not `player.sendMessage`
- No `ChatColor` for user text

## DI

- No `new` on managed classes
- No redundant `@Component` on `@RegisterListener` / `@Command`

## Config

- `Map<String, Dto>` for `@YamlCollection`
- `@YamlItem` on batch DTOs

## GUIs

- `Gui` / `PaginatedGui` not raw `Bukkit.createInventory` with hardcoded titles

## General

- Match surrounding plugin conventions
- Minimal scope changes - do not refactor unrelated code
- Use Lombok where the plugin already does

## References

- [docs/code-conventions.md](../../../docs/code-conventions.md)
