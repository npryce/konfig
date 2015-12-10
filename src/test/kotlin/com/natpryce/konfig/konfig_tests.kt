package com.natpryce.konfig

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test
import java.io.File
import java.util.*
import kotlin.test.assertFalse
import kotlin.test.assertTrue


class FromProperties {
    val config = ConfigurationProperties(Properties().apply {
        setProperty("name", "alice")
        setProperty("x", "1")
        setProperty("y", "2")
    })

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

    @Test
    fun contains() {
        assertTrue(config.contains(name))
        assertTrue(config.contains(x))
        assertTrue(config.contains(y))
        assertFalse(config.contains(Key("bob", stringType)))
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

    @Test
    fun contains() {
        assertTrue(config.contains(x))
        assertTrue(config.contains(y))
        assertFalse(config.contains(Key("bob", stringType)))
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
        val config = EnvironmentVariables(prefix = "XXX_", lookup = { varname -> env[varname] })

        val name = Key("name", stringType)

        assertThat(config[name], equalTo("alice"))
    }
}

class OverridingAndFallingBack {
    val overrides = ConfigurationMap("x" to "XX", "z" to "ZZ", location = Location("overrides"))
    val defaults = ConfigurationMap("x" to "x", "y" to "y", location = Location("defaults"))

    val config = overrides overriding defaults

    val x = Key("x", stringType)
    val y = Key("y", stringType)
    val z = Key("z", stringType)

    @Test
    fun overrides_default_properties() {
        assertThat(config[x], equalTo("XX"))
        assertThat(config[y], equalTo("y"))
        assertThat(config[z], equalTo("ZZ"))
    }

    @Test
    fun contains() {
        assertTrue(config.contains(x))
        assertTrue(config.contains(y))
        assertTrue(config.contains(z))
        assertFalse(config.contains(Key("bob", stringType)))
    }

    @Test
    fun value_location() {
        assertThat(config.location(x), equalTo(overrides.location(x)))
        assertThat(config.location(y), equalTo(defaults.location(y)))
        assertThat(config.location(z), equalTo(overrides.location(z)))
    }
}

class ConfigSubset {
    val fullSet = ConfigurationMap(
            "a.one" to "a1",
            "a.two" to "a2",
            "b.one" to "b1",
            "b.two" to "b2",
            "b.three" to "b3")

    val subsetA = Subset("a", fullSet)
    val subsetB = Subset("b", fullSet)

    val key1 = Key("one", stringType)
    val key2 = Key("two", stringType)
    val key3 = Key("three", stringType)


    @Test
    fun subset_properties() {
        assertThat(subsetA[key1], equalTo("a1"))
        assertThat(subsetA[key2], equalTo("a2"))

        assertThat(subsetB[key1], equalTo("b1"))
        assertThat(subsetB[key2], equalTo("b2"))
    }

    @Test
    fun contains() {
        assertTrue(fullSet.contains(Key("a.one", stringType)))
        assertTrue(fullSet.contains(Key("b.one", stringType)))

        assertTrue(subsetA.contains(key1))
        assertTrue(subsetB.contains(key2))

        assertFalse(subsetA.contains(key3))
        assertTrue(subsetB.contains(key3))
    }

    @Test
    fun value_location() {
        assertThat(subsetA.location(key1), equalTo(fullSet.location(Key("a.one", stringType))))
        assertThat(subsetB.location(key2), equalTo(fullSet.location(Key("b.two", stringType))))
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