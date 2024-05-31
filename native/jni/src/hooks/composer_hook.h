#pragma once
#include <stdio.h>

namespace ComposerHook {
    enum {
        JS_TAG_FIRST       = -11,
        JS_TAG_BIG_DECIMAL = -11,
        JS_TAG_BIG_INT     = -10,
        JS_TAG_BIG_FLOAT   = -9,
        JS_TAG_SYMBOL      = -8,
        JS_TAG_STRING      = -7,
        JS_TAG_MODULE      = -3,
        JS_TAG_FUNCTION_BYTECODE = -2,
        JS_TAG_OBJECT      = -1,

        JS_TAG_INT         = 0,
        JS_TAG_BOOL        = 1,
        JS_TAG_NULL        = 2,
        JS_TAG_UNDEFINED   = 3,
        JS_TAG_UNINITIALIZED = 4,
        JS_TAG_CATCH_OFFSET = 5,
        JS_TAG_EXCEPTION   = 6,
        JS_TAG_FLOAT64     = 7,
    };

    typedef struct JSRefCountHeader {
        int ref_count;
    } JSRefCountHeader;

    struct JSString {
        JSRefCountHeader header;
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

    typedef union JSValueUnion {
        int32_t int32;
        double float64;
        void *ptr;
    } JSValueUnion;

    typedef struct JSValue {
        JSValueUnion u;
        int64_t tag;
    } JSValue;

    typedef struct list_head {
        struct list_head *next, *prev;
    } list_head;

    struct JSGCObjectHeader {
        int ref_count;
        uint8_t gc_obj_type : 4;
        uint8_t mark : 4;
        uint8_t dummy1;
        uint16_t dummy2;
        struct list_head link;
    };

    struct JSContext {
        JSGCObjectHeader header;
        void *rt;
        struct list_head link;

        uint16_t binary_object_count;
        int binary_object_size;

        JSValue *array_shape;
        JSValue *class_proto;
        JSValue function_proto;
        JSValue function_ctor;
        JSValue array_ctor;
        JSValue regexp_ctor;
        JSValue promise_ctor;
        JSValue native_error_proto[8];
        JSValue iterator_proto;
        JSValue async_iterator_proto;
        JSValue array_proto_values;
        JSValue throw_type_error;
        JSValue eval_obj;

        JSValue global_obj;
        JSValue global_var_obj;
    };

    static void* global_instance;
    static JSContext *global_ctx;
    static std::string* composer_loader;

    HOOK_DEF(JSValue, js_eval, void* instance, JSContext *ctx, void* this_obj, char *input, uintptr_t input_len, const char *filename, unsigned int flags, unsigned int scope_idx) {
        if (global_instance == nullptr || global_ctx == nullptr) {
            global_instance = instance;
            global_ctx = ctx;

            if (composer_loader != nullptr) {
                LOGD("Injecting composer loader");
                composer_loader->resize(composer_loader->size() + input_len);
                memcpy((void*) (composer_loader->c_str() + composer_loader->size() - input_len), input, input_len);

                input = (char*) composer_loader->c_str();
                input_len = composer_loader->size();
            }
        } else {
            if (composer_loader != nullptr) {
                delete composer_loader;
                composer_loader = nullptr;
            }
        }
        return js_eval_original(instance, ctx, this_obj, input, input_len, filename, flags, scope_idx);
    }

    void setComposerLoader(JNIEnv *env, jobject, jstring code) {
        global_instance = nullptr;
        global_ctx = nullptr;
        auto code_str = env->GetStringUTFChars(code, nullptr);
        if (composer_loader != nullptr) delete composer_loader;
        composer_loader = new std::string(code_str, env->GetStringUTFLength(code));
        env->ReleaseStringUTFChars(code, code_str);
    }

    jstring composerEval(JNIEnv *env, jobject, jstring script) {
        if (!ARM64) return env->NewStringUTF("Architecture not supported");
        if (global_instance == 0 || global_ctx == nullptr) {
            return env->NewStringUTF("Composer not ready");
        }

        auto script_str = env->GetStringUTFChars(script, nullptr);
        auto length = env->GetStringUTFLength(script);
        auto jsvalue = js_eval_original(global_instance, global_ctx, (void*) &global_ctx->global_obj, (char *) script_str, length, "<eval>", 0, 0);
        env->ReleaseStringUTFChars(script, script_str);

        if (jsvalue.tag == JS_TAG_STRING) {
            auto str = (JSString *) jsvalue.u.ptr;
            return env->NewStringUTF((const char *) str->u.str8);
        }

        std::string result;
        switch (jsvalue.tag) {
            case JS_TAG_INT:
                result = std::to_string(jsvalue.u.int32);
                break;
            case JS_TAG_BOOL:
                result = jsvalue.u.int32 ? "true" : "false";
                break;
            case JS_TAG_NULL:
                result = "null";
                break;
            case JS_TAG_UNDEFINED:
                result = "undefined";
                break;
            case JS_TAG_OBJECT:
                result = "[object Object]";
                break;
            case JS_TAG_EXCEPTION:
                result = "Failed to evaluate script";
                break;
            case JS_TAG_FLOAT64:
                result = std::to_string(jsvalue.u.float64);
                break;
            default:
                result = "[unknown tag " + std::to_string(jsvalue.tag) + "]";
                break;
        }

        return env->NewStringUTF(result.c_str());
    }

    void init() {
        auto js_eval_ptr = util::find_signature(
            common::client_module.base,
            common::client_module.size,
            ARM64 ? "00 E4 00 6F 29 00 80 52 76 00 04 8B" : "A1 B0 07 92 81 46",
            ARM64 ? -0x28 : -0x7
        );
        if (js_eval_ptr == 0) {
            LOGE("js_eval_ptr signature not found");
            return;
        }
        SafeHook((void*) js_eval_ptr, (void *) js_eval, (void **) &js_eval_original);
    }
}