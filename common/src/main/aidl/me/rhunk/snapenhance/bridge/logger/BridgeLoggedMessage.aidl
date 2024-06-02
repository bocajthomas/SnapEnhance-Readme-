package me.rhunk.snapenhance.bridge.logger;

parcelable BridgeLoggedMessage {
    long messageId;
    String conversationId;
    String userId;
    String username;
    long sendTimestamp;
    @nullable String groupTitle;
    byte[] messageData;
}