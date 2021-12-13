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

    internal fun oppdaterMinimumInntektsvurdering(skjæringstidspunkt: LocalDate, grunnlagsdata: Grunnlagsdata, oppfyllerKravTilMinimumInntekt: Boolean) {
        innslag.add(skjæringstidspunkt, grunnlagsdata.kopierMedMinimumInntektsvurdering(oppfyllerKravTilMinimumInntekt))
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
                vilkårsgrunnlagElement.begrunnelserForAvvisteVilkår(alder, skjæringstidspunkt)
            }.filterValues { it.isNotEmpty() }

        internal fun inntektsopplysningPerSkjæringstidspunktPerArbeidsgiver() =
            vilkårsgrunnlag.mapValues { (_, vilkårsgrunnlagElement) ->
                vilkårsgrunnlagElement.inntektsopplysningPerArbeidsgiver()
            }

        internal companion object {
            internal fun List<Innslag>.sisteId() = this.first().id
        }
    }

    internal interface VilkårsgrunnlagElement: Aktivitetskontekst {
        fun skjæringstidspunkt(): LocalDate
        fun valider(aktivitetslogg: Aktivitetslogg)
        fun accept(skjæringstidspunkt: LocalDate, vilkårsgrunnlagHistorikkVisitor: VilkårsgrunnlagHistorikkVisitor)
        fun sykepengegrunnlag(): Inntekt
        fun grunnlagsBegrensning(): Sykepengegrunnlag.Begrensning
        fun grunnlagForSykepengegrunnlag(): Inntekt
        fun inntektsopplysningPerArbeidsgiver(): Map<String, Inntektshistorikk.Inntektsopplysning>
        fun gjelderFlereArbeidsgivere(): Boolean
        fun oppdaterManglendeMinimumInntekt(person: Person, skjæringstidspunkt: LocalDate) {}
        fun sjekkAvviksprosent(aktivitetslogg: IAktivitetslogg): Boolean = true
        fun begrunnelserForAvvisteVilkår(alder: Alder, skjæringstidspunkt: LocalDate): List<Begrunnelse>
    }

    internal class Grunnlagsdata(
        private val skjæringstidspunkt: LocalDate,
        private val sykepengegrunnlag: Sykepengegrunnlag,
        private val sammenligningsgrunnlag: Inntekt,
        private val avviksprosent: Prosent?,
        private val antallOpptjeningsdagerErMinst: Int,
        private val harOpptjening: Boolean,
        private val medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus,
        private val harMinimumInntekt: Boolean?,
        private val vurdertOk: Boolean,
        private val meldingsreferanseId: UUID?,
        private val vilkårsgrunnlagId: UUID
    ) : VilkårsgrunnlagElement {
        private companion object {
            private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
        }

        override fun skjæringstidspunkt() = skjæringstidspunkt

        override fun valider(aktivitetslogg: Aktivitetslogg) {}

        override fun sjekkAvviksprosent(aktivitetslogg: IAktivitetslogg): Boolean {
            if (avviksprosent == null) return true
            return Inntektsvurdering.sjekkAvvik(avviksprosent, aktivitetslogg, IAktivitetslogg::error)
        }

        override fun oppdaterManglendeMinimumInntekt(person: Person, skjæringstidspunkt: LocalDate) {
            if (harMinimumInntekt != null) return
            sykepengegrunnlag.oppdaterHarMinimumInntekt(skjæringstidspunkt, person, this)
            sikkerLogg.info("Vi antar at det ikke finnes forlengelser til perioder som har harMinimumInntekt = null lenger")
        }

        override fun accept(skjæringstidspunkt: LocalDate, vilkårsgrunnlagHistorikkVisitor: VilkårsgrunnlagHistorikkVisitor) {
            vilkårsgrunnlagHistorikkVisitor.preVisitGrunnlagsdata(
                skjæringstidspunkt,
                this,
                sykepengegrunnlag,
                sammenligningsgrunnlag,
                avviksprosent,
                antallOpptjeningsdagerErMinst,
                harOpptjening,
                medlemskapstatus,
                harMinimumInntekt,
                vurdertOk,
                meldingsreferanseId,
                vilkårsgrunnlagId
            )
            sykepengegrunnlag.accept(vilkårsgrunnlagHistorikkVisitor)
            vilkårsgrunnlagHistorikkVisitor.postVisitGrunnlagsdata(
                skjæringstidspunkt,
                this,
                sykepengegrunnlag,
                sammenligningsgrunnlag,
                avviksprosent,
                antallOpptjeningsdagerErMinst,
                harOpptjening,
                medlemskapstatus,
                harMinimumInntekt,
                vurdertOk,
                meldingsreferanseId,
                vilkårsgrunnlagId
            )
        }

        override fun sykepengegrunnlag() = sykepengegrunnlag.sykepengegrunnlag
        override fun grunnlagsBegrensning() = sykepengegrunnlag.begrensning
        override fun grunnlagForSykepengegrunnlag() = sykepengegrunnlag.grunnlagForSykepengegrunnlag

        override fun inntektsopplysningPerArbeidsgiver() = sykepengegrunnlag.inntektsopplysningPerArbeidsgiver()
        override fun gjelderFlereArbeidsgivere() = inntektsopplysningPerArbeidsgiver().size > 1

        override fun begrunnelserForAvvisteVilkår(alder: Alder, skjæringstidspunkt: LocalDate): List<Begrunnelse> {
            if (vurdertOk) return emptyList()
            val begrunnelser = mutableListOf<Begrunnelse>()
            if (medlemskapstatus == Medlemskapsvurdering.Medlemskapstatus.Nei) begrunnelser.add(Begrunnelse.ManglerMedlemskap)
            if (harMinimumInntekt == false) begrunnelser.add(alder.begrunnelseForMinimumInntekt(skjæringstidspunkt))
            if (!harOpptjening) begrunnelser.add(Begrunnelse.ManglerOpptjening)
            return begrunnelser
        }

        override fun toSpesifikkKontekst() = SpesifikkKontekst(
            kontekstType = "vilkårsgrunnlag",
            kontekstMap = mapOf(
                "vilkårsgrunnlagId" to vilkårsgrunnlagId.toString(),
                "skjæringstidspunkt" to skjæringstidspunkt.toString(),
                "vilkårsgrunnlagtype" to "Spleis"
            )
        )

        internal fun kopierMedMinimumInntektsvurdering(minimumInntektVurdering: Boolean) = Grunnlagsdata(
            skjæringstidspunkt = skjæringstidspunkt,
            sykepengegrunnlag = sykepengegrunnlag,
            sammenligningsgrunnlag = sammenligningsgrunnlag,
            avviksprosent = avviksprosent,
            antallOpptjeningsdagerErMinst = antallOpptjeningsdagerErMinst,
            harOpptjening = harOpptjening,
            medlemskapstatus = medlemskapstatus,
            harMinimumInntekt = minimumInntektVurdering,
            vurdertOk = vurdertOk && minimumInntektVurdering,
            meldingsreferanseId = meldingsreferanseId,
            vilkårsgrunnlagId = UUID.randomUUID()
        ).also {
            sikkerLogg.info("Oppretter nytt Grunnlagsdata med harMinimumInntekt=$minimumInntektVurdering")
        }

        internal fun kopierGrunnlagsdataMed(
            sykepengegrunnlag: Sykepengegrunnlag,
            sammenligningsgrunnlag: Inntekt,
            sammenligningsgrunnlagVurdering: Boolean,
            avviksprosent: Prosent,
            minimumInntektVurdering: Boolean,
            meldingsreferanseId: UUID
        ) = Grunnlagsdata(
            skjæringstidspunkt = skjæringstidspunkt,
            sykepengegrunnlag = sykepengegrunnlag,
            sammenligningsgrunnlag = sammenligningsgrunnlag,
            avviksprosent = avviksprosent,
            antallOpptjeningsdagerErMinst = antallOpptjeningsdagerErMinst,
            harOpptjening = harOpptjening,
            medlemskapstatus = medlemskapstatus,
            harMinimumInntekt = minimumInntektVurdering,
            vurdertOk = minimumInntektVurdering && sammenligningsgrunnlagVurdering,
            meldingsreferanseId = meldingsreferanseId,
            vilkårsgrunnlagId = UUID.randomUUID()
        )

        fun harInntektFraSkatt(): Boolean = inntektsopplysningPerArbeidsgiver().values.any { it is Inntektshistorikk.SkattComposite }
    }

    internal class InfotrygdVilkårsgrunnlag(
        private val skjæringstidspunkt: LocalDate,
        private val sykepengegrunnlag: Sykepengegrunnlag,
        private val vilkårsgrunnlagId: UUID = UUID.randomUUID()
    ) : VilkårsgrunnlagElement {
        override fun skjæringstidspunkt() = skjæringstidspunkt

        override fun valider(aktivitetslogg: Aktivitetslogg) {
        }

        override fun accept(skjæringstidspunkt: LocalDate, vilkårsgrunnlagHistorikkVisitor: VilkårsgrunnlagHistorikkVisitor) {
            vilkårsgrunnlagHistorikkVisitor.preVisitInfotrygdVilkårsgrunnlag(this, skjæringstidspunkt, sykepengegrunnlag, vilkårsgrunnlagId)
            sykepengegrunnlag.accept(vilkårsgrunnlagHistorikkVisitor)
            vilkårsgrunnlagHistorikkVisitor.postVisitInfotrygdVilkårsgrunnlag(this, skjæringstidspunkt, sykepengegrunnlag, vilkårsgrunnlagId)
        }

        override fun sykepengegrunnlag() = sykepengegrunnlag.sykepengegrunnlag
        override fun grunnlagsBegrensning() = sykepengegrunnlag.begrensning

        override fun grunnlagForSykepengegrunnlag() = sykepengegrunnlag.grunnlagForSykepengegrunnlag

        override fun inntektsopplysningPerArbeidsgiver() = sykepengegrunnlag.inntektsopplysningPerArbeidsgiver()
        override fun gjelderFlereArbeidsgivere() = inntektsopplysningPerArbeidsgiver().size > 1

        // Vi har ingen avviste vilkår dersom vilkårsprøving er gjort i Infotrygd
        override fun begrunnelserForAvvisteVilkår(alder: Alder, skjæringstidspunkt: LocalDate): List<Begrunnelse> = emptyList()
        override fun toSpesifikkKontekst() = SpesifikkKontekst(
            kontekstType = "vilkårsgrunnlag",
            kontekstMap = mapOf(
                "vilkårsgrunnlagId" to vilkårsgrunnlagId.toString(),
                "skjæringstidspunkt" to skjæringstidspunkt.toString(),
                "vilkårsgrunnlagtype" to "Infotrygd"
            )
        )
    }
}
