package io.minimum.minecraft.superbvote.votes;

import io.minimum.minecraft.superbvote.SuperbVote;
import io.minimum.minecraft.superbvote.configuration.message.MessageContext;
import io.minimum.minecraft.superbvote.util.PlayerVotes;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class VoteReminder implements Runnable {

    @Override
    public void run() {
        List<UUID> onlinePlayers = Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.hasPermission("superbvote.notify"))
                .map(Player::getUniqueId)
                .collect(Collectors.toList());

        for (UUID uuid : onlinePlayers) {
            Player player = Bukkit.getPlayer(uuid);
            PlayerVotes playerVotes = SuperbVote.getPlugin().getVoteStorage().getVotes(uuid);

            if (SuperbVote.getPlugin().getVoteServiceCooldown().canVote(uuid) && player != null) {
                MessageContext context = new MessageContext(null, playerVotes, player);
                SuperbVote.getPlugin().getConfiguration().getReminderMessage().sendAsReminder(player, context);
            }
        }
    }
}
