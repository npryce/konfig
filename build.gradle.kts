import org.jetbrains.dokka.gradle.DokkaTask

buildscript {
    repositories {
        jcenter()
    }

    dependencies {
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:0.9.15")
    }
}

plugins {
    kotlin("jvm") version "1.1.51"
    maven
    signing
    //id("org.jetbrains.dokka") version "0.9.15"
}

apply {
    from("groovy.gradle")
    plugin("org.jetbrains.dokka")
}

group = "com.natpryce"
version = findProperty("-version") ?: "SNAPSHOT"

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

        afterTest(closureOf { _: TestDescriptor, result: TestResult ->
            println(" -> ${result.resultType}")
        })
    }

    val dokka = "dokka"(DokkaTask::class){
        outputFormat = "javadoc"
        outputDirectory = "build/javadoc"
    }

    val ossrhAuthentication by creating {
        doLast {
            if (!(hasProperty("ossrh.username") && hasProperty("ossrh.password"))) {
                throw InvalidUserDataException("no OSSRH username and/or password!")
            }
        }
    }

    val sourcesJar by creating(Jar::class) {
        classifier = "sources"
        from(java.sourceSets["main"].allSource)
    }

    val javaDoc by creating(Jar::class) {
        dependsOn(dokka)
        classifier = "javadoc"
        from("build/javadoc")
    }

    artifacts {
        add("archives", sourcesJar.archivePath)
        add("archives", javaDoc.archivePath)
    }

    "uploadArchives"(Upload::class) {
        dependsOn(ossrhAuthentication)

        repositories {
            withConvention(MavenRepositoryHandlerConvention::class) {
                mavenDeployer {
                    beforeDeployment { signing.signPom(this) }

                    withGroovyBuilder {
                        "repository"("url" to uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")) {
                            "authentication"("userName" to properties["ossrh.username"], "password" to properties["ossrh.password"])
                        }
                        "snapshotRepository"("url" to uri("https://oss.sonatype.org/content/repositories/snapshots/")) {
                            "authentication"("userName" to properties["ossrh.username"], "password" to properties["ossrh.password"])
                        }
                    }

                    pom.project {
                        withGroovyBuilder {
                            "name"("Konfig")
                            "packaging"("jar")
                            "description"("Konfiguration for Cotlin... no, Configuration for Kotlin")
                            "url"("https://github.com/npryce/konfig")

                            "scm" {
                                "connection"("git@github.com:npryce/konfig.git")
                                "url"("https://github.com/npryce/konfig")
                            }

                            "licenses" {
                                "license" {
                                    "name"("Apache 2.0")
                                    "url"("http://opensource.org/licenses/Apache-2.0")
                                }
                            }

                            "developers" {
                                "developer" {
                                    "id"("npryce")
                                    "name"("Nat Pryce")
                                }
                                "developer" {
                                    "id"("dmcg")
                                    "name"("Duncan McGregor")
                                }
                            }
                        }
                    }

                    pom { withGroovyBuilder { } }
                }
            }
        }
    }
}

fun <T : Any, U : Any> Any.closureOf(action: (T) -> U) = KotlinClosure1(action, this, this)
fun <T : Any, U : Any, V : Any> Any.closureOf(action: (T, U) -> V) = KotlinClosure2(action, this, this)
