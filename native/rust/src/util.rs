use std::error::Error;

use jni::{objects::JString, JNIEnv};

pub fn get_jni_string(env: &mut JNIEnv, obj: JString) -> Result<String, Box<dyn Error>> {
    let string = env.get_string(&obj)?;
    Ok(string.to_str()?.to_string())
}
