package no.nav.helse.tournament

import no.nav.helse.sykdomstidslinje.NyDag
import no.nav.helse.sykdomstidslinje.dag.Dag
import no.nav.helse.sykdomstidslinje.dag.Ubestemtdag

internal interface Dagturnering {
    fun beste(venstre: Dag, høyre: Dag): Dag
}

internal interface NyDagturnering {
    fun beste(venstre: NyDag, høyre: NyDag): NyDag
}

internal val søknadDagturnering: Dagturnering = CsvDagturnering("/dagturneringSøknad.csv")
internal val nySøknadDagturnering: NyDagturnering = CsvDagturnering("/nyDagturneringSøknad.csv")
internal val historiskDagturnering: Dagturnering = CsvDagturnering("/dagturnering.csv")

private class CsvDagturnering(private val source: String) : Dagturnering, NyDagturnering {

    private val strategies: Map<Turneringsnøkkel, Map<Turneringsnøkkel, Strategy>> = readStrategies()

    override fun beste(venstre: Dag, høyre: Dag): Dag {
        val leftKey = Turneringsnøkkel.fraDag(venstre)
        val rightKey = Turneringsnøkkel.fraDag(høyre)

        return strategies[leftKey]?.get(rightKey)?.decide(venstre, høyre)
            ?: strategies[rightKey]?.get(leftKey)?.decideInverse(venstre, høyre)
            ?: throw RuntimeException("Fant ikke strategi for $leftKey + $rightKey")

    }

    override fun beste(venstre: NyDag, høyre: NyDag): NyDag {
        val leftKey = Turneringsnøkkel.fraDag(venstre)
        val rightKey = Turneringsnøkkel.fraDag(høyre)

        return strategies[leftKey]?.get(rightKey)?.decide(venstre, høyre)
            ?: strategies[rightKey]?.get(leftKey)?.decideInverse(venstre, høyre)
            ?: throw RuntimeException("Fant ikke strategi for $leftKey + $rightKey")
    }

    private fun readStrategies(): Map<Turneringsnøkkel, Map<Turneringsnøkkel, Strategy>> {
        val csv = this::class.java.getResourceAsStream(source)
            .bufferedReader(Charsets.UTF_8)
            .readLines()
            .map { it.split(",") }
            .map { it.first() to it.drop(1) }

        val (_, columnHeaders) = csv.first()

        return csv
            .drop(1)
            .map { (key, row) ->
                enumValueOf<Turneringsnøkkel>(key) to row
                    .mapIndexed { index, cell -> columnHeaders[index] to cell }
                    .filter { (_, cell) -> cell.isNotBlank() }
                    .map { (columnHeader, cell) -> enumValueOf<Turneringsnøkkel>(columnHeader) to strategyFor(cell) }
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
    abstract fun decide(row: NyDag, column: NyDag): NyDag
    abstract fun decideInverse(row: NyDag, column: NyDag): NyDag
}

internal object Undecided : Strategy() {
    override fun decide(row: Dag, column: Dag): Dag = Ubestemtdag(row.dagen)
    override fun decideInverse(row: Dag, column: Dag) = Ubestemtdag(row.dagen)
    override fun decide(row: NyDag, column: NyDag): NyDag = row.problem(column)
    override fun decideInverse(row: NyDag, column: NyDag) = column.problem(row)
}

internal object Row : Strategy() {
    override fun decide(row: Dag, column: Dag): Dag = row
    override fun decideInverse(row: Dag, column: Dag) = column
    override fun decide(row: NyDag, column: NyDag): NyDag = row
    override fun decideInverse(row: NyDag, column: NyDag) = column
}

internal object Column : Strategy() {
    override fun decide(row: Dag, column: Dag): Dag = column
    override fun decideInverse(row: Dag, column: Dag) = row
    override fun decide(row: NyDag, column: NyDag): NyDag = column
    override fun decideInverse(row: NyDag, column: NyDag) = row
}

internal object Latest : Strategy() {
    override fun decide(row: Dag, column: Dag): Dag = column
    override fun decideInverse(row: Dag, column: Dag) = column
    override fun decide(row: NyDag, column: NyDag): NyDag = column
    override fun decideInverse(row: NyDag, column: NyDag) = column
}

internal object Impossible : Strategy() {
    override fun decide(row: Dag, column: Dag): Dag =
        throw RuntimeException("Nøklene ${Turneringsnøkkel.fraDag(row)} + ${Turneringsnøkkel.fraDag(column)} er en ugyldig sammenligning")

    override fun decideInverse(row: Dag, column: Dag) =
        throw RuntimeException("Nøklene ${Turneringsnøkkel.fraDag(row)} + ${Turneringsnøkkel.fraDag(column)} er en ugyldig sammenligning")

    override fun decide(row: NyDag, column: NyDag): NyDag =
        throw RuntimeException("Nøklene ${Turneringsnøkkel.fraDag(row)} + ${Turneringsnøkkel.fraDag(column)} er en ugyldig sammenligning")

    override fun decideInverse(row: NyDag, column: NyDag) =
        throw RuntimeException("Nøklene ${Turneringsnøkkel.fraDag(row)} + ${Turneringsnøkkel.fraDag(column)} er en ugyldig sammenligning")
}
