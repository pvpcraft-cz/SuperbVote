package io.minimum.minecraft.superbvote.votes;

import com.google.common.base.Strings;
import lombok.NoArgsConstructor;
import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedList;
import java.util.List;

@NoArgsConstructor
public class CommandReward {

    private List<String> commands = new LinkedList<>();

    public CommandReward(List<String> commands) {
        this.commands = commands;
    }

    public CommandReward(String command) {
        this.commands.add(command);
    }

    public static CommandReward from(ConfigurationSection root, String path) {

        if (root == null)
            throw new IllegalArgumentException("Root cannot be null");

        if (Strings.isNullOrEmpty(path))
            throw new IllegalArgumentException("Path cannot be null");

        ConfigurationSection section = root.getConfigurationSection(path);

        if (section == null || !section.contains("commands"))
            return new CommandReward();

        if (section.isList("commands"))
            return new CommandReward(section.getStringList("commands"));

        if (section.isString("commands"))
            return new CommandReward(section.getString("commands"));

        return new CommandReward();
    }
}