# Code Conventions

Style and quality rules for VortexCore plugin development.

---

## Messaging

- All user-facing strings in `lang.yml`.
- Use `Lang.send` / `Lang.getComponent` - never `player.sendMessage` with raw strings.
- No `ChatColor` for player-facing text.
- GUI titles and item names through `AdventureUtils` or `Lang`.

---

## Dependency Injection

- Never `new` on `@Component` / `@Service` / managed types.
- Do not add `@Component` on `@RegisterListener` or `@Command` classes unless another class injects that type.
- Use `@Component(registerSubclasses = ApiInterface.class)` on implementations.
- API module exposes interfaces + `@Api` facade only.

---

## Configuration

- Prefer `Map<String, Dto>` for `@YamlCollection` on `@YamlDirectory` holders.
- Use `@YamlItem` + `@YamlId` on batch DTOs.
- Do not `@Inject` repositories into YAML config classes.
- Require `vinject-maven-plugin` for `@Entity` and `@YamlId` DTOs.

---

## GUIs

- Use `Gui` for fixed layouts; `PaginatedGui` for paginated lists.
- No raw `Bukkit.createInventory` with hardcoded titles for user-facing menus.
- Use `setPreviusGui` / `setPreviousGui` for back navigation.

---

## Java Style

### Imports

- **Prefer import statements** at the top of the file.
- **No inline fully-qualified class names** unless two classes share the same simple name (collision).

```java
// WRONG
net.vortexdevelopment.vortexcore.text.lang.Lang.send(player, "key");

// RIGHT
import net.vortexdevelopment.vortexcore.text.lang.Lang;
Lang.send(player, "key");

// OK when colliding
import java.util.Date;
// use java.sql.Date in one place only if both are needed
```

### General

- Match naming, structure, and patterns of the plugin you are editing.
- Keep changes focused - do not refactor unrelated code.
- Use Lombok (`@Getter`, `@Data`) where the surrounding plugin already does.

---

## Reload

- Implement `ReloadHook` + `@RegisterReloadHook` for reload participants.
- Call `VortexPlugin.getInstance().runReloadHooks()` from reload commands.
- Use `@RegisterReloadHook(priority = N)` when order matters.

---

## Shading

- Keep `net/**/platform/**` when using `minimizeJar`.
- Relocate shaded VortexCore under `@Root` scan scope.

---

## Canonical References

| Topic | Doc |
| --- | --- |
| Annotations | [annotations.md](annotations.md) |
| Config | [config-system.md](config-system.md) |
| Messaging | [messaging.md](messaging.md) |
| GUI | [gui-system.md](gui-system.md) |
| Agent skill | `.cursor/skills/vortex-plugindev/SKILL.md` |
