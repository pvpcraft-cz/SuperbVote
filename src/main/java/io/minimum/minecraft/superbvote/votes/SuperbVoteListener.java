package io.minimum.minecraft.superbvote.votes;

import com.vexsoftware.votifier.model.VotifierEvent;
import io.minimum.minecraft.superbvote.SuperbVote;
import io.minimum.minecraft.superbvote.commands.SuperbVoteCommand;
import io.minimum.minecraft.superbvote.configuration.message.MessageContext;
import io.minimum.minecraft.superbvote.storage.MysqlVoteStorage;
import io.minimum.minecraft.superbvote.util.PlayerVotes;
import io.minimum.minecraft.superbvote.votes.rewards.VoteReward;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Date;
import java.util.List;
import java.util.logging.Level;

public class SuperbVoteListener implements Listener {
    @EventHandler
    public void onVote(final VotifierEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(SuperbVote.getPlugin(), () -> {
            OfflinePlayer op = Bukkit.getPlayerExact(event.getVote().getUsername());
            String worldName = null;
            if (op.isOnline()) {
                worldName = op.getPlayer().getWorld().getName();
            }

            PlayerVotes pvCurrent = SuperbVote.getPlugin().getVoteStorage().getVotes(op.getUniqueId());
            PlayerVotes pv = new PlayerVotes(op.getUniqueId(), pvCurrent.getVotes() + 1, PlayerVotes.Type.FUTURE);
            Vote vote = new Vote(op.getName(), op.getUniqueId(), event.getVote().getServiceName(),
                    event.getVote().getAddress().equals(SuperbVoteCommand.FAKE_HOST_NAME_FOR_VOTE), worldName, new Date());

            if (!vote.isFakeVote()) {
                if (SuperbVote.getPlugin().getCooldownHandler().triggerCooldown(vote)) {
                    SuperbVote.getPlugin().getLogger().log(Level.WARNING, "Ignoring vote from " + vote.getName() + " (service: " +
                            vote.getServiceName() + ") due to service cooldown.");
                    return;
                }
            }

            processVote(pv, vote, SuperbVote.getPlugin().getConfig().getBoolean("broadcast.enabled"),
                    !op.isOnline() && SuperbVote.getPlugin().getConfiguration().requirePlayersOnline(),
                    false);
        });
    }

    private void processVote(PlayerVotes pv, Vote vote, boolean broadcast, boolean queue, boolean wasQueued) {
        List<VoteReward> bestRewards = SuperbVote.getPlugin().getConfiguration().getBestRewards(vote, pv);
        SuperbPreVoteEvent preVoteEvent = new SuperbPreVoteEvent(vote, bestRewards);
        if (queue) {
            preVoteEvent.setResult(SuperbPreVoteEvent.Result.QUEUE_VOTE);
        }
        Bukkit.getPluginManager().callEvent(preVoteEvent);

        MessageContext context = new MessageContext(vote, pv, Bukkit.getOfflinePlayer(vote.getUuid()));

        switch (preVoteEvent.getResult()) {
            case PROCESS_VOTE:
                if (preVoteEvent.getVoteRewards().isEmpty()) {
                    throw new RuntimeException("No vote reward found for '" + vote + "'");
                }

                if (!vote.isFakeVote() || SuperbVote.getPlugin().getConfig().getBoolean("votes.process-fake-votes")) {
                    SuperbVote.getPlugin().getVoteStorage().issueVote(vote);
                }

                if (!wasQueued) {
                    for (VoteReward reward : preVoteEvent.getVoteRewards()) {
                        reward.broadcastVote(context, SuperbVote.getPlugin().getConfig().getBoolean("broadcast.message-player"), broadcast);
                    }
                }

                Bukkit.getScheduler().runTask(SuperbVote.getPlugin(), () -> Bukkit.getPluginManager().callEvent(new SuperbVoteEvent(vote, preVoteEvent.getVoteRewards())));
                break;
            case QUEUE_VOTE:
                SuperbVote.getPlugin().getLogger().log(Level.WARNING, "Queuing vote from " + vote.getName() + " to be run later");
                for (VoteReward reward : preVoteEvent.getVoteRewards()) {
                    reward.broadcastVote(context, false, broadcast && SuperbVote.getPlugin().getConfig().getBoolean("broadcast.queued"));
                }
                SuperbVote.getPlugin().getQueuedVotes().addVote(vote);
                break;
            case CANCEL:
                SuperbVote.getPlugin().getLogger().log(Level.WARNING, "Vote from " + vote.getName() + " cancelled (event)");
                break;
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(SuperbVote.getPlugin(), () -> {
            // Update names in MySQL, if it is being used.
            if (SuperbVote.getPlugin().getVoteStorage() instanceof MysqlVoteStorage) {
                ((MysqlVoteStorage) SuperbVote.getPlugin().getVoteStorage()).updateName(event.getPlayer());
            }

            // Process queued votes.
            PlayerVotes dummyPv = new PlayerVotes(event.getPlayer().getUniqueId(), 0, PlayerVotes.Type.CURRENT); // we don't broadcast/send messages for queued votes
            List<Vote> votes = SuperbVote.getPlugin().getQueuedVotes().getAndRemoveVotes(event.getPlayer().getUniqueId());
            votes.forEach(v -> processVote(dummyPv, v, false, false, true));

            // Remind players to vote.
            if (SuperbVote.getPlugin().getConfig().getBoolean("vote-reminder.on-join") &&
                    event.getPlayer().hasPermission("superbvote.notify") &&
                    !SuperbVote.getPlugin().getVoteStorage().hasVotedToday(event.getPlayer().getUniqueId())) {
                MessageContext context = new MessageContext(null,
                        SuperbVote.getPlugin().getVoteStorage().getVotes(event.getPlayer().getUniqueId()),
                        event.getPlayer());
                SuperbVote.getPlugin().getConfiguration().getReminderMessage().sendAsReminder(event.getPlayer(), context);
            }
        });
    }
}
