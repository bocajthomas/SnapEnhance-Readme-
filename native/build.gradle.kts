plugins {
    alias(libs.plugins.rust.android)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
}

val nativeName = rootProject.ext.get("buildHash")

android {
    namespace = rootProject.ext["applicationId"].toString() + ".nativelib"
    compileSdk = 34

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        buildConfigField("String", "NATIVE_NAME", "\"$nativeName\".toString()")
        minSdk = 28
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

cargo {
    module = "rust"
    libname = nativeName.toString()
    targetIncludes = arrayOf("libsnapenhance.so")
    targets = listOf("arm64", "arm")
}

fun getNativeFiles() = File(projectDir, "build/rustJniLibs/android").listFiles()?.flatMap { abiFolder ->
    abiFolder.takeIf { it.isDirectory }?.listFiles()?.toList() ?: emptyList()
}

tasks.register("cleanNatives") {
    doLast {
        println("Cleaning native files")
        getNativeFiles()?.forEach { file ->
            file.deleteRecursively()
        }
    }
}

tasks.named("preBuild").configure {
    dependsOn("cleanNatives", "cargoBuild")
    doLast {
        getNativeFiles()?.forEach { file ->
            if (file.name.endsWith(".so")) {
                println("Renaming ${file.absolutePath}")
                file.renameTo(File(file.parent, "lib$nativeName.so"))
            }
        }
    }
}
