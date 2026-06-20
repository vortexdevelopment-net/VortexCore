# Commands & Listeners

## Listener

```java
@RegisterListener
public class MyListener implements Listener {
    @Inject private WandsDirectory config;

    @EventHandler
    public void onUse(PlayerInteractEvent event) {
        Lang.send(event.getPlayer(), "General.No Permission");
    }
}
```

No `@Component` unless another class injects this listener type.

## Command

```java
@Command(value = "myplugin")
public class MyCommand {
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

Implement `CommandSuggester` for tab completion.

## References

- [docs/commands-and-listeners.md](../../../docs/commands-and-listeners.md)
- `VortexStacker/.../StackerCommand.java`
