package no.nav.helse.person

import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosent
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class VilkårsgrunnlagHistorikk(private val historikk: MutableList<Innslag>) {

    internal constructor() : this(mutableListOf())

    private val innslag
        get() = (historikk.firstOrNull()?.clone() ?: Innslag(UUID.randomUUID(), LocalDateTime.now()))
            .also { historikk.add(0, it) }

    internal fun accept(visitor: VilkårsgrunnlagHistorikkVisitor) {
        visitor.preVisitVilkårsgrunnlagHistorikk()
        historikk.forEach { it.accept(visitor) }
        visitor.postVisitVilkårsgrunnlagHistorikk()
    }

    internal fun lagre(vilkårsgrunnlag: Vilkårsgrunnlag, skjæringstidspunkt: LocalDate) {
        innslag.add(skjæringstidspunkt, vilkårsgrunnlag.grunnlagsdata())
    }

    internal fun lagre(skjæringstidspunkt: LocalDate, grunnlagselement: VilkårsgrunnlagElement) {
        innslag.add(skjæringstidspunkt, grunnlagselement)
    }

    internal fun vilkårsgrunnlagFor(skjæringstidspunkt: LocalDate) = historikk.firstOrNull()?.vilkårsgrunnlagFor(skjæringstidspunkt)

    internal fun avvisUtbetalingsdagerMedBegrunnelse(tidslinjer: List<Utbetalingstidslinje>) {
        Utbetalingstidslinje.avvis(tidslinjer, finnBegrunnelser())
    }

    private fun finnBegrunnelser(): Map<LocalDate, List<Begrunnelse>> = historikk.firstOrNull()?.finnBegrunnelser() ?: emptyMap()

    internal class Innslag(private val id: UUID, private val opprettet: LocalDateTime) {
        private val vilkårsgrunnlag = mutableMapOf<LocalDate, VilkårsgrunnlagElement>()

        internal fun accept(visitor: VilkårsgrunnlagHistorikkVisitor) {
            visitor.preVisitInnslag(this, id, opprettet)
            vilkårsgrunnlag.forEach { (skjæringstidspunkt, element) ->
                element.accept(skjæringstidspunkt, visitor)
            }
            visitor.postVisitInnslag(this, id, opprettet)
        }

        internal fun clone() = Innslag(UUID.randomUUID(), LocalDateTime.now()).also {
            it.vilkårsgrunnlag.putAll(this.vilkårsgrunnlag)
        }

        internal fun add(skjæringstidspunkt: LocalDate, vilkårsgrunnlagElement: VilkårsgrunnlagElement) {
            vilkårsgrunnlag[skjæringstidspunkt] = vilkårsgrunnlagElement
        }

        internal fun vilkårsgrunnlagFor(skjæringstidspunkt: LocalDate) = vilkårsgrunnlag[skjæringstidspunkt]

        internal fun finnBegrunnelser(): Map<LocalDate, List<Begrunnelse>> {
            val begrunnelserForSkjæringstidspunkt = mutableMapOf<LocalDate, List<Begrunnelse>>()
            vilkårsgrunnlag.forEach { (skjæringstidspunkt, vilkårsgrunnlagElement) ->
                if (vilkårsgrunnlagElement is Grunnlagsdata && !vilkårsgrunnlagElement.isOk()) {
                    val begrunnelser = mutableListOf<Begrunnelse>()

                    if (vilkårsgrunnlagElement.medlemskapstatus == Medlemskapsvurdering.Medlemskapstatus.Nei) begrunnelser.add(Begrunnelse.ManglerMedlemskap)
                    if (vilkårsgrunnlagElement.harMinimumInntekt == false) begrunnelser.add(Begrunnelse.MinimumInntekt)
                    if (!vilkårsgrunnlagElement.harOpptjening) begrunnelser.add(Begrunnelse.ManglerOpptjening)
                    begrunnelserForSkjæringstidspunkt[skjæringstidspunkt] = begrunnelser
                }
            }
            return begrunnelserForSkjæringstidspunkt
        }
    }

    internal interface VilkårsgrunnlagElement {
        fun valider(aktivitetslogg: Aktivitetslogg)
        fun isOk(): Boolean
        fun accept(skjæringstidspunkt: LocalDate, vilkårsgrunnlagHistorikkVisitor: VilkårsgrunnlagHistorikkVisitor)
    }

    internal class Grunnlagsdata(
        internal val sammenligningsgrunnlag: Inntekt,
        internal val avviksprosent: Prosent?,
        internal val antallOpptjeningsdagerErMinst: Int,
        internal val harOpptjening: Boolean,
        internal val medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus,
        internal val harMinimumInntekt: Boolean?,
        internal val vurdertOk: Boolean,
        internal val meldingsreferanseId: UUID?
    ) : VilkårsgrunnlagElement {

        override fun valider(aktivitetslogg: Aktivitetslogg) {
        }

        override fun isOk() = vurdertOk

        override fun accept(skjæringstidspunkt: LocalDate, vilkårsgrunnlagHistorikkVisitor: VilkårsgrunnlagHistorikkVisitor) {
            vilkårsgrunnlagHistorikkVisitor.visitGrunnlagsdata(skjæringstidspunkt, this)
        }

        internal fun grunnlagsdataMedMinimumInntektsvurdering(minimumInntektVurdering: Boolean) = Grunnlagsdata(
            sammenligningsgrunnlag = sammenligningsgrunnlag,
            avviksprosent = avviksprosent,
            antallOpptjeningsdagerErMinst = antallOpptjeningsdagerErMinst,
            harOpptjening = harOpptjening,
            medlemskapstatus = medlemskapstatus,
            harMinimumInntekt = minimumInntektVurdering,
            vurdertOk = vurdertOk && minimumInntektVurdering,
            meldingsreferanseId = meldingsreferanseId
        )
    }

    internal class InfotrygdVilkårsgrunnlag : VilkårsgrunnlagElement {
        override fun valider(aktivitetslogg: Aktivitetslogg) {
        }

        override fun isOk() = true

        override fun accept(skjæringstidspunkt: LocalDate, vilkårsgrunnlagHistorikkVisitor: VilkårsgrunnlagHistorikkVisitor) {
            vilkårsgrunnlagHistorikkVisitor.visitInfotrygdVilkårsgrunnlag(skjæringstidspunkt, this)
        }
    }
}
