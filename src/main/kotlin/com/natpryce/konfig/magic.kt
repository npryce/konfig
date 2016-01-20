package com.natpryce.konfig

import kotlin.reflect.KProperty
import kotlin.reflect.memberProperties


open class AllProperties {
    fun keys(): List<Key<*>> {
        val k = javaClass.kotlin
        return k.memberProperties.map { it.get(this) }.filterIsInstance<Key<*>>() +
               k.nestedClasses.map { it.objectInstance }.filterIsInstance<PropertyGroup>().flatMap { it.keys() }
    }
}

open class PropertyGroup(private val outer: PropertyGroup? = null) : AllProperties() {
    private fun outer() = outer ?: javaClass.enclosingClass.kotlin.objectInstance as? PropertyGroup
    private fun name() : String = namePrefix() + groupName()
    private fun namePrefix() = outer()?.name()?.let { it + "." } ?: ""
    private fun groupName() = javaClass.kotlin.simpleName?.substringBefore("$") ?:
            throw IllegalArgumentException("cannot determine name of property group")

    fun <T> key(keySimpleName: String, type: (PropertyLocation, String) -> T): Key<T> {
        return Key((name() + "." + keySimpleName).replace('_', '-'), type)
    }
}

operator fun <G : PropertyGroup, T> ((PropertyLocation, String) -> T).getValue(group: G, property: KProperty<*>) =
        group.key(property.name, this)

operator fun <SCOPE, T> ((PropertyLocation, String) -> T).getValue(scope: SCOPE?, property: KProperty<*>) =
        Key(property.name, this)
