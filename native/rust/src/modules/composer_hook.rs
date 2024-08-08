#![allow(dead_code, unused_imports)]

use super::util::composer_utils::{ComposerModule, ModuleTag};
use std::{collections::HashMap, ffi::{c_void, CStr}, sync::Mutex};
use jni::{objects::JString, sys::jobject, JNIEnv};
use once_cell::sync::Lazy;
use crate::{common, config, def_hook, dobby_hook, dobby_hook_sym, sig, util::get_jni_string};

const JS_TAG_BIG_DECIMAL: i64 = -11;
const JS_TAG_BIG_INT: i64 = -10;
const JS_TAG_BIG_FLOAT: i64 = -9;
const JS_TAG_SYMBOL: i64 = -8;
const JS_TAG_STRING: i64 = -7;
const JS_TAG_MODULE: i64 = -3;
const JS_TAG_FUNCTION_BYTECODE: i64 = -2;
const JS_TAG_OBJECT: i64 = -1;
const JS_TAG_INT: i64 = 0;
const JS_TAG_BOOL: i64 = 1;
const JS_TAG_NULL: i64 = 2;
const JS_TAG_UNDEFINED: i64 = 3;
const JS_TAG_UNINITIALIZED: i64 = 4;
const JS_TAG_CATCH_OFFSET: i64 = 5;
const JS_TAG_EXCEPTION: i64 = 6;
const JS_TAG_FLOAT64: i64 = 7;

#[repr(C)]
struct JsString {
    /*
    original structure : 
    struct JSString {
        struct JSRefCountHeader {
            int ref_count;
        };
        uint32_t len : 31;
        uint8_t is_wide_char : 1;
        uint32_t hash : 30;
        uint8_t atom_type : 2;
        uint32_t hash_next;

        union {
            uint8_t str8[0];
            uint16_t str16[0];
        } u;
    };
    */
    pad: [u32; 4],
    str8: [u8; 0],
    str16: [u16; 0],
}

#[repr(C)]
#[derive(Copy, Clone)]
union JsValueUnion {
    int32: i32,
    float64: f64,
    ptr: *mut c_void,
}

#[repr(C)]
#[derive(Copy, Clone)]
struct JsValue {
    u: JsValueUnion,
    tag: i64,
}

static AASSET_MAP: Lazy<Mutex<HashMap<usize, Vec<u8>>>> = Lazy::new(|| Mutex::new(HashMap::new()));
static COMPOSER_LOADER_DATA: Mutex<Option<String>> = Mutex::new(None);

def_hook!(
    aasset_get_length,
    i32,
    |arg0: *mut c_void| {
        if let Some(buffer) = AASSET_MAP.lock().unwrap().get(&(arg0 as usize)) {
            return buffer.len() as i32;
        }
        aasset_get_length_original.unwrap()(arg0)
    }
);

def_hook!(
    aasset_get_buffer,
    *const c_void,
    |arg0: *mut c_void| {
        if let Some(buffer) = AASSET_MAP.lock().unwrap().get(&(arg0 as usize)) {
            return buffer.as_ptr() as *const c_void;
        }
        aasset_get_buffer_original.unwrap()(arg0)
    }
);

def_hook!(
    aasset_manager_open,
    *mut c_void,
    |arg0: *mut c_void, arg1: *const u8, arg2: i32| {
        let handle = aasset_manager_open_original.unwrap()(arg0, arg1, arg2);

        let path = Lazy::new(|| CStr::from_ptr(arg1).to_str().unwrap());
        if !handle.is_null() && path.starts_with("bridge_observables") {
            let asset_buffer = aasset_get_buffer_original.unwrap()(handle);
            let asset_length = aasset_get_length_original.unwrap()(handle);
            debug!("asset buffer: {:p}, length: {}", asset_buffer, asset_length);

            let composer_loader = COMPOSER_LOADER_DATA.lock().unwrap().clone().expect("No composer loader data");

            let archive_buffer: Vec<u8> = std::slice::from_raw_parts(asset_buffer as *const u8, asset_length as usize).to_vec();
            let decompressed = zstd::stream::decode_all(&archive_buffer[..]).expect("Failed to decompress composer archive");
            let mut composer_module = ComposerModule::parse(decompressed).expect("Failed to parse composer module");

            let mut tags = composer_module.get_tags();
            let mut new_tags = Vec::new();

            for (tag1, _) in tags.iter_mut() {
                let name = tag1.to_string().unwrap();
                if !name.ends_with("src/utils/converter.js") {
                    continue;
                }

                let old_file_name = name.split_once(".").unwrap().0.to_owned() + rand::random::<u32>().to_string().as_str();
                tag1.set_buffer((old_file_name.to_owned() + ".js").as_bytes().to_vec());
                let original_module_path = path.split_once(".").unwrap().0.to_owned() + "/" + &old_file_name;

                let hooked_module = format!("{};module.exports = require(\"{}\");", composer_loader, original_module_path);

                new_tags.push(
                    (
                        ModuleTag::new(128, name.as_bytes().to_vec()),
                        ModuleTag::new(128, hooked_module.as_bytes().to_vec())
                    )
                );

                debug!("composer loader injected in {}", name);
                break;
            }

            tags.extend(new_tags);
            composer_module.set_tags(tags);

            let compressed = composer_module.to_bytes();
            let compressed = zstd::stream::encode_all(&compressed[..], 3).expect("Failed to compress");

            AASSET_MAP.lock().unwrap().insert(handle as usize, compressed);
        }
        handle
    }
);

