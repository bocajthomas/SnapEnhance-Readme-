#![allow(dead_code, unused_mut)] 

use std::{cell::Cell, ffi::{c_void, CStr}, sync::Mutex};
use jni::{objects::JString, sys::jobject, JNIEnv};
use crate::{common, config, def_hook, dobby_hook, sig, util::get_jni_string};

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

static mut GLOBAL_INSTANCE: Option<*mut c_void> = None;
static mut GLOBAL_CTX: Option<*mut c_void> = None;
static COMPOSER_LOADER_DATA: Mutex<Cell<Option<Box<String>>>> = Mutex::new(Cell::new(None));

static mut JS_EVAL_ORIGINAL2: Option<unsafe extern "C" fn(*mut c_void, *mut c_void, *mut c_void, *mut u8, usize, *const u8, u32) -> JsValue> = None;

def_hook!(
    js_eval,
    *mut c_void,
    |arg0: *mut c_void, arg1: *mut c_void, arg2: *mut c_void, arg3: *const u8, arg4: *const u8, arg5: *const u8, arg6: *mut c_void, arg7: u32| {
        let mut arg3 = arg3;
        let mut arg4 = arg4;
        let mut arg5 = arg5;

        if GLOBAL_INSTANCE.is_none() || GLOBAL_CTX.is_none() {
            GLOBAL_INSTANCE = Some(arg0);
            GLOBAL_CTX = Some(arg1);

            let mut loader_data = COMPOSER_LOADER_DATA.lock().unwrap();
            let mut loader_data = loader_data.get_mut();

            let loader_data = loader_data.as_mut().unwrap();
            loader_data.push_str("\n");
            #[cfg(target_arch = "aarch64")]
            {
                loader_data.push_str(CStr::from_ptr(arg3).to_str().unwrap());
                arg3 = loader_data.as_mut_ptr();
                arg4 = loader_data.len() as *const u8;
            }

            // On arm the original JS_Eval function is inlined so the arguments are shifted
            #[cfg(target_arch = "arm")]
            {
                loader_data.push_str(CStr::from_ptr(arg4).to_str().unwrap());
                arg4 = loader_data.as_mut_ptr();
                arg5 = loader_data.len() as *const u8;
            }

            debug!("injected composer loader!");
        } else {
            COMPOSER_LOADER_DATA.lock().unwrap().take();
        }

        js_eval_original.unwrap()(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7)
    }
);

pub fn set_composer_loader(mut env: JNIEnv, _: *mut c_void, code: JString) {
    let new_code = get_jni_string(&mut env, code).expect("Failed to get code");
   
    COMPOSER_LOADER_DATA.lock().unwrap().replace(Some(Box::new(new_code)));
}

#[allow(unreachable_code, unused_variables)]
pub unsafe fn composer_eval(env: JNIEnv, _: *mut c_void, script: JString) -> jobject {
    #[cfg(not(target_arch = "aarch64"))]
    {
        return env.new_string("Architecture not supported").unwrap().into_raw();
    }

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
    
    env.new_string(result).unwrap().into_raw()
}

pub fn init() {
    if !config::native_config().composer_hooks {
        return
    }
    
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

