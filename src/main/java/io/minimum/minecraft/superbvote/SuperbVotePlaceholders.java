package io.minimum.minecraft.superbvote;

import io.minimum.minecraft.superbvote.util.PlayerVotes;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.List;

public class SuperbVotePlaceholders extends PlaceholderExpansion {

    @Override
    public String getIdentifier() {
        return "superbvote";
    }

    @Override
    public String getAuthor() {
        return "Wertik1206";
    }

    @Override
    public String getVersion() {
        return SuperbVote.getPlugin().getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player p, String params) {

        // params == top_1_votes

        String[] arr = params.split("_");

        if (arr.length < 1)
            return "not_enough_args";
        else {
            if (arr[0].equalsIgnoreCase("votes")) {
                if (p == null)
                    return "";

                return String.valueOf(SuperbVote.getPlugin().getVoteStorage().getVotes(p.getUniqueId()).getVotes());
            } else if (arr[0].equalsIgnoreCase("stored")) {
                if (p == null)
                    return "";

                return String.valueOf(SuperbVote.getPlugin().getQueuedVotes().getVotes(p.getUniqueId()).size());
            } else {
                if (arr.length < 3)
                    return "not_enough_args";

                if (params.split("_")[0].equalsIgnoreCase("top")) {
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
                }
            }
        }

        return null;
    }
}
