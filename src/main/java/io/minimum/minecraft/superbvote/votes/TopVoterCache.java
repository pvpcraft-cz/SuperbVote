package io.minimum.minecraft.superbvote.votes;

import io.minimum.minecraft.superbvote.SuperbVote;
import io.minimum.minecraft.superbvote.util.PlayerVotes;

import java.util.ArrayList;
import java.util.List;

public class TopVoterCache implements Runnable {

    private List<PlayerVotes> topVoters = new ArrayList<>();

    public PlayerVotes get(int n) {
        return topVoters.size() > n ? topVoters.get(n) : null;
    }

    @Override
    public void run() {
        int players = SuperbVote.getPlugin().getConfig().getInt("top-cache.players");
        topVoters = SuperbVote.getPlugin().getVoteStorage().getTopVoters(players, 0);
    }
}
