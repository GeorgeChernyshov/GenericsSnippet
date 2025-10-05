package org.example

import kotlin.reflect.KClass

interface Table

data class TableColumn<T : Any>(
    val name: String,
    val type: KClass<T>
)

object Users : Table {
    val id = TableColumn("id", Int::class)
    val name = TableColumn("name", String::class)
    val age = TableColumn("age", Int::class)
}

object Products : Table {
    val productId = TableColumn("product_id", Int::class)
    val productName = TableColumn("name", String::class)
    val stock = TableColumn("stock_count", Int::class)
    val price = TableColumn("price", Double::class)
}

interface NotSet
interface NoColumnsSelected
interface ColumnsSelected

interface NoFromTable
interface FromTable<T : Table>

interface NoWhereCondition
interface WhereCondition

fun Any?.toSqlLiteral() = when (this) {
    is String -> "'${this.replace("'", "''")}'"
    null -> "NULL"
    else -> this.toString()
}

class WhereConditionFragment(
    private val value: String,
    val isComplex: Boolean
) {
    fun evaluate() = value
}

class WhereClauseBuilder(
    private val fromTableKClass: KClass<out Table>
) {

    infix fun <T : Any> TableColumn<T>.eq(value: T): WhereConditionFragment {
        checkColumnScope(this)

        if (this.type != value::class) {
            error("Type mismatch in WHERE clause: Column '${this.name}' expects type ${this.type.simpleName} but received ${value::class.simpleName}.")
        }

        return WhereConditionFragment(
            value = "${this.name} = ${value.toSqlLiteral()}",
            isComplex = false
        )
    }

    infix fun <T : Comparable<T>> TableColumn<T>.lt(value: T): WhereConditionFragment {
        checkColumnScope(this)

        if (this.type != value::class) {
            error("Type mismatch in WHERE clause: Column '${this.name}' expects type ${this.type.simpleName} but received ${value::class.simpleName}.")
        }

        return WhereConditionFragment(
            value = "${this.name} < ${value.toSqlLiteral()}",
            isComplex = false
        )
    }

    infix fun <T : Any> TableColumn<T>.`in`(values: List<T>): WhereConditionFragment {
        checkColumnScope(this)
        if (values.any { it::class != this.type }) {
            error("Type mismatch in WHERE clause: All values in 'IN' list for column '${this.name}' must be of type ${this.type.simpleName}.")
        }

        val sqlValues = values.joinToString(", ") {
            it.toSqlLiteral()
        }

        return WhereConditionFragment(
            value = "${this.name} IN ($sqlValues)",
            isComplex = false
        )
    }

    infix fun <T : Comparable<T>> TableColumn<T>.gt(value: T): WhereConditionFragment {
        checkColumnScope(this)

        if (this.type != value::class) {
            error("Type mismatch in WHERE clause: Column '${this.name}' expects type ${this.type.simpleName} but received ${value::class.simpleName}.")
        }

        return WhereConditionFragment(
            value = "${this.name} > ${value.toSqlLiteral()}",
            isComplex = false
        )
    }

    infix fun WhereConditionFragment.and(
        other: WhereConditionFragment
    ) : WhereConditionFragment {
        val leftSql = if (isComplex)
            "(${evaluate()})"
        else evaluate()

        val rightSql = if (other.isComplex)
            "(${other.evaluate()})"
        else other.evaluate()

        return WhereConditionFragment(
            value = "$leftSql AND $rightSql",
            isComplex = true
        )
    }

    infix fun WhereConditionFragment.or(
        other: WhereConditionFragment
    ) : WhereConditionFragment {
        val leftSql = if (isComplex)
            "(${evaluate()})"
        else evaluate()

        val rightSql = if (other.isComplex)
            "(${other.evaluate()})"
        else other.evaluate()

        return WhereConditionFragment(
            value = "$leftSql OR $rightSql",
            isComplex = true
        )
    }

    private fun checkColumnScope(column: TableColumn<*>) {
        val tableColumns = fromTableKClass.objectInstance?.let { tableObject ->
            tableObject::class.members
                .filter { it.returnType.classifier == TableColumn::class }
                .map { it.call(tableObject) as TableColumn<*> }
        } ?: emptyList()

        if (tableColumns.none { it.name == column.name && it.type == column.type }) {
            error("Column '${column.name}' is not part of the table '${fromTableKClass.simpleName}' in the FROM clause.")
        }
    }
}

