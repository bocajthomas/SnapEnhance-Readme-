package me.rhunk.snapenhance.bridge.location;

parcelable FriendLocation {
    String username;
    @nullable String displayName;
    @nullable String bitmojiId;
    @nullable String bitmojiSelfieId;
    double latitude;
    double longitude;
    long lastUpdated;
    String locality;
    List<String> localityPieces;
}
