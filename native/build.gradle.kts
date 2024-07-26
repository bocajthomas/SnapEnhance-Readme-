plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
}

val nativeName = rootProject.ext.get("buildHash")

android {
    namespace = rootProject.ext["applicationId"].toString() + ".nativelib"
    compileSdk = 34
    buildToolsVersion = "34.0.0"
    ndkVersion = "26.3.11579264"

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        buildConfigField("String", "NATIVE_NAME", "\"$nativeName\"")
        packaging {
            jniLibs {
                excludes += "**/libdobby.so"
            }
        }
        externalNativeBuild {
            cmake {
                arguments += listOf(
                    "-DOBFUSCATED_NAME=$nativeName",
                    "-DBUILD_PACKAGE=${rootProject.ext["applicationId"]}",
                    "-DBUILD_NAMESPACE=${namespace!!.replace(".", "/")}"
                )
            }
            ndk {
                //noinspection ChromeOsAbiSupport
                abiFilters += properties["debug_abi_filters"]?.toString()?.split(",")
                    ?: listOf("arm64-v8a", "armeabi-v7a")
            }
        }
        minSdk = 28
    }

    externalNativeBuild {
        cmake {
            path("jni/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}