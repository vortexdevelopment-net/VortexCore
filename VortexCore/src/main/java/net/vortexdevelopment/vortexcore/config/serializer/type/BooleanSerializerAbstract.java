package net.vortexdevelopment.vortexcore.config.serializer.type;

import net.vortexdevelopment.vortexcore.config.serializer.AbstractItemSerializer;

/**
 * Base serializer for boolean values.
 */
public abstract class BooleanSerializerAbstract extends AbstractItemSerializer<Boolean> {

    /**
     * Constructor for BooleanSerializerAbstract.
     *
     * @param path The path to the boolean value in the configuration
     */
    public BooleanSerializerAbstract(String path) {
        super(path);
    }

    /**
     * Constructor for BooleanSerializerAbstract with priority.
     *
     * @param path The path to the boolean value in the configuration
     * @param priority The priority of this serializer (lower is processed first)
     */
    public BooleanSerializerAbstract(String path, int priority) {
        super(path, priority);
    }
}