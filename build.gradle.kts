plugins {
    kotlin("jvm") version "2.2.20"
    application
}

group = "io.github.flaredgitee"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.5")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}

application {
    // Main entry - Kotlin produces a MainKt class for top-level main
    mainClass.set("io.github.flaredgitee.MainKt")
}

// Add a fatJar task so users can run `java -jar build/libs/<name>-all.jar` directly.
tasks.register<Jar>("fatJar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveClassifier.set("all")
    manifest {
        attributes["Main-Class"] = "io.github.flaredgitee.MainKt"
    }
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith(".jar") }.map { zipTree(it) }
    })
}
