plugins {
    application
    kotlin("jvm") version "1.7.20"
    kotlin("plugin.serialization") version "1.7.20"
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://jitpack.io")
    }
}

application {
    mainClass.set("com.example.AppKt")
}

dependencies {
    implementation("com.github.ajalt.clikt:clikt:3.5.0")
    implementation("io.ktor:ktor-client-core:2.1.2")
    implementation("io.ktor:ktor-client-cio:2.1.2")
    implementation("io.ktor:ktor-client-content-negotiation:2.1.2")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.1.2")
}
