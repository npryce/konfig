package com.natpryce.konfig

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test


class StaticallyTypedConfigKeys {
    object group : PropertyGroup {
        val a by stringType
        val b by intType
    }

    @Test
    fun key_names_of_singleton_object() {
        assertThat(group.a.name, equalTo("group.a"))
        assertThat(group.b.name, equalTo("group.b"))
    }

    @Test
    fun key_names_of_local_object() {
        val more = object : PropertyGroup {
            val x by stringType
        }

        assertThat(more.x.name, equalTo("more.x"))
    }

    @Test
    fun underscore_replaced_with_hyphen() {
        val a_group = object : PropertyGroup {
            val a_property by stringType
        }

        assertThat(a_group.a_property.name, equalTo("a-group.a-property"))
    }
}
