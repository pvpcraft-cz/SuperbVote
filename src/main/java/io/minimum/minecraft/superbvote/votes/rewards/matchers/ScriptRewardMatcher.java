package io.minimum.minecraft.superbvote.votes.rewards.matchers;

import io.minimum.minecraft.superbvote.SuperbVote;
import io.minimum.minecraft.superbvote.util.PlayerVotes;
import io.minimum.minecraft.superbvote.votes.Vote;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;

public class ScriptRewardMatcher implements RewardMatcher {
    private final ScriptEngine engine;
    private final Path path;

    public ScriptRewardMatcher(Path path) throws IOException, ScriptException {
        this.path = path;

        // Yes, we have to switch the context classloader.
        ClassLoader previousCtx = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(SuperbVote.getPlugin()._exposeClassLoader());
        try {
            ScriptEngineManager manager = new ScriptEngineManager();
            engine = manager.getEngineByName("JavaScript");

            // Read SuperbVote helper library first
            try (Reader reader = new BufferedReader(new InputStreamReader(SuperbVote.getPlugin().getResource("superbvote_lib.js")))) {
                engine.eval(reader);
            }

            try (Reader reader = Files.newBufferedReader(path)) {
                engine.eval(reader);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(previousCtx);
        }
    }

    @Override
    public boolean matches(Vote vote, PlayerVotes pv) {
        Invocable invocable = (Invocable) engine;
        try {
            Object result = invocable.invokeFunction("matchVote", vote);
            return result != null && (result.equals(Boolean.TRUE) || result.equals(0));
        } catch (ScriptException | NoSuchMethodException e) {
            SuperbVote.getPlugin().getLogger().log(Level.WARNING, "Unable to execute 'matchVote' function in " + path, e);
            return false;
        }
    }
}
