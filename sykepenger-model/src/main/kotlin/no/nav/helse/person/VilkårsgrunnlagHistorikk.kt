package no.nav.helse.person

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.Grunnbeløp
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.til
import no.nav.helse.mai
import no.nav.helse.person.builders.VedtakFattetBuilder
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiode
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Prosent
import no.nav.helse.økonomi.Økonomi

internal class VilkårsgrunnlagHistorikk private constructor(private val historikk: MutableList<Innslag>) {

    internal constructor() : this(mutableListOf())

    private fun sisteInnlag() = historikk.firstOrNull()

    internal fun accept(visitor: VilkårsgrunnlagHistorikkVisitor) {
        visitor.preVisitVilkårsgrunnlagHistorikk()
        historikk.forEach { it.accept(visitor) }
        visitor.postVisitVilkårsgrunnlagHistorikk()
    }

    internal fun lagre(vararg grunnlagselement: VilkårsgrunnlagElement) {
        if (grunnlagselement.isEmpty()) return
        val siste = sisteInnlag()
        val nytt = Innslag(siste, grunnlagselement.toList())
        if (nytt == siste) return
        historikk.add(0, nytt)
    }

    internal fun sisteId() = sisteInnlag()!!.id


    internal fun build(skjæringstidspunkt: LocalDate, builder: VedtakFattetBuilder) {
        vilkårsgrunnlagFor(skjæringstidspunkt)?.build(builder)
    }

    internal fun vilkårsgrunnlagFor(skjæringstidspunkt: LocalDate) =
        sisteInnlag()?.vilkårsgrunnlagFor(skjæringstidspunkt)

    internal fun avvisInngangsvilkår(tidslinjer: List<Utbetalingstidslinje>) {
        sisteInnlag()?.avvis(tidslinjer)
    }

    internal fun skjæringstidspunkterFraSpleis() = sisteInnlag()?.skjæringstidspunkterFraSpleis() ?: emptySet()

    internal fun erRelevant(organisasjonsnummer: String, skjæringstidspunkter: List<LocalDate>) =
        sisteInnlag()?.erRelevant(organisasjonsnummer, skjæringstidspunkter) ?: false

    internal fun medInntekt(organisasjonsnummer: String, dato: LocalDate, økonomi: Økonomi, arbeidsgiverperiode: Arbeidsgiverperiode?, regler: ArbeidsgiverRegler, subsumsjonObserver: SubsumsjonObserver) =
        sisteInnlag()?.medInntekt(organisasjonsnummer, dato, økonomi, arbeidsgiverperiode, regler, subsumsjonObserver)

    internal fun utenInntekt(dato: LocalDate, økonomi: Økonomi, arbeidsgiverperiode: Arbeidsgiverperiode?) =
        sisteInnlag()!!.utenInntekt(dato, økonomi, arbeidsgiverperiode)

