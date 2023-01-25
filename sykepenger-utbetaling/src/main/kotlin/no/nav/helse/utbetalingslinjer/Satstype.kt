package no.nav.helse.utbetalingslinjer

class Satstype private constructor(private val navn: String, private val beløpStrategy: (beløp: Int, stønadsdager: Int) -> Int) {
    companion object {
        val Daglig = Satstype("DAG") { beløp, stønadsdager -> beløp * stønadsdager }
        val Engang = Satstype("ENG") { beløp, _ -> beløp }

        fun fromString(string: String) = when (string.lowercase()) {
            "dag" -> Daglig
            "eng" -> Engang
            else -> throw IllegalArgumentException("kjenner ikke til satstype $string")
        }
    }

    fun totalbeløp(beløp: Int, stønadsdager: Int) = beløpStrategy(beløp, stønadsdager)

    override fun toString() = navn
}
