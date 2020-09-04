package io.minimum.minecraft.superbvote.votes.reminder.struct.operator;

import com.google.common.base.Strings;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiPredicate;

public class SignOperator implements BiPredicate<Object, Object> {

    private final static Map<String, SignOperator> validOperators = new HashMap<String, SignOperator>() {{
        put("=", new SignOperator() {
            @Override
            public boolean test(Object input1, Object input2) {
                return input1.equals(input2);
            }
        });

        put("!=", new SignOperator() {
            @Override
            public boolean test(Object input1, Object input2) {
                return !input1.equals(input2);
            }
        });

        put(">", new SignOperator() {
            @Override
            public boolean test(Object input1, Object input2) {
                if (input1 instanceof Number && input2 instanceof Number) {
                    return ((Number) input1).floatValue() > ((Number) input2).floatValue();
                }
                return false;
            }
        });

        put("<", new SignOperator() {
            @Override
            public boolean test(Object input1, Object input2) {
                if (input1 instanceof Number && input2 instanceof Number) {
                    return ((Number) input1).floatValue() < ((Number) input2).floatValue();
                }
                return false;
            }
        });

        put(">=", new SignOperator() {
            @Override
            public boolean test(Object input1, Object input2) {
                if (input1 instanceof Number && input2 instanceof Number) {
                    return ((Number) input1).floatValue() >= ((Number) input2).floatValue();
                }
                return false;
            }
        });

        put("<=", new SignOperator() {
            @Override
            public boolean test(Object input1, Object input2) {
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

    @Override
    public boolean test(Object input1, Object input2) {
        return true;
    }

    public static SignOperator fromSign(String sign) {
        return validOperators.get(sign);
    }
}
