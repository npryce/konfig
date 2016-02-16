# Konfig - A Type Safe Configuration API for Kotlin

[![Kotlin](https://img.shields.io/badge/kotlin-1.0.0-blue.svg)](http://kotlinlang.org)
[![Build Status](https://travis-ci.org/npryce/konfig.svg?branch=master)](https://travis-ci.org/npryce/konfig)
[![Maven Central](https://img.shields.io/maven-central/v/com.natpryce/konfig.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.natpryce%22%20AND%20a%3A%22konfig%22)

To get started, add `com.natpryce:konfig:<version>` as a dependency, import `com.natpryce.konfig.*` and then:

1. Define typed property keys

    ```kotlin
    object server : PropertyGroup() {
        val port by intType
        val host by stringType
    }
    ```

2. Build a Configuration object that loads properties:

    ```kotlin
    val config = systemProperties() overriding
                 EnvironmentVariables() overriding
                 ConfigurationProperties.fromFile(File("/etc/myservice.properties")) overriding
                 ConfigurationProperties.fromResource("defaults.properties")
    ```

3. Define some properties.  For example, in `defaults.properties`:

    ```properties
    server.port=8080
    server.host=0.0.0.0
    ```
    
4. Look up properties by key. They are returned as typed values, not strings, and so can be used directly:

    ```kotlin
    val server = Server(config[server.port], config[server.host])
    server.start()
    ```


Konfig can load properties from:

* Java property files and resources
* Java system properties
* Environment variables
* Hard-coded maps (with convenient syntax)
* Command-line parameters (with long and short option syntax)

Konfig can easily be extended with new property types and sources of configuration data.

Konfig can report where configuration properties are searched for and where they were found.