class QueryBuilder<
    SELECT_STATE,
    FROM_TABLE_KLASS,
    WHERE_STATE
> private constructor() {

    internal var _selectedColumns: List<TableColumn<*>>? = null
    internal var _fromTableKClass: KClass<out Table>? = null
    internal var _selectedPredicate: (WhereClauseBuilder.() -> WhereConditionFragment)? = null

    companion object {
        operator fun invoke() = QueryBuilder<
                NoColumnsSelected,
                NoFromTable,
                NoWhereCondition>()
    }
}

inline fun <FINAL_SELECT_STATE,
        FINAL_FROM_STATE,
        FINAL_WHERE_STATE>
    query(
        block: QueryBuilder<
                NoColumnsSelected,
                NoFromTable,
                NoWhereCondition>.() ->
            QueryBuilder<
                FINAL_SELECT_STATE,
                FINAL_FROM_STATE,
                FINAL_WHERE_STATE>
    ): QueryBuilder<
        FINAL_SELECT_STATE,
        FINAL_FROM_STATE,
        FINAL_WHERE_STATE>
{
    val builder = QueryBuilder()
    return builder.block()
}

fun <FROM_TABLE_KLASS,
        WHERE_STATE>
QueryBuilder<
        NoColumnsSelected,
        FROM_TABLE_KLASS,
        WHERE_STATE
    >.select(vararg columns: TableColumn<*>) : QueryBuilder<
        ColumnsSelected,
        FROM_TABLE_KLASS,
        WHERE_STATE>
{
    this._selectedColumns = columns.toList()

    @Suppress("UNCHECKED_CAST")
    return this as QueryBuilder<
            ColumnsSelected,
            FROM_TABLE_KLASS,
            WHERE_STATE>
}

fun <SELECT_STATE,
        T : Table,
        WHERE_STATE>
    QueryBuilder<
            SELECT_STATE,
            NoFromTable,
            WHERE_STATE
    >.from(table: T): QueryBuilder<
        SELECT_STATE,
        FromTable<T>,
        WHERE_STATE>
{
    this._selectedColumns?.let {
        val selected = it
        val tableKClass = table::class as KClass<out Table>

        selected.forEach { col ->
            val tableColumns = tableKClass.objectInstance?.let { tableObject ->
                tableObject::class.members
                    .filter { it.returnType.classifier == TableColumn::class }
                    .map { it.call(tableObject) as TableColumn<*> }
            } ?: emptyList()

            if (tableColumns.none {
                    it.name == col.name && it.type == col.type
                }) {
                error("Runtime check failed or column not found: Column '${col.name}' selected is not part of table '${tableKClass.simpleName}' specified in FROM clause.")
            }
        }
    }

    this._fromTableKClass = table::class

    @Suppress("UNCHECKED_CAST")
    return this as QueryBuilder<
            SELECT_STATE,
            FromTable<T>,
            WHERE_STATE>
}

fun <T : Table> QueryBuilder<
        ColumnsSelected,
        FromTable<T>,
        NoWhereCondition
>.where(
    predicate: WhereClauseBuilder.() -> WhereConditionFragment
) : QueryBuilder<
        ColumnsSelected,
        FromTable<T>,
        WhereCondition>
{
    this._selectedPredicate = predicate

    @Suppress("UNCHECKED_CAST")
    return this as QueryBuilder<
            ColumnsSelected,
            FromTable<T>,
            WhereCondition>
}

fun <T : Table, WHERE_STATE> QueryBuilder<
        ColumnsSelected,
        FromTable<T>,
        WHERE_STATE
    >.build() : String
{
    val selectedColumns = _selectedColumns!!
        .joinToString(", ") { it.name }

    val selectClause =  "SELECT $selectedColumns"
    val fromClause = "FROM ${_fromTableKClass!!.simpleName}"
    val whereClause = _selectedPredicate?.let {
        val builder = WhereClauseBuilder(_fromTableKClass!!)
        "WHERE ${it(builder).evaluate()}"
    } ?: ""

    return "$selectClause $fromClause $whereClause".trim()
}