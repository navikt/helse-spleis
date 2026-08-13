package no.nav.helse.opptjening.domain

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.forrigeDag
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioderMedHensynTilHelg
import no.nav.helse.hendelser.til

sealed class Opptjening : Vilkårsvurdering {

    class ManuellVurdering(
        override val fødselsnummer: String,
        override val skjæringstidspunkt: LocalDate,
        val saksbehandlerIdent: String,
        val fritekstbegrunnelse: String,
        override val kodeverkkode: Kodeverkkode?,
        override val id: UUID,
    ) : Opptjening() {
        override val erKomplett: Boolean = true
    }

    class AutomatiskVurdering private constructor(
        override val fødselsnummer: String,
        override val skjæringstidspunkt: LocalDate,
        val versjonAvKildekode: String,
         grunnlagForAutomatiskVurdering: OpptjeningsgrunnlagForAutomatiskVurdering?,
         kodeverkkode: Kodeverkkode?,
        override val id: UUID,
        erKomplett: Boolean
    ) : Opptjening() {

        override var erKomplett: Boolean = erKomplett
            private set

        var grunnlagForAutomatiskVurdering: OpptjeningsgrunnlagForAutomatiskVurdering? = grunnlagForAutomatiskVurdering
            private set

        override var kodeverkkode: Kodeverkkode? = kodeverkkode
            private set

        fun fullfør(grunnlagForAutomatiskVurdering: OpptjeningsgrunnlagForAutomatiskVurdering) {
            this.grunnlagForAutomatiskVurdering = grunnlagForAutomatiskVurdering
            kodeverkkode = grunnlagForAutomatiskVurdering.kodeverkkode(skjæringstidspunkt)
            erKomplett = true
        }

        companion object {
            fun nyAutomatiskVurdering(fødselsnummer: String, skjæringstidspunkt: LocalDate, versjonAvKildekode: String): AutomatiskVurdering {
                return AutomatiskVurdering(
                    fødselsnummer = fødselsnummer,
                    skjæringstidspunkt = skjæringstidspunkt,
                    versjonAvKildekode = versjonAvKildekode,
                    grunnlagForAutomatiskVurdering = null,
                    kodeverkkode = null,
                    id = UUID.randomUUID(),
                    erKomplett = false
                )
            }

            fun fraLagring(
                fødselsnummer: String,
                skjæringstidspunkt: LocalDate,
                versjonAvKildekode: String,
                grunnlagForAutomatiskVurdering: OpptjeningsgrunnlagForAutomatiskVurdering,
                kodeverkkode: Kodeverkkode,
                id: UUID,
                erKomplett: Boolean
            ): AutomatiskVurdering {
                return AutomatiskVurdering(
                    fødselsnummer = fødselsnummer,
                    skjæringstidspunkt = skjæringstidspunkt,
                    versjonAvKildekode = versjonAvKildekode,
                    grunnlagForAutomatiskVurdering = grunnlagForAutomatiskVurdering,
                    kodeverkkode = kodeverkkode,
                    id = id,
                    erKomplett = erKomplett
                )
            }
        }

        sealed interface OpptjeningsgrunnlagForAutomatiskVurdering {
            fun kodeverkkode(skjæringstidspunkt: LocalDate): Kodeverkkode

            class ForArbeidstaker(
                val arbeidsforhold: List<Arbeidsforhold>,
            ) : OpptjeningsgrunnlagForAutomatiskVurdering {
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

            object ForSelvstendigNæringsdrivende : OpptjeningsgrunnlagForAutomatiskVurdering {
                override fun kodeverkkode(skjæringstidspunkt: LocalDate): Kodeverkkode {
                    return Kodeverkkode.OPPTJENING_MINST_4_UKER
                }
            }
        }
    }
}
