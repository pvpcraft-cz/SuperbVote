package io.minimum.minecraft.superbvote.votes.reminder.struct.condition;

import com.google.common.base.Strings;
import io.minimum.minecraft.superbvote.votes.reminder.ParserUtil;
import io.minimum.minecraft.superbvote.votes.reminder.struct.operator.OperatorWrapper;
import io.minimum.minecraft.superbvote.votes.reminder.struct.operator.SignOperator;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.function.Predicate;

public class PlaceholderCondition implements Predicate<Player> {

    private final String condition;

    public PlaceholderCondition(String condition) {
        this.condition = ParserUtil.parsePlaceholders(condition, null);
    }

    @Override
    public boolean test(Player player) {
        String condition = this.condition;

        condition = ParserUtil.parsePlaceholders(condition, player);

        if (Strings.isNullOrEmpty(condition))
            return true;

        OperatorWrapper wrapper = SignOperator.fromString(condition);

        // No operator, true by default
        if (wrapper == null)
            return true;

        String[] args = condition.split(wrapper.sign());

        if (args.length != 2) {
            Bukkit.getLogger().severe("Invalid condition: " + this.condition);
            return false;
        }

        String leftSide = args[0];
        String rightSide = args[1];

        Object parsedLeft = ParserUtil.parseObject(leftSide);
        Object parsedRight = ParserUtil.parseObject(rightSide);

        return wrapper.operator().test(parsedLeft, parsedRight);
    }

    @Override
    public String toString() {
        return condition;
    }
}