package net.vortexdevelopment.vortexcore.text;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class MiniMessagePlaceholder {

    @Getter
    private final String placeholder;
    @Getter
    private final String value;

    public MiniMessagePlaceholder(String placeholder, String value) {
        this.placeholder = placeholder;
        this.value = AdventureUtils.replaceLegacy(value);
    }

    public MiniMessagePlaceholder(String placeholder, Number value) {
        this.placeholder = placeholder;
        this.value = String.valueOf(value);
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

    @Override
    public String toString() {
        return "MiniMessagePlaceholder{" +
                "placeholder='" + placeholder + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
