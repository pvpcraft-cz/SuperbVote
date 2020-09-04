package io.minimum.minecraft.superbvote.migration;

import io.minimum.minecraft.superbvote.SuperbVote;
import io.minimum.minecraft.superbvote.storage.JsonVoteStorage;
import io.minimum.minecraft.superbvote.storage.MysqlVoteStorage;
import io.minimum.minecraft.superbvote.util.PlayerVotes;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MySQLMigration implements Migration {

    @Override
    public String getName() {
        return "SuperbVote JSON storage";
    }

    @Override
    public void execute(ProgressListener listener) {
        SuperbVote plugin = SuperbVote.getPlugin();

        // Initialize both storages
        MysqlVoteStorage mysqlVoteStorage;
        if (plugin.getVoteStorage() instanceof MysqlVoteStorage) {
            mysqlVoteStorage = (MysqlVoteStorage) plugin.getVoteStorage();
        } else {
            mysqlVoteStorage = plugin.getConfiguration().initializeMySQL();
        }

        if (mysqlVoteStorage == null) {
            throw new RuntimeException("Could not initialize MySQL storage, make sure it's reachable.");
        }

        JsonVoteStorage jsonVoteStorage;
        if (plugin.getVoteStorage() instanceof JsonVoteStorage) {
            jsonVoteStorage = (JsonVoteStorage) plugin.getVoteStorage();
        } else {
            try {
                jsonVoteStorage = plugin.getConfiguration().initializeJson();
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("Could not initialize JSON storage.");
            }
        }

        // Load from json
        final Set<UUID> players = new HashSet<>(jsonVoteStorage.getVoteCounts().keySet());

        listener.onStart(players.size());

        int divisor = ProgressUtil.findBestDivisor(players.size());
        CompletableFuture.runAsync(() -> {
            int count = 0;
            for (UUID uniqueID : players) {

                PlayerVotes votes = jsonVoteStorage.getVotes(uniqueID);
                long lastVote = jsonVoteStorage.getLastVote(uniqueID);

                mysqlVoteStorage.setVotes(uniqueID, votes.getVotes(), lastVote);
                count++;

                if (count % divisor == 0)
                    listener.onRecordBatch(count, players.size());
            }
        }).exceptionally((exc) -> {
            throw new RuntimeException("An error has occurred.");
        }).thenRun(() -> listener.onFinish(players.size()));
    }
}