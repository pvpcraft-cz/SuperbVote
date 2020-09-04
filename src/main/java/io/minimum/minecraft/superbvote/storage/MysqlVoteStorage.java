package io.minimum.minecraft.superbvote.storage;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.zaxxer.hikari.pool.HikariPool;
import io.minimum.minecraft.superbvote.SuperbVote;
import io.minimum.minecraft.superbvote.util.PlayerVotes;
import io.minimum.minecraft.superbvote.votes.Vote;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

@RequiredArgsConstructor
public class MysqlVoteStorage implements VoteStorage {
    private static final int TABLE_VERSION_2 = 2;
    private static final int TABLE_VERSION_3 = 3;
    private static final int TABLE_VERSION_4 = 4;
    private static final int TABLE_VERSION_CURRENT = TABLE_VERSION_4;

    private final HikariPool dbPool;
    private final String tableName;
    private final boolean readOnly;

    public Connection getConnection() {
        try {
            return dbPool.getConnection();
        } catch (SQLException e) {
            return null;
        }
    }

    public void initialize() {
        // Load current DB version from disk
        YamlConfiguration dbInfo = YamlConfiguration.loadConfiguration(new File(SuperbVote.getPlugin().getDataFolder(), "db_version.yml"));
        dbInfo.options().header("DO NOT EDIT - SuperbVote internal use");

        int ver = dbInfo.getInt("db_version", 1);
        boolean isUpdated = false;

        if (!readOnly) {
            try (Connection connection = dbPool.getConnection()) {
                try (ResultSet t = connection.getMetaData().getTables(null, null, tableName, null)) {
                    if (!t.next()) {
                        try (Statement statement = connection.createStatement()) {
                            statement.executeUpdate("CREATE TABLE " + tableName + " (uuid VARCHAR(36) PRIMARY KEY NOT NULL, last_name VARCHAR(16), votes INT NOT NULL, last_vote TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
                            // This may speed up leaderboards
                            statement.executeUpdate("CREATE INDEX uuid_votes_idx ON " + tableName + " (uuid, votes)");
                        }
                        isUpdated = true;
                    } else {
                        if (ver < TABLE_VERSION_CURRENT) {
                            SuperbVote.getPlugin().getLogger().log(Level.INFO, "Migrating database from version " + ver + " to " + TABLE_VERSION_CURRENT + ", this may take a while...");
                            // We may need to add in the new last_vote column
                            if (ver < TABLE_VERSION_2) {
                                try (Statement statement = connection.createStatement()) {
                                    statement.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN last_vote TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
                                }
                                isUpdated = true;
                            }
                            if (ver < TABLE_VERSION_3) {
                                try (Statement statement = connection.createStatement()) {
                                    statement.executeUpdate("ALTER TABLE " + tableName + " CHANGE last_vote last_vote TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
                                }
                                isUpdated = true;
                            }
                            if (ver < TABLE_VERSION_4) {
                                try (Statement statement = connection.createStatement()) {
                                    // In case invalid vote counts snuck in, tell MySQL to say it's zero.
                                    statement.executeUpdate("ALTER TABLE " + tableName + " MODIFY votes int(11) NOT NULL DEFAULT 0");
                                }
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                SuperbVote.getPlugin().getLogger().log(Level.SEVERE, "Unable to initialize database", e);
            }

            if (isUpdated) {
                dbInfo.set("db_version", TABLE_VERSION_CURRENT);
                try {
                    dbInfo.save(new File(SuperbVote.getPlugin().getDataFolder(), "db_version.yml"));
                } catch (IOException e) {
                    SuperbVote.getPlugin().getLogger().log(Level.SEVERE, "Unable to save DB info", e);
                }
            }
        }
    }

    @Override
    public void addVote(Vote vote) {
        if (readOnly)
            return;

        Preconditions.checkNotNull(vote, "vote");
        try (Connection connection = dbPool.getConnection()) {
            if (vote.getName() != null) {
                try (PreparedStatement statement = connection.prepareStatement("INSERT INTO " + tableName + " (uuid, last_name, votes, last_vote) VALUES (?, ?, 1, ?)" +
                        " ON DUPLICATE KEY UPDATE votes = votes + 1, last_name = ?, last_vote = ?")) {

                    statement.setString(1, vote.getUuid().toString());
                    statement.setString(2, vote.getName());
                    statement.setTimestamp(3, new Timestamp(vote.getReceived().getTime()));
                    statement.setString(4, vote.getName());
                    statement.setTimestamp(5, new Timestamp(vote.getReceived().getTime()));
                    statement.executeUpdate();
                }
            } else {
                try (PreparedStatement statement = connection.prepareStatement("INSERT INTO " + tableName + " (uuid, last_name, votes, last_vote) VALUES (?, ?, 1, ?)" +
                        " ON DUPLICATE KEY UPDATE votes = votes + 1, last_vote = ?")) {

                    statement.setString(1, vote.getUuid().toString());
                    statement.setString(2, vote.getName());
                    statement.setTimestamp(3, new Timestamp(vote.getReceived().getTime()));
                    statement.setTimestamp(4, new Timestamp(vote.getReceived().getTime()));
                    statement.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to add vote for " + vote.getUuid().toString(), e);
        }
    }

    private String getLastName(UUID uniqueID) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uniqueID);
        return offlinePlayer.getName();
    }

    @Override
    public void setVotes(UUID player, int votes, long ts) {
        if (readOnly)
            return;

        Preconditions.checkNotNull(player, "player");
        try (Connection connection = dbPool.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO " + tableName + " (uuid, last_name, votes, last_vote) VALUES (?, ?, ?, ?)" +
                    " ON DUPLICATE KEY UPDATE votes = ?, last_vote = ?, last_name = ?")) {
                statement.setString(1, player.toString());
                statement.setString(2, getLastName(player));
                statement.setInt(3, votes);
                statement.setTimestamp(4, new Timestamp(ts));
                statement.setInt(5, votes);
                statement.setTimestamp(6, new Timestamp(ts));
                statement.setString(7, getLastName(player));
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to set votes for " + player.toString(), e);
        }
    }

    @Override
    public void clearVotes() {
        if (readOnly)
            return;

        try (Connection connection = dbPool.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("TRUNCATE TABLE " + tableName)) {
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            SuperbVote.getPlugin().getLogger().log(Level.SEVERE, "Unable to clear votes", e);
        }
    }

    @Override
    public PlayerVotes getVotes(UUID player) {
        Preconditions.checkNotNull(player, "player");
        try (Connection connection = dbPool.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("SELECT last_name, votes FROM " + tableName + " WHERE uuid = ?")) {
                statement.setString(1, player.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return new PlayerVotes(player, resultSet.getString(1), resultSet.getInt(2), PlayerVotes.Type.CURRENT);
                    } else {
                        return new PlayerVotes(player, null, 0, PlayerVotes.Type.CURRENT);
                    }
                }
            }
        } catch (SQLException e) {
            SuperbVote.getPlugin().getLogger().log(Level.SEVERE, "Unable to get votes for " + player.toString(), e);
            return new PlayerVotes(player, null, 0, PlayerVotes.Type.CURRENT);
        }
    }

    @Override
    public List<PlayerVotes> getTopVoters(int amount, int page) {
        int offset = page * amount;
        try (Connection connection = dbPool.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("SELECT uuid, last_name, votes FROM " + tableName + " WHERE votes > 0 ORDER BY votes DESC " +
                    "LIMIT " + amount + " OFFSET " + offset)) {

                try (ResultSet resultSet = statement.executeQuery()) {
                    List<PlayerVotes> records = new ArrayList<>();
                    while (resultSet.next()) {
                        UUID uuid = UUID.fromString(resultSet.getString(1));
                        String name = resultSet.getString(2);
                        records.add(new PlayerVotes(uuid, name, resultSet.getInt(3), PlayerVotes.Type.CURRENT));
                    }
                    return records;
                }
            }
        } catch (SQLException e) {
            SuperbVote.getPlugin().getLogger().log(Level.SEVERE, "Unable to get top votes", e);
            return Collections.emptyList();
        }
    }

    @Override
    public int getPagesAvailable(int amount) {
        try (Connection connection = dbPool.getConnection()) {
            // Ugly SQL, but who cares
            try (PreparedStatement statement = connection.prepareStatement("SELECT CEIL(COUNT(uuid) / " + amount + ") FROM " + tableName)) {
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next() ? resultSet.getInt(1) : 0;
                }
            }
        } catch (SQLException e) {
            SuperbVote.getPlugin().getLogger().log(Level.SEVERE, "Unable to get top votes page count", e);
            return 0;
        }
    }

    @Override
    public long getLastVote(UUID player) {
        try (Connection connection = dbPool.getConnection()) {
            try (PreparedStatement statement = connection
                    .prepareStatement("SELECT last_vote FROM " + tableName + " WHERE uuid = ?")) {
                statement.setString(1, player.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getTimestamp("last_vote").getTime();
                    }
                }
            }
        } catch (SQLException e) {
            SuperbVote.getPlugin().getLogger().log(Level.SEVERE, "Unable to get top votes page count", e);
        }
        return 0;
    }

    @Override
    public boolean hasVotedToday(UUID player) {
        try (Connection connection = dbPool.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM " + tableName + " WHERE uuid = ? AND DATE(last_vote) = CURRENT_DATE()")) {
                statement.setString(1, player.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next();
                }
            }
        } catch (SQLException e) {
            SuperbVote.getPlugin().getLogger().log(Level.SEVERE, "Unable to get top votes page count", e);
            return false;
        }
    }

    @Override
    public List<PlayerVotes> getAllPlayersWithNoVotesToday(List<UUID> onlinePlayers) {
        if (onlinePlayers.isEmpty()) {
            return ImmutableList.of();
        }
        List<PlayerVotes> votes = new ArrayList<>();
        try (Connection connection = dbPool.getConnection()) {
            String valueStatement = Joiner.on(", ").join(Collections.nCopies(onlinePlayers.size(), "?"));
            try (PreparedStatement statement = connection.prepareStatement("SELECT uuid, last_name, votes, (DATE(last_vote) = CURRENT_DATE()) AS has_voted_today FROM " + tableName + " WHERE uuid IN (" + valueStatement + ")")) {
                for (int i = 0; i < onlinePlayers.size(); i++) {
                    statement.setString(i + 1, onlinePlayers.get(i).toString());
                }
                List<UUID> found = new ArrayList<>();
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        UUID uuid = UUID.fromString(resultSet.getString(1));
                        found.add(uuid);
                        if (resultSet.getBoolean(4)) {
                            continue; // already voted today
                        }
                        PlayerVotes pv = new PlayerVotes(UUID.fromString(resultSet.getString(1)),
                                resultSet.getString(2),
                                resultSet.getInt(3),
                                PlayerVotes.Type.CURRENT);
                        votes.add(pv);
                    }
                }

                // We may have players without a voting record. Add these missing players.
                List<UUID> missing = new ArrayList<>(onlinePlayers);
                missing.removeAll(found);
                for (UUID uuid : missing) {
                    votes.add(new PlayerVotes(uuid, null, 0, PlayerVotes.Type.CURRENT));
                }
            }
            return votes;
        } catch (SQLException e) {
            SuperbVote.getPlugin().getLogger().log(Level.SEVERE, "Unable to batch-get votes", e);
            return ImmutableList.of();
        }
    }

    @Override
    public void save() {
        // No-op
    }

    @Override
    public void close() {
        try {
            dbPool.shutdown();
        } catch (InterruptedException e) {
            // Not much we can do about that...
        }
    }
}
