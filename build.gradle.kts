apply {
    from("groovy.gradle")
}

plugins {
    kotlin("jvm") version "1.1.3"
//    id("org.jetbrains.dokka") version "0.9.12"
}

group = "com.natpryce"
version = property("-version") ?: "SNAPSHOT"

dependencies {
    compile(kotlin("stdlib"))
    compile(kotlin("reflect"))

    testCompile(kotlin("test"))
    testCompile("junit", "junit", "4.+")
    testCompile("com.natpryce", "hamkrest", "1.+")
}


repositories {
    mavenCentral()
    jcenter()
}
