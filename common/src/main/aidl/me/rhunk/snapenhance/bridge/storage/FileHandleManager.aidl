package me.rhunk.snapenhance.bridge.storage;

import me.rhunk.snapenhance.bridge.storage.FileHandle;

interface FileHandleManager {
    @nullable FileHandle getFileHandle(String scope, String name);
}