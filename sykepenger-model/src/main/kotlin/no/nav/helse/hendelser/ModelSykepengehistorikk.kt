package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogger
import java.time.LocalDate

class ModelSykepengehistorikk(perioder: List<Pair<LocalDate, LocalDate>>,
                              aktivitetslogger: Aktivitetslogger
) {

    init {
        perioder.forEach { (fom, tom) ->
            if (fom > tom) aktivitetslogger.severe("fom ($fom) er større enn tom ($tom) for sykepengehistorikk")
        }
    }

    private val sisteFraværsdag: LocalDate? = perioder.maxBy { it.second }?.second

    fun sisteFraværsdag() = sisteFraværsdag

}
