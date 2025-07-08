plugins {
    kotlin("jvm") version "2.1.10"
    application
    id("com.gradleup.shadow") version "9.0.0-rc1"
}

group = "net.saifs"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.7.3")
    implementation("org.springframework:spring-context:6.2.7")
    implementation("org.apache.logging.log4j:log4j-api:2.24.3")
    implementation("org.apache.logging.log4j:log4j-core:2.24.3")
    implementation("org.duckdb:duckdb_jdbc:1.3.1.0")
    implementation("aws.sdk.kotlin:s3:1.4.119")
    implementation("io.micrometer:micrometer-core:1.15.1")
    implementation("io.micrometer:micrometer-registry-influx:1.15.1")
    implementation("com.github.ajalt.clikt:clikt:5.0.3")
    implementation("com.zaxxer:HikariCP:5.0.1")
}

application {
    mainClass.set("net.saifs.reactivemigrations.lifecycle.StartupKt")
}