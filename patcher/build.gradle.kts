plugins {
    kotlin("jvm") version "1.8.0"
    `java-library`
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    implementation("io.github.microutils:kotlin-logging:4.0.0-beta-2")

    api("org.ow2.asm:asm:9.5")
    api("org.ow2.asm:asm-util:9.5")
    api("org.ow2.asm:asm-tree:9.5")
    api("io.github.classgraph:classgraph:4.8.157")
}
