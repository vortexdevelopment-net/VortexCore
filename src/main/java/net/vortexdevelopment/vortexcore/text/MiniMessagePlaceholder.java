package net.vortexdevelopment.vortexcore.text;

import java.util.List;

public class MiniMessagePlaceholder {

    private final String placeholder;
    private final String value;

    public MiniMessagePlaceholder(String placeholder, String value) {
        this.placeholder = placeholder;
        this.value = value;
    }

    public MiniMessagePlaceholder(String placeholder, Number value) {
        this.placeholder = placeholder;
        this.value = String.valueOf(value);
    }

    public String getPlaceholder() {
        return this.placeholder;
    }

    public String getValue() {
        return this.value;
    }

    public String replace(String string) {
        return string.replaceAll("<" + this.placeholder + ">", this.value);
    }

    public List<String> replace(List<String> list) {
        List<String> newList = new java.util.ArrayList<>();
        for (String line : list) {
            newList.add(replace(line));
        }
        return newList;
    }
}
