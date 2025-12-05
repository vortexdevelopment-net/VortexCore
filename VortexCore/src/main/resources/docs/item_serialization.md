# Item Serialization System

The VortexCore item serialization system allows you to convert ItemStacks to and from configuration sections in a human-readable format. This makes it easy to store and load items in configuration files.

## Using the System

### Serializing an Item

To serialize an ItemStack to a configuration section:

```java
// Get your ItemStack
ItemStack item = /* your item */;

// Create or get a configuration section
ConfigurationSection section = config.createSection("item");

// Serialize the item
Config config = new Config("your_config.yml");
config.writeItem(item, section);

// Or use the registry directly
ItemSerializerRegistry.serialize(item, section);

// Save the configuration
config.save();
```

### Deserializing an Item

To deserialize an ItemStack from a configuration section:

```java
// Get the configuration section
ConfigurationSection section = config.getConfigurationSection("item");

// Deserialize the item
Config config = new Config("your_config.yml");
ItemStack item = config.readItem(section);

// Or use the registry directly
ItemStack item = ItemSerializerRegistry.deserialize(section);
```

## Supported Item Properties

The following item properties are supported by the serialization system:

### Basic Properties
- **Material** - The material type of the item
- **Name** - The display name of the item
- **Lore** - The lore (description) of the item
- **Amount** - The stack size of the item
- **Durability** - The damage/durability of the item

### Advanced Properties
- **Enchantments** - Item enchantments in the format "ENCHANTMENT:LEVEL" (e.g., "SHARPNESS:5")
- **ItemFlags** - Item flags (e.g., "HIDE_ENCHANTS", "HIDE_ATTRIBUTES")
- **CustomModelData** - Custom model data for resource packs
- **Unbreakable** - Whether the item is unbreakable

## Example Configuration

Here's an example of how an item might look in a YAML configuration file:

```yaml
item:
  Material: DIAMOND_SWORD
  Name: "§6Legendary Sword"
  Lore:
    - "§7A powerful sword forged"
    - "§7in the depths of the nether"
  Enchantments:
    - "DAMAGE_ALL:5"
    - "FIRE_ASPECT:2"
    - "DURABILITY:3"
  ItemFlags:
    - "HIDE_ATTRIBUTES"
    - "HIDE_ENCHANTS"
  CustomModelData: 1234
  Unbreakable: true
  Amount: 1
```

## Creating Custom Serializers

You can create custom serializers for additional item properties by extending the appropriate base serializer class:

- `ItemSerializer<T>` - Base class for all serializers
- `StringSerializer` - For string values
- `IntegerSerializer` - For integer values
- `BooleanSerializer` - For boolean values
- `StringListSerializer` - For lists of strings

Then register your serializer with the registry:

```java
ItemSerializerRegistry.register(new YourCustomSerializer());
```

See the existing serializers in the `net.vortexdevelopment.vortexcore.config.serializer.item` package for examples.