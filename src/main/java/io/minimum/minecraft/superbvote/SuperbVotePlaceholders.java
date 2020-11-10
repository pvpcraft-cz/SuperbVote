package io.minimum.minecraft.superbvote;

import io.minimum.minecraft.superbvote.util.PlayerVotes;
import lombok.RequiredArgsConstructor;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import space.devport.utils.utility.TimeElement;
import space.devport.utils.utility.TimeUtil;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@RequiredArgsConstructor
public class SuperbVotePlaceholders extends PlaceholderExpansion {

    private final SuperbVote plugin;

    private final String identifier;

    @Override
    public @NotNull String getIdentifier() {
        return identifier;
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {

        String[] args = params.split("_");

        if (args.length < 1)
            return "not_enough_args";

        if (player == null)
            return "no_player";

        switch (args[0].toLowerCase()) {
            case "votes":
                return String.valueOf(SuperbVote.getPlugin().getVoteStorage().getVotes(player.getUniqueId()).getVotes());
            case "stored":
                return String.valueOf(SuperbVote.getPlugin().getQueuedVotes().getVotes(player.getUniqueId()).size());
            case "top":
                if (args.length < 3)
                    return "not_enough_args";

                int pos = Integer.parseInt(params.split("_")[1]) - 1;

                if (pos < 0)
                    return ChatColor.translateAlternateColorCodes('&', SuperbVote.getPlugin().getConfig().getString("top-cache.no-player"));

                PlayerVotes vote = SuperbVote.getPlugin().getTopVoterCache().get(pos);

                if (vote == null) {

                    if (SuperbVote.getPlugin().getConfig().getBoolean("top-cache.attempt-load")) {
                        // Attempt to load from DB
                        List<PlayerVotes> votes = SuperbVote.getPlugin().getVoteStorage().getTopVoters(pos + 1, 0);
                        vote = votes.size() > pos ? votes.get(pos) : null;
                    }

                    if (vote == null)
                        return ChatColor.translateAlternateColorCodes('&', SuperbVote.getPlugin().getConfig().getString("top-cache.no-player"));
                }

                String type = params.split("_")[2].toLowerCase();

                if (type.equalsIgnoreCase("votes")) {
                    return String.valueOf(vote.getVotes());
                } else if (type.equalsIgnoreCase("name")) {
                    return vote.getAssociatedUsername();
                } else return "unkown_type";
            case "cooldown":

                if (args.length < 2)
                    return "not_enough_args";

                boolean start = args.length > 2 && args[2].equalsIgnoreCase("start");

                TimeElement element = TimeElement.fromString(args[1]);

                if (element == null) return "invalid_element";

                LocalDateTime time = SuperbVote.getPlugin().getVoteServiceCooldown().getTime(player.getUniqueId());

                if (time == null) return "0";

                long until = time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() - System.currentTimeMillis();

                return String.valueOf(TimeUtil.takeElement(until, element, start));
            case "canvote":
                return SuperbVote.getPlugin().getVoteServiceCooldown().canVote(player.getUniqueId()) ? "yes" : "no";
        }

        return null;
    }
}