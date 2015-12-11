# Konfig - A Type Safe Configuration API for Kotlin

[![Build Status](https://travis-ci.org/npryce/konfig.svg?branch=master)](https://travis-ci.org/npryce/konfig)

To get started, import `com.natpryce.konfig.*` and then:

1. define typed property keys

        val HTTP_PORT = Key("http.port", intType)

2. build a Configuration object that loads properties:

        val config = systemProperties() overriding
                     EnvironmentVariables() overriding
                     ConfigurationProperties.fromFile(File("/etc/myservice.properties")) overriding
                     ConfigurationProperties.fromResource("/defaults.properties")

3. look up properties by key:

        val port = config[HTTP_PORT] // port is an Int


Konfig can load properties from:

* Java property files and resources
* Java system properties
* Environment variables
* Hard-coded maps (with convenient syntax)
* Command-line parameters (with long and short option syntax)

Konfig can easily be extended with new property types and sources of configuration data.

Konfig can report where configuration properties are searched for and where they were found.
