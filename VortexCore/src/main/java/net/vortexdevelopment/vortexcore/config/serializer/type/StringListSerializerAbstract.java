package net.vortexdevelopment.vortexcore.config.serializer.type;

import net.vortexdevelopment.vortexcore.config.serializer.AbstractItemSerializer;

import java.util.List;

public abstract class StringListSerializerAbstract extends AbstractItemSerializer<List<String>> {
    public StringListSerializerAbstract(String path) {
        super(path);
    }
}