def_hook!(
    aasset_close,
    c_void,
    |handle: *mut c_void| {
        AASSET_MAP.lock().unwrap().remove(&(handle as usize));
        aasset_close_original.unwrap()(handle)
    }
);
    
#[cfg(target_arch = "aarch64")]
static mut GLOBAL_INSTANCE: Option<*mut c_void> = None;
#[cfg(target_arch = "aarch64")]
static mut GLOBAL_CTX: Option<*mut c_void> = None;

#[cfg(target_arch = "aarch64")]
static mut JS_EVAL_ORIGINAL2: Option<unsafe extern "C" fn(*mut c_void, *mut c_void, *mut c_void, *mut u8, usize, *const u8, u32) -> JsValue> = None;

def_hook!(
    js_eval,
    *mut c_void,
    |arg0: *mut c_void, arg1: *mut c_void, arg2: *mut c_void, arg3: *const u8, arg4: *const u8, arg5: *const u8, arg6: *mut c_void, arg7: u32| {
        #[cfg(target_arch = "aarch64")]
        {
            GLOBAL_INSTANCE = Some(arg0);
            GLOBAL_CTX = Some(arg1);
        }
        js_eval_original.unwrap()(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7)
    }
);

pub fn set_composer_loader(mut env: JNIEnv, _: *mut c_void, code: JString) {
    let new_code = get_jni_string(&mut env, code).expect("Failed to get composer loader code");
    COMPOSER_LOADER_DATA.lock().unwrap().replace(new_code);
}

#[allow(unreachable_code, unused_variables)]
pub unsafe fn composer_eval(env: JNIEnv, _: *mut c_void, script: JString) -> jobject {
    #[cfg(target_arch = "aarch64")]
    {
        let mut env = env;

        let script_str = get_jni_string(&mut env, script).expect("Failed to get script");
        let script_length = script_str.len();
    
        let js_value = JS_EVAL_ORIGINAL2.expect("No js eval found")(
            GLOBAL_INSTANCE.expect("No global instance found"), 
            GLOBAL_CTX.expect("No global context found"),
            std::ptr::null_mut(),
            (script_str + "\0").as_ptr() as *mut u8, 
            script_length, 
            "<eval>\0".as_ptr(), 
            0
        );
    
        let result: String =  if js_value.tag == JS_TAG_STRING {
            let string = js_value.u.ptr as *mut JsString;
            CStr::from_ptr((*string).str8.as_ptr() as *const u8).to_str().unwrap().into()
        } else if js_value.tag == JS_TAG_INT {
            js_value.u.int32.to_string()
        } else if js_value.tag == JS_TAG_BOOL {
            if js_value.u.int32 == 1 { "true" } else { "false" }.into()
        } else if js_value.tag == JS_TAG_NULL {
            "null".into()
        } else if js_value.tag == JS_TAG_UNDEFINED {
            "undefined".into()
        } else if js_value.tag == JS_TAG_OBJECT {
            "[object]".into()
        } else if js_value.tag == JS_TAG_FLOAT64 {
            js_value.u.float64.to_string()
        } else if js_value.tag == JS_TAG_EXCEPTION {
            "Failed to evaluate script".into()
        } else {
            "[unknown tag ".to_owned() + &js_value.tag.to_string() + "]".into()
        };
        
        return env.new_string(result).unwrap().into_raw()
    }

    return env.new_string("Architecture not supported").unwrap().into_raw();
}

pub fn init() {
    if !config::native_config().composer_hooks {
        return
    }

    dobby_hook_sym!("libandroid.so", "AAsset_getBuffer", aasset_get_buffer);
    dobby_hook_sym!("libandroid.so", "AAsset_getLength", aasset_get_length);
    dobby_hook_sym!("libandroid.so", "AAsset_close", aasset_close);
    dobby_hook_sym!("libandroid.so", "AAssetManager_open", aasset_manager_open);
    
    #[cfg(target_arch = "aarch64")]
    {
        if let Some(signature) = sig::find_signature(
            &common::CLIENT_MODULE,
            "00 E4 00 6F 29 00 80 52 76 00 04 8B", -0x28,
            "A1 B0 07 92 81 46", -0x7
        ) {
            dobby_hook!(signature as *mut c_void, js_eval);
            
            unsafe { 
                JS_EVAL_ORIGINAL2 = Some(std::mem::transmute(js_eval_original.unwrap()));
            }
    
            debug!("js_eval {:#x}", signature);
        } else {
            warn!("Unable to find js_eval signature");
        }
    }
}

