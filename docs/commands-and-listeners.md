# Commands & Listeners

VortexCore command framework and Bukkit event registration.

---

## Commands

Annotate a class with `@Command`. Methods use framework parameter annotations.

| Annotation | Purpose |
| --- | --- |
| `@BaseCommand` | Default handler (no subcommand) |
| `@SubCommand("literal {param}")` | Subcommand with path |
| `@Sender` | Inject `CommandSender` / `Player` |
| `@Param("name")` | Bind command argument |
| `@Permission` | Required permission node |
| `@TabComplete` / `@TabArgs` | Tab completion |
| `@Current` | Inject current context object |

Implement `CommandSuggester` for tab completion on the same class.

```java
@Command(value = "stacker", aliases = {"st"})
public class StackerCommand implements CommandSuggester {
    @BaseCommand
    public void help(@Sender CommandSender sender) {
        Lang.send(sender, "Commands.Help");
    }

    @SubCommand("reload")
    @Permission("stacker.reload")
    public void reload(@Sender CommandSender sender) {
        VortexPlugin.getInstance().runReloadHooks();
        Lang.send(sender, "General.Reload Success");
    }
}
```

**Do not use `@Component`** on command classes unless another class must `@Inject` the command type.

Reference: `VortexStacker-Plugin/.../command/StackerCommand.java`

### Dynamic Command Registration

If `plugin.yml` does not declare the command, `CommandManager` creates and registers it via `CommandMaps` (Paper/Spigot bridge).

---

## Listeners

```java
@RegisterListener
public class MyListener implements Listener {
    @Inject private MyConfig config;

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Lang.send(event.getPlayer(), "General.Welcome");
    }
}
```

- Must implement `org.bukkit.event.Listener`.
- **`@RegisterListener` alone is sufficient** for `@Inject` - do not add `@Component`.
- Add `@Component` only if other classes need to `@Inject` this listener.

Reference: `VortexSellWands-Plugin/.../listener/SellWandListener.java` (with `@Component` - redundant pattern to avoid).

---

## Parameter Resolvers

```java
@Resolver
public class EntityTypeResolver implements ParameterResolver<EntityType> {
    @Override
    public EntityType resolve(String input) { /* ... */ }

    @Override
    public boolean supports(Class<?> type) {
        return EntityType.class.isAssignableFrom(type);
    }
}
```

---

## Canonical References

| Pattern | File |
| --- | --- |
| Full command | `VortexStacker/.../StackerCommand.java` |
| Listener | `VortexSellWands/.../SellWandListener.java` |
| CommandManager | `VortexCore/.../command/CommandManager.java` |
| RegisterListenerHandler | `VortexCore/.../handler/RegisterListenerHandler.java` |
