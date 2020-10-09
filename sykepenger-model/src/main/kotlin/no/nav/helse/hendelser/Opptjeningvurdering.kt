package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
import java.lang.Integer.max
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

    internal fun valider(aktivitetslogg: Aktivitetslogg, orgnummer: String, beregningsdato: LocalDate): Aktivitetslogg {
        Arbeidsforhold.opptjeningsdager(arbeidsforhold, antallOpptjeningsdager, beregningsdato)
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
        private fun opptjeningsdager(beregningsdato: LocalDate): Int {
            if (fom > beregningsdato) return 0
            if (tom != null && tom < beregningsdato) return 0
            return fom.datesUntil(beregningsdato).count().toInt()
        }

        internal companion object {
            fun opptjeningsdager(
                liste: List<Arbeidsforhold>,
                map: MutableMap<String, Int>,
                beregningsdato: LocalDate
            ) {
                liste.forEach { arbeidsforhold ->
                    map.compute(arbeidsforhold.orgnummer) { _, opptjeningsdager ->
                        max(arbeidsforhold.opptjeningsdager(beregningsdato), opptjeningsdager ?: 0)
                    }
                }
            }
        }
    }
}
