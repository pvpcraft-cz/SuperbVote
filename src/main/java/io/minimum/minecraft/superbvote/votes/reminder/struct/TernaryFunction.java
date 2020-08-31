package io.minimum.minecraft.superbvote.votes.reminder.struct;

public interface TernaryFunction<S, U, V> {
    S apply(U input1, V input2);
}