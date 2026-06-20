# Annotations

VortexCore custom annotations and when to use `@Component`.

---

## Registry Annotations (Not `@Component`)

These are processed by `@Registry` handlers at `RegistryOrder.COMPONENTS`. They are **not** meta-annotated with `@Component`.

| Annotation | Handler | Effect |
| --- | --- | --- |
| `@Command` | `CommandHandler` | Registers with `CommandManager` |
| `@RegisterListener` | `RegisterListenerHandler` | Registers with Bukkit `PluginManager` |
| `@Api` | `ApiHandler` | Instantiates static facade, emits `vortexcore.api.load` |
| `@RegisterReloadHook` | `ReloadRegisterHookHandler` | Registers `ReloadHook` implementations |

At runtime the handler calls `dependencyContainer.newInstance(clazz)`, which:

- Resolves `@Inject` fields
- Invokes `@PostConstruct`
- Caches the instance in the container

**Do not add `@Component` just because the class has `@Inject` fields.**

---

## When to Add `@Component`

| Scenario | Use `@Component`? |
| --- | --- |
| Listener with `@Inject` deps, nothing injects the listener | **No** - `@RegisterListener` only |
| Command class with `@Inject` deps | **No** - `@Command` only |
| Manager/service other classes `@Inject` | **Yes** |
| Impl registered under API interface | **Yes** + `registerSubclasses` |
| `@YamlDirectory` config holder | **Usually no** - see config guide |
| Class implements `ReloadHook` without `@RegisterReloadHook` | **Yes** (via `ReloadHookInterceptor`) |

---

## `@RegisterListener`

```java
@RegisterListener
public class SellWandListener implements Listener {
    @Inject private WandsDirectory wands;

    @EventHandler
    public void onInteract(PlayerInteractEvent event) { }
}
```

- Must implement `org.bukkit.event.Listener`.
- **Wrong:** `@Component` + `@RegisterListener` (redundant unless another class injects this listener type).

---

## `@Command`

```java
@Command(value = "myplugin", aliases = {"mp"})
public class MyCommand implements CommandSuggester {
    @Inject private MyConfig config;

    @BaseCommand
    public void help(@Sender CommandSender sender) {
        Lang.send(sender, "Commands.Help");
    }

    @SubCommand("reload")
    @Permission("myplugin.reload")
    public void reload(@Sender CommandSender sender) {
        VortexPlugin.getInstance().runReloadHooks();
        Lang.send(sender, "General.Reload Success");
    }
}
```

---

## `@Api`

Static facade in the API module:

```java
@Api
public final class VortexStackerApi {
    @Inject @Getter private static StackedEntityManager entityManager;
}
```

Expose **interfaces** only, never implementation classes.

---

## `@RegisterReloadHook` and `ReloadHook`

```java
@YamlDirectory(dir = "wands", target = SellWandTypeEntry.class)
@RegisterReloadHook
public final class WandsDirectory {
    @YamlCollection
    private Map<String, SellWandType> wands = new ConcurrentHashMap<>();
}
```

- Implement `ReloadHook` with `onReload()` for custom reload logic.
- `@RegisterReloadHook` on `@YamlConfiguration` auto-calls `ConfigurationContainer.reloadConfig(thisClass)`.
- `@YamlDirectory` batch reload may need explicit logic in `onReload()`.
- Call `VortexPlugin.getInstance().runReloadHooks()` from a reload subcommand.

---

## `@Root.componentAnnotations` (IDE Only)

```java
@Root(
        packageName = "net.vortexdevelopment.myplugin",
        componentAnnotations = {RegisterListener.class, RegisterCommand.class}
)
```

This helps the IntelliJ Vinject plugin treat custom annotations as components in the IDE. It does **not** register classes at runtime - the `@Registry` handler does.

---

## Canonical References

| Pattern | File |
| --- | --- |
| Listener without `@Component` | Inspect plugins using only `@RegisterListener` |
| Listener with redundant `@Component` | `VortexMinions/.../MinionClickListener.java` (avoid this pattern) |
| Config + reload hook | `VortexSellWands/.../WandsDirectory.java` |
| Command + reload | `VortexStacker/.../StackerCommand.java` |
