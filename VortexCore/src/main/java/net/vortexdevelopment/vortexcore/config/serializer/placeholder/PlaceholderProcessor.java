package net.vortexdevelopment.vortexcore.config.serializer.placeholder;

public interface PlaceholderProcessor {

    String process(String input);

    static PlaceholderProcessor defaultProcessor() {
        return input -> input;
    }
}
