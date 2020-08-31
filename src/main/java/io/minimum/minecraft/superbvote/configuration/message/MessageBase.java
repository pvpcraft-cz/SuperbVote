package io.minimum.minecraft.superbvote.configuration.message;

import io.minimum.minecraft.superbvote.configuration.message.placeholder.PlaceholderProvider;
import io.minimum.minecraft.superbvote.votes.reminder.ParserUtil;

class MessageBase {

    String replace(String message, MessageContext context) {
        String replaced = message;
        for (PlaceholderProvider provider : ParserUtil.PROVIDER_LIST) {
            if (provider.canUse()) {
                replaced = provider.apply(replaced, context);
            }
        }
        return replaced;
    }
}
