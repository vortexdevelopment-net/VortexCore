# Bootstrap

## Plugin Skeleton

```java
@Root(packageName = "net.vortexdevelopment.myplugin", createInstance = false)
public final class MyPlugin extends VortexPlugin {
    @Override public void onPluginLoad() { initDatabase(); }
    @Override protected void onPluginEnable() { }
    @Override protected void onPluginDisable() { }
    @Override protected void verifyLicense() { }
    @Override protected Integer getBstatsPluginId() { return null; }
}
```

## @Root Scan Scope

- `packageName` must cover your plugin classes AND allow handler discovery
- `includedPackages = {"net.vortexdevelopment.vortexcore"}` when VortexCore is not relocated
- `ignoredPackages` to exclude optional modules from scan
- Shade + relocate VortexCore under your package OR use broad package name

## Multi-Module

| Module | Contents |
| --- | --- |
| `*-API` | Interfaces, events, `@Api` facade |
| `*-Plugin` | Impls, commands, listeners, YAML |

Reference: VortexStacker-API + VortexStacker-Plugin

## Shading Warnings

- No `minimizeJar` without keeping `net/**/platform/**`
- `MavenYamlTransformer` for plugin.yml
- `vinject-maven-plugin` for `@Entity` and `@YamlId`

## References

- [docs/plugin-bootstrap.md](../../../docs/plugin-bootstrap.md)
- `VortexCore/.../VortexPlugin.java`
