# Vortex Plugin Catalog

Use this to pick reference implementations and to set expectations per project maturity.

## Released (production)

These plugins are public and stable. Prefer them as reference patterns. Avoid unnecessary breaking changes; consider migrations when altering persisted data or config shapes.

| Plugin | Workspace folder | Notes |
| --- | --- | --- |
| VortexFallingStars | `FallingStars` | Celestial events, weighted loot, in-game editor |
| VortexFileSync | `VortexFileSync` | Multi-server config sync |
| VortexGens | `VortexGens` | Regenerating ores, item drops, mob gens |
| VortexPacks | `VortexPacks` | Resource pack manager, multi-version generation |
| VortexPrisonCore | `VortexPrisonCore` | Prison core, async mines, integrated features |
| VortexSellChests | `VortexSellChest` | Auto-collect/sell chests, multipliers, boosters |
| VortexStacker | `VortexStacker` | Spawner/mob/item/block stacking, lootable editor |
| VortexVouchers | `VortexVouchers` | Custom voucher items, conditions, anti-dupe |

## In development (unreleased)

These plugins are active in the workspace but **not released**. Treat them as unstable.

| Plugin | Workspace folder |
| --- | --- |
| VortexBazaar | `VortexBazaar` |
| VortexMinions | `VortexMinions` |
| VortexTrade | `VortexTrade` |
| VortexSellWands | `VortexSellWands` |

### Agent assumptions for in-dev projects

- Breaking changes are fine at any time - **no migration path required**
- Config or schema changes do not need backward compatibility unless the user asks
- Assume the user **rebuilt and redeployed the plugin JAR (and regenerated configs if needed) before each server test**
- In-dev code may still be useful as a pattern reference, but prefer **released** plugins when both exist

## Reference priority

1. **Released plugin** with the same pattern (e.g. `VortexStacker` for API split, `VortexGens` for `@YamlDirectory`)
2. **In-dev plugin** only when no released example exists or the user is editing that project directly
3. **VortexCore / VInject** framework source for handler and bootstrap behavior

## Quick pattern map (released first)

| Pattern | Prefer |
| --- | --- |
| API + Plugin split | `VortexStacker` |
| `@YamlDirectory` + `Map<String, Dto>` | `VortexGens`, `VortexPacks`, `VortexPrisonCore` |
| Single `@YamlConfiguration` | `VortexStacker` |
| Fixed `Gui` | `VortexStacker`, `VortexVouchers` |
| `PaginatedGui` | `VortexPrisonCore`, `VortexVouchers` |
| `lang.yml` richness | `VortexStacker`, `VortexVouchers`, `VortexGens` |
| `@Entity` / `@Repository` | `VortexStacker`, `VortexPrisonCore`, `VortexVouchers` |

In-dev-only examples (use with dev assumptions above): `VortexBazaar` GUIs, `VortexSellWands` wands directory, `VortexMinions` minion configs.

## Release platforms (released plugins)

Each released plugin repo has a root `release.yml` with explicit `gameVersions`, Modrinth project id, and Hangar settings.

| Platform | Status |
| --- | --- |
| Vortex Marketplace | All 8 released plugins |
| Modrinth | All 8 (automated via Jenkins on `Release v...` commits) |
| Hangar | FallingStars, FileSync, Stacker, Vouchers (Gens/Packs/PrisonCore/SellChest TODO) |

Pipeline docs: `_pipeline/docs/multi-platform-releases.md`
