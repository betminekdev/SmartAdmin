plugins {
    java
}

group = "cz.betminekdev"
version = "0.1.1-beta"

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
}

val smartAdminSelfTest by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "Runs lightweight SmartAdmin logic self-tests."

    val testSourceSet = sourceSets.test.get()
    classpath = testSourceSet.runtimeClasspath
    mainClass.set("cz.betminekdev.smartadmin.SelfTest")
}

tasks.jar {
    archiveBaseName.set("SmartAdmin")
}

tasks.test {
    dependsOn(smartAdminSelfTest)
    exclude("**/*")
}
