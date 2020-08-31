package io.minimum.minecraft.superbvote.votes.reminder.struct.condition;

import org.bukkit.entity.Player;

import java.util.function.Function;

public interface ReminderCondition extends Function<Player, Boolean> {
}