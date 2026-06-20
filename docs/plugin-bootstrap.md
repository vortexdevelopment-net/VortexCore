# Plugin Bootstrap

How to start a VortexCore plugin and wire the DI container.

---

## Minimal Plugin Class

```java
@Root(
        packageName = "net.vortexdevelopment.myplugin",
        createInstance = false,
        templateDependencies = {
                @TemplateDependency(groupId = "net.vortexdevelopment", artifactId = "VortexCore", version = "2.0.1")
        }
)
public final class MyPlugin extends VortexPlugin {

    @Override
    public void onPluginLoad() {
        initDatabase(/* migrations */);
    }

    @Override
    protected void onPluginEnable() { }

    @Override
    protected void onPluginDisable() { }

    @Override
    protected void verifyLicense() { }

    @Override
    protected Integer getBstatsPluginId() {
        return 12345;
    }
}
```

`createInstance = false` because Bukkit constructs the plugin; Vinject scans and builds the container in `onEnable`.

---

## What `VortexPlugin` Bootstraps

| Phase | Action |
| --- | --- |
| `onLoad` | `ConfigurationContainer.setRootDirectory(dataFolder)`, license check, `CommandManager.init` |
| `onEnable` | Platform bridges, `GuiManager.register`, `DependencyContainer` build, migrations |
| `onDisable` | `GuiManager.disable`, container teardown |

---

## `@Root` Scan Scope

| Attribute | When to use |
| --- | --- |
| `packageName` | Your plugin's root package. Must include classes that use `@Command`, `@RegisterListener`, etc. |
| `includedPackages` | Add `net.vortexdevelopment.vortexcore` when VortexCore is not relocated under your package (VortexGens, VortexMinions pattern). |
| `ignoredPackages` | Exclude optional modules from scan (VortexPrisonCore pattern). |
| `componentAnnotations` | **IDE-only** - helps IntelliJ recognize `@RegisterListener` as a component stereotype. Not runtime. |

### Handler Discovery

`@Registry` handlers (CommandHandler, RegisterListenerHandler, ...) must be on the classpath inside scan scope. Common approaches:

1. **Shade + relocate** VortexCore under your plugin package (VortexStacker).
2. **Broad `packageName`** (e.g. `net.vortexdevelopment`).
3. **`includedPackages = {"net.vortexdevelopment.vortexcore"}`**.

If handlers are missing, `@Command` / `@RegisterListener` classes are discovered but never registered.

---

## Multi-Module Layout (VortexStacker Model)

| Module | Contains |
| --- | --- |
| `*-API` | Interfaces, DTOs, events, `@Api` static facade |
| `*-Plugin` | Implementations, commands, listeners, YAML, database |

Reference: `VortexStacker-API` + `VortexStacker-Plugin`.

API module exposes interfaces only. Plugin module uses:

```java
@Component(registerSubclasses = StackedEntityManager.class)
public class StackedEntityManagerImpl implements StackedEntityManager { }
```

---

## Shading

Use unified `VortexCore` artifact. In `maven-shade-plugin`:

- Do **not** use `minimizeJar` without keeping `net/**/platform/**` (breaks `CommandMaps`, `SkullProfiles`, `BukkitAdventureBridges`).
- Relocate VortexCore under your scan package if using narrow `packageName`.
- Add `MavenYamlTransformer` for `plugin.yml` / `paper-plugin.yml`.

`VortexPlugin.onEnable` warns if platform bridges failed to install.

---

## Maven Dependencies

```xml
<dependency>
    <groupId>net.vortexdevelopment</groupId>
    <artifactId>VortexCore</artifactId>
    <version>2.0.1</version>
</dependency>
```

Also require `vinject-maven-plugin` for `@Entity` and `@YamlId` batch DTOs.

---

## Canonical References

| Pattern | File |
| --- | --- |
| API + Plugin split | `VortexStacker/VortexStacker-API`, `VortexStacker-Plugin` |
| `includedPackages` | `VortexGens`, `VortexMinions` plugin classes |
| `componentAnnotations` (IDE) | `VortexPrisonCore/.../VortexPrisonCore.java` |
| Plugin base class | `VortexCore/.../VortexPlugin.java` |