    internal class Innslag private constructor(
        internal val id: UUID,
        private val opprettet: LocalDateTime,
        private val vilkårsgrunnlag: MutableMap<LocalDate, VilkårsgrunnlagElement> = mutableMapOf()
    ) {
        internal constructor(other: Innslag?, elementer: List<VilkårsgrunnlagElement>) : this(
            UUID.randomUUID(),
            LocalDateTime.now()
        ) {
            if (other != null) this.vilkårsgrunnlag.putAll(other.vilkårsgrunnlag)
            elementer.forEach { it.add(this) }
        }

        internal fun accept(visitor: VilkårsgrunnlagHistorikkVisitor) {
            visitor.preVisitInnslag(this, id, opprettet)
            vilkårsgrunnlag.forEach { (_, element) ->
                element.accept(visitor)
            }
            visitor.postVisitInnslag(this, id, opprettet)
        }

        internal fun add(skjæringstidspunkt: LocalDate, vilkårsgrunnlagElement: VilkårsgrunnlagElement) {
            vilkårsgrunnlag[skjæringstidspunkt] = vilkårsgrunnlagElement
        }

        internal fun vilkårsgrunnlagFor(skjæringstidspunkt: LocalDate) =
            vilkårsgrunnlag[skjæringstidspunkt] ?: vilkårsgrunnlag.keys.sorted()
                .firstOrNull { it > skjæringstidspunkt }
                ?.let { vilkårsgrunnlag[it] }
                ?.takeIf { it is InfotrygdVilkårsgrunnlag }


        internal fun skjæringstidspunkterFraSpleis() = vilkårsgrunnlag
            .filterValues { it is Grunnlagsdata }
            .keys

        internal fun erRelevant(organisasjonsnummer: String, skjæringstidspunkter: List<LocalDate>) =
            skjæringstidspunkter.mapNotNull(vilkårsgrunnlag::get)
                .any {
                    it.sykepengegrunnlag().inntektsopplysningPerArbeidsgiver().containsKey(organisasjonsnummer)
                            || it.sammenligningsgrunnlagPerArbeidsgiver().containsKey(organisasjonsnummer)
                }

        internal fun avvis(tidslinjer: List<Utbetalingstidslinje>) {
            vilkårsgrunnlag.forEach { (skjæringstidspunkt, element) ->
                element.avvis(tidslinjer, skjæringstidspunkt)
            }
        }

        internal fun medInntekt(
            organisasjonsnummer: String,
            dato: LocalDate,
            økonomi: Økonomi,
            arbeidsgiverperiode: Arbeidsgiverperiode?,
            regler: ArbeidsgiverRegler,
            subsumsjonObserver: SubsumsjonObserver
        ): Økonomi? {
            return VilkårsgrunnlagElement.medInntekt(
                vilkårsgrunnlag.values,
                organisasjonsnummer,
                dato,
                økonomi,
                arbeidsgiverperiode,
                regler,
                subsumsjonObserver
            )
        }

        internal fun utenInntekt(
            dato: LocalDate,
            økonomi: Økonomi,
            arbeidsgiverperiode: Arbeidsgiverperiode?
        ): Økonomi {
            return VilkårsgrunnlagElement.utenInntekt(vilkårsgrunnlag.values, dato, økonomi, arbeidsgiverperiode)
        }

        override fun hashCode(): Int {
            return this.vilkårsgrunnlag.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            if (other !is Innslag) return false
            return this.vilkårsgrunnlag == other.vilkårsgrunnlag
        }

        internal companion object {
            fun gjenopprett(
                id: UUID,
                opprettet: LocalDateTime,
                elementer: Map<LocalDate, VilkårsgrunnlagElement>
            ): Innslag {
                return Innslag(id, opprettet).also {
                    it.vilkårsgrunnlag.putAll(elementer)
                }
            }
        }
    }

    internal abstract class VilkårsgrunnlagElement(
        protected val vilkårsgrunnlagId: UUID,
        protected val skjæringstidspunkt: LocalDate,
        protected val sykepengegrunnlag: Sykepengegrunnlag,
    ) : Aktivitetskontekst {
        internal fun add(innslag: Innslag) {
            innslag.add(skjæringstidspunkt, this)
        }

        internal fun valider(aktivitetslogg: IAktivitetslogg, organisasjonsnummer: List<String>): Boolean {
            sykepengegrunnlag.validerInntekt(aktivitetslogg, organisasjonsnummer)
            valider(aktivitetslogg)
            return !aktivitetslogg.hasErrorsOrWorse()
        }

        protected open fun valider(aktivitetslogg: IAktivitetslogg) {}

        internal abstract fun accept(vilkårsgrunnlagHistorikkVisitor: VilkårsgrunnlagHistorikkVisitor)
        internal fun sykepengegrunnlag() = sykepengegrunnlag
        internal abstract fun sammenligningsgrunnlagPerArbeidsgiver(): Map<String, Inntektshistorikk.Inntektsopplysning>
        internal fun build(builder: VedtakFattetBuilder) {
            sykepengegrunnlag.build(builder)
        }

        internal fun gjelderFlereArbeidsgivere() = sykepengegrunnlag.inntektsopplysningPerArbeidsgiver().size > 1
        internal open fun sjekkAvviksprosent(aktivitetslogg: IAktivitetslogg): Boolean = true
        internal open fun avvis(tidslinjer: List<Utbetalingstidslinje>, skjæringstidspunkt: LocalDate) {}

        final override fun toSpesifikkKontekst() = SpesifikkKontekst(
            kontekstType = "vilkårsgrunnlag",
            kontekstMap = mapOf(
                "vilkårsgrunnlagId" to vilkårsgrunnlagId.toString(),
                "skjæringstidspunkt" to skjæringstidspunkt.toString(),
                "vilkårsgrunnlagtype" to vilkårsgrunnlagtype()
            )
        )

        protected abstract fun vilkårsgrunnlagtype(): String

        private fun medInntekt(
            organisasjonsnummer: String,
            dato: LocalDate,
            økonomi: Økonomi,
            arbeidsgiverperiode: Arbeidsgiverperiode?,
            regler: ArbeidsgiverRegler,
            subsumsjonObserver: SubsumsjonObserver
        ): Økonomi? {
            return sykepengegrunnlag.medInntekt(
                organisasjonsnummer,
                dato,
                økonomi,
                arbeidsgiverperiode,
                regler,
                subsumsjonObserver
            )
        }

        internal companion object {
            internal fun medInntekt(
                elementer: MutableCollection<VilkårsgrunnlagElement>,
                organisasjonsnummer: String,
                dato: LocalDate,
                økonomi: Økonomi,
                arbeidsgiverperiode: Arbeidsgiverperiode?,
                regler: ArbeidsgiverRegler,
                subsumsjonObserver: SubsumsjonObserver
            ): Økonomi? {
                return finnVilkårsgrunnlag(elementer, dato)?.medInntekt(
                    organisasjonsnummer,
                    dato,
                    økonomi,
                    arbeidsgiverperiode,
                    regler,
                    subsumsjonObserver
                )
            }

            internal fun utenInntekt(
                elementer: MutableCollection<VilkårsgrunnlagElement>,
                dato: LocalDate,
                økonomi: Økonomi,
                arbeidsgiverperiode: Arbeidsgiverperiode?
            ): Økonomi {
                return økonomi.inntekt(
                    aktuellDagsinntekt = INGEN,
                    dekningsgrunnlag = INGEN,
                    skjæringstidspunkt = finnVilkårsgrunnlag(elementer, dato)?.skjæringstidspunkt ?: dato,
                    arbeidsgiverperiode = arbeidsgiverperiode
                )
            }

            private fun finnVilkårsgrunnlag(
                elementer: MutableCollection<VilkårsgrunnlagElement>,
                dato: LocalDate
            ): VilkårsgrunnlagElement? {
                return elementer
                    .filter { it.skjæringstidspunkt <= dato }
                    .maxByOrNull { it.skjæringstidspunkt }
            }
        }
    }

