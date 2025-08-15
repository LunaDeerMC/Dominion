package cn.lunadeer.dominion.utils.command;

import java.util.List;
import java.util.Objects;

public class Argument {
    private final String name;
    private final boolean required;
    private Suggestion suggestion = null;
    private String value = "";

    public Argument copy() {
        Argument copy = new Argument(name, required, suggestion);
        copy.value = this.value;
        return copy;
    }

    /**
     * Constructs an Argument with the specified name, required status, and default value.
     *
     * @param name     The name of the argument.
     * @param required Whether the argument is required.
     */
    public Argument(String name, boolean required) {
        this(name, required, null);
    }

    /**
     * Constructs an Option Argument with the specified name and default value.
     *
     * @param name         The name of the argument.
     * @param defaultValue The default value of the argument.
     */
    public Argument(String name, String defaultValue) {
        this(name, false, null);
        this.value = defaultValue;
    }

    public Argument(String name, boolean required, Suggestion suggestion) {
        this.name = name;
        this.required = required;
        this.suggestion = suggestion;
    }

    public String getName() {
        return name;
    }

    public boolean isRequired() {
        return required;
    }

    public Suggestion getSuggestion() {
        return Objects.requireNonNullElseGet(suggestion, () -> (sender, preArguments) -> List.of(Argument.this.toString()));
    }

    public void setSuggestion(Suggestion suggestion) {
        this.suggestion = suggestion;
    }

    public String toString() {
        if (required) {
            return "<" + name + ">";
        } else {
            return "[" + name + "]";
        }
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}
