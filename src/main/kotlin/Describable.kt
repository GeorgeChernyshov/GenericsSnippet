package org.example

import java.io.Serializable

interface Describable {
    fun getDescription(): String
}

fun <T> debugDescription(item: T) : String
    where T : Serializable,
          T : Describable
{
    return item.getDescription()
}