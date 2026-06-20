# Config

## Patterns

| Need | Use |
| --- | --- |
| One settings file | `@YamlConfiguration(file = "...")` |
| Keyed section | `@Key` + `Map<String, Dto>` |
| Folder of files | `@YamlDirectory` + `@YamlCollection` + `Map<String, Dto>` |
| Batch DTO | `@YamlItem` + `@YamlId` |

## Preferred: Map on @YamlDirectory

```java
@YamlDirectory(dir = "wands", target = SellWandTypeEntry.class)
@RegisterReloadHook
public final class WandsDirectory {
    @YamlCollection
    private Map<String, SellWandType> wands = new ConcurrentHashMap<>();
}
```

Avoid `List<Dto>` for new code (legacy in VortexStacker `LootTablesConfig`).

## DTO

```java
@Data
@YamlItem
public class SellWandTypeEntry implements SellWandType {
    @YamlId private String id;
    @Key("Display Name") private String displayName;
}
```

Requires `vinject-maven-plugin` for `@YamlId`.

## YAML Phase Rule

Never `@Inject` repositories or services into YAML config classes (`VINJECT-DEP-004`). Inject configs into `@Component` classes instead.

## Reload

- `@YamlConfiguration`: `@RegisterReloadHook` or `ConfigurationContainer.reloadConfig(Class)`
- `@YamlDirectory`: custom `ReloadHook.onReload()` for full batch refresh
- Trigger: `VortexPlugin.getInstance().runReloadHooks()`

## References

- [docs/config-system.md](../../../docs/config-system.md)
- `VortexSellWands/.../WandsDirectory.java`
- `VortexStacker/.../StackerConfig.java`
- `VInject/docs/yaml_configuration.md`
