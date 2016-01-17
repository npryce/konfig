# Konfig - A Type Safe Configuration API for Kotlin

[![Build Status](https://travis-ci.org/npryce/konfig.svg?branch=master)](https://travis-ci.org/npryce/konfig)
[![Maven Central](https://img.shields.io/maven-central/v/com.natpryce/konfig.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.natpryce%22%20AND%20a%3A%22konfig%22)

To get started, add `com.natpryce:konfig:<version>` as a dependency, import `com.natpryce.konfig.*` and then:

1. define typed property keys

        object http : PropertyGroup() {
            val port by intType
        }

2. build a Configuration object that loads properties:

        val config = systemProperties() overriding
                     EnvironmentVariables() overriding
                     ConfigurationProperties.fromFile(File("/etc/myservice.properties")) overriding
                     ConfigurationProperties.fromResource("defaults.properties")

3. define some properties.  For example, in `defaults.properties`:

        http.port=8080

4. look up properties by key:

        val port = config[http.port]
        
        println(port)
        // prints 8080


Konfig can load properties from:

* Java property files and resources
* Java system properties
* Environment variables
* Hard-coded maps (with convenient syntax)
* Command-line parameters (with long and short option syntax)

Konfig can easily be extended with new property types and sources of configuration data.

Konfig can report where configuration properties are searched for and where they were found.
