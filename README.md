# Konfig - A Type Safe Configuration API for Kotlin

[![Kotlin](https://img.shields.io/badge/kotlin-1.0.0-blue.svg)](http://kotlinlang.org)
[![Build Status](https://travis-ci.org/npryce/konfig.svg?branch=master)](https://travis-ci.org/npryce/konfig)
[![Maven Central](https://img.shields.io/maven-central/v/com.natpryce/konfig.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.natpryce%22%20AND%20a%3A%22konfig%22)

Konfig provides an extensible, type-safe API for configuration
properties gathered from multiple sources — built in resources, system
properties, property files, environment variables, command-line
arguments, etc.

A secondary goal of Konfig is to make configuration "self explanatory”.

Misconfiguration errors are reported with the location and “true name”
of the badly configured property. E.g. a program may look up a key
defined as `Key("http.port", intType)`. At runtime, it will be parsed
from an environment variable named `HTTP_PORT`. So the error message
reports the name of the environment variable, so that the user can
easily find and fix the error.

Configuration can be inspected and listed.  For example, it can be
exposed by HTTP to a network management system to help site
reliability engineers understand the current configuration of a
running application.



Getting Started
---------------

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
    val config = ConfigurationProperties.systemProperties() overriding
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
