package no.nav.helse.utbetalingslinjer

internal class Satstype private constructor(private val navn: String, private val beløpStrategy: (beløp: Int, stønadsdager: Int) -> Int) {
    internal companion object {
        internal val Daglig = Satstype("DAG") { beløp, stønadsdager -> beløp * stønadsdager }
        internal val Engang = Satstype("ENG") { beløp, _ -> beløp }

        internal fun fromString(string: String) = when (string.lowercase()) {
            "dag" -> Daglig
            "eng" -> Engang
            else -> throw IllegalArgumentException("kjenner ikke til satstype $string")
        }
    }

    internal fun totalbeløp(beløp: Int, stønadsdager: Int) = beløpStrategy(beløp, stønadsdager)

    override fun toString() = navn
}
