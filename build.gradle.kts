plugins {
    java
}

group = "cz.betminekdev"
version = "0.4.0-beta"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    testImplementation("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    testRuntimeOnly("org.xerial:sqlite-jdbc:3.53.4.0")
}

val smartAdminSelfTest by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "Runs SmartAdmin regression and SQLite integration tests."
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(21))
    })

    val testSourceSet = sourceSets.test.get()
    classpath = testSourceSet.runtimeClasspath
    mainClass.set("cz.betminekdev.smartadmin.SelfTest")
}

tasks.jar {
    archiveBaseName.set("SmartAdmin")
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

tasks.test {
    dependsOn(smartAdminSelfTest)
    exclude("**/*")
}
