package no.nav.helse.opptjening.domain

import java.time.LocalDate
import no.nav.helse.forrigeDag
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioderMedHensynTilHelg
import no.nav.helse.hendelser.til

/**
 * Selve vilkårsregelen: en ren, total funksjon fra grunnlag til utfall.
 *
 * All I/O og asynkroni hører hjemme i [Opptjeningsprøving] og applikasjonslaget. Her finnes bare regelen,
 * slik at den kan testes isolert og kjøres om igjen på et historisk grunnlag.
 */
internal object Opptjeningsregel {
    const val VERSJON = "1"

    private const val ANTALL_OPPTJENINGSDAGER_SOM_KREVES = 28

    fun vurder(skjæringstidspunkt: LocalDate, grunnlag: Opptjeningsgrunnlag): Kodeverkkode = when (grunnlag) {
        is Opptjeningsgrunnlag.Arbeidstaker -> vurderArbeidstaker(skjæringstidspunkt, grunnlag.arbeidsforhold)
        Opptjeningsgrunnlag.SelvstendigNæringsdrivende -> Kodeverkkode.OPPTJENING_MINST_4_UKER
    }

    private fun vurderArbeidstaker(skjæringstidspunkt: LocalDate, arbeidsforhold: List<Arbeidsforhold>): Kodeverkkode {
        val opptjeningsdagerFørSkjæringstidspunktet = arbeidsforhold
            .map { it.ansettelseperiode }
            .grupperSammenhengendePerioderMedHensynTilHelg()
            .find { skjæringstidspunkt.forrigeDag in it }
            ?.let { it.subset(it.start til skjæringstidspunkt.forrigeDag) }
            ?.count() ?: 0

        return if (opptjeningsdagerFørSkjæringstidspunktet >= ANTALL_OPPTJENINGSDAGER_SOM_KREVES) {
            Kodeverkkode.OPPTJENING_MINST_4_UKER
        } else {
            Kodeverkkode.IKKE_OPPTJENING_ARBEID_ELLER_YTELSE
        }
    }
}
