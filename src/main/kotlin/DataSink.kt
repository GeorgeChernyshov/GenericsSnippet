package org.example

interface DataSink<in T> {
    fun consume(item: T)
}

class IntDataSink : DataSink<Int> {
    override fun consume(item: Int) = print(item)
}

fun processAndConsume(
    integer: Int,
    dataSink: DataSink<Int>
) = dataSink.consume(integer)