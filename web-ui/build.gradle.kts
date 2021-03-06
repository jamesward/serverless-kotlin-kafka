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
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    runtimeOnly("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    implementation("org.springframework.boot:spring-boot-starter-webflux")

    implementation("org.webjars:bootstrap:4.5.3")
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.7.2")

    testImplementation("org.testcontainers:kafka:1.15.2")

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
    dependsOn(":ws-to-kafka:bootBuildImage")
    dependsOn(":ksqldb-setup:bootBuildImage")
    args("--spring.profiles.active=dev")
    classpath += sourceSets["test"].runtimeClasspath
}

application {
    mainClass.set("skk.MainKt")
}
