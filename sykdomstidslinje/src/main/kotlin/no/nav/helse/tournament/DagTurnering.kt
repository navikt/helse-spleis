package no.nav.helse.tournament

import no.nav.helse.sykdomstidslinje.dag.Dag
import no.nav.helse.sykdomstidslinje.dag.Ubestemtdag

class DagTurnering(val source: String = "/dagturnering.csv") {

    internal val strategies: Map<Dag.Nøkkel, Map<Dag.Nøkkel, Strategy>> = readStrategies()

    fun slåss(left: Dag, right: Dag): Dag {
        val leftKey = left.nøkkel()
        val rightKey = right.nøkkel()

        return strategies[leftKey]?.get(rightKey)?.decide(left, right)
            ?: strategies[rightKey]?.get(leftKey)?.decide(right, left)
            ?: throw RuntimeException("Fant ikke strategi for $leftKey + $rightKey")

    }

    private fun readStrategies(): Map<Dag.Nøkkel, Map<Dag.Nøkkel, Strategy>> {
        val reader = this::class.java.getResourceAsStream(source)
            .bufferedReader(Charsets.UTF_8)
        val result: MutableMap<Dag.Nøkkel, MutableMap<Dag.Nøkkel, Strategy>> = mutableMapOf()

        val columnHeaders = reader.readLine().split(",").drop(1).toList()

        reader
            .readLines()
            .forEach { rowText ->
                val row = rowText.split(",")
                row.drop(1)
                    .forEachIndexed { columnNumber, cell ->

                        if (cell != "") {
                            val strategy = strategyFor(cell)
                            val rowKey = row[0]
                            val columnKey = columnHeaders[columnNumber]
                            result.getOrPut(enumValueOf(rowKey)) { mutableMapOf() }[enumValueOf(columnKey)] = strategy
                        }
                    }
            }

        return result
    }

    private fun strategyFor(cellValue: String) =
        when (cellValue) {
            "U" -> Undecided
            "R" -> Row
            "C" -> Column
            "X" -> Impossible
            "L" -> Latest
            else -> throw RuntimeException("$cellValue is not a known strategy for deciding between days")
        }
}

internal sealed class Strategy {
    abstract fun decide(row: Dag, column: Dag): Dag
}

internal object Undecided : Strategy() {
    override fun decide(row: Dag, column: Dag): Dag = Ubestemtdag(row, column)
}

internal object Row : Strategy() {
    override fun decide(row: Dag, column: Dag): Dag = row.erstatter(column)
}

internal object Column : Strategy() {
    override fun decide(row: Dag, column: Dag): Dag = column.erstatter(row)
}

internal object Latest : Strategy() {
    override fun decide(row: Dag, column: Dag): Dag =
        if (row.sisteHendelse() > (column.sisteHendelse())) row else column
}

internal object Impossible : Strategy() {
    override fun decide(row: Dag, column: Dag): Dag = throw RuntimeException("there was never a thing here.")
}
