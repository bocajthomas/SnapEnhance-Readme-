#pragma once

#include <sys/stat.h>

namespace CustomEmojiFont {
    HOOK_DEF(int, open_hook, const char *pathname, int flags, mode_t mode) {
        auto custom_path = common::native_config->custom_emoji_font_path;
        if (strstr(pathname, "/system/fonts/NotoColorEmoji.ttf") != 0 && custom_path[0] != 0) {
            struct stat buffer;
            if (stat(custom_path, &buffer) == 0) {
                pathname = custom_path;
            }
        }
        return open_hook_original(pathname, flags, mode);
    }

    void init() {
        SafeHook((void *) DobbySymbolResolver("libc.so", "open"), (void *)open_hook, (void **)&open_hook_original);
    }
}