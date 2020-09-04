package io.minimum.minecraft.superbvote.votes;

import com.vexsoftware.votifier.model.VotifierEvent;
import io.minimum.minecraft.superbvote.SuperbVote;
import io.minimum.minecraft.superbvote.commands.SuperbVoteCommand;
import io.minimum.minecraft.superbvote.configuration.message.MessageContext;
import io.minimum.minecraft.superbvote.util.BrokenNag;
import io.minimum.minecraft.superbvote.util.PlayerVotes;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.PluginEnableEvent;

import java.util.Date;
import java.util.List;

public class SuperbVoteListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onVote(final VotifierEvent event) {
        if (SuperbVote.getPlugin().getConfiguration().isConfigurationError()) {
            SuperbVote.getPlugin().getLogger().severe("Refusing to process vote because your configuration is invalid. Please check your logs.");
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(SuperbVote.getPlugin(), () -> {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(event.getVote().getUsername());
            String worldName = null;

            if (offlinePlayer.isOnline()) {
                worldName = offlinePlayer.getPlayer().getWorld().getName();
            }

            PlayerVotes pvCurrent = SuperbVote.getPlugin().getVoteStorage().getVotes(offlinePlayer.getUniqueId());
            PlayerVotes pv = new PlayerVotes(offlinePlayer.getUniqueId(), offlinePlayer.getName(), pvCurrent.getVotes() + 1, PlayerVotes.Type.FUTURE);

            Vote vote = new Vote(offlinePlayer.getName(), offlinePlayer.getUniqueId(), event.getVote().getServiceName(),
                    event.getVote().getAddress().equals(SuperbVoteCommand.FAKE_HOST_NAME_FOR_VOTE), worldName, new Date());

            if (!vote.isFakeVote() || SuperbVote.getPlugin().getConfig().getBoolean("votes.process-fake-votes")) {
                if (SuperbVote.getPlugin().getVoteServiceCooldown().triggerCooldown(vote)) {
                    Bukkit.getLogger().info("Started service cooldown for " + vote.getName());
                }
            }

            boolean queue;

            if (SuperbVote.getPlugin().getConfig().getBoolean("claim.enabled")) {
                if (offlinePlayer.isOnline())
                    queue = SuperbVote.getPlugin().getConfig().getBoolean("claim.online");
                else queue = true;
            } else
                queue = (!offlinePlayer.isOnline() && SuperbVote.getPlugin().getConfiguration().requirePlayersOnline());

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
                    if (SuperbVote.getPlugin().getVoteReminder() != null) {
                        MessageContext context = new MessageContext(null, pv, event.getPlayer());
                        SuperbVote.getPlugin().getVoteReminder().sendMessage(event.getPlayer(), context);
                    }
                }
            }

            // Remind players to vote.
            if (SuperbVote.getPlugin().getConfig().getBoolean("vote-reminder.on-join") &&
                    !SuperbVote.getPlugin().getVoteStorage().hasVotedToday(event.getPlayer().getUniqueId())) {

                if (SuperbVote.getPlugin().getVoteReminder() != null) {
                    MessageContext context = new MessageContext(null, pv, event.getPlayer());
                    SuperbVote.getPlugin().getVoteReminder().sendMessage(event.getPlayer(), context);
                }
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