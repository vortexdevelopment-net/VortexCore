# Packet Scoreboards

Packet scoreboards are opt-in. ProtocolLib must be installed and enabled, and PlaceholderAPI is used automatically when it is installed.

The recommended YAML layout is:

```yaml
Placeholder Updates:
  - "%player_name%:0"       # Resolve once, during the initial render.
  - "%player_suffix%:100"   # Refresh the cached value every 100 ticks.

Animations:
  server-logo:
    Interval: 5
    Frames:
      - "<yellow>S"
      - "<yellow>SE"
      - "<yellow>SER"
      - "<yellow>SERV"

Scoreboards:
  spawn:
    Title: "<gold>Welcome, %player_name%"
    Update: 20              # Default line interval, in ticks.
    Lines:
      - Text: "<gray>Balance: <gold>%vault_eco_balance%"
        Update: 10
      - Text: "<white><animation:server-logo>"
      - Text: "<gray>Suffix: <white>%player_suffix%"

  wilderness:
    Title: "<green>%player_name% Stats"
    Lines:
      - Text: "<gray>World: <white>%player_world%"
```

An animation can also be written as `Animation: server-logo`. The short form `<server-logo>` is accepted when it matches a configured animation name.

The reader accepts either a Bukkit `ConfigurationSection`, a VInject `ConfigurationSection`, or a raw map:

```java
@YamlConfiguration(file = "scoreboards.yml", autoSave = false)
@RegisterReloadHook
public final class ScoreboardsConfig implements net.vortexdevelopment.vinject.config.ConfigurationSection {
    public ScoreboardConfiguration read() {
        return ScoreboardConfigReader.read(this);
    }
}
```

Inject `ScoreboardsConfig` into your plugin component in the usual VInject way.

Then create a configured scoreboard from `onPluginEnable()`:

```java
private ScoreboardService scoreboardService;
private ConfiguredScoreboard scoreboard;
@Inject
private ScoreboardsConfig scoreboardsConfig;

@Override
protected void onPluginEnable() {
    scoreboardService = ScoreboardService.init();
    scoreboard = scoreboardService.create(scoreboardsConfig.read(), "spawn");
}
```

Show it for a player and stop it when they leave:

```java
scoreboard.show(player);
scoreboard.hide(player);
```

`Component` lines can use modern team prefixes and suffixes for long text. The implementation supports up to 128 visible characters in each part (up to 256 characters combined) on modern servers and falls back to legacy limits on older protocol versions. Text beyond the protocol limit is truncated. On 1.20.3 and newer it sends Minecraft's blank number format so the sidebar score numbers are not shown.

Call `hide(player)` when a viewer no longer needs the board and `destroy()` when the board is no longer used. `VortexPlugin` shuts down an initialized service automatically during plugin disable; if `init()` is never called, no scoreboard executor is created.

Placeholder behavior is intentionally cached: a placeholder omitted from `Placeholder Updates` follows its line's `Update` interval, `:0` resolves only during the initial render, and a positive interval refreshes only that placeholder's cached value. PlaceholderAPI expansions must be safe to call asynchronously.
