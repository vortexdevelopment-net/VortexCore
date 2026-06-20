# Task Preparation

Run this checklist before any plugin edit or new feature.

## Step 1 - Classify the Task

| Domain | Signals |
| --- | --- |
| Bootstrap | New plugin, `@Root`, shading, module layout |
| Annotations | `@Component` vs `@RegisterListener` vs `@Command` confusion |
| Config | `@YamlConfiguration`, `@YamlDirectory`, reload |
| Messaging | `lang.yml`, player messages, MiniMessage |
| GUI | `Gui`, `PaginatedGui`, inventory menus |
| Commands/Listeners | `@Command`, `@RegisterListener`, tab complete |
| Database/API | `@Entity`, `@Repository`, `@Api`, `@Qualifier` |
| Code style | Imports, conventions, refactoring |

## Step 2 - Read One Resource File

Open only the matching file from this skill's `resources/` folder. Do not read all resources.

## Step 3 - Find a Reference Implementation

Search sibling plugins in the workspace for the same pattern:

```
# Example: find YamlDirectory with Map
rg "@YamlDirectory" --glob "*.java"
rg "Map<String" --glob "*Config*.java"
```

Prefer newer plugins (VortexSellWands, VortexMinions, VortexPacks) over legacy List-based configs.

## Step 4 - Pull Canonical Files

Read 1-2 reference files fully before writing. Examples:

| Task | Read first |
| --- | --- |
| New listener | Similar listener in same plugin; `RegisterListenerHandler.java` if unsure |
| New YAML directory | `VortexSellWands/.../WandsDirectory.java` |
| New GUI | Similar GUI class in same plugin |
| New command | `VortexStacker/.../StackerCommand.java` |

## Step 5 - Verify Annotation Choice

Before adding `@Component`, ask: "Does another class need to `@Inject` this exact type?"

- No -> use only `@RegisterListener`, `@Command`, or YAML annotation
- Yes -> `@Component` (interfaces auto-register; use `@Qualifier` when multiple beans share a type)

## Deep Dives

- [docs/README.md](../../../docs/README.md) - VortexCore guides
- `VInject/docs/agent-reference.md` - Vinject quick lookup
