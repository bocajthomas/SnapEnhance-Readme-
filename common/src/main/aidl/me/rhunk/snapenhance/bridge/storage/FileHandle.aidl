package me.rhunk.snapenhance.bridge.storage;

interface FileHandle {
    boolean exists();
    boolean create();
    boolean delete();

    @nullable ParcelFileDescriptor open(int mode);
}