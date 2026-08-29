# VortexCore Documentation

Plugin development guides for VortexCore on top of VInject.

## Guides

| Guide | Topic |
| --- | --- |
| [Plugin Bootstrap](plugin-bootstrap.md) | `VortexPlugin`, `@Root`, shading, multi-module layout |
| [Annotations](annotations.md) | `@Command`, `@RegisterListener`, `@Api` - when not to use `@Component` |
| [Config System](config-system.md) | YAML mapping, `@YamlDirectory`, Map vs List, reload |
| [Messaging](messaging.md) | `lang.yml`, `Lang`, `AdventureUtils`, MiniMessage |
| [GUI System](gui-system.md) | `Gui`, `PaginatedGui`, YAML-driven menus |
| [Commands & Listeners](commands-and-listeners.md) | Command framework and event listeners |
| [Packet Scoreboards](scoreboard.md) | Async ProtocolLib scoreboards with long lines |
| [Code Conventions](code-conventions.md) | Style rules for plugin code |
| [ItemStack YAML serializer](itemstack-yaml-serializer.md) | ItemStack field keys in YAML |

## For Agents

Start with `.cursor/skills/vortex-plugindev/SKILL.md` in this repo, then read only the resource file matching your task domain.

VInject DI docs: `../VInject/docs/` (components, lifecycle, YAML internals).
