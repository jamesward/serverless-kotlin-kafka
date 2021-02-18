import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    id("org.springframework.boot") version "2.4.2"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    kotlin("jvm")
    kotlin("plugin.spring")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    implementation(kotlin("reflect"))
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.4.2")

    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("io.projectreactor.kafka:reactor-kafka")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    implementation("org.webjars:bootstrap:4.5.3")
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.7.2")

    testImplementation("org.testcontainers:kafka:1.15.1")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
}

tasks.withType<org.springframework.boot.gradle.tasks.run.BootRun> {
    dependsOn("testClasses")
    classpath += sourceSets["test"].runtimeClasspath
}

application {
    mainClass.set("skk.MainKt")
}
