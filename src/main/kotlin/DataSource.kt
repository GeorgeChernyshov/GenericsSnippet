package org.example

interface DataSource<out T> {
    fun getData(): List<T>
}

class StringDataSource: DataSource<String> {
    override fun getData() = listOf("a", "b", "c")
}

fun printData(source: DataSource<Any>) {
    source.getData().forEach { println(it) }
}