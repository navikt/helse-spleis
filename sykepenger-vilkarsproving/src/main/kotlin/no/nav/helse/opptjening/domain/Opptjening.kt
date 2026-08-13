package no.nav.helse.opptjening.domain

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.forrigeDag
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioderMedHensynTilHelg
import no.nav.helse.hendelser.til

sealed class Opptjening: Vilkårsvurdering {

    class ManuellVurdering(
        override val fødselsnummer: String,
        override val skjæringstidspunkt: LocalDate,
        val saksbehandlerIdent: String,
        val fritekstbegrunnelse: String,
        override val kodeverkkode: Kodeverkkode,
        override val id: UUID
    ) : Opptjening()

    class AutomatiskVurdering private constructor(
        override val fødselsnummer: String,
        override val skjæringstidspunkt: LocalDate,
        val versjonAvKildekode: String,
        val grunnlagForAutomatiskVurdering: OpptjeningsgrunnlagForAutomatiskVurdering,
        override val kodeverkkode: Kodeverkkode,
        override val id: UUID
    ) : Opptjening() {

        companion object {
            fun nyAutomatiskVurdering(fødselsnummer: String, skjæringstidspunkt: LocalDate, versjonAvKildekode: String, grunnlagForAutomatiskVurdering: OpptjeningsgrunnlagForAutomatiskVurdering): AutomatiskVurdering {
                return AutomatiskVurdering(
                    fødselsnummer = fødselsnummer,
                    skjæringstidspunkt = skjæringstidspunkt,
                    versjonAvKildekode = versjonAvKildekode,
                    grunnlagForAutomatiskVurdering = grunnlagForAutomatiskVurdering,
                    kodeverkkode = grunnlagForAutomatiskVurdering.kodeverkkode(skjæringstidspunkt),
                    id = UUID.randomUUID()
                )
            }

            fun fraLagring(
                fødselsnummer: String,
                skjæringstidspunkt: LocalDate,
                versjonAvKildekode: String,
                grunnlagForAutomatiskVurdering: OpptjeningsgrunnlagForAutomatiskVurdering,
                kodeverkkode: Kodeverkkode,
                id: UUID
            ): AutomatiskVurdering {
                return AutomatiskVurdering(
                    fødselsnummer = fødselsnummer,
                    skjæringstidspunkt = skjæringstidspunkt,
                    versjonAvKildekode = versjonAvKildekode,
                    grunnlagForAutomatiskVurdering = grunnlagForAutomatiskVurdering,
                    kodeverkkode = kodeverkkode,
                    id = id
                )
            }
        }

        sealed interface OpptjeningsgrunnlagForAutomatiskVurdering {
            fun kodeverkkode(skjæringstidspunkt: LocalDate): Kodeverkkode

            class ForArbeidstaker(
                val arbeidsforhold: List<Arbeidsforhold>,
            ): OpptjeningsgrunnlagForAutomatiskVurdering {
                override fun kodeverkkode(skjæringstidspunkt: LocalDate): Kodeverkkode {
                    val antallOpptjeningsdagerFørSkjæringstidspunktet = arbeidsforhold.map { it.ansettelseperiode }.grupperSammenhengendePerioderMedHensynTilHelg()
                        .find { skjæringstidspunkt.forrigeDag in it }
                        ?.let { it.subset(it.start til skjæringstidspunkt.forrigeDag) }
                        ?.count() ?: 0

                    return if (antallOpptjeningsdagerFørSkjæringstidspunktet >= 28) {
                        Kodeverkkode.OPPTJENING_MINST_4_UKER
                    } else {
                        Kodeverkkode.IKKE_OPPTJENING_ARBEID_ELLER_YTELSE
                    }
                }
            }

            object ForSelvstendigNæringsdrivende: OpptjeningsgrunnlagForAutomatiskVurdering {
                override fun kodeverkkode(skjæringstidspunkt: LocalDate): Kodeverkkode {
                    return Kodeverkkode.OPPTJENING_MINST_4_UKER
                }
            }
        }
    }
}
