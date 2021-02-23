plugins {
  kotlin("jvm") version "1.4.30" apply false
  kotlin("plugin.spring") version "1.4.30" apply false
  id("org.springframework.boot") version "2.4.2" apply false
  id("io.spring.dependency-management") version "1.0.11.RELEASE" apply false
}

allprojects {
  repositories {
    mavenCentral()
    jcenter()
    maven("https://packages.confluent.io/maven")
    maven("https://ksqldb-maven.s3.amazonaws.com/maven")
    maven("https://jenkins-confluent-packages-beta-maven.s3.amazonaws.com/6.2.0-beta201122193350-cp5/3/maven/")
    maven("https://jitpack.io")
    maven("https://repository.mulesoft.org/nexus/content/repositories/public/")
  }
}
