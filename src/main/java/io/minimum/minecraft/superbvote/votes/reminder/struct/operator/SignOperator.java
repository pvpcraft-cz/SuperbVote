package io.minimum.minecraft.superbvote.votes.reminder.struct.operator;

import io.minimum.minecraft.superbvote.votes.reminder.struct.TernaryFunction;
import jdk.internal.joptsimple.internal.Strings;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class SignOperator implements TernaryFunction<Boolean, Object, Object> {

    private final static Map<String, SignOperator> validOperators = new HashMap<String, SignOperator>() {{
        put("=", new SignOperator() {
            @Override
            public Boolean apply(Object input1, Object input2) {
                return input1.equals(input2);
            }
        });

        put("!=", new SignOperator() {
            @Override
            public Boolean apply(Object input1, Object input2) {
                return !input1.equals(input2);
            }
        });

        put(">", new SignOperator() {
            @Override
            public Boolean apply(Object input1, Object input2) {
                if (input1 instanceof Number && input2 instanceof Number) {
                    return ((Number) input1).floatValue() > ((Number) input2).floatValue();
                }
                return false;
            }
        });

        put("<", new SignOperator() {
            @Override
            public Boolean apply(Object input1, Object input2) {
                if (input1 instanceof Number && input2 instanceof Number) {
                    return ((Number) input1).floatValue() < ((Number) input2).floatValue();
                }
                return false;
            }
        });

        put(">=", new SignOperator() {
            @Override
            public Boolean apply(Object input1, Object input2) {
                if (input1 instanceof Number && input2 instanceof Number) {
                    return ((Number) input1).floatValue() >= ((Number) input2).floatValue();
                }
                return false;
            }
        });

        put("<=", new SignOperator() {
            @Override
            public Boolean apply(Object input1, Object input2) {
                if (input1 instanceof Number && input2 instanceof Number) {
                    return ((Number) input1).floatValue() <= ((Number) input2).floatValue();
                }
                return false;
            }
        });
    }};

    @Nullable
    public static OperatorWrapper fromString(String str) {
        if (Strings.isNullOrEmpty(str)) return null;

        for (String key : validOperators.keySet()) {
            if (str.contains(key))
                return new OperatorWrapper(key, validOperators.get(key));
        }
        return null;
    }

    public static SignOperator fromSign(String sign) {
        return validOperators.get(sign);
    }

    @Override
    public Boolean apply(Object input1, Object input2) {
        return false;
    }
}
