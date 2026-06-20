# Config System

YAML-backed configuration using VInject annotations.

---

## Decision Guide

| Use case | Pattern |
| --- | --- |
| One settings file | `@YamlConfiguration` |
| Keyed section in one file | `@YamlConfiguration` + `@Key` + `Map<String, Dto>` |
| One YAML file per entry in a folder | `@YamlDirectory` + `@YamlCollection` + `Map<String, Dto>` |
| Batch entry identity | `@YamlId` on DTO field |
| Compact nested DTO | `@YamlItem` on class |
| `ItemStack` in YAML | Automatic via `ItemStackSerializer` |

---

## `@YamlConfiguration` - Single File

```java
@Getter
@YamlConfiguration(file = "config.yml")
public class StackerConfig {
    @Comment("Enable or disable the Items module.")
    @Key("Modules.Items.Enabled")
    private boolean itemsEnabled = true;

    @Key("Items")
    private Map<String, ItemSetting> items = new HashMap<>();
}
```

Reference: `VortexStacker-Plugin/.../config/StackerConfig.java`

Inject elsewhere: `@Inject private StackerConfig config;`

Reload: `ConfigurationContainer.getInstance().reloadConfig(StackerConfig.class)` or `@RegisterReloadHook`.

---

## `@YamlDirectory` - Batch Loading

**Prefer `Map<String, Dto>` over `List<Dto>`** for `@YamlCollection`. Maps preserve entry identity by key/filename and allow O(1) lookup.

```java
@YamlDirectory(dir = "wands", target = SellWandTypeEntry.class)
@RegisterReloadHook
@Getter
public final class WandsDirectory {
    @YamlCollection
    private Map<String, SellWandType> wands = new ConcurrentHashMap<>();
}
```

Reference: `VortexSellWands-Plugin/.../config/WandsDirectory.java`

### DTO (`target` class)

```java
@Data
@YamlItem
public class SellWandTypeEntry implements SellWandType {
    @YamlId
    private String id;

    @Key("Display Name")
    private String displayName;
}
```

- `@YamlItem` - compact YAML serialization (no blank lines between fields).
- `@YamlId` - receives map key / filename id. Requires `vinject-maven-plugin` transformer.

### Legacy Pattern (Avoid for New Code)

`List<Dto>` on `@YamlDirectory` still works but loses keyed lookup:

```java
@YamlCollection
private List<LootTable> tables = new ArrayList<>();
```

Reference: `VortexStacker-Plugin/.../config/LootTablesConfig.java` (legacy).

---

## YAML Phase Constraint

YAML config classes load **before** components and repositories. Do not `@Inject` repositories or services into YAML holders (`VINJECT-DEP-004`).

```java
// WRONG
@YamlConfiguration(file = "config.yml")
public class BadConfig {
    @Inject private OrderRepository orders;
}

// RIGHT
@Component
public class OrderService {
    @Inject private MyConfig config;
    @Inject private OrderRepository orders;
}
```

---

## GUI YAML Configs

```java
@YamlDirectory(dir = "gui", target = BazaarGuiSettings.class)
public class BazaarGuiConfig {
    @YamlCollection
    private Map<String, BazaarGuiSettings> guis = new HashMap<>();
}
```

Reference: `VortexBazaar-Plugin/.../config/gui/BazaarGuiConfig.java`

---

## Reload

| Type | Reload approach |
| --- | --- |
| `@YamlConfiguration` | `reloadConfig(Class)` or `@RegisterReloadHook` |
| `@YamlDirectory` | Custom `ReloadHook.onReload()` - batch trees are not fully reloaded by `reloadConfig(holder)` alone |

Call `VortexPlugin.getInstance().runReloadHooks()` from `/plugin reload`.

---

## ItemStack Fields

Any `ItemStack`-typed field deserializes via VortexCore `ItemStackSerializer`. See [itemstack-yaml-serializer.md](itemstack-yaml-serializer.md).

---

## Canonical References

| Pattern | File |
| --- | --- |
| Map batch directory | `VortexSellWands/.../WandsDirectory.java` |
| Single file + Map section | `VortexStacker/.../ItemsConfig.java` |
| GUI YAML batch | `VortexBazaar/.../BazaarGuiConfig.java` |
| VInject YAML internals | `../VInject/docs/yaml_configuration.md` |
