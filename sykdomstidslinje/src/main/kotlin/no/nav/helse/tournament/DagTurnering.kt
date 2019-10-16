package no.nav.helse.tournament

import no.nav.helse.sykdomstidslinje.dag.Dag
import no.nav.helse.sykdomstidslinje.dag.Ubestemtdag

val dagTurnering = DagTurnering()

class DagTurnering(val source: String = "/dagturnering.csv") {

    internal val strategies: Map<Dag.Nøkkel, Map<Dag.Nøkkel, Strategy>> = readStrategies()

    fun slåss(venstre: Dag, høyre: Dag): Dag {
        val leftKey = venstre.nøkkel()
        val rightKey = høyre.nøkkel()

        return strategies[leftKey]?.get(rightKey)?.decide(venstre, høyre)
            ?: strategies[rightKey]?.get(leftKey)?.decide(høyre, venstre)
            ?: throw RuntimeException("Fant ikke strategi for $leftKey + $rightKey")

    }

    private fun readStrategies(): Map<Dag.Nøkkel, Map<Dag.Nøkkel, Strategy>> {
        val csv = this::class.java.getResourceAsStream(source)
            .bufferedReader(Charsets.UTF_8)
            .readLines()
            .map { it.split(",") }
            .map { it.first() to it.drop(1) }

        val (_, columnHeaders) = csv.first()

        return csv
            .drop(1)
            .map { (key, row) ->
                enumValueOf<Dag.Nøkkel>(key) to row
                    .mapIndexed { index, cell -> columnHeaders[index] to cell }
                    .filter { (_, cell) -> cell != "" }
                    .map { (columnHeader, cell) -> enumValueOf<Dag.Nøkkel>(columnHeader) to strategyFor(cell) }
                    .toMap()
            }
            .toMap()
    }


    private fun strategyFor(cellValue: String) =
        when (cellValue) {
            "U" -> Undecided
            "R" -> Row
            "C" -> Column
            "X" -> Impossible
            "LR" -> LatestOrRow
            "LC" -> LatestOrColumn
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

internal object LatestOrRow : Strategy() {
    override fun decide(row: Dag, column: Dag): Dag =
        if (row.sisteHendelse() >= (column.sisteHendelse())) row.erstatter(column) else column.erstatter(row)
}

internal object LatestOrColumn : Strategy() {
    override fun decide(row: Dag, column: Dag): Dag =
        if (row.sisteHendelse() > (column.sisteHendelse())) row.erstatter(column) else column.erstatter(row)
}

internal object Impossible : Strategy() {
    override fun decide(row: Dag, column: Dag): Dag =
        throw RuntimeException("Nøklene ${row.nøkkel()} + ${column.nøkkel()} er en ugyldig sammenligning")
}
