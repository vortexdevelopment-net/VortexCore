# Messaging

User-facing text through `lang.yml` and Adventure/MiniMessage APIs.

---

## Rules

1. **No hardcoded player-facing strings** in Java code.
2. **No `player.sendMessage(String)`** - use `Lang.send`.
3. **No `ChatColor`** for user text.
4. All text rendering through **`AdventureUtils`** (items, GUIs, chat).

---

## `lang.yml` Structure

```yaml
Colors:
  primary-color: "#137FFF"
  secondary-color: "#FFAA00"

General:
  Plugin Prefix: "<bold><gradient:#9200B7:#137FFF>PluginName -</gradient></bold>"
  No Permission: "<prefix> <primary-color>You do not have permission."

GUI:
  Back Button Name: "<primary-color>Back"
```

Static placeholders from `lang.yml`:
- `<prefix>`, `<primary-color>`, `<secondary-color>` (loaded by `Lang.onReload()`)

Dynamic placeholders use `<name>` format with `MiniMessagePlaceholder`:

```java
Lang.send(player, "Orders.Item Removed",
    new MiniMessagePlaceholder("item", displayName),
    new MiniMessagePlaceholder("amount", formattedAmount));
```

Reference: `VortexStacker-Plugin/src/main/resources/lang.yml`

---

## `Lang` API

```java
Lang.send(player, "General.No Permission");
Lang.send(sender, "Commands.Help");
Lang.getComponent("GUI.Back Button Name");
```

`Lang` is a `@Component` that loads `lang.yml` from the plugin data folder and merges missing keys from the bundled default.

Source: `VortexCore/.../text/lang/Lang.java`

---

## `AdventureUtils`

```java
Component title = AdventureUtils.formatComponent(langPath, placeholders);
AdventureUtils.sendMessage(component, player);
AdventureUtils.formatItemName(miniMessageString);
AdventureUtils.formatItemLore(lines);
```

- Paper: native Adventure components.
- Spigot: legacy `§` bridge via `BukkitAdventureBridges`.
- Legacy `&` / `§` in strings auto-converted via `replaceLegacy()`.

---

## MiniMessage Conventions

### Placeholders

Use `<error>`, `<amount>`, `<item>` in lang strings. Pass values via `MiniMessagePlaceholder`.

### Colors

- In `lang.yml`, use `<primary-color>` and `<secondary-color>` static tags.
- For inline hex in code-built strings: `<#137FFF>`.

### Gradients

Always close `</gradient>` explicitly:

```
"<gradient:#9200B7:#137FFF>Text</gradient>"
```

Omitting `</gradient>` causes incorrect color stretching.

---

## Anti-Patterns

```java
// WRONG
player.sendMessage(ChatColor.RED + "No permission");
player.sendMessage("§cError");

// WRONG - hardcoded GUI title
Inventory inv = Bukkit.createInventory(null, 27, "Shop");

// RIGHT
Lang.send(player, "General.No Permission");
Gui gui = new Gui(Lang.getComponent("GUI.Shop Title"), 3);
```

---

## Canonical References

| Pattern | File |
| --- | --- |
| Rich lang.yml | `VortexBazaar-Plugin/src/main/resources/lang.yml` |
| Lang API | `VortexCore/.../text/lang/Lang.java` |
| AdventureUtils | `VortexCore/.../text/AdventureUtils.java` |
| MiniMessagePlaceholder | `VortexCore/.../text/MiniMessagePlaceholder.java` |
