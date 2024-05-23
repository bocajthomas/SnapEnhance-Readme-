plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
}

android {
    namespace = rootProject.ext["applicationId"].toString() + ".composer"
    compileSdk = 34
}

task("compileTypeScript") {
    doLast {
        project.exec {
            commandLine("npx", "--yes", "tsc", "--project", "tsconfig.json")
        }
        project.exec {
            commandLine("npx", "--yes", "rollup", "--config", "rollup.config.js", "--bundleConfigAsCjs")
        }
        for (buildType in listOf("debug", "release")) {
            project.copy {
                from("build/loader.js")
                into("build/intermediates/library_assets/$buildType/out/composer")
            }
        }
    }
}

tasks.named("preBuild").configure {
    dependsOn("compileTypeScript")
}
