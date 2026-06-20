# GUI

## Class Choice

- **`Gui`** - fixed slot layout (default)
- **`PaginatedGui`** - list spanning multiple pages

## Gui Pattern

```java
Gui gui = new Gui(Lang.getComponent("GUI.Shop Title"), 4);
gui.setPreviusGui(parentGui);
gui.addItem(item, (event, holder, guiItem) -> { }, x, y);
gui.show(player);
```

## PaginatedGui Pattern

```java
PaginatedGui gui = new PaginatedGui(title, rows);
gui.setPreviousGui(parentGui);
gui.addItem(new GuiItem(item, handler, -1, -1)); // auto slot
gui.show(player);
```

Bottom row: back (0), prev (3), next (5), page indicator (center).

## Rules

- Titles from `Lang.getComponent` or YAML gui config with MiniMessage
- Item text via `AdventureUtils.formatItemName` / `formatItemLore`
- No hardcoded inventory titles
- `GuiManager` registered by `VortexPlugin` - do not double-register

## YAML-Driven

See VortexBazaar `BazaarGuiConfig` + `BazaarGuiSettings`.

## References

- [docs/gui-system.md](../../../docs/gui-system.md)
- `VortexStacker/.../SpawnerUpgradeGui.java`
- `VortexBazaar/.../CategoryItemsGui.java`
