# Messaging

## Rules

- All user strings in `lang.yml`
- `Lang.send(sender, "Section.Key", placeholders...)`
- `AdventureUtils` for item names, lore, formatted components
- No `ChatColor`, no `player.sendMessage("raw string")`

## lang.yml

```yaml
General:
  Plugin Prefix: "<bold><gradient:#9200B7:#137FFF>Name -</gradient></bold>"
  No Permission: "<prefix> <primary-color>No permission."

Commands:
  Help: "<prefix> <primary-color>Help text"
```

Static tags: `<prefix>`, `<primary-color>`, `<secondary-color>`

## Dynamic Placeholders

```java
Lang.send(player, "Shop.Purchased",
    new MiniMessagePlaceholder("item", itemName),
    new MiniMessagePlaceholder("price", price));
```

Lang string: `"<prefix> Bought <item> for <price>"`

## Gradients

Always close `</gradient>` in MiniMessage strings.

## References

- [docs/messaging.md](../../../docs/messaging.md)
- `VortexStacker-Plugin/src/main/resources/lang.yml`
- `VortexCore/.../text/lang/Lang.java`
