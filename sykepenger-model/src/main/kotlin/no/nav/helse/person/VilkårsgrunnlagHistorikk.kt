package no.nav.helse.person

import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.Utbetalingshistorikk
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosent
import java.time.LocalDate

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

    internal fun lagre(utbetalingshistorikk: Utbetalingshistorikk, skjæringstidspunkt: LocalDate) {
        historikk[skjæringstidspunkt] = utbetalingshistorikk.grunnlagsdata()
    }

    internal fun lagre(skjæringstidspunkt: LocalDate, grunnlagselement: VilkårsgrunnlagElement) {
        historikk[skjæringstidspunkt] = grunnlagselement
    }

    internal fun vilkårsgrunnlagFor(skjæringstidspunkt: LocalDate) = historikk[skjæringstidspunkt]

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
        internal val vurdertOk: Boolean
    ) : VilkårsgrunnlagElement {

        override fun valider(aktivitetslogg: Aktivitetslogg) {
        }

        override fun isOk() = false

        override fun accept(skjæringstidspunkt: LocalDate, vilkårsgrunnlagHistorikkVisitor: VilkårsgrunnlagHistorikkVisitor) {
            vilkårsgrunnlagHistorikkVisitor.visitGrunnlagsdata(skjæringstidspunkt, this)
        }
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
