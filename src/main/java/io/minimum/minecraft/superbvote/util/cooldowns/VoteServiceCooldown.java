package io.minimum.minecraft.superbvote.util.cooldowns;

import io.minimum.minecraft.superbvote.votes.Vote;

import java.time.LocalDateTime;
import java.util.UUID;

public class VoteServiceCooldown extends CooldownHandler<UUID> {

    public VoteServiceCooldown(int max) {
        super(max);
    }

    public boolean triggerCooldown(Vote vote) {
        return super.triggerCooldown(vote.getUuid());
    }

    public boolean canVote(UUID uniqueID) {
        return super.getTime(uniqueID).isBefore(LocalDateTime.now());
    }
}