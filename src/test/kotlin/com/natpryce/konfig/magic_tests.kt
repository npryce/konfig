package com.natpryce.konfig

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test

val foo by stringType

object group : PropertyGroup() {
    val a by stringType
    val b by intType
}

class StaticallyTypedConfigKeys {
    val bar by stringType

    @Test
    fun simply_named_keys_defined_as_variables() {
        assertThat(foo.name, equalTo("foo"))
        assertThat(bar.name, equalTo("bar"))
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

    @Test
    fun anonymous_object_in_function() {
        val outer = object : PropertyGroup() {
            val p by stringType
        }

        assertThat(outer.p.name, equalTo("outer.p"))
    }


    object outer : PropertyGroup() {
        object inner : PropertyGroup(this) {
            val p by stringType
        }
    }

    @Test
    fun nested_groups_with_nesting_defined_explicitly() {
        assertThat(outer.inner.p.name, equalTo("outer.inner.p"))
    }

    object outer2 : PropertyGroup() {
        object inner : PropertyGroup() {
            val p by stringType
        }
    }

    @Test
    fun nesting_detected_by_reflection() {
        assertThat(outer2.inner.p.name, equalTo("outer2.inner.p"))
    }

    open class Common : PropertyGroup() {
        val a by stringType
    }

    object outer3 : PropertyGroup() {
        object x : Common()
    }

    @Test
    fun common_property_definition_as_a_singleton_object() {
        assertThat(outer3.x.a.name, equalTo("outer3.x.a"))
    }
}


class IntrospectionOfMagicPropertyKeys {
    object keys : PropertyKeys() {
        val p by intType

        object g : PropertyGroup() {
            val q by stringType

            object a : PropertyGroup() {
                val a1 by stringType
                val a2 by stringType
            }

            object b : PropertyGroup() {
                val b1 by stringType
                val b2 by stringType
            }
        }
    }

    @Test
    fun introspection() {
        assertThat(keys.toList(), equalTo(listOf<Key<*>>(
                Key("p", intType),
                Key("g.q", stringType),
                Key("g.a.a1", stringType),
                Key("g.a.a2", stringType),
                Key("g.b.b1", stringType),
                Key("g.b.b2", stringType)
        )))
    }
}