    internal class Grunnlagsdata(
        skjæringstidspunkt: LocalDate,
        sykepengegrunnlag: Sykepengegrunnlag,
        private val sammenligningsgrunnlag: Sammenligningsgrunnlag,
        private val avviksprosent: Prosent?,
        internal val opptjening: Opptjening,
        private val medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus,
        private val vurdertOk: Boolean,
        private val meldingsreferanseId: UUID?,
        vilkårsgrunnlagId: UUID
    ) : VilkårsgrunnlagElement(vilkårsgrunnlagId, skjæringstidspunkt, sykepengegrunnlag) {
        override fun valider(aktivitetslogg: IAktivitetslogg) {
            sjekkAvviksprosent(aktivitetslogg)
        }

        override fun sjekkAvviksprosent(aktivitetslogg: IAktivitetslogg): Boolean {
            if (avviksprosent == null) return true
            return Inntektsvurdering.sjekkAvvik(avviksprosent, aktivitetslogg, IAktivitetslogg::error)
        }

        override fun accept(vilkårsgrunnlagHistorikkVisitor: VilkårsgrunnlagHistorikkVisitor) {
            vilkårsgrunnlagHistorikkVisitor.preVisitGrunnlagsdata(
                skjæringstidspunkt,
                this,
                sykepengegrunnlag,
                sammenligningsgrunnlag.sammenligningsgrunnlag,
                avviksprosent,
                opptjening,
                vurdertOk,
                meldingsreferanseId,
                vilkårsgrunnlagId,
                medlemskapstatus
            )
            sykepengegrunnlag.accept(vilkårsgrunnlagHistorikkVisitor)
            sammenligningsgrunnlag.accept(vilkårsgrunnlagHistorikkVisitor)
            opptjening.accept(vilkårsgrunnlagHistorikkVisitor)
            vilkårsgrunnlagHistorikkVisitor.postVisitGrunnlagsdata(
                skjæringstidspunkt,
                this,
                sykepengegrunnlag,
                sammenligningsgrunnlag.sammenligningsgrunnlag,
                avviksprosent,
                medlemskapstatus,
                vurdertOk,
                meldingsreferanseId,
                vilkårsgrunnlagId
            )
        }

        override fun sammenligningsgrunnlagPerArbeidsgiver() =
            sammenligningsgrunnlag.inntektsopplysningPerArbeidsgiver()

        override fun avvis(tidslinjer: List<Utbetalingstidslinje>, skjæringstidspunkt: LocalDate) {
            val begrunnelser = mutableListOf<Begrunnelse>()
            if (medlemskapstatus == Medlemskapsvurdering.Medlemskapstatus.Nei) begrunnelser.add(Begrunnelse.ManglerMedlemskap)
            sykepengegrunnlag.begrunnelse(begrunnelser)
            if (!opptjening.erOppfylt()) begrunnelser.add(Begrunnelse.ManglerOpptjening)
            if (begrunnelser.isEmpty()) return
            Utbetalingstidslinje.avvis(tidslinjer, setOf(skjæringstidspunkt), begrunnelser)
        }

        override fun vilkårsgrunnlagtype() = "Spleis"

        internal fun kopierGrunnlagsdataMed(
            hendelse: PersonHendelse,
            sykepengegrunnlag: Sykepengegrunnlag,
            sammenligningsgrunnlag: Sammenligningsgrunnlag,
            sammenligningsgrunnlagVurdering: Boolean,
            avviksprosent: Prosent,
            nyOpptjening: Opptjening,
            meldingsreferanseId: UUID
        ): Grunnlagsdata {
            val sykepengegrunnlagOk = sykepengegrunnlag.valider(hendelse)
            return Grunnlagsdata(
                skjæringstidspunkt = skjæringstidspunkt,
                sykepengegrunnlag = sykepengegrunnlag,
                sammenligningsgrunnlag = sammenligningsgrunnlag,
                avviksprosent = avviksprosent,
                opptjening = nyOpptjening,
                medlemskapstatus = medlemskapstatus,
                vurdertOk = nyOpptjening.erOppfylt() && sykepengegrunnlagOk && sammenligningsgrunnlagVurdering,
                meldingsreferanseId = meldingsreferanseId,
                vilkårsgrunnlagId = UUID.randomUUID()
            )
        }

        fun harInntektFraAOrdningen(): Boolean = sykepengegrunnlag.inntektsopplysningPerArbeidsgiver().values
            .any { it is Inntektshistorikk.SkattComposite || it is Inntektshistorikk.IkkeRapportert }
    }

