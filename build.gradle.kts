apply {
    from("groovy.gradle")
}

plugins {
    kotlin("jvm") version "1.1.3"
//    id("org.jetbrains.dokka") version "0.9.12"
}

dependencies {
}


repositories {
    mavenCentral()
    jcenter()
}
