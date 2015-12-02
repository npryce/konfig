package com.natpryce.konfig

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test
import java.io.File
import java.util.*


class FromProperties {
    val config = ConfigurationProperties(Properties().apply {
        setProperty("name", "alice")
        setProperty("x", "1")
        setProperty("y", "2")
    })

    val name = Key("name") { it }
    val x = Key("x", intType)
    val y = Key("y", intType)

    @Test
    fun looks_up_string_properties() {
        assertThat(config[name], equalTo("alice"))
    }

    @Test
    fun looks_up_int_properties() {
        assertThat(config[x], equalTo(1))
        assertThat(config[y], equalTo(2))
    }
}

class FromMap {
    val config = ConfigurationMap("name" to "alice", "x" to "1", "y" to "2")

    val name = Key("name", stringType)
    val x = Key("x", intType)
    val y = Key("y", intType)

    @Test
    fun looks_up_string_properties() {
        assertThat(config[name], equalTo("alice"))
    }

    @Test
    fun looks_up_int_properties() {
        assertThat(config[x], equalTo(1))
        assertThat(config[y], equalTo(2))
    }

}

class FromEnvironment {
    @Test
    fun translates_property_name_to_upper_case() {
        val env = mapOf("X" to "1", "Y" to "2")
        val config = EnvironmentVariables(lookup = { varname -> env[varname] })

        val x = Key("x", intType)
        val y = Key("y", intType)

        assertThat(config[x], equalTo(1))
        assertThat(config[y], equalTo(2))
    }

    @Test
    fun translates_dotted_property_name_to_upper_case_and_underscore() {
        val env = mapOf("NAME_FIRST" to "alice", "NAME_LAST" to "band")
        val config = EnvironmentVariables(lookup = { varname -> env[varname] })

        val firstName = Key("name.first", stringType)
        val lastName = Key("name.last", stringType)

        assertThat(config[firstName], equalTo("alice"))
        assertThat(config[lastName], equalTo("band"))
    }

    @Test
    fun environment_variables_can_be_prefixed() {
        val env = mapOf("XXX_NAME" to "alice")
        val config = EnvironmentVariables(prefix="XXX_", lookup = { varname -> env[varname] })

        val name = Key("name", stringType)

        assertThat(config[name], equalTo("alice"))
    }
}

class OverridingAndFallingBack {
    @Test
    fun overrides_default_properties() {
        val defaults = ConfigurationMap("x" to "x", "y" to "y")
        val overrides = ConfigurationMap("x" to "XX", "z" to "ZZ")

        val config = overrides overriding defaults

        assertThat(config[Key("x", stringType)], equalTo("XX"))
        assertThat(config[Key("y", stringType)], equalTo("y"))
        assertThat(config[Key("z", stringType)], equalTo("ZZ"))
    }
}

class ConfigSubset {
    @Test
    fun subset_properties() {
        val original = ConfigurationMap("a.one" to "a1", "a.two" to "a2", "b.one" to "b1", "b.two" to "b2")

        val configA = Subset("a", original)
        val configB = Subset("b", original)

        val key1 = Key("one", stringType)
        val key2 = Key("two", stringType)

        assertThat(configA[key1], equalTo("a1"))
        assertThat(configA[key2], equalTo("a2"))

        assertThat(configB[key1], equalTo("b1"))
        assertThat(configB[key2], equalTo("b2"))
    }
}

class FromResources {
    val a = Key("a", intType)
    val b = Key("b", stringType)

    @Test
    fun can_load_from_resource() {
        val config = ConfigurationProperties.fromResource(javaClass, "example.properties")

        assertThat(config[a], equalTo(1))
        assertThat(config[b], equalTo("two"))
    }

    @Test
    fun can_load_from_absolute_resource() {
        val config = ConfigurationProperties.fromResource("com/natpryce/konfig/example.properties")

        assertThat(config[a], equalTo(1))
        assertThat(config[b], equalTo("two"))
    }

    @Test
    fun can_load_from_file() {
        val config = ConfigurationProperties.fromFile(File("src/test/resources/com/natpryce/konfig/example.properties"))

        assertThat(config[a], equalTo(1))
        assertThat(config[b], equalTo("two"))
    }
}