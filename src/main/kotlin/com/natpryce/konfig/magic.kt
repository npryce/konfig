package com.natpryce.konfig

import kotlin.reflect.KProperty


interface PropertyGroup {
    fun <T> key(name: String, type: (PropertyLocation, String) -> T): Key<T> {
        val groupClassName: String = javaClass.kotlin.simpleName ?:
                throw IllegalArgumentException("cannot determine name of property group class")
        val root = groupClassName.substringBefore('$')

        return Key((root + "." + name).replace('_', '-'), type)
    }
}

operator fun <G : PropertyGroup, T> ((PropertyLocation, String) -> T).getValue(group: G, property: KProperty<*>) =
        group.key(property.name, this)
