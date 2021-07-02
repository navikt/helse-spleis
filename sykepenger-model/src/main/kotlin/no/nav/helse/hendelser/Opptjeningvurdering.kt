package no.nav.helse.hendelser

import no.nav.helse.person.IAktivitetslogg
import java.time.LocalDate

class Opptjeningvurdering(
    private val arbeidsforhold: List<Arbeidsforhold>
) {
    private companion object {
        private const val TILSTREKKELIG_ANTALL_OPPTJENINGSDAGER = 28
    }

    internal var antallOpptjeningsdager: Int = 0
        private set

    internal fun harOpptjening() =
        antallOpptjeningsdager >= TILSTREKKELIG_ANTALL_OPPTJENINGSDAGER

    internal fun valider(aktivitetslogg: IAktivitetslogg, skjæringstidspunkt: LocalDate): Boolean {
        antallOpptjeningsdager = Arbeidsforhold.opptjeningsdager(arbeidsforhold, aktivitetslogg, skjæringstidspunkt)
        if (harOpptjening()) {
            aktivitetslogg.lovtrace.`§8-2 ledd 1`(true)
            aktivitetslogg.info(
                "Har minst %d dager opptjening",
                TILSTREKKELIG_ANTALL_OPPTJENINGSDAGER
            )
            return true
        }
        aktivitetslogg.lovtrace.`§8-2 ledd 1`(false)
        aktivitetslogg.warn("Perioden er avslått på grunn av manglende opptjening")
        return false
    }
}
