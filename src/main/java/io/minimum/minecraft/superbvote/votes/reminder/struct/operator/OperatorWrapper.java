package io.minimum.minecraft.superbvote.votes.reminder.struct.operator;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OperatorWrapper {

    private final String sign;
    private final SignOperator operator;

    public String sign() {
        return this.sign;
    }

    public SignOperator operator() {
        return this.operator;
    }
}