package no.nav.helse.tournament

import no.nav.helse.sykdomstidslinje.BesteStrategy
import no.nav.helse.sykdomstidslinje.Dag


internal class Dagturnering private constructor(private val source: String) : BesteStrategy {
    companion object {
        internal val SØKNAD = Dagturnering("/dagturneringSøknad.csv")
        internal val TURNERING = Dagturnering("/dagturnering.csv")
    }

    private val strategies: Map<Turneringsnøkkel, Map<Turneringsnøkkel, Strategy>> = readStrategies()

    override fun beste(venstre: Dag, høyre: Dag): Dag {
        val leftKey = Turneringsnøkkel.fraDag(venstre)
        val rightKey = Turneringsnøkkel.fraDag(høyre)

        return strategies[leftKey]?.get(rightKey)?.decide(venstre, høyre)
            ?: strategies[rightKey]?.get(leftKey)?.decideInverse(venstre, høyre)
            ?: throw RuntimeException("Fant ikke strategi for $leftKey + $rightKey")
    }

    private fun readStrategies(): Map<Turneringsnøkkel, Map<Turneringsnøkkel, Strategy>> {
        val csv = this::class.java.getResourceAsStream(source)!!
            .bufferedReader(Charsets.UTF_8)
            .readLines()
            .map { it.split(",") }
            .map { it.first() to it.drop(1) }

        val (_, columnHeaders) = csv.first()
        return csv.drop(1)
            .associate { (key, row) ->
                enumValueOf<Turneringsnøkkel>(key) to columnHeaders
                    .zip(row)
                    .filter { (_, cell) -> cell.isNotBlank() }
                    .associate { (columnHeader, cell) -> enumValueOf<Turneringsnøkkel>(columnHeader) to strategyFor(cell) }
            }
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

private sealed class Strategy {
    abstract fun decide(row: Dag, column: Dag): Dag
    open fun decideInverse(row: Dag, column: Dag) = decide(column, row)
}

private object Undecided : Strategy() {
    override fun decide(row: Dag, column: Dag): Dag = row.problem(column)
}

private object Row : Strategy() {
    override fun decide(row: Dag, column: Dag): Dag = row
}

private object Column : Strategy() {
    override fun decide(row: Dag, column: Dag): Dag = column
}

private object Latest : Strategy() {
    override fun decide(row: Dag, column: Dag): Dag = column
    override fun decideInverse(row: Dag, column: Dag) = column
}

private object Impossible : Strategy() {
    override fun decide(row: Dag, column: Dag): Dag =
        throw RuntimeException("Nøklene ${Turneringsnøkkel.fraDag(row)} + ${Turneringsnøkkel.fraDag(column)} er en ugyldig sammenligning")
}
