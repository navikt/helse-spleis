package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
import java.time.LocalDate

class Opptjeningvurdering(
    private val arbeidsforhold: List<Arbeidsforhold>
) {
    private companion object {
        private const val TILSTREKKELIG_ANTALL_OPPTJENINGSDAGER = 28
    }

    private val antallOpptjeningsdager = mutableMapOf<String, Int>()

    internal fun opptjeningsdager(orgnummer: String) = antallOpptjeningsdager[orgnummer] ?: 0
    internal fun harOpptjening(orgnummer: String) =
        opptjeningsdager(orgnummer) >= TILSTREKKELIG_ANTALL_OPPTJENINGSDAGER

    internal fun valider(aktivitetslogg: Aktivitetslogg, orgnummer: String, førsteFraværsdag: LocalDate): Aktivitetslogg {
        Arbeidsforhold.opptjeningsdager(arbeidsforhold, antallOpptjeningsdager, førsteFraværsdag)
        if (harOpptjening(orgnummer)) aktivitetslogg.info(
            "Har minst %d dager opptjening",
            TILSTREKKELIG_ANTALL_OPPTJENINGSDAGER
        )
        else aktivitetslogg.error("Har mindre enn %d dager opptjening", TILSTREKKELIG_ANTALL_OPPTJENINGSDAGER)
        return aktivitetslogg
    }

    class Arbeidsforhold(
        private val orgnummer: String,
        private val fom: LocalDate,
        private val tom: LocalDate? = null
    ) {
        private fun opptjeningsdager(førsteFraværsdag: LocalDate): Int {
            if (fom > førsteFraværsdag) return 0
            if (tom != null && tom < førsteFraværsdag) return 0
            return fom.datesUntil(førsteFraværsdag).count().toInt()
        }

        internal companion object {
            fun opptjeningsdager(
                liste: List<Arbeidsforhold>,
                map: MutableMap<String, Int>,
                førsteFraværsdag: LocalDate
            ) {
                liste.forEach { map[it.orgnummer] = it.opptjeningsdager(førsteFraværsdag) }
            }
        }
    }
}
