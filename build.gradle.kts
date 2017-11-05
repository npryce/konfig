apply {
    from("groovy.gradle")
}

plugins {
    kotlin("jvm") version "1.1.3"
    maven
    signing
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

tasks {
    "jar"(Jar::class) {
        manifest.attributes.putAll(mapOf(
                "Implementation-Title" to "konfig",
                "Implementation-Vendor" to "com.natpryce",
                "Implementation-Version" to version
        ))
    }

    "test"(Test::class) {
        include("com/natpryce/konfig/**")
        isScanForTestClasses = true
        reports {
            junitXml.isEnabled = true
            html.isEnabled = true
        }

        beforeTest(closureOf { descriptor: TestDescriptor ->
            println("${descriptor.className?.substring("com.natpryce.konfig.".length)}: ${descriptor.name.replace("_", " ")}")
        })

        afterTest(closureOf { descriptor: TestDescriptor, result: TestResult ->
            println(" -> ${result.resultType}")
        })
    }

    create("dokka") {

    }
}

fun <T : Any, U : Any> Any.closureOf(action: (T) -> U) = KotlinClosure1(action, this, this)
fun <T : Any, U : Any, V: Any> Any.closureOf(action: (T, U) -> V) = KotlinClosure2(action, this, this)
