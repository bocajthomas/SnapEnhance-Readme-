plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
}

android {
    namespace = rootProject.ext["applicationId"].toString() + ".composer"
    compileSdk = 34

    sourceSets {
        getByName("main") {
            assets.srcDirs("build/assets")
        }
    }
}

task("compileTypeScript") {
    doLast {
        project.exec {
            commandLine("npx", "--yes", "tsc", "--project", "tsconfig.json")
        }
        project.exec {
            commandLine("npx", "--yes", "rollup", "--config", "rollup.config.js", "--bundleConfigAsCjs")
        }
        project.copy {
            from("build/loader.js")
            into("build/assets/composer")
        }
    }
}

tasks.named("preBuild").configure {
    dependsOn("compileTypeScript")
}
