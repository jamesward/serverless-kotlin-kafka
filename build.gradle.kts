plugins {
    kotlin("jvm") version "1.4.21" apply false
    kotlin("plugin.spring") version "1.4.21" apply false
}

allprojects {
    repositories {
        mavenCentral()
        jcenter()
        google()
    }
}
