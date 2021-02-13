plugins {
  kotlin("jvm") version "1.4.30" apply false
  kotlin("plugin.spring") version "1.4.30" apply false
}

allprojects {
  repositories {
    mavenCentral()
    jcenter()
    maven(url = "https://packages.confluent.io/maven")
    maven(url = "https://repository.mulesoft.org/nexus/content/repositories/public/")
  }
}
