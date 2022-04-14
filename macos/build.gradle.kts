plugins {
    java
    kotlin("jvm")
    id("dev.nokee.jni-library")
    id("dev.nokee.objective-cpp-language")
    id("com.google.devtools.ksp")
    `uber-jni-jar`
    `use-prebuilt-binaries`
    `apple-m1-toolchain`
}

dependencies {
    ksp(libs.autoservice.processor)
    implementation(libs.autoservice.annotations)
    compileOnly(kotlin("stdlib-jdk8"))
}

library {
    dependencies {
        jvmImplementation(projects.autoDarkModeBase)
        jvmLibImplementation(libs.darklaf.nativeUtils)
        nativeLibImplementation(libs.macos.appKit)
    }
    targetMachines.addAll(machines.macOS.x86_64, machines.macOS.architecture("arm64"))
    variants.configureEach {
        resourcePath.set("com/github/weisj/darkmode/${project.name}/${targetMachine.variantName}")
        sharedLibrary {
            val isArm = targetMachine.architectureString == "arm64"
            val minOs = if (isArm) "11" else "10.10"
            compileTasks.configureEach {
                compilerArgs.addAll("-mmacosx-version-min=$minOs")
                // Build type not modeled yet, assuming release
                optimizedBinary()
            }
            linkTask.configure {
                linkerArgs.addAll(
                    "-lobjc", "-mmacosx-version-min=$minOs",
                    // "-framework", "AppKit",
                )
            }
        }
    }
}
