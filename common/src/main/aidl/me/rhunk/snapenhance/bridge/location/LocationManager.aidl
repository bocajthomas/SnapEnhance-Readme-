package me.rhunk.snapenhance.bridge.location;

import me.rhunk.snapenhance.bridge.location.FriendLocation;

interface LocationManager {
    void provideFriendsLocation(in List<FriendLocation> friendsLocation);
}