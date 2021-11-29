package no.nav.helse.person

import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.person.VilkårsgrunnlagHistorikk.Innslag.Companion.sisteId
import no.nav.helse.utbetalingstidslinje.Alder
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosent
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class VilkårsgrunnlagHistorikk private constructor(private val historikk: MutableList<Innslag>) {

    internal constructor() : this(mutableListOf())

    private val innslag
        get() = (historikk.firstOrNull()?.clone() ?: Innslag(UUID.randomUUID(), LocalDateTime.now()))
            .also { historikk.add(0, it) }

    internal fun accept(visitor: VilkårsgrunnlagHistorikkVisitor) {
        visitor.preVisitVilkårsgrunnlagHistorikk()
        historikk.forEach { it.accept(visitor) }
        visitor.postVisitVilkårsgrunnlagHistorikk()
    }

    internal fun lagre(skjæringstidspunkt: LocalDate, vilkårsgrunnlag: Vilkårsgrunnlag) = lagre(skjæringstidspunkt, vilkårsgrunnlag.grunnlagsdata())

    internal fun lagre(skjæringstidspunkt: LocalDate, grunnlagselement: VilkårsgrunnlagElement) {
        innslag.add(skjæringstidspunkt, grunnlagselement)
    }

    internal fun sisteId() = historikk.sisteId()

    internal fun vilkårsgrunnlagFor(skjæringstidspunkt: LocalDate) = historikk.firstOrNull()?.vilkårsgrunnlagFor(skjæringstidspunkt)

    internal fun avvisUtbetalingsdagerMedBegrunnelse(tidslinjer: List<Utbetalingstidslinje>, alder: Alder) {
        Utbetalingstidslinje.avvis(tidslinjer, finnBegrunnelser(alder))
    }

    private fun finnBegrunnelser(alder: Alder): Map<LocalDate, List<Begrunnelse>> = historikk.firstOrNull()?.finnBegrunnelser(alder) ?: emptyMap()

    internal fun inntektsopplysningPerSkjæringstidspunktPerArbeidsgiver() = historikk.firstOrNull()?.inntektsopplysningPerSkjæringstidspunktPerArbeidsgiver()


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

        internal fun vilkårsgrunnlagFor(skjæringstidspunkt: LocalDate) =
            vilkårsgrunnlag[skjæringstidspunkt] ?: vilkårsgrunnlag.keys.sorted()
                .firstOrNull { it > skjæringstidspunkt }
                ?.let { vilkårsgrunnlag[it] }
                ?.takeIf { it is InfotrygdVilkårsgrunnlag }

        internal fun finnBegrunnelser(alder: Alder) =
            vilkårsgrunnlag.mapValues { (skjæringstidspunkt, vilkårsgrunnlagElement) ->
                if (vilkårsgrunnlagElement is Grunnlagsdata) {
                    vilkårsgrunnlagElement.begrunnelserForFeiletVilkårsprøving(alder, skjæringstidspunkt)
                } else emptyList()
            }.filterValues { it.isNotEmpty() }

        internal fun inntektsopplysningPerSkjæringstidspunktPerArbeidsgiver() =
            vilkårsgrunnlag.mapValues { (_, vilkårsgrunnlagElement) ->
                vilkårsgrunnlagElement.inntektsopplysningPerArbeidsgiver()
            }

        internal companion object {
            internal fun List<Innslag>.sisteId() = this.first().id
        }
    }

    internal interface VilkårsgrunnlagElement {
        fun valider(aktivitetslogg: Aktivitetslogg)
        fun isOk(): Boolean
        fun accept(skjæringstidspunkt: LocalDate, vilkårsgrunnlagHistorikkVisitor: VilkårsgrunnlagHistorikkVisitor)
        fun sykepengegrunnlag(): Inntekt
        fun grunnlagsBegrensning(): Sykepengegrunnlag.Begrensning
        fun grunnlagForSykepengegrunnlag(): Inntekt
        fun inntektsopplysningPerArbeidsgiver(): Map<String, Inntektshistorikk.Inntektsopplysning>
        fun gjelderFlereArbeidsgivere(): Boolean
        fun oppdaterManglendeMinimumInntekt(person: Person, skjæringstidspunkt: LocalDate) {}
        fun sjekkAvviksprosent(aktivitetslogg: IAktivitetslogg): Boolean = true
    }

    internal class Grunnlagsdata(
        private val sykepengegrunnlag: Sykepengegrunnlag,
        private val sammenligningsgrunnlag: Inntekt,
        private val avviksprosent: Prosent?,
        private val antallOpptjeningsdagerErMinst: Int,
        private val harOpptjening: Boolean,
        internal val medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus,
        internal val harMinimumInntekt: Boolean?,
        internal val vurdertOk: Boolean,
        internal val meldingsreferanseId: UUID?
    ) : VilkårsgrunnlagElement {
        private companion object {
            private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
        }

        override fun valider(aktivitetslogg: Aktivitetslogg) {}

        override fun sjekkAvviksprosent(aktivitetslogg: IAktivitetslogg): Boolean {
            if (avviksprosent == null) return true
            return Inntektsvurdering.sjekkAvvik(avviksprosent, aktivitetslogg)
        }

        override fun oppdaterManglendeMinimumInntekt(person: Person, skjæringstidspunkt: LocalDate) {
            if (harMinimumInntekt != null) return
            person.oppdaterHarMinimumInntekt(skjæringstidspunkt, this)
            sikkerLogg.info("Vi antar at det ikke finnes forlengelser til perioder som har harMinimumInntekt = null lenger")
        }

        override fun isOk() = vurdertOk

        override fun accept(skjæringstidspunkt: LocalDate, vilkårsgrunnlagHistorikkVisitor: VilkårsgrunnlagHistorikkVisitor) {
            vilkårsgrunnlagHistorikkVisitor.preVisitGrunnlagsdata(
                skjæringstidspunkt,
                this,
                sykepengegrunnlag,
                sammenligningsgrunnlag,
                avviksprosent,
                antallOpptjeningsdagerErMinst,
                harOpptjening
            )
            sykepengegrunnlag.accept(vilkårsgrunnlagHistorikkVisitor)
            vilkårsgrunnlagHistorikkVisitor.postVisitGrunnlagsdata(
                skjæringstidspunkt,
                this,
                sykepengegrunnlag,
                sammenligningsgrunnlag,
                avviksprosent,
                antallOpptjeningsdagerErMinst,
                harOpptjening
            )
        }

        override fun sykepengegrunnlag() = sykepengegrunnlag.sykepengegrunnlag
        override fun grunnlagsBegrensning() = sykepengegrunnlag.begrensning
        override fun grunnlagForSykepengegrunnlag() = sykepengegrunnlag.grunnlagForSykepengegrunnlag

        override fun inntektsopplysningPerArbeidsgiver() = sykepengegrunnlag.inntektsopplysningPerArbeidsgiver()
        override fun gjelderFlereArbeidsgivere() = inntektsopplysningPerArbeidsgiver().size > 1

        internal fun begrunnelserForFeiletVilkårsprøving(alder: Alder, skjæringstidspunkt: LocalDate): List<Begrunnelse> {
            if (isOk()) return emptyList()
            val begrunnelser = mutableListOf<Begrunnelse>()
            if (medlemskapstatus == Medlemskapsvurdering.Medlemskapstatus.Nei) begrunnelser.add(Begrunnelse.ManglerMedlemskap)
            if (harMinimumInntekt == false) begrunnelser.add(alder.begrunnelseForMinimumInntekt(skjæringstidspunkt))
            if (!harOpptjening) begrunnelser.add(Begrunnelse.ManglerOpptjening)
            return begrunnelser
        }

        internal fun grunnlagsdataMedMinimumInntektsvurdering(minimumInntektVurdering: Boolean) = Grunnlagsdata(
            sykepengegrunnlag = sykepengegrunnlag,
            sammenligningsgrunnlag = sammenligningsgrunnlag,
            avviksprosent = avviksprosent,
            antallOpptjeningsdagerErMinst = antallOpptjeningsdagerErMinst,
            harOpptjening = harOpptjening,
            medlemskapstatus = medlemskapstatus,
            harMinimumInntekt = minimumInntektVurdering,
            vurdertOk = vurdertOk && minimumInntektVurdering,
            meldingsreferanseId = meldingsreferanseId
        ).also {
            sikkerLogg.info("Oppretter nytt Grunnlagsdata med harMinimumInntekt=$harMinimumInntekt")
        }

        internal fun kopierGrunnlagsdataMed(
            sykepengegrunnlag: Sykepengegrunnlag,
            sammenligningsgrunnlag: Inntekt,
            sammenligningsgrunnlagVurdering: Boolean,
            avviksprosent: Prosent,
            minimumInntektVurdering: Boolean,
            meldingsreferanseId: UUID
        ) = Grunnlagsdata(
            sykepengegrunnlag = sykepengegrunnlag,
            sammenligningsgrunnlag = sammenligningsgrunnlag,
            avviksprosent = avviksprosent,
            antallOpptjeningsdagerErMinst = antallOpptjeningsdagerErMinst,
            harOpptjening = harOpptjening,
            medlemskapstatus = medlemskapstatus,
            harMinimumInntekt = minimumInntektVurdering,
            vurdertOk = minimumInntektVurdering && sammenligningsgrunnlagVurdering,
            meldingsreferanseId = meldingsreferanseId
        )
    }

    internal class InfotrygdVilkårsgrunnlag(private val sykepengegrunnlag: Sykepengegrunnlag) : VilkårsgrunnlagElement {
        override fun valider(aktivitetslogg: Aktivitetslogg) {
        }

        override fun isOk() = true

        override fun accept(skjæringstidspunkt: LocalDate, vilkårsgrunnlagHistorikkVisitor: VilkårsgrunnlagHistorikkVisitor) {
            vilkårsgrunnlagHistorikkVisitor.preVisitInfotrygdVilkårsgrunnlag(this, skjæringstidspunkt, sykepengegrunnlag)
            sykepengegrunnlag.accept(vilkårsgrunnlagHistorikkVisitor)
            vilkårsgrunnlagHistorikkVisitor.postVisitInfotrygdVilkårsgrunnlag(skjæringstidspunkt, this)
        }

        override fun sykepengegrunnlag() = sykepengegrunnlag.sykepengegrunnlag
        override fun grunnlagsBegrensning() = sykepengegrunnlag.begrensning

        override fun grunnlagForSykepengegrunnlag() = sykepengegrunnlag.grunnlagForSykepengegrunnlag

        override fun inntektsopplysningPerArbeidsgiver() = sykepengegrunnlag.inntektsopplysningPerArbeidsgiver()
        override fun gjelderFlereArbeidsgivere() = inntektsopplysningPerArbeidsgiver().size > 1

    }
}
