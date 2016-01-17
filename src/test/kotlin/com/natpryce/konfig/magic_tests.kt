package com.natpryce.konfig

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test


class StaticallyTypedConfigKeys {
    object group : PropertyGroup() {
        val a by stringType
        val b by intType
    }

    @Test
    fun key_names_of_singleton_object() {
        assertThat(group.a.name, equalTo("group.a"))
        assertThat(group.b.name, equalTo("group.b"))
    }


    object a_group : PropertyGroup() {
        val a_property by stringType
    }

    @Test
    fun underscore_replaced_with_hyphen() {

        assertThat(a_group.a_property.name, equalTo("a-group.a-property"))
    }


    object outer : PropertyGroup() {
        object inner : PropertyGroup(this) {
            val p by stringType
        }
    }

    @Test
    fun nested_groups() {
        assertThat(outer.inner.p.name, equalTo("outer.inner.p"))
    }
}
