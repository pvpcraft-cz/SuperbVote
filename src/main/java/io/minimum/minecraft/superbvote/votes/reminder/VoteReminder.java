package io.minimum.minecraft.superbvote.votes.reminder;

import io.minimum.minecraft.superbvote.SuperbVote;
import io.minimum.minecraft.superbvote.configuration.message.MessageContext;
import io.minimum.minecraft.superbvote.configuration.message.PlainStringMessage;
import io.minimum.minecraft.superbvote.util.PlayerVotes;
import io.minimum.minecraft.superbvote.votes.reminder.struct.condition.PlaceholderCondition;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class VoteReminder implements Runnable {

    private final SuperbVote plugin;

    private final Predicate<Player> condition;
    private final List<String> commands = new ArrayList<>();

    private BukkitTask task;

    public VoteReminder(SuperbVote plugin, String condition) {
        this.plugin = plugin;
        this.condition = new PlaceholderCondition(condition);
    }

    public void stop() {
        if (task == null) return;
        task.cancel();
        task = null;
    }

    public void start(int interval) {
        if (task != null) stop();

        task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this, 20 * interval, 20 * interval);
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {

            if (!player.hasPermission("superbvote.notify"))
                continue;

            run(player);
        }
    }

    public void run(Player player, MessageContext context) {

        if (!condition.test(player))
            return;

        sendMessage(player, context);

        for (String command : commands) {
            PlainStringMessage cmd = new PlainStringMessage(command);
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), cmd.getReplaced(context)));
        }
    }

    public void run(Player player) {
        PlayerVotes playerVotes = plugin.getVoteStorage().getVotes(player.getUniqueId());
        MessageContext context = new MessageContext(null, playerVotes, player);

        run(player, context);
    }

    public void sendMessage(Player player, MessageContext context) {
        plugin.getConfiguration().getReminderMessage().sendAsReminder(player, context);
    }

    public void setCommands(List<String> commands) {
        this.commands.clear();
        this.commands.addAll(commands);
    }
}
