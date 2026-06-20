# VortexCore

[![Build Status](https://img.shields.io/github/actions/workflow/status/vortexdevelopment-net/VortexCore/ci.yml?branch=main)](https://github.com/VortexDevelopment/VortexCore/actions)
[![License](https://img.shields.io/github/license/vortexdevelopment-net/VortexCore)](./LICENSE)

A modern Minecraft development framework built on the Paper API, designed to simplify plugin development and provide powerful tools for server administrators.

## Documentation

- [Documentation index](docs/README.md) - all plugin development guides
- [Agent skill](.cursor/skills/vortex-plugindev/SKILL.md) - compact agent router for plugin tasks
- [AGENTS.md](AGENTS.md) - agent quick start
- [ItemStack YAML serializer](docs/itemstack-yaml-serializer.md) - all YAML keys for `ItemStack` fields in Vinject configs

## Features

- Unified Paper and Spigot runtime support
- Comprehensive plugin management system
- Advanced configuration handling
- Built-in command framework
- Event management system
- Database integration support
- Custom inventory management
- Player data handling
- Multi-language support
- Plugin dependency management
- MiniMessage + Legacy color support at the same time

## Requirements

- Java 17 or higher
- Java 17-compatible Paper or Spigot server
- Maven 3.6.0 or higher

## Installation

1. Clone the repository
2. Build the project using Maven:
```bash
mvn clean install
```

### Using VortexCore as a Dependency

The build publishes one unified `VortexCore` artifact:

- **`VortexCore`**: unified runtime artifact for both Paper and Spigot. It bundles both platform bridges and prefers Paper implementations when Paper APIs are available.

**Adventure on items and GUIs:** In code you still author with MiniMessage / `Component` via `AdventureUtils`. On Paper, names, lore, inventory titles, and `Component` messages use native Adventure Bukkit APIs. On Spigot, the same APIs are backed by **legacy section strings** (`§`); behavior should match for typical text.

Add the Vortexdevelopment repository to your `pom.xml`:

```xml
<repository>
    <id>vortex-repo</id>
    <url>https://repo.vortexdevelopment.net/repository/maven-public/</url>
</repository>
```

Then add the unified runtime dependency:

```xml
<dependency>
    <groupId>net.vortexdevelopment</groupId>
    <artifactId>VortexCore</artifactId>
    <version>2.0.1</version>
    <scope>compile</scope>
</dependency>
```

### Plugin.yml Transformer Setup

For proper `plugin.yml` and `paper-plugin.yml` handling in child projects, add the following to your `pom.xml`:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-shade-plugin</artifactId>
            <version>3.6.0</version>
            <dependencies>
                <dependency>
                    <groupId>net.vortexdevelopment</groupId>
                    <artifactId>MavenYamlTransformer</artifactId>
                    <version>1.0.0</version>
                </dependency>
            </dependencies>
            <executions>
                <execution>
                    <phase>package</phase>
                    <goals>
                        <goal>shade</goal>
                    </goals>
                    <configuration>
                        <transformers>
                            <transformer implementation="net.vortexdevelopment.MavenYamlTransformer">
                                <paths>
                                    <path>plugin.yml</path>
                                    <path>paper-plugin.yml</path>
                                </paths>
                            </transformer>
                        </transformers>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

## Example usage:

```java
package org.example.myplugin;

import net.vortexdevelopment.vinject.annotation.Root;
import net.vortexdevelopment.vinject.annotation.TemplateDependency;
import net.vortexdevelopment.vortexcore.VortexPlugin;

@Root(
        packageName = "org.example.myplugin",
        createInstance = false,
        templateDependencies = {
                //For intellij plugin
                @TemplateDependency(groupId = "net.vortexdevelopment", artifactId = "VortexCore", version = "2.0.1")
        }
)
public final class MyPlugin extends VortexPlugin {

    @Override
    public void onPreComponentLoad() {
    }

    @Override
    public void onPluginLoad() {
        
    }

    @Override
    protected void onPluginEnable() {
    }

    @Override
    protected void onPluginDisable() {

    }
}
```

### Example command

```java
import net.vortexdevelopment.vortexcore.command.annotation.BaseCommand;
import net.vortexdevelopment.vortexcore.command.annotation.Command;
import net.vortexdevelopment.vortexcore.command.annotation.Param;
import net.vortexdevelopment.vortexcore.command.annotation.Permission;
import net.vortexdevelopment.vortexcore.command.annotation.Sender;
import net.vortexdevelopment.vortexcore.command.annotation.SubCommand;
import net.vortexdevelopment.vortexcore.text.MiniMessagePlaceholder;
import net.vortexdevelopment.vortexcore.text.lang.Lang;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Command(value = "pm", aliases = {"msg", "tell"})
@Permission("pm.use")
public class PrivateMessageCommand {

    @BaseCommand
    public void baseCommand(@Sender CommandSender sender) {
        Lang.send(sender, "Commands.Private Message Usage");
    }

    @SubCommand(command = "{player} {**}")
    public void sendPrivateMessage(
            @Param("player") Player recipient,
            @Param("**") String message,
            @Sender CommandSender sender
    ) {
        if (message.trim().isEmpty()) {
            Lang.send(sender, "Commands.Private Message Empty");
            return;
        }
        Lang.send(sender, "Commands.Private Message Sent",
                new MiniMessagePlaceholder("recipient", recipient.getName()),
                new MiniMessagePlaceholder("message", message));
        Lang.send(recipient, "Commands.Private Message Received",
                new MiniMessagePlaceholder("sender", sender.getName()),
                new MiniMessagePlaceholder("message", message));
    }
}
```

See [docs/commands-and-listeners.md](docs/commands-and-listeners.md) and [docs/messaging.md](docs/messaging.md) for full patterns.

### Register a Command Parameter Resolver

```java
import net.vortexdevelopment.vortexcore.command.annotation.Resolver;

@Resolver // Register the parameter resolver
public class EntityTypeResolver implements ParameterResolver<EntityType> {
    @Override
    public EntityType resolve(String input) {
        try {
            EntityType type = EntityType.valueOf(input.toUpperCase());
            return type;
        } catch (IllegalArgumentException e) {
            System.err.println("Failed to resolve entity type: " + input);
            // Try a more forgiving approach
            for (EntityType type : EntityType.values()) {
                if (type.name().equalsIgnoreCase(input)) {
                    System.err.println("Found match using case-insensitive comparison: " + type);
                    return type;
                }
            }
            return null;
        }
    }

    @Override
    public boolean supports(Class<?> type) {
        return EntityType.class.isAssignableFrom(type);
    }

    @Override
    public Set<Class<?>> getSupportedTypes() {
        return Set.of(EntityType.class);
    }
}
```

## IntelliJ Plugin

For even faster development and dependency injection highlighting and suggestion, use the IntelliJ plugin. You can find the plugin repository at [IntelliJ Plugin Repository](https://github.com/vortexdevelopment-net/Vinject-Intellij-Plugin).

Additionally, check out the VInject project for more information on dependency injection. Visit the VInject repository at [VInject Project](https://github.com/vortexdevelopment/VInject).


## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Support

For support, please open an issue in the GitHub repository or contact the development team on [discord](https://dc.vortexdevelopment.net).
