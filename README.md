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
    val server_port = Key("server.port", intType)
    val server_host = Key("server.host", stringType)
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
    val server = Server(config[server_port], config[server_host])
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

# Naming of Properties

Konfig's Configuration objects expect property names to follow Java property name conventions: dots to represent hierarchy, lower-case identifiers within the hierarchy, hyphens to separate words in those identifiers. 

For example: `servers.file-storage.s3-api-key`, `servers.file-storage.s3-bucket`.

Each Configuration implementation maps from that naming convetion to the convention used by the underlying configuration store. E.g. the `EnvironmentVariables` implementation maps Java property name convention to the upper-case-and-underscores convention used for Unix environment variables.

Configuration is an interface and Key<T> is a data class. This makes it straight forward to write an implementation of Configuration that translates the names of keys to different naming conventions, if your configuration follows an unusual convention.


# Reflectomagic key definition

Konfig has a few ways to reduce boilerplate code when defining configuration keys.

1) You can use Kotlin's delgated property protocol to name keys after the constants that hold them:

    ```
    val host by stringType
    val port by intType
    
    ...
    
    val client = TcpClient(configuration[host], configuration[port])
    ```

2) You can declare objects that extend PropertyGroup to define hierarchies of property keys that follow the Konfig naming conventions described above:

    ```
    object server : PropertyGroup() {
        val base_uri by uriType   // defines a key named "server.base-uri"
        val api_key by stringType // defines a key named "server.api-key"
    }

    ...

    val client = HttpClient(configuration[server.base_uri], configuration[server.api_key])
    ```

