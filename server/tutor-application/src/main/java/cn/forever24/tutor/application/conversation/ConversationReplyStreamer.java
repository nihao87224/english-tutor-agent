package cn.forever24.tutor.application.conversation;

import java.util.List;

public interface ConversationReplyStreamer {

    List<ConversationStreamEvent> streamReply(ConversationStreamContext context);
}
