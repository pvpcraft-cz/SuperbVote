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
        LocalDateTime time = super.getTime(uniqueID);
        if (time == null) return true;
        return time.isBefore(LocalDateTime.now());
    }
}