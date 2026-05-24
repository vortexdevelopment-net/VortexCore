# ItemStack YAML serializer

VortexCore registers `ItemStackSerializer` (`net.vortexdevelopment.vortexcore.vinject.serializer.ItemStackSerializer`) as a Vinject `@YamlSerializer`. Any **`ItemStack`-typed field** in a `@YamlConfiguration` class, `@YamlItem` mapping, or `@YamlDirectory` batch DTO deserializes from a YAML mapping using the keys below.

- **Keys are case-sensitive** and must match the names in this document (they mirror the serializer map keys).
- **`Name`** and **`Lore`** use **MiniMessage** strings (parsed via `AdventureUtils` / `BukkitAdventureBridges`).
- **Nested items** (`Bundle Items`, `Shulker Contents`) use the **same** mapping shape recursively.
- **Skull texture fields** (`UUID`, `Texture`, `Owner`) are written by `SkullProfiles` (Paper vs Spigot); install VortexCore normally so the platform service is registered.

---

## Required and common fields

| Key | Type | Description |
|-----|------|-------------|
| `Material` | string | Bukkit `Material` enum name (e.g. `DIAMOND_SWORD`). Required for a non-empty stack; missing or unknown material yields `null` on deserialize. |
| `Amount` | number | Stack size. Default `1` if omitted. |

---

## Display and item meta

| Key | Type | Description |
|-----|------|-------------|
| `Name` | string | Item display name (MiniMessage). |
| `Lore` | list of strings | Lore lines (MiniMessage), one string per line. |
| `Enchants` | list of strings | Applied enchants: `"<ENCHANT_KEY>:<level>"`. The key is the **minecraft** resource path segment (serializer outputs uppercase), e.g. `SHARPNESS:5`. Parsed with `Enchantment.getByKey(NamespacedKey.minecraft(...))`. |
| `Stored Enchants` | list of strings | Same format as `Enchants`, for **enchanted books** (`EnchantmentStorageMeta`). |
| `Item Flags` | list of strings | `org.bukkit.inventory.ItemFlag` enum names, e.g. `HIDE_ENCHANTS`, `HIDE_ATTRIBUTES`. |
| `Unbreakable` | boolean | Sets unbreakable flag when `true`. |
| `Durability` | number | Damage on damageable items (`Damageable` meta). |
| `Custom Model Data` | number | Vanilla custom model data integer. |
| `Custom Model` | string | Paper **item model** `NamespacedKey` as string (e.g. `mypack:gui/arrow`). Only when the server supports item components; see `ServerVersion.isItemComponentsAvailable()`. |

---

## Attributes

| Key | Type | Description |
|-----|------|-------------|
| `Attributes` | list of strings | Each entry: **`<registry_key>:<amount>:<OPERATION>[:<slot_group>]`** where `OPERATION` is an `AttributeModifier.Operation` name (`ADD_NUMBER`, `ADD_SCALAR`, `MULTIPLY_SCALAR_1`), and optional `slot_group` is resolved with `EquipmentSlotGroup.getByName` (deserialize lowercases the segment). The first segment is resolved with `Registry.ATTRIBUTE` / `NamespacedKey.minecraft(...)`. **Easiest workflow:** serialize an item in-game or from code and copy the exact strings the serializer emits. |

---

## Potions

| Key | Type | Description |
|-----|------|-------------|
| `Potion` | string | `PotionType` enum name for **`POTION`** or **`TIPPED_ARROW`** only (`PotionMeta`). |

---

## Leather armor

| Key | Type | Description |
|-----|------|-------------|
| `Color` | string | RGB hex: `#RRGGBB` (optional `#` prefix accepted on deserialize). |

---

## Player heads (`PLAYER_HEAD` / skull meta)

| Key | Type | Description |
|-----|------|-------------|
| `UUID` | string | Optional profile UUID string. |
| `Texture` | string | Skin texture **URL** (e.g. `http://textures.minecraft.net/texture/...`). If present, used in preference to `Owner`. |
| `Owner` | string | Player name when no texture URL is set (resolved via player profile / offline player APIs depending on platform). |

---

## Fireworks

### Rocket (`FireworkMeta`)

| Key | Type | Description |
|-----|------|-------------|
| `Power` | number | Flight duration / power. |
| `Firework Effects` | map | Keys are numeric strings (`"0"`, `"1"`, …). Values are **effect maps** (see below). Order is sorted numerically on deserialize. |

### Firework star (`FireworkEffectMeta`)

| Key | Type | Description |
|-----|------|-------------|
| `Firework Effect` | map | A single **effect map** (see below). |

### Effect map shape (`serializeFireworkEffect` / `deserializeFireworkEffect`)

| Key | Type | Description |
|-----|------|-------------|
| `Type` | string | `FireworkEffect.Type` enum name (e.g. `BALL_LARGE`, `BURST`). |
| `Colors` | list of strings | Hex colors `#RRGGBB`. |
| `Fade Colors` | list of strings | Fade hex colors. |
| `Flicker` | boolean | Optional flicker. |
| `Trail` | boolean | Optional trail. |

---

## Banners

| Key | Type | Description |
|-----|------|-------------|
| `Banner Patterns` | list of strings | Each string: **`<DYE_COLOR>:<pattern_identifier>`** where `DYE_COLOR` is a `DyeColor` name and `pattern_identifier` is a `PatternType` identifier (see `PatternType.getByIdentifier`). |

---

## Maps (`MapMeta` / `MapView`)

| Key | Type | Description |
|-----|------|-------------|
| `Map ID` | number | Bukkit map id; loads `Bukkit.getMap(id)` when present. |
| `Map Locked` | boolean | Applied to the `MapView` when `Map ID` resolves. |
| `Map Renderers` | list of strings | Fully qualified **class names**; each class must implement `MapRenderer` and have a no-arg constructor. |
| `Map Scaling` | boolean | Scaling flag on map meta. |
| `Location Name` | string | Map location label. |
| `Map Color` | string | Hex `#RRGGBB` for map meta tint. |

---

## Other specialized meta

| Key | Type | Description |
|-----|------|-------------|
| `Axolotl Variant` | string | `Axolotl.Variant` enum name (bucket meta). |
| `Bundle Items` | map | Keys: `"0"`, `"1"`, … Values: **nested full ItemStack maps** (sorted numerically on deserialize). |
| `Shulker Contents` | map | For shulker box **item** meta: keys are **inventory slot indices** as strings; values are **nested ItemStack maps**. Only applies when block state is a shulker box. |

---

## Persistent data container (PDC)

| Key | Type | Description |
|-----|------|-------------|
| `PDC` | list of strings | Each entry encodes one primitive tag: **`<namespaced_key>:<TYPE>:<value>`** where `TYPE` is one of **`STRING`**, **`INTEGER`**, **`DOUBLE`**, **`LONG`**, **`FLOAT`**, **`SHORT`**, **`BYTE`**, and `namespaced_key` matches the serializer pattern (see `ItemStackSerializer.PDC_PATTERN`). Example: `myplugin:token:STRING:abc`. Prefer keys without ambiguous extra colons. |

---

## Paper data components

| Key | Type | Description |
|-----|------|-------------|
| `Tooltip Style` | string | Adventure `Key` string for `DataComponentTypes.TOOLTIP_STYLE` when supported (`ServerVersion.isTooltipStyleSupported()`). Applied **after** `setItemMeta`. |

---

## Full example (single YAML document)

Below is one **maximal** example showing many keys at once. In real configs you only include the sections you need. Values are illustrative; adjust materials, keys, and MiniMessage to your server and resource pack.

```yaml
# Example root: could be your ItemStack field in a @YamlConfiguration class
ShowcaseItem:
  Material: PLAYER_HEAD
  Amount: 1

  Name: "<gradient:#00AAFF:#FF00AA><bold>Demo Head</bold></gradient>"
  Lore:
    - "<gray>One line of lore"
    - "<#FFAA00>Accent color line"
    - "<italic:false><dark_gray>ID: demo-showcase"

  Enchants:
    - "UNBREAKING:1"

  Item Flags:
    - "HIDE_ENCHANTS"
    - "HIDE_ATTRIBUTES"

  Unbreakable: true

  Custom Model Data: 1000
  Custom Model: "my_plugin:items/showcase_head"

  Attributes:
    - "generic.max_health:2.0:ADD_NUMBER:MAINHAND"

  UUID: "550e8400-e29b-41d4-a716-446655440000"
  Texture: "http://textures.minecraft.net/texture/3b60fd6134e4a6184c93e4390e2ff581143d00b5c18a76b2f6a1939e8ec3658"

  PDC:
    - "myplugin:demo_flag:STRING:showcase"
    - "myplugin:demo_int:INTEGER:7"

  Tooltip Style: "minecraft:tooltip_style/custom"

---

# Other item types (normally separate items; shown here as separate roots for clarity)

HealingPotion:
  Material: POTION
  Amount: 3
  Name: "<red>Strong Heal"
  Potion: "STRONG_HEALING"

LeatherChest:
  Material: LEATHER_CHESTPLATE
  Color: "#3366FF"
  Name: "<blue>Team Cape"

EnchantedBook:
  Material: ENCHANTED_BOOK
  Stored Enchants:
    - "MENDING:1"
    - "UNBREAKING:3"

FireworkRocket:
  Material: FIREWORK_ROCKET
  Power: 2
  Firework Effects:
    "0":
      Type: BALL_LARGE
      Colors: [ "#FF0000", "#00FF00" ]
      Fade Colors: [ "#FFFFFF" ]
      Flicker: true
      Trail: true
    "1":
      Type: BURST
      Colors: [ "#FFFF00" ]

FireworkStar:
  Material: FIREWORK_STAR
  Firework Effect:
    Type: CREEPER
    Colors: [ "#00FF00" ]
    Trail: true

Banner:
  Material: WHITE_BANNER
  Name: "<gold>Guild Banner"
  Banner Patterns:
    - "RED:stripe_center"
    - "BLUE:circle"
    - "YELLOW:cross"

MapItem:
  Material: FILLED_MAP
  Map ID: 0
  Map Locked: false
  Map Scaling: true
  Location Name: "Spawn Map"
  Map Color: "#AA4444"

AxolotlBucket:
  Material: AXOLOTL_BUCKET
  Axolotl Variant: "WILD"

BundleExample:
  Material: BUNDLE
  Name: "<green>Starter Kit"
  Bundle Items:
    "0":
      Material: COOKED_BEEF
      Amount: 16
    "1":
      Material: IRON_PICKAXE
      Name: "<gray>Kit Pick"
      Enchants:
        - "EFFICIENCY:2"

ShulkerPortable:
  Material: SHULKER_BOX
  Name: "<light_purple>Portable Storage"
  Shulker Contents:
    "0":
      Material: DIAMOND
      Amount: 4
    "1":
      Material: GOLD_INGOT
      Amount: 32
    "13":
      Material: PLAYER_HEAD
      Texture: "http://textures.minecraft.net/texture/3b60fd6134e4a6184c93e4390e2ff581143d00b5c18a76b2f6a1939e8ec3658"
```

---

## Implementation reference

Source of truth for behavior and edge cases: [`ItemStackSerializer.java`](../VortexCore/src/main/java/net/vortexdevelopment/vortexcore/vinject/serializer/ItemStackSerializer.java).

Skull keys are produced/consumed by `SkullProfiles` (`PaperSkullProfileService` / `SpigotSkullProfileService`).
