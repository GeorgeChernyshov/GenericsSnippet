package org.example

import kotlin.reflect.KClass

interface Event

interface EventListener<in T : Event> {
    fun onEvent(event: T)
}

class EventBus {

    val listeners = mutableMapOf<KClass<out Event>, MutableList<EventListener<*>>>()

    inline fun <reified T : Event> subscribe(listener: EventListener<T>) {
        if (listeners.containsKey(T::class))
            listeners[T::class]?.add(listener)
        else listeners[T::class] = mutableListOf(listener)
    }

    inline fun <reified T : Event> unsubscribe(listener: EventListener<T>) {
        listeners[T::class]?.remove(listener)
    }

    inline fun <reified T : Event> dispatch(event: T) {
        val typedListeners = listeners
            .filterKeys { it.java.isAssignableFrom(T::class.java) }
            .values
            .flatten()
            .map { it as EventListener<T> }

        typedListeners.forEach {
            it.onEvent(event)
        }
    }
}