package io.minimum.minecraft.superbvote.util;

import lombok.Value;
import org.bukkit.Bukkit;

import java.util.UUID;

@Value
public class PlayerVotes {

    UUID uuid;
    String associatedUsername;
    int votes;
    Type type;

    public String getAssociatedUsername() {
        if (associatedUsername == null) {
            return Bukkit.getOfflinePlayer(uuid).getName();
        }
        return associatedUsername;
    }

    public enum Type {
        CURRENT,
        FUTURE
    }
}
