package com.natpryce.konfig

import kotlin.reflect.KProperty
import kotlin.reflect.full.memberProperties


open class PropertyKeys : Iterable<Key<*>> {
    override operator fun iterator(): Iterator<Key<*>> {
        val k = javaClass.kotlin
        return (k.memberProperties.map { it.get(this) }.filterIsInstance<Key<*>>() +
               k.nestedClasses.map { it.objectInstance }.filterIsInstance<PropertyGroup>().flatten()).iterator()
    }
}

open class PropertyGroup(private val outer: PropertyGroup? = null) : PropertyKeys() {
    private fun outer() = outer ?: javaClass.enclosingClass?.kotlin?.objectInstance as? PropertyGroup
    private fun name(): String = namePrefix() + groupName()
    private fun namePrefix() = outer()?.name()?.let { "$it." } ?: ""

    // anonymous objects don't get names in their KClass, so we have to resort to the JVM's name
    private fun groupName() = javaClass.kotlin.simpleName?.substringBefore("$")
        ?: javaClass.name.split("$").let { chunks ->
            chunks.getOrNull(chunks.size - 2)
                ?: throw IllegalArgumentException("cannot determine name of property group for $javaClass")
        }

    fun <T> key(keySimpleName: String, type: (PropertyLocation, String) -> T): Key<T> {
        return Key((name() + "." + keySimpleName).replace('_', '-'), type)
    }
}

operator fun <G : PropertyGroup, T> ((PropertyLocation, String) -> T).getValue(group: G, property: KProperty<*>) =
        group.key(property.name, this)

operator fun <SCOPE, T> ((PropertyLocation, String) -> T).getValue(scope: SCOPE?, property: KProperty<*>) =
        Key(property.name, this)
