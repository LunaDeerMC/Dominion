package cn.lunadeer.dominion.utils.command;

import java.util.List;

/**
 * Represents an option argument with a list of possible values.
 * This class extends the Argument class and provides specific functionality for options.
 * <p>
 * Mostly used for boolean, enum, or other fixed value options.
 */
public class Option extends Argument {
    private final List<String> options;

    /**
     * Constructs an Option with the specified list of options.(required)
     *
     * @param options the list of possible values for this option
     */
    public Option(List<String> options) {
        super(options.get(0), true);
        this.options = options;
    }

    /**
     * Constructs an optional Option with the specified list of options and default value.
     *
     * @param options      the list of possible values for this option
     * @param defaultValue the default value for this option
     */
    public Option(List<String> options, String defaultValue) {
        super(options.get(0), false);
        this.options = options;
        setValue(defaultValue);
    }

    /**
     * Returns the suggestions for this option.
     *
     * @return a Suggestion containing the list of possible values for this option
     */
    @Override
    public Suggestion getSuggestion() {
        return (commandSender) -> options;
    }

    /**
     * Returns a string representation of this option.
     * The string representation is a list of possible values enclosed in angle brackets and separated by a pipe character.
     *
     * @return a string representation of this option
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (isRequired()) {
            sb.append("<");
        } else {
            sb.append("[");
        }
        for (int i = 0; i < options.size(); i++) {
            sb.append(options.get(i));
            if (i != options.size() - 1) {
                sb.append("|");
            }
        }
        if (isRequired()) {
            sb.append(">");
        } else {
            sb.append("]");
        }
        return sb.toString();
    }

    /**
     * Returns the list of possible values for this option.
     *
     * @return the list of possible values for this option
     */
    public List<String> getOptions() {
        return options;
    }

    @Override
    public Option copy() {
        Option copy = new Option(options, getValue());
        copy.setValue(this.getValue());
        return copy;
    }
}
