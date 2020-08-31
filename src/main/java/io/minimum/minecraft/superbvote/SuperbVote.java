package io.minimum.minecraft.superbvote;

import io.minimum.minecraft.superbvote.commands.SuperbVoteCommand;
import io.minimum.minecraft.superbvote.commands.gui.ClaimGUI;
import io.minimum.minecraft.superbvote.configuration.SuperbVoteConfiguration;
import io.minimum.minecraft.superbvote.scoreboard.ScoreboardHandler;
import io.minimum.minecraft.superbvote.signboard.TopPlayerSignFetcher;
import io.minimum.minecraft.superbvote.signboard.TopPlayerSignListener;
import io.minimum.minecraft.superbvote.signboard.TopPlayerSignStorage;
import io.minimum.minecraft.superbvote.storage.QueuedVotesStorage;
import io.minimum.minecraft.superbvote.storage.VoteStorage;
import io.minimum.minecraft.superbvote.util.BrokenNag;
import io.minimum.minecraft.superbvote.util.cooldowns.VoteServiceCooldown;
import io.minimum.minecraft.superbvote.votes.SuperbVoteListener;
import io.minimum.minecraft.superbvote.votes.TopVoterCache;
import io.minimum.minecraft.superbvote.votes.reminder.VoteReminder;
import io.minimum.minecraft.superbvote.votes.VoteService;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import space.devport.utils.configuration.Configuration;
import space.devport.utils.text.message.Message;

import java.io.File;
import java.io.IOException;

public class SuperbVote extends JavaPlugin {

    @Getter
    private static SuperbVote plugin;

    @Getter
    private SuperbVoteConfiguration configuration;

    @Getter
    private VoteStorage voteStorage;

    @Getter
    private QueuedVotesStorage queuedVotes;

    @Getter
    private ScoreboardHandler scoreboardHandler;

    @Getter
    private VoteServiceCooldown voteServiceCooldown;

    @Getter
    private TopPlayerSignStorage topPlayerSignStorage;

    @Getter
    private VoteService voteService;

    private BukkitTask voteReminderTask;

    private TopVoterCache topVoterCache;

    public TopVoterCache getTopVoterCache() {
        return topVoterCache;
    }

    private BukkitTask voteTopUpdateTask;

    @Override
    public void onEnable() {
        plugin = this;
        saveDefaultConfig();

        configuration = new SuperbVoteConfiguration(getConfig());

        if (configuration.isConfigurationError()) {
            BrokenNag.nag(getServer().getConsoleSender());
        }

        try {
            voteStorage = configuration.initializeVoteStorage();
        } catch (Exception e) {
            throw new RuntimeException("Exception whilst initializing vote storage", e);
        }

        try {
            queuedVotes = new QueuedVotesStorage(new File(getDataFolder(), "queued_votes.json"));
        } catch (IOException e) {
            throw new RuntimeException("Exception whilst initializing queued vote storage", e);
        }

        scoreboardHandler = new ScoreboardHandler();
        voteServiceCooldown = new VoteServiceCooldown(getConfig().getInt("votes.cooldown-per-service", 3600));

        topPlayerSignStorage = new TopPlayerSignStorage();
        try {
            topPlayerSignStorage.load(new File(getDataFolder(), "top_voter_signs.json"));
        } catch (IOException e) {
            throw new RuntimeException("Exception whilst loading top player signs", e);
        }

        voteService = new VoteService();

        getCommand("superbvote").setExecutor(new SuperbVoteCommand());
        getCommand("vote").setExecutor(configuration.getVoteCommand());

        getServer().getPluginManager().registerEvents(new SuperbVoteListener(), this);
        getServer().getPluginManager().registerEvents(new TopPlayerSignListener(), this);
        getServer().getPluginManager().registerEvents(new ClaimGUI(), this);

        getServer().getScheduler().runTaskTimerAsynchronously(this, voteStorage::save, 20, 20 * 30);
        getServer().getScheduler().runTaskTimerAsynchronously(this, queuedVotes::save, 20, 20 * 30);
        getServer().getScheduler().runTaskAsynchronously(this, SuperbVote.getPlugin().getScoreboardHandler()::doPopulate);
        getServer().getScheduler().runTaskAsynchronously(this, new TopPlayerSignFetcher(topPlayerSignStorage.getSignList()));

        setupVoteReminder();

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            getLogger().info("Using clip's PlaceholderAPI to provide extra placeholders.");

            if (getConfig().getBoolean("top-cache.enabled")) {
                int updateCycle = getConfig().getInt("top-cache.update-cycle");

                topVoterCache = new TopVoterCache();
                voteTopUpdateTask = getServer().getScheduler().runTaskTimerAsynchronously(this, topVoterCache, 20 * updateCycle, 20 * updateCycle);
                getLogger().info("Top Voters cache update cycle started..");

                new SuperbVotePlaceholders(this).register();
                getLogger().info("Registered custom placeholders for PAPI.");
            }
        }
    }

    @Override
    public void onDisable() {
        if (voteReminderTask != null) {
            voteReminderTask.cancel();
            voteReminderTask = null;
        }

        voteStorage.save();
        queuedVotes.save();
        voteStorage.close();

        try {
            topPlayerSignStorage.save(new File(getDataFolder(), "top_voter_signs.json"));
        } catch (IOException e) {
            throw new RuntimeException("Exception whilst saving top player signs", e);
        }
    }

    public void reloadPlugin() {
        reloadConfig();
        configuration = new SuperbVoteConfiguration(getConfig());
        scoreboardHandler.reload();
        voteServiceCooldown = new VoteServiceCooldown(getConfig().getInt("votes.cooldown-per-service", 3600));

        if (getConfig().getBoolean("top-cache.enabled")) {
            int updateCycle = getConfig().getInt("top-cache.update-cycle");

            voteTopUpdateTask = getServer().getScheduler().runTaskTimerAsynchronously(this, topVoterCache, 20 * updateCycle, 20 * updateCycle);
            getLogger().info("Top Voters cache update cycle re-started..");
        }

        getServer().getScheduler().runTaskAsynchronously(this, getScoreboardHandler()::doPopulate);
        getCommand("vote").setExecutor(configuration.getVoteCommand());

        if (voteReminderTask != null) {
            voteReminderTask.cancel();
            voteReminderTask = null;
        }

        setupVoteReminder();
    }

    private void setupVoteReminder() {
        int interval = getConfig().getInt("vote-reminder.repeat");
        Configuration configuration = new Configuration(this, "config");

        Message message = configuration.getMessage("vote-reminder.message");

        if (message != null && !message.isEmpty()) {
            if (interval > 0) {
                voteReminderTask = getServer().getScheduler().runTaskTimerAsynchronously(this, new VoteReminder(getConfig().getString("vote-reminder.condition", "")), 20 * interval, 20 * interval);
                getLogger().info("Started Vote Reminder with interval " + interval);
            }
        }
    }

    public ClassLoader _exposeClassLoader() {
        return getClassLoader();
    }
}