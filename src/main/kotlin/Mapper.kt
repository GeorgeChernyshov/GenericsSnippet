package org.example

interface Mapper<A, B> {
    fun map(input: A) : B
}

class StringToIntMapper : Mapper<String, Int> {
    override fun map(input: String): Int {
        return input.toInt()
    }
}

fun <A, B> applyMapper(
    list: List<A>,
    mapper: Mapper<A, B>
) : List<B> {
    return list.map { mapper.map(it) }
}