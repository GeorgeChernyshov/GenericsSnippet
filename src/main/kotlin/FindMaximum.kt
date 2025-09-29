package org.example

fun <T : Comparable<T>> findMaximum(items: List<T>) : T? {
    return items.maxOrNull()
}