#pragma once

namespace CustomEmojiFont {
    HOOK_DEF(int, open_hook, const char *pathname, int flags, mode_t mode) {
        if (strstr(pathname, "/system/fonts/NotoColorEmoji.ttf") != 0 && common::native_config->custom_emoji_font_path[0] != 0) {
            pathname = common::native_config->custom_emoji_font_path;
        }
        return open_hook_original(pathname, flags, mode);
    }

    void init() {
        DobbyHook((void *) DobbySymbolResolver("libc.so", "open"), (void *)open_hook, (void **)&open_hook_original);
    }
}