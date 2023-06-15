plugins {
    kotlin("jvm") version "1.8.0"
    `java-library`
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

// Use "api" for dependencies that are part of the public API of the library.
dependencies {
    implementation("org.slf4j:slf4j-api:2.0.7")
    implementation(project(":patcher"))

    testImplementation(kotlin("test"))
}
