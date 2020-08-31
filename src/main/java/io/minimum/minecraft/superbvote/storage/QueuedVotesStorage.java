package io.minimum.minecraft.superbvote.storage;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.minimum.minecraft.superbvote.votes.Vote;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class QueuedVotesStorage {
    private final Map<UUID, List<Vote>> queuedVotes = new ConcurrentHashMap<>(32, 0.75f, 2);
    private final Gson gson = new Gson();
    private final File saveTo;

    public QueuedVotesStorage(File file) throws IOException {
        Preconditions.checkNotNull(file, "file");
        saveTo = file;

        if (!file.exists()) file.createNewFile();

        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            Map<UUID, List<Vote>> votes = gson.fromJson(reader, new TypeToken<Map<UUID, List<Vote>>>() {
            }.getType());
            if (votes != null) {
                for (Map.Entry<UUID, List<Vote>> entry : votes.entrySet()) {
                    queuedVotes.put(entry.getKey(), new CopyOnWriteArrayList<>(entry.getValue()));
                }
            }
        }
    }

    public void addVote(Vote vote) {
        Preconditions.checkNotNull(vote, "votes");
        List<Vote> votes = queuedVotes.computeIfAbsent(vote.getUuid(), (ignored) -> new CopyOnWriteArrayList<>());
        votes.add(vote);
    }

    public void clearVotes() {
        queuedVotes.clear();
    }

    public List<Vote> getAndRemoveVotes(UUID player) {
        Preconditions.checkNotNull(player, "player");
        List<Vote> votes = queuedVotes.remove(player);
        return votes != null ? votes : ImmutableList.of();
    }

    public List<Vote> getVotes(UUID player) {
        Preconditions.checkNotNull(player, "player");
        List<Vote> votes = queuedVotes.get(player);
        return votes != null ? votes : ImmutableList.of();
    }

    public void save() {
        // Save to a temporary file and then copy over the existing file.
        try {
            Path tempPath = Files.createTempFile("superbvote-", ".json");
            try (Writer writer = Files.newBufferedWriter(tempPath, StandardOpenOption.WRITE)) {
                gson.toJson(queuedVotes, writer);
            }

            Files.move(tempPath, saveTo.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Unable to save queued votes to " + saveTo, e);
        }
    }
}
