# Agents

This repository includes agent-oriented documentation for VortexCore plugin development.

## Start Here

1. Read [`.cursor/skills/vortex-plugindev/SKILL.md`](.cursor/skills/vortex-plugindev/SKILL.md)
2. Identify your task domain (config, GUI, listener, etc.)
3. Read **only** the matching `resources/0X-*.md` file from the skill directory
4. Pull 1-2 canonical reference files from a **released** similar plugin before writing code (see skill `09-project-catalog.md`)

## Documentation

- [docs/README.md](docs/README.md) - full guide index
- [../VInject/docs/README.md](../VInject/docs/README.md) - VInject DI and lifecycle docs

## Critical Rules (Summary)

- `@RegisterListener` / `@Command` do **not** need `@Component` for `@Inject` to work
- User messages via `Lang.send`, not `player.sendMessage`
- `@YamlCollection` on `@YamlDirectory`: prefer `Map<String, Dto>` over `List`
- Use `Gui` / `PaginatedGui`, not raw inventory hacks
- Prefer import statements; no inline FQCN unless name collision
