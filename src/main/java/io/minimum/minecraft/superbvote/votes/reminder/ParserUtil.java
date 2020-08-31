package io.minimum.minecraft.superbvote.votes.reminder;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import io.minimum.minecraft.superbvote.SuperbVote;
import io.minimum.minecraft.superbvote.configuration.message.MessageContext;
import io.minimum.minecraft.superbvote.configuration.message.placeholder.ClipsPlaceholderProvider;
import io.minimum.minecraft.superbvote.configuration.message.placeholder.PlaceholderProvider;
import io.minimum.minecraft.superbvote.configuration.message.placeholder.SuperbVotePlaceholderProvider;
import io.minimum.minecraft.superbvote.util.PlayerVotes;
import lombok.experimental.UtilityClass;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@UtilityClass
public class ParserUtil {

    public final List<PlaceholderProvider> PROVIDER_LIST = ImmutableList.of(new SuperbVotePlaceholderProvider(),
            new ClipsPlaceholderProvider());

    @Nullable
    public Object parseObject(String str) {

        if (Strings.isNullOrEmpty(str)) return str;

        str = str.trim();

        Object obj = null;

        try {
            obj = Integer.parseInt(str);
        } catch (NumberFormatException ignored) {
        }

        if (obj == null)
            try {
                obj = Double.parseDouble(str);
            } catch (NumberFormatException ignored) {
            }

        return obj == null ? str : obj;
    }

    @Nullable
    public String parsePlaceholders(@Nullable String str, @Nullable Player player) {

        if (str == null) return null;

        PlayerVotes voteRecord = player == null ? null : SuperbVote.getPlugin().getVoteStorage().getVotes(player.getUniqueId());

        MessageContext context = new MessageContext(null, voteRecord, player);

        // Custom placeholders
        for (PlaceholderProvider provider : PROVIDER_LIST) {
            if (provider.canUse())
                str = provider.apply(str, context);
        }

        return str;
    }
}
