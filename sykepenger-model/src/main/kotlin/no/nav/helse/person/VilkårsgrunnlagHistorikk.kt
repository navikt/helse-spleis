package no.nav.helse.person

import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosent
import java.time.LocalDate
import java.util.*

internal class VilkårsgrunnlagHistorikk private constructor(
    private val historikk: MutableMap<LocalDate, VilkårsgrunnlagElement>
) {
    internal constructor() : this(mutableMapOf())

    internal fun accept(vilkårsgrunnlagHistorikkVisitor: VilkårsgrunnlagHistorikkVisitor) {
        vilkårsgrunnlagHistorikkVisitor.preVisitVilkårsgrunnlagHistorikk()
        historikk.forEach { (skjæringstidspunkt, element) ->
            element.accept(skjæringstidspunkt, vilkårsgrunnlagHistorikkVisitor)
        }
        vilkårsgrunnlagHistorikkVisitor.postVisitVilkårsgrunnlagHistorikk()
    }

    internal fun lagre(vilkårsgrunnlag: Vilkårsgrunnlag, skjæringstidspunkt: LocalDate) {
        historikk[skjæringstidspunkt] = vilkårsgrunnlag.grunnlagsdata()
    }

    internal fun lagre(skjæringstidspunkt: LocalDate, grunnlagselement: VilkårsgrunnlagElement) {
        historikk[skjæringstidspunkt] = grunnlagselement
    }

    internal fun vilkårsgrunnlagFor(skjæringstidspunkt: LocalDate) = historikk[skjæringstidspunkt]

    internal fun avvisUtbetalingsdagerMedBegrunnelse(tidslinjer: List<Utbetalingstidslinje>) {
        Utbetalingstidslinje.avvis(tidslinjer, finnBegrunnelser())
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

    private fun finnBegrunnelser(): Map<LocalDate, List<Begrunnelse>> {
        val begrunnelserForSkjæringstidspunkt = mutableMapOf<LocalDate, List<Begrunnelse>>()
        historikk.forEach { historikk ->
            val vilkårsgrunnlagElement = historikk.value
            if (vilkårsgrunnlagElement is Grunnlagsdata && !vilkårsgrunnlagElement.isOk()) {
                val begrunnelser = mutableListOf<Begrunnelse>()

                if (vilkårsgrunnlagElement.medlemskapstatus == Medlemskapsvurdering.Medlemskapstatus.Nei) begrunnelser.add(Begrunnelse.ManglerMedlemskap)
                if (vilkårsgrunnlagElement.harMinimumInntekt == false) begrunnelser.add(Begrunnelse.ManglerMedlemskap)
                if (!vilkårsgrunnlagElement.harOpptjening) begrunnelser.add(Begrunnelse.ManglerOpptjening)
                begrunnelserForSkjæringstidspunkt[historikk.key] = begrunnelser
            }
        }
        return begrunnelserForSkjæringstidspunkt
    }
}
