# VortexCore

[![Build Status](https://img.shields.io/github/actions/workflow/status/vortexdevelopment-net/VortexCore/ci.yml?branch=main)](https://github.com/VortexDevelopment/VortexCore/actions)
[![License](https://img.shields.io/github/license/vortexdevelopment-net/VortexCore)](./LICENSE)

A modern Minecraft development framework built on the Paper API, designed to simplify plugin development and provide powerful tools for server administrators.

## Features

- Modern Paper API integration
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
- Paper API 1.21.3 or higher
- Maven 3.6.0 or higher

## Installation

1. Clone the repository
2. Build the project using Maven:
```bash
mvn clean install
```

### Using VortexCore as a Dependency

Add the Vortexdevelopment repository to your `pom.xml`:

```xml
<repository>
    <id>vortex-repo</id>
    <url>https://repo.vortexdevelopment.net/repository/maven-public/</url>
</repository>
```

Then add the dependency:

```xml
<dependency>
    <groupId>net.vortexdevelopment</groupId>
    <artifactId>VortexCore</artifactId>
    <version>1.0.1</version>
    <scope>compile</scope>
</dependency>
```

### Plugin.yml Transformer Setup

For proper plugin.yml handling in child projects, add the following to your `pom.xml`:

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
                @TemplateDependency(groupId = "net.vortexdevelopment", artifactId = "VortexCore", version = "1.0.0-SNAPSHOT")
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
@Command(value = "pm", aliases = {"msg", "tell"}) //Registers the command /pm with aliases /msg and /tell
@Permission("pm.use") //Requires the permission pm.use to execute
public class PrivateMessageCommand {

    @BaseCommand
    public void baseCommand(@Sender CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "Usage: /pm <player> <message>");
    }

    @SubCommand(command = "{player} {**}") // {**} means the rest of the command
    public void sendPrivateMessage(
            @Param("player") Player recipient, 
            @Param("**") String message, 
            @Sender CommandSender sender //Annotated parameters can be placed in any order
    ) {
        if (message.trim().isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Your message cannot be empty.");
            return;
        }

        String senderName = sender instanceof Player ? ((Player) sender).getDisplayName() : "Console";

        // Format for the sender
        sender.sendMessage(ChatColor.GRAY + "[" + ChatColor.GREEN + "You" + ChatColor.GRAY + " -> " +
                ChatColor.GREEN + recipient.getDisplayName() + ChatColor.GRAY + "] " +
                ChatColor.WHITE + message);

        // Format for the recipient
        recipient.sendMessage(ChatColor.GRAY + "[" + ChatColor.GREEN + senderName + ChatColor.GRAY + " -> " +
                ChatColor.GREEN + "You" + ChatColor.GRAY + "] " +
                ChatColor.WHITE + message);
    }

    @SubCommand(command = "all {**}" )
    @Permission("pm.all") //Requires the permission pm.all to execute
    public void sendMessageToAll(@Sender CommandSender sender, @Param("**") String message) {
        if (message.trim().isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Your message cannot be empty.");
            return;
        }

        String fullMessage = ChatColor.GRAY + "[" + ChatColor.GREEN + "Broadcast" + ChatColor.GRAY + "] " +
                ChatColor.WHITE + message;

        Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage(fullMessage));
    }

    //Registers a tab completion for the {player} parameter
    @TabComplete(param = "player")
    public List<String> tabCompletePlayer(@Sender Player player/*, @Args String[] args, @TabIndex int index*/) {
        // Only suggest players that aren't the sender
        return Bukkit.getOnlinePlayers().stream()
                .filter(p -> !p.equals(player))
                .map(Player::getName)
                .toList();
    }
}
```

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

For support, please open an issue in the GitHub repository or contact the development team on [discord](https://dc.vortexdevelopment.net) 
`
