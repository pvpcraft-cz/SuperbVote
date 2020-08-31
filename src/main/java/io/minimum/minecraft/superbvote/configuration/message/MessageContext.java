package io.minimum.minecraft.superbvote.configuration.message;

import io.minimum.minecraft.superbvote.util.PlayerVotes;
import io.minimum.minecraft.superbvote.votes.Vote;
import org.bukkit.OfflinePlayer;

import java.util.Optional;

public class MessageContext {

    private final Vote vote;
    private final PlayerVotes voteRecord;
    private final OfflinePlayer voter;

    // The player that's looking at the message.
    private OfflinePlayer player;

    public MessageContext(Vote vote, PlayerVotes voteRecord, OfflinePlayer voter) {
        this.vote = vote;
        this.voteRecord = voteRecord;
        this.voter = voter;
        this.player = voter;
    }

    public Optional<Vote> getVote() {
        return Optional.ofNullable(vote);
    }

    public PlayerVotes getVoteRecord() {
        return voteRecord;
    }

    public Optional<OfflinePlayer> getVoter() {
        return Optional.ofNullable(voter);
    }

    public void setPlayer(OfflinePlayer player) {
        this.player = player;
    }

    public Optional<OfflinePlayer> getPlayer() {
        return Optional.ofNullable(player);
    }

    @Override
    public String toString() {
        return "MessageContext{" +
                "vote=" + vote +
                ", voteRecord=" + voteRecord +
                ", player=" + voter +
                '}';
    }
}
