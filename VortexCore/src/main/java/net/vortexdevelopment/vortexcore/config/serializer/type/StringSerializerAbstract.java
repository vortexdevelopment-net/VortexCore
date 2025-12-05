package net.vortexdevelopment.vortexcore.config.serializer.type;

import net.vortexdevelopment.vortexcore.config.serializer.AbstractItemSerializer;

public abstract class StringSerializerAbstract extends AbstractItemSerializer<String> {


    public StringSerializerAbstract(String path) {
        super(path);
    }

    public StringSerializerAbstract(String path, int priority) {
        super(path, priority);
    }


}
