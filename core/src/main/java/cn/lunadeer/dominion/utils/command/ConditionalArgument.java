package cn.lunadeer.dominion.utils.command;

import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ConditionalArgument extends Argument {

    private final Map<Integer, String> conditionArguments = new HashMap<>();

    public abstract List<String> handelCondition(CommandSender sender);

    public ConditionalArgument(String name, List<Integer> conditionArgumentsIndex) {
        super(name, true);
        this.setSuggestion(this::handelCondition);
        for (Integer index : conditionArgumentsIndex) {
            this.conditionArguments.put(index, null);
        }
    }

    public ConditionalArgument setConditionArguments(Integer index, String value) {
        if (!conditionArguments.containsKey(index)) {
            throw new IllegalArgumentException("Index out of range.");
        }
        conditionArguments.put(index, value);
        return this;
    }

    public Map<Integer, String> getConditionArguments() {
        return conditionArguments;
    }

    public List<Integer> getConditionArgumentsIndex() {
        return new ArrayList<>(conditionArguments.keySet());
    }

    @Override
    public ConditionalArgument copy() {
        // create a new instance of ConditionalArgument with the same name and condition arguments
        ConditionalArgument copy = new ConditionalArgument(getName(), new ArrayList<>(this.conditionArguments.keySet())) {
            @Override
            public List<String> handelCondition(CommandSender sender) {
                return ConditionalArgument.this.handelCondition(sender);
            }
        };
        copy.setValue(this.getValue());
        // copy the condition arguments
        copy.conditionArguments.putAll(this.conditionArguments);
        return copy;
    }
}