    internal class InfotrygdVilkårsgrunnlag(
        skjæringstidspunkt: LocalDate,
        sykepengegrunnlag: Sykepengegrunnlag,
        vilkårsgrunnlagId: UUID = UUID.randomUUID()
    ) : VilkårsgrunnlagElement(vilkårsgrunnlagId, skjæringstidspunkt, sykepengegrunnlag) {
        override fun valider(aktivitetslogg: IAktivitetslogg) {
            sjekkGammeltSkjæringstidspunkt(aktivitetslogg)
        }

        private fun sjekkGammeltSkjæringstidspunkt(aktivitetslogg: IAktivitetslogg) {
            if (skjæringstidspunkt !in 1.mai(2021) til 16.mai(2021)) return
            val gammeltGrunnbeløp = Grunnbeløp.`6G`.beløp(LocalDate.of(2021, 4, 30))
            if (sykepengegrunnlag < gammeltGrunnbeløp) return
            aktivitetslogg.warn("Første utbetalingsdag er i Infotrygd og mellom 1. og 16. mai. Kontroller at riktig grunnbeløp er brukt.")
        }

        override fun accept(vilkårsgrunnlagHistorikkVisitor: VilkårsgrunnlagHistorikkVisitor) {
            vilkårsgrunnlagHistorikkVisitor.preVisitInfotrygdVilkårsgrunnlag(
                this,
                skjæringstidspunkt,
                sykepengegrunnlag,
                vilkårsgrunnlagId
            )
            sykepengegrunnlag.accept(vilkårsgrunnlagHistorikkVisitor)
            vilkårsgrunnlagHistorikkVisitor.postVisitInfotrygdVilkårsgrunnlag(
                this,
                skjæringstidspunkt,
                sykepengegrunnlag,
                vilkårsgrunnlagId
            )
        }

        override fun sammenligningsgrunnlagPerArbeidsgiver() = emptyMap<String, Inntektshistorikk.Inntektsopplysning>()

        override fun vilkårsgrunnlagtype() = "Infotrygd"

        override fun equals(other: Any?): Boolean {
            if (other !is InfotrygdVilkårsgrunnlag) return false
            return skjæringstidspunkt == other.skjæringstidspunkt && sykepengegrunnlag == other.sykepengegrunnlag
        }

        override fun hashCode(): Int {
            var result = skjæringstidspunkt.hashCode()
            result = 31 * result + sykepengegrunnlag.hashCode()
            return result
        }
    }

    internal companion object {
        internal fun ferdigVilkårsgrunnlagHistorikk(innslag: List<Innslag>) =
            VilkårsgrunnlagHistorikk(innslag.toMutableList())
    }
}
