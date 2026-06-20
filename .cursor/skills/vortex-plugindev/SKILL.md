---
name: vortex-plugindev
description: >-
  Develops Minecraft plugins with VortexCore and Vinject. Use when working on
  VortexPlugin, @Root, @Component, @Inject, @YamlConfiguration, @YamlDirectory,
  @YamlCollection, @RegisterListener, @Command, @Api, ReloadHook, Lang.send,
  AdventureUtils, lang.yml, Gui, PaginatedGui, ItemStack YAML, @Qualifier,
  or any Vortex plugin module. Read task-specific resource files before editing.
---

# Vortex Plugin Development

VortexCore on Vinject. **Read only the resource files needed for your task** - do not load all resources.

## 1. Task Preparation (Mandatory)

Before writing code:

1. **Identify domain:** bootstrap | annotations | config | messaging | gui | commands | database/api | code style
2. **Read matching resource** from [resources/](resources/) (see index below)
3. **Grep a reference plugin** for the same pattern
4. **Pull 1-2 canonical source files** into context before editing

## 2. Annotation Decision Tree

| Class role | Annotation | Also `@Component`? |
| --- | --- | --- |
| Manager/service others inject | `@Component` | Yes |
| API impl bound to interface | `@Component` (auto-registers interfaces; `@Qualifier` if multiple) | Yes |
| Bukkit listener | `@RegisterListener` | **No** (unless another class injects this listener type) |
| Command class | `@Command` | **No** (unless injected elsewhere) |
| API static facade | `@Api` | No |
| YAML batch holder | `@YamlDirectory` | Usually no |
| YAML single file | `@YamlConfiguration` | No |

**Never add `@Component` just because a class has `@Inject` fields.** Registry handlers call `newInstance()` which resolves injection.

`@Root.componentAnnotations` is IDE-only, not runtime.

## 3. Non-Negotiable Conventions

- `Lang.send` / `Lang.getComponent` - not `player.sendMessage` with raw strings
- No `ChatColor` for user-facing text
- `Gui` for fixed layouts; `PaginatedGui` for paginated lists
- `Map<String, Dto>` for `@YamlCollection` on `@YamlDirectory` (not `List`)
- `@YamlItem` + `@YamlId` on batch DTOs
- Never `new` on managed classes
- Import statements at top; no inline FQCN unless name collision
- Match existing plugin style and naming

## 4. Resource Index

| File | Read when |
| --- | --- |
| [00-task-prep.md](resources/00-task-prep.md) | Starting any plugin task |
| [01-annotations.md](resources/01-annotations.md) | `@Component`, `@RegisterListener`, `@Command`, `@Api` |
| [02-bootstrap.md](resources/02-bootstrap.md) | Plugin class, `@Root`, shading, multi-module |
| [03-config.md](resources/03-config.md) | YAML configs, reload |
| [04-messaging.md](resources/04-messaging.md) | `lang.yml`, MiniMessage, `Lang` |
| [05-gui.md](resources/05-gui.md) | `Gui`, `PaginatedGui` |
| [06-commands-listeners.md](resources/06-commands-listeners.md) | Commands, listeners |
| [07-database-api.md](resources/07-database-api.md) | `@Entity`, `@Repository`, `@Api`, `@Qualifier` |
| [08-code-style.md](resources/08-code-style.md) | Imports, style rules |

## 5. Canonical Repo Docs

- VortexCore: [docs/README.md](../../docs/README.md)
- Vinject: `VInject/docs/README.md` (components, lifecycle, YAML internals)

## 6. Reference Plugins

| Task | Inspect |
| --- | --- |
| API + Plugin split | VortexStacker |
| Map `@YamlDirectory` | VortexSellWands `WandsDirectory` |
| Single `@YamlConfiguration` | VortexStacker `StackerConfig` |
| GUI YAML batch | VortexBazaar `BazaarGuiConfig` |
| `PaginatedGui` | VortexBazaar `CategoryItemsGui` |
| Fixed `Gui` | VortexStacker `SpawnerUpgradeGui` |
| `lang.yml` | VortexStacker or VortexBazaar plugin resources |
| `includedPackages` | VortexGens, VortexMinions |
