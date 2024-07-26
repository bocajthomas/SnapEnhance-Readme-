#pragma once

#include <map>

namespace LinkerHook {
    static auto linker_openat_hooks = std::map<std::string, std::pair<uintptr_t, size_t>>();

    void JNICALL addLinkerSharedLibrary(JNIEnv *env, jobject, jstring path, jbyteArray content) {
        const char *path_str = env->GetStringUTFChars(path, nullptr);
        jsize content_len = env->GetArrayLength(content);
        jbyte *content_ptr = env->GetByteArrayElements(content, nullptr);

        auto allocated_content = (jbyte *) malloc(content_len);
        memcpy(allocated_content, content_ptr, content_len);
        linker_openat_hooks[path_str] = std::make_pair((uintptr_t) allocated_content, content_len);

        LOGD("added linker hook for %s, size=%d", path_str, content_len);

        env->ReleaseStringUTFChars(path, path_str);
        env->ReleaseByteArrayElements(content, content_ptr, JNI_ABORT);
    }

    HOOK_DEF(int, linker_openat, int dirfd, const char *pathname, int flags, mode_t mode) {
        for (const auto &item: linker_openat_hooks) {
            if (strstr(pathname, item.first.c_str())) {
                LOGD("found openat hook for %s", pathname);
                static auto memfd_create = (int (*)(const char *, unsigned int)) DobbySymbolResolver("libc.so", "memfd_create");
                auto fd = memfd_create("me.rhunk.snapenhance", 0);
                LOGD("memfd created: %d", fd);

                if (fd == -1) {
                    LOGE("memfd_create failed: %d", errno);
                    return -1;
                }
                if (write(fd, (void *) item.second.first, item.second.second) == -1) {
                    LOGE("write failed: %d", errno);
                    return -1;
                }
                lseek(fd, 0, SEEK_SET);

                free((void *) item.second.first);
                linker_openat_hooks.erase(item.first);

                LOGD("memfd written");
                return fd;
            }
        }
        return linker_openat_original(dirfd, pathname, flags, mode);
    }

    void init() {
        DobbyHook((void *) DobbySymbolResolver(ARM64 ? "linker64" : "linker", "__dl___openat"), (void *) linker_openat, (void **) &linker_openat_original);
    }
}