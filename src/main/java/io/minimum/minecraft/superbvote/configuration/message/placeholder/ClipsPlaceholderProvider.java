package io.minimum.minecraft.superbvote.configuration.message.placeholder;

import io.minimum.minecraft.superbvote.configuration.message.MessageContext;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;

public class ClipsPlaceholderProvider implements PlaceholderProvider {

    @Override
    public String apply(String message, MessageContext context) {

        if (context.getPlayer().isPresent() && context.getPlayer().get().isOnline()) {
            message = PlaceholderAPI.setPlaceholders(context.getPlayer().get().getPlayer(), message)
                    .replaceAll("(?i)%voter_", "%");
        }

        if (context.getVoter().isPresent() && context.getVoter().get().isOnline()) {
            message = PlaceholderAPI.setPlaceholders(context.getVoter().get().getPlayer(), message);
        }

        return message;
    }

    @Override
    public boolean canUse() {
        return Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }
}
