package io.minimum.minecraft.superbvote.votes;

import io.minimum.minecraft.superbvote.SuperbVote;
import io.minimum.minecraft.superbvote.configuration.message.MessageContext;
import io.minimum.minecraft.superbvote.signboard.TopPlayerSignFetcher;
import io.minimum.minecraft.superbvote.util.PlayerVotes;
import io.minimum.minecraft.superbvote.votes.rewards.VoteReward;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.logging.Level;

public class VoteService {
    public void processVote(PlayerVotes pv, Vote vote, boolean broadcast, boolean queue, boolean wasQueued, boolean addVote) {

        List<VoteReward> bestRewards = SuperbVote.getPlugin().getConfiguration().getBestRewards(vote, pv);
        MessageContext context = new MessageContext(vote, pv, Bukkit.getOfflinePlayer(vote.getUuid()));

        if (bestRewards.isEmpty()) {
            throw new RuntimeException("No vote rewards found for '" + vote + "'");
        }

        if (queue) {
            SuperbVote.getPlugin().getLogger().log(Level.INFO, "Queuing vote from " + vote.getName() + " to be run later, but adding a vote for him.");

            for (VoteReward reward : bestRewards) {
                reward.broadcastVote(context, false, broadcast && SuperbVote.getPlugin().getConfig().getBoolean("broadcast.queued"));
            }

            SuperbVote.getPlugin().getQueuedVotes().addVote(vote);
            SuperbVote.getPlugin().getVoteStorage().addVote(vote);

            Bukkit.getScheduler().runTaskAsynchronously(SuperbVote.getPlugin(), this::afterVoteProcessing);
        } else {
            if (!vote.isFakeVote() || SuperbVote.getPlugin().getConfig().getBoolean("votes.process-fake-votes")) {
                if (addVote)
                    SuperbVote.getPlugin().getVoteStorage().addVote(vote);
            }

            if (!wasQueued) {
                for (VoteReward reward : bestRewards)
                    reward.broadcastVote(context, SuperbVote.getPlugin().getConfig().getBoolean("broadcast.message-player"), broadcast);
            }

            Bukkit.getScheduler().runTaskAsynchronously(SuperbVote.getPlugin(), this::afterVoteProcessing);
            Bukkit.getScheduler().runTask(SuperbVote.getPlugin(), () -> bestRewards.forEach(reward -> reward.runCommands(vote)));
        }
    }

    public void afterVoteProcessing() {
        SuperbVote.getPlugin().getScoreboardHandler().doPopulate();
        new TopPlayerSignFetcher(SuperbVote.getPlugin().getTopPlayerSignStorage().getSignList()).run();
    }
}
