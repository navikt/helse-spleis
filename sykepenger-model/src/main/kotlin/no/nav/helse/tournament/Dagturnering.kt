package no.nav.helse.tournament

import no.nav.helse.sykdomstidslinje.dag.Dag
import no.nav.helse.sykdomstidslinje.dag.ImplisittDag
import no.nav.helse.sykdomstidslinje.dag.Ubestemtdag

internal interface Dagturnering {
    fun beste(venstre: Dag, høyre: Dag): Dag
}


internal val sendtSøknadDagturnering: Dagturnering = CsvDagturnering("/dagturneringSøknad.csv")
internal val historiskDagturnering: Dagturnering = CsvDagturnering("/dagturnering.csv")

internal object KonfliktskyDagturnering : Dagturnering {
    override fun beste(venstre: Dag, høyre: Dag): Dag {
        return when {
            venstre is ImplisittDag -> høyre
            høyre is ImplisittDag -> venstre
            else -> error("Strategien ${this::class.simpleName} kan ikke bestemme beste dag siden ingen er ImplisittDag. Venstre=${venstre::class.simpleName}, høyre=${høyre::class.simpleName}")
        }
    }
}

private class CsvDagturnering(private val source: String): Dagturnering {

    private val strategies: Map<Dag.Nøkkel, Map<Dag.Nøkkel, Strategy>> = readStrategies()

    override fun beste(venstre: Dag, høyre: Dag): Dag {
        val leftKey = venstre.nøkkel()
        val rightKey = høyre.nøkkel()

        return strategies[leftKey]?.get(rightKey)?.decide(venstre, høyre)
            ?: strategies[rightKey]?.get(leftKey)?.decideInverse(venstre, høyre)
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
                    .filter { (_, cell) -> cell.isNotBlank() }
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
            "L" -> Latest
            else -> throw RuntimeException("$cellValue is not a known strategy for deciding between days")
        }
}

internal sealed class Strategy {
    abstract fun decide(row: Dag, column: Dag): Dag
    abstract fun decideInverse(row: Dag, column: Dag): Dag
}

internal object Undecided : Strategy() {
    override fun decide(row: Dag, column: Dag): Dag = Ubestemtdag(row, column)
    override fun decideInverse(row: Dag, column: Dag) = Ubestemtdag(row, column)
}

internal object Row : Strategy() {
    override fun decide(row: Dag, column: Dag): Dag = row
    override fun decideInverse(row: Dag, column: Dag) = column
}

internal object Column : Strategy() {
    override fun decide(row: Dag, column: Dag): Dag = column
    override fun decideInverse(row: Dag, column: Dag) = row
}

internal object Latest : Strategy() {
    override fun decide(row: Dag, column: Dag): Dag = column
    override fun decideInverse(row: Dag, column: Dag) = column
}

internal object Impossible : Strategy() {
    override fun decide(row: Dag, column: Dag): Dag =
        throw RuntimeException("Nøklene ${row.nøkkel()} + ${column.nøkkel()} er en ugyldig sammenligning")

    override fun decideInverse(row: Dag, column: Dag) =
        throw RuntimeException("Nøklene ${row.nøkkel()} + ${column.nøkkel()} er en ugyldig sammenligning")
}
