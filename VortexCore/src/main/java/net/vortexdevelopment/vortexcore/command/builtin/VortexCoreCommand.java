package net.vortexdevelopment.vortexcore.command.builtin;

import net.vortexdevelopment.vortexcore.command.annotation.BaseCommand;
import net.vortexdevelopment.vortexcore.command.annotation.Sender;
import net.vortexdevelopment.vortexcore.command.annotation.SubCommand;
import org.bukkit.command.CommandSender;

//@Command("vortexcore")
//@Permission("vortexcore.admin")
public class VortexCoreCommand {

    @BaseCommand
    public void onBaseCommand(@Sender CommandSender sender) {
        // This method is called when the base command is executed
        // You can add your logic here
    }

    @SubCommand("database export")
    public void onExportCommand(@Sender CommandSender sender) {
        //TODO make database export/import
    }

    @SubCommand("database import")
    public void onImportCommand(@Sender CommandSender sender) {
        //TODO make database export/import
    }
}
