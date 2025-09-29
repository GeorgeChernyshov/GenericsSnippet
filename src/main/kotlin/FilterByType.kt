package org.example

inline fun <reified T> filterByType(list: List<Any>) : List<T> {
    return list.filter { it is T }
        .map { it as T }
}