package no.nav.helse.hendelser

import no.nav.helse.hendelser.Vilkårsgrunnlag.Arbeidsforhold.Companion.toEtterlevelseMap
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import java.time.LocalDate

class Opptjeningvurdering(
    private val arbeidsforhold: List<Vilkårsgrunnlag.Arbeidsforhold>
) {
    private companion object {
        private const val TILSTREKKELIG_ANTALL_OPPTJENINGSDAGER = 28
    }

    internal var antallOpptjeningsdager: Int = 0
        private set

    internal fun harOpptjening() =
        antallOpptjeningsdager >= TILSTREKKELIG_ANTALL_OPPTJENINGSDAGER

    internal fun valider(aktivitetslogg: IAktivitetslogg, skjæringstidspunkt: LocalDate, subsumsjonObserver: SubsumsjonObserver): Boolean {
        antallOpptjeningsdager = Vilkårsgrunnlag.Arbeidsforhold.opptjeningsdager(arbeidsforhold, aktivitetslogg, skjæringstidspunkt)
        val harOpptjening = harOpptjening()
        subsumsjonObserver.`§ 8-2 ledd 1`(harOpptjening, skjæringstidspunkt, TILSTREKKELIG_ANTALL_OPPTJENINGSDAGER, arbeidsforhold.toEtterlevelseMap(), antallOpptjeningsdager)
        if (harOpptjening) aktivitetslogg.info("Har minst %d dager opptjening", TILSTREKKELIG_ANTALL_OPPTJENINGSDAGER)
        else aktivitetslogg.warn("Perioden er avslått på grunn av manglende opptjening")
        return harOpptjening
    }
}
