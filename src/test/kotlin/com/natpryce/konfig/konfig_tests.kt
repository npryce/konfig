package com.natpryce.konfig

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import org.junit.Test
import java.io.File
import java.util.Properties
import kotlin.test.assertFalse
import kotlin.test.assertTrue


class FromProperties {
    val config = ConfigurationProperties(location = Location("location"), properties = Properties().apply {
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
    
    @Test
    fun in_operator() {
        assertTrue(name in config)
        assertTrue(x in config)
        assertTrue(y in config)
        assertFalse(Key("bob", stringType) in config)
    }
    
    @Test
    fun reports_entire_contents() {
        val expectedPropertiesAsMap = mapOf(
            "name" to "alice",
            "x" to "1",
            "y" to "2")
        
        assertThat(config.list(), equalTo(listOf(config.location to expectedPropertiesAsMap)))
    }
    
}

class FromMap {
    val map = mapOf("name" to "alice", "x" to "1", "y" to "2")
    val config = ConfigurationMap(map, Location("from-map"))
    
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
    
    @Test
    fun reports_entire_contents() {
        assertThat(config.list(), equalTo(listOf(config.location to map)))
    }
}

class FromEnvironment {
    @Test
    fun translates_property_name_to_upper_case() {
        val env = mapOf("X" to "1", "Y" to "2")
        val config = EnvironmentVariables(lookup = { env[it] })
        
        val x = Key("x", intType)
        val y = Key("y", intType)
        
        assertThat(config[x], equalTo(1))
        assertThat(config[y], equalTo(2))
    }
    
    @Test
    fun translates_dotted_property_name_to_upper_case_and_underscore() {
        val env = mapOf("NAME_FIRST" to "alice", "NAME_LAST" to "band")
        val config = EnvironmentVariables(lookup = { env[it] })
        
        val firstName = Key("name.first", stringType)
        val lastName = Key("name.last", stringType)
        
        assertThat(config[firstName], equalTo("alice"))
        assertThat(config[lastName], equalTo("band"))
    }
    
    @Test
    fun translates_hyphens_in_property_name_to_underscore_in_envvar_name() {
        val env = mapOf("SEARCH_ENGINE_HTTP_PORT" to "9090")
        val config = EnvironmentVariables(lookup = { env[it] })
        
        assertThat(config[Key("search-engine.http-port", intType)], equalTo(9090))
    }
    
    @Test
    fun environment_variables_can_be_prefixed() {
        val env = mapOf("XXX_NAME" to "alice")
        val config = EnvironmentVariables(prefix = "XXX_", lookup = { env[it] })
        
        val name = Key("name", stringType)
        
        assertThat(config[name], equalTo("alice"))
    }
    
    @Test
    fun lists_only_those_variables_that_start_with_the_prefix() {
        val env = mapOf("PREFIX_A" to "1", "PREFIX_B" to "2", "OTHER_C" to "3")
        val config = EnvironmentVariables(prefix = "PREFIX_", lookup = { env[it] }, all = { env })
        
        assertThat(config.list(), equalTo(listOf(config.location to mapOf("PREFIX_A" to "1", "PREFIX_B" to "2"))))
    }
    
    @Test
    fun lists_all_variables_when_no_prefix() {
        val env = mapOf("X" to "1", "Y" to "2")
        val config = EnvironmentVariables(lookup = { env[it] }, all = { env })
        
        assertThat(config.list(), equalTo(listOf(config.location to env)))
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
    fun search_path() {
        for (k in listOf(x, y, z)) {
            assertThat(config.searchPath(k), equalTo(listOf(
                PropertyLocation(k, overrides.location, k.name),
                PropertyLocation(k, defaults.location, k.name))))
        }
    }
    
    @Test
    fun missing_property_diagnostic_lists_both_searched_locations() {
        val missing = Key("missing.property.name", stringType)
        
        val e = expectThrown<Misconfiguration> { config[missing] }
        
        assertThat(e.message, present(containsSubstring("missing.property.name in overrides")))
        assertThat(e.message, present(containsSubstring("missing.property.name in defaults")))
    }
    
    @Test
    fun lists_both_configurations_in_priority_order() {
        assertThat(config.list(), equalTo(overrides.list() + defaults.list()))
    }
}

class SearchConfigurationList {
    val configA = ConfigurationMap("a" to "A1", location = Location("a"))
    val configB = ConfigurationMap("a" to "A2", "b" to "B1", location = Location("b"))
    val configC = ConfigurationMap("a" to "A3", "b" to "B2", "c" to "C1", location = Location("c"))
    
    val config = search(configA, configB, configC)
    
    val a = Key("a", stringType)
    val b = Key("b", stringType)
    val c = Key("c", stringType)
    
    @Test
    fun overrides_default_properties() {
        assertThat(config[a], equalTo("A1"))
        assertThat(config[b], equalTo("B1"))
        assertThat(config[c], equalTo("C1"))
    }
    
    @Test
    fun contains() {
        assertTrue(config.contains(a))
        assertTrue(config.contains(b))
        assertTrue(config.contains(c))
        assertFalse(config.contains(Key("bob", stringType)))
    }
}

internal inline fun <reified T : Exception> expectThrown(block: () -> Unit): T =
    try {
        block()
        throw AssertionError("should have thrown ${T::class.simpleName}")
    }
    catch (e: Exception) {
        when (e) {
            is T -> e
            else -> throw e
        }
    }


class ConfigSubset {
    val fullSet = ConfigurationMap(
        "a.one" to "a1",
        "a.two" to "a2",
        "b.one" to "b1",
        "b.two" to "b2",
        "b.three" to "b3",
        "a.x.b" to "axb")
    
    
    val key1 = Key("one", stringType)
    val key2 = Key("two", stringType)
    val key3 = Key("three", stringType)
    
    val keyA = Key("a", stringType)
    val keyB = Key("b", stringType)
    
    val keyX = Key("x", stringType)
    
    
    @Test
    fun prefixed_properties() {
        val subsetA = Subset(namePrefix = "a", configuration = fullSet)
        val subsetB = Subset(namePrefix = "b", configuration = fullSet)
        
        assertThat(subsetA[key1], equalTo("a1"))
        assertThat(subsetA[key2], equalTo("a2"))
        
        assertThat(subsetB[key1], equalTo("b1"))
        assertThat(subsetB[key2], equalTo("b2"))
    }
    
    @Test
    fun suffixed_properties() {
        val subset1 = Subset(nameSuffix = "one", configuration = fullSet)
        val subset2 = Subset(nameSuffix = "two", configuration = fullSet)
        
        assertThat(subset1[keyA], equalTo("a1"))
        assertThat(subset2[keyA], equalTo("a2"))
        
        assertThat(subset1[keyB], equalTo("b1"))
        assertThat(subset2[keyB], equalTo("b2"))
    }
    
    @Test
    fun suffixed_and_prefixed_properties() {
        val subset1 = Subset(namePrefix = "a", nameSuffix = "b", configuration = fullSet)
        
        assertThat(subset1[keyX], equalTo("axb"))
    }
    
    
    @Test
    fun contains() {
        val subsetA = Subset("a", fullSet)
        val subsetB = Subset("b", fullSet)
    
        assertTrue(fullSet.contains(Key("a.one", stringType)))
        assertTrue(fullSet.contains(Key("b.one", stringType)))
        
        assertTrue(subsetA.contains(key1))
        assertTrue(subsetB.contains(key2))
        
        assertFalse(subsetA.contains(key3))
        assertTrue(subsetB.contains(key3))
    }
    
    @Test
    fun lists_only_properties_in_the_subset() {
        assertThat("prefixed", Subset(fullSet, namePrefix = "a").list(), equalTo(listOf(
            fullSet.location to mapOf("a.one" to "a1", "a.two" to "a2", "a.x.b" to "axb"))))
        
        assertThat("suffixed", Subset(fullSet, nameSuffix= "two").list(), equalTo(listOf(
            fullSet.location to mapOf("a.two" to "a2", "b.two" to "b2"))))
        
        assertThat("pre/suff-ixed", Subset(fullSet, namePrefix = "a", nameSuffix= "b").list(), equalTo(listOf(
            fullSet.location to mapOf("a.x.b" to "axb"))))
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

class Wrapping {
    @Test
    fun can_wrap_parsed_value() {
        data class Example(val value: Int)
    
        val wrapperType = intType.wrappedAs(::Example)
        val a = Key("a", wrapperType)
        
        val config = ConfigurationMap("a" to "1")
        
        assertThat(config[a], equalTo(Example(1)))
    }
    
}