package org.example

class Container<T> {
    private var item: T? = null

    fun get() = item

    fun put(item: T) {
        this.item = item
    }
}

fun <T, R> processItem(
    container: Container<T>,
    transformation: (T) -> R
) : R? = container.get()
    ?.let(transformation)