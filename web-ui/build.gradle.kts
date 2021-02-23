import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("plugin.spring")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    implementation(project(":common"))
    implementation(kotlin("reflect"))
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    implementation("com.github.gAmUssA:reactor-ksqldb:e81f4a7fc0")
    implementation("io.confluent.ksql:ksqldb-api-client:0.15.0") {
        exclude("org.slf4j", "slf4j-log4j12")
    }

    implementation("org.webjars:bootstrap:4.5.3")
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.7.2")

    testImplementation("org.testcontainers:kafka:1.15.1")
    testImplementation("io.confluent:kafka-json-schema-serializer:6.0.0")
    testImplementation("com.github.gAmUssA:testcontainers-java-module-confluent-platform:master-SNAPSHOT")
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
    args("--spring.profiles.active=dev")
    classpath += sourceSets["test"].runtimeClasspath
}

application {
    mainClass.set("skk.MainKt")
}
