package io.minimum.minecraft.superbvote.votes;

import com.vexsoftware.votifier.model.VotifierEvent;
import io.minimum.minecraft.superbvote.SuperbVote;
import io.minimum.minecraft.superbvote.commands.SuperbVoteCommand;
import io.minimum.minecraft.superbvote.configuration.message.MessageContext;
import io.minimum.minecraft.superbvote.storage.MysqlVoteStorage;
import io.minimum.minecraft.superbvote.util.BrokenNag;
import io.minimum.minecraft.superbvote.util.Configuration;
import io.minimum.minecraft.superbvote.util.PlayerVotes;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.PluginEnableEvent;

import java.util.Date;
import java.util.List;
import java.util.logging.Level;

public class SuperbVoteListener implements Listener {

    @EventHandler
    public void onVote(final VotifierEvent event) {
        if (SuperbVote.getPlugin().getConfiguration().isConfigurationError()) {
            SuperbVote.getPlugin().getLogger().severe("Refusing to process vote because your configuration is invalid. Please check your logs.");
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(SuperbVote.getPlugin(), () -> {
            OfflinePlayer op = Bukkit.getOfflinePlayer(event.getVote().getUsername());
            String worldName = null;

            if (op.isOnline()) {
                worldName = op.getPlayer().getWorld().getName();
            }

            PlayerVotes pvCurrent = SuperbVote.getPlugin().getVoteStorage().getVotes(op.getUniqueId());
            PlayerVotes pv = new PlayerVotes(op.getUniqueId(), op.getName(), pvCurrent.getVotes() + 1, PlayerVotes.Type.FUTURE);

            Vote vote = new Vote(op.getName(), op.getUniqueId(), event.getVote().getServiceName(),
                    event.getVote().getAddress().equals(SuperbVoteCommand.FAKE_HOST_NAME_FOR_VOTE), worldName, new Date());

            if (!vote.isFakeVote()) {
                if (SuperbVote.getPlugin().getVoteServiceCooldown().triggerCooldown(vote)) {
                    SuperbVote.getPlugin().getLogger().log(Level.WARNING, "Ignoring vote from " + vote.getName() + " (service: " +
                            vote.getServiceName() + ") due to service cooldown.");
                    return;
                }
            }

            boolean queue;

            if (SuperbVote.getPlugin().getConfig().getBoolean("claim.enabled")) {
                if (op.isOnline())
                    queue = SuperbVote.getPlugin().getConfig().getBoolean("claim.online");
                else queue = true;
            } else
                queue = (!op.isOnline() && SuperbVote.getPlugin().getConfiguration().requirePlayersOnline());

            // If online, process votes already, if offline, store them.
            // --> If claim is enabled and is set to online: true, store them anyway.
            SuperbVote.getPlugin().getVoteService().processVote(pv, vote,
                    SuperbVote.getPlugin().getConfig().getBoolean("broadcast.enabled"),
                    queue,
                    false,
                    true);
        });
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (SuperbVote.getPlugin().getConfiguration().isConfigurationError()) {
            if (event.getPlayer().hasPermission("superbvote.admin")) {
                Player player = event.getPlayer();
                Bukkit.getScheduler().runTaskLater(SuperbVote.getPlugin(), () -> BrokenNag.nag(player), 40);
            }
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(SuperbVote.getPlugin(), () -> {

            // Update names in MySQL, if it is being used.
            if (SuperbVote.getPlugin().getVoteStorage() instanceof MysqlVoteStorage) {
                ((MysqlVoteStorage) SuperbVote.getPlugin().getVoteStorage()).updateName(event.getPlayer());
            }

            // Process queued votes.
            PlayerVotes pv = SuperbVote.getPlugin().getVoteStorage().getVotes(event.getPlayer().getUniqueId());
            List<Vote> votes = SuperbVote.getPlugin().getQueuedVotes().getVotes(event.getPlayer().getUniqueId());

            if (!votes.isEmpty()) {

                // Disable if we're using a claim system
                if (!SuperbVote.getPlugin().getConfig().getBoolean("claim.enabled")) {

                    // Remove them this time
                    votes = SuperbVote.getPlugin().getQueuedVotes().getAndRemoveVotes(event.getPlayer().getUniqueId());

                    for (Vote vote : votes) {
                        SuperbVote.getPlugin().getVoteService().processVote(pv, vote, false, false, true, true);
                        pv = new PlayerVotes(pv.getUuid(), event.getPlayer().getName(), pv.getVotes() + 1, PlayerVotes.Type.CURRENT);
                    }

                    SuperbVote.getPlugin().getVoteService().afterVoteProcessing();
                } else {
                    // Remind them there are unclaimed rewards waiting for them.
                    // Using a Configuration util for easier string and matrix loading.
                    // Todo Replace with SuperbVoteConfiguration later to remove useless utility classes.

                    Configuration cfg = new Configuration(SuperbVote.getPlugin(), "config");
                    event.getPlayer().sendMessage(cfg.getColoredMessage("claim.reminder")
                            .replaceAll("%votes%", String.valueOf(votes.size())));
                }
            }

            // Remind players to vote.
            if (SuperbVote.getPlugin().getConfig().getBoolean("vote-reminder.on-join") &&
                    event.getPlayer().hasPermission("superbvote.notify") &&
                    !SuperbVote.getPlugin().getVoteStorage().hasVotedToday(event.getPlayer().getUniqueId())) {

                MessageContext context = new MessageContext(null, pv, event.getPlayer());
                SuperbVote.getPlugin().getConfiguration().getReminderMessage().sendAsReminder(event.getPlayer(), context);
            }
        });
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        if (event.getPlugin().getName().equals("PlaceholderAPI")) {
            SuperbVote.getPlugin().getLogger().info("Using clip's PlaceholderAPI to provide extra placeholders.");
        }
    }
}
