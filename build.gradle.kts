import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    kotlin("jvm") version "1.3.72"
    maven
    signing
}

group = "com.natpryce"
version = findProperty("-version") ?: "SNAPSHOT"

println("building version $version")

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))

    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.12")
    testImplementation("com.natpryce:hamkrest:1.4.0.0")
}

java {
    withSourcesJar()
}

tasks {
    jar {
        manifest {
            attributes(
                mapOf(
                    "Implementation-Title" to "konfig",
                    "Implementation-Vendor" to "com.natpryce",
                    "Implementation-Version" to project.version.toString()
                )
            )
        }
    }

    test {
        useJUnit()

        testLogging {
            events(TestLogEvent.PASSED, TestLogEvent.FAILED, TestLogEvent.SKIPPED)
        }
    }

    register("ossrhAuthentication") {
        doLast {
            if (!(project.hasProperty("ossrh.username") && project.hasProperty("ossrh.password"))) {
                throw InvalidUserDataException("no OSSRH username and/or password!")
            }
        }
    }
}

artifacts {
    add("archives", tasks["sourcesJar"])
}

signing {
    setRequired({ hasProperty("sign") || gradle.taskGraph.hasTask("uploadArchives") })
    listOf("jar", "sourcesJar").forEach { t -> sign(tasks[t]) }
}

apply(from = "publishing.gradle")
