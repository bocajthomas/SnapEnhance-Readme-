use std::{ffi::CStr, fs};

use nix::libc::{self, c_uint};

use crate::{config, def_hook, dobby_hook_sym};

def_hook!(
    open_hook,
    i32,
    |path: *const u8, flags: i32, mode: c_uint| {
        if let Ok(pathname) = CStr::from_ptr(path).to_str() {
            if pathname == "/system/fonts/NotoColorEmoji.ttf"  {
                if let Some(font_path) = config::native_config().custom_emoji_font_path {
                    if fs::metadata(&font_path).is_ok() {
                        return libc::openat(libc::AT_FDCWD, font_path.as_ptr() as *const u8, flags, mode);
                    } else {
                        warn!("custom emoji font path does not exist: {}", font_path);
                    }
                }
            }
        }

        open_hook_original.unwrap()(path, flags, mode)
    }
);


pub fn init() {
    if config::native_config().custom_emoji_font_path.is_none() {
        return;
    }

    dobby_hook_sym!("libc.so", "open", open_hook);
}