package com.natpryce.konfig

import kotlin.reflect.KProperty


open class PropertyGroup(private val outer: PropertyGroup? = null) {
    val name: String by lazy { (namePrefix() + groupName()) }

    private fun namePrefix() = outer?.name?.let { it + "." } ?: ""

    private fun groupName() = javaClass.kotlin.simpleName?.substringBefore("$") ?:
            throw IllegalArgumentException("cannot determine name of property group")

    fun <T> key(keySimpleName: String, type: (PropertyLocation, String) -> T): Key<T> {
        return Key((name + "." + keySimpleName).replace('_', '-'), type)
    }
}

operator fun <G : PropertyGroup, T> ((PropertyLocation, String) -> T).getValue(group: G, property: KProperty<*>) =
        group.key(property.name, this)
