package de.triplet.gradle.play.internal

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty

internal class Alias<T>(private val delegate: KMutableProperty0<T>) : ReadWriteProperty<Any?, T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>) = delegate.get()

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) = delegate.set(value)
}
