package io.minimum.minecraft.superbvote.votes.reminder;

import io.minimum.minecraft.superbvote.SuperbVote;
import io.minimum.minecraft.superbvote.configuration.message.MessageContext;
import io.minimum.minecraft.superbvote.configuration.message.PlainStringMessage;
import io.minimum.minecraft.superbvote.votes.reminder.struct.condition.PlaceholderCondition;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

// Too lazy adding something to VoteReminder, this was faster and easier.
public class JoinReminder {

    private final SuperbVote plugin;

    private final List<String> commands = new ArrayList<>();
    private final Predicate<Player> condition;

    public JoinReminder(SuperbVote plugin, String conditionString) {
        this.plugin = plugin;
        this.condition = new PlaceholderCondition(conditionString);
    }

    public void addCommands(List<String> commands) {
        this.commands.addAll(commands);
    }

    public void run(Player player, MessageContext messageContext) {

        if (!condition.test(player))
            return;

        for (String command : commands) {
            PlainStringMessage cmd = new PlainStringMessage(command);
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), cmd.getReplaced(messageContext)));
        }
    }
}