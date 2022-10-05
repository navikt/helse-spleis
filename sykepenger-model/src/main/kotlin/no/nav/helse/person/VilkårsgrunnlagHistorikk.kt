package no.nav.helse.person

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.Toggle
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.OverstyrInntekt
import no.nav.helse.person.Varselkode.RV_IT_16
import no.nav.helse.person.Varselkode.RV_IT_17
import no.nav.helse.person.Varselkode.RV_IV_2
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

    private fun forrigeInnslag() = historikk.elementAtOrNull(1)

    internal fun accept(visitor: VilkårsgrunnlagHistorikkVisitor) {
        visitor.preVisitVilkårsgrunnlagHistorikk()
        historikk.forEach { it.accept(visitor) }
        visitor.postVisitVilkårsgrunnlagHistorikk()
    }

    internal fun lagre(vararg grunnlagselement: Grunnlagsdata) {
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

    internal fun oppdaterHistorikk(aktivitetslogg: IAktivitetslogg, skjæringstidspunkter: List<LocalDate>) {
        if (Toggle.ForkasteVilkårsgrunnlag.disabled) return
        val nyttInnslag = sisteInnlag()?.oppdaterHistorikk(aktivitetslogg, skjæringstidspunkter) ?: return
        if (nyttInnslag == sisteInnlag()) return
        historikk.add(0, nyttInnslag)
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

    internal fun blitt6GBegrensetSidenSist(skjæringstidspunkt: LocalDate): Boolean {
        if (sisteInnlag()?.vilkårsgrunnlagFor(skjæringstidspunkt)?.er6GBegrenset() == false) return false
        return forrigeInnslag()?.vilkårsgrunnlagFor(skjæringstidspunkt)?.er6GBegrenset() == false
    }

    internal class Innslag private constructor(
        internal val id: UUID,
        private val opprettet: LocalDateTime,
        private val vilkårsgrunnlag: MutableMap<LocalDate, VilkårsgrunnlagElement>
    ) {
        internal constructor(vilkårsgrunnlag: Map<LocalDate, VilkårsgrunnlagElement>) : this(
            UUID.randomUUID(),
            LocalDateTime.now(),
            vilkårsgrunnlag.toMutableMap()
        )

        internal constructor(other: Innslag?, elementer: List<VilkårsgrunnlagElement>) : this(other?.vilkårsgrunnlag?.toMap()?: emptyMap()) {
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
                .any { it.erRelevant(organisasjonsnummer) }

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

        internal fun oppdaterHistorikk(aktivitetslogg: IAktivitetslogg, skjæringstidspunkter: List<LocalDate>): Innslag {
            val gyldigeVilkårsgrunnlag = vilkårsgrunnlag.filter { (key, _)-> key in skjæringstidspunkter }
            val diff = this.vilkårsgrunnlag.size - gyldigeVilkårsgrunnlag.size
            if (diff > 0) aktivitetslogg.info("Fjernet $diff vilkårsgrunnlagselementer")
            return Innslag(gyldigeVilkårsgrunnlag)
        }

        internal companion object {
            fun gjenopprett(
                id: UUID,
                opprettet: LocalDateTime,
                elementer: Map<LocalDate, VilkårsgrunnlagElement>
            ): Innslag = Innslag(id, opprettet, elementer.toMutableMap())
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

        internal fun valider(overstyrInntekt: OverstyrInntekt): Boolean {
            validerOverstyrInntekt(overstyrInntekt)
            return !overstyrInntekt.harFunksjonelleFeilEllerVerre()
        }

        internal fun valider(aktivitetslogg: IAktivitetslogg, organisasjonsnummer: List<String>, erForlengelse: Boolean): Boolean {
            sykepengegrunnlag.validerInntekt(aktivitetslogg, organisasjonsnummer)
            valider(aktivitetslogg, erForlengelse)
            return !aktivitetslogg.harFunksjonelleFeilEllerVerre()
        }

        protected open fun validerOverstyrInntekt(overstyrInntekt: OverstyrInntekt) {}
        protected open fun valider(aktivitetslogg: IAktivitetslogg, erForlengelse: Boolean) {}

        internal abstract fun accept(vilkårsgrunnlagHistorikkVisitor: VilkårsgrunnlagHistorikkVisitor)
        internal open fun inngårISammenligningsgrunnlaget(organisasjonsnummer: String): Boolean = false
        internal fun build(builder: VedtakFattetBuilder) {
            sykepengegrunnlag.build(builder)
        }

        internal fun erRelevant(organisasjonsnummer: String): Boolean {
            return sykepengegrunnlag.erRelevant(organisasjonsnummer) || inngårISammenligningsgrunnlaget(organisasjonsnummer)
        }
        internal fun inntektskilde() = sykepengegrunnlag.inntektskilde()

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

        internal fun er6GBegrenset() = sykepengegrunnlag.er6GBegrenset()
        internal fun harNødvendigInntektForVilkårsprøving(organisasjonsnummer: String) =
            sykepengegrunnlag.harNødvendigInntektForVilkårsprøving(organisasjonsnummer)

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
        override fun valider(aktivitetslogg: IAktivitetslogg, erForlengelse: Boolean) {
            sjekkAvviksprosent(aktivitetslogg)
        }

        override fun sjekkAvviksprosent(aktivitetslogg: IAktivitetslogg): Boolean {
            if (avviksprosent == null) return true
            return Inntektsvurdering.sjekkAvvik(avviksprosent, aktivitetslogg, IAktivitetslogg::funksjonellFeil, RV_IV_2)
        }

        override fun accept(vilkårsgrunnlagHistorikkVisitor: VilkårsgrunnlagHistorikkVisitor) {
            vilkårsgrunnlagHistorikkVisitor.preVisitGrunnlagsdata(
                skjæringstidspunkt,
                this,
                sykepengegrunnlag,
                sammenligningsgrunnlag,
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
                sammenligningsgrunnlag,
                avviksprosent,
                medlemskapstatus,
                vurdertOk,
                meldingsreferanseId,
                vilkårsgrunnlagId
            )
        }

        override fun inngårISammenligningsgrunnlaget(organisasjonsnummer: String) =
            sammenligningsgrunnlag.erRelevant(organisasjonsnummer)

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

        internal fun harInntektFraAOrdningen(): Boolean = sykepengegrunnlag.harInntektFraAOrdningen()
    }

    internal class InfotrygdVilkårsgrunnlag(
        skjæringstidspunkt: LocalDate,
        sykepengegrunnlag: Sykepengegrunnlag,
        vilkårsgrunnlagId: UUID = UUID.randomUUID()
    ) : VilkårsgrunnlagElement(vilkårsgrunnlagId, skjæringstidspunkt, sykepengegrunnlag) {
        override fun validerOverstyrInntekt(overstyrInntekt: OverstyrInntekt) {
            overstyrInntekt.funksjonellFeil(RV_IT_17)
        }

        override fun valider(aktivitetslogg: IAktivitetslogg, erForlengelse: Boolean) {
            if (erForlengelse) {
                aktivitetslogg.info("Perioden har opphav i Infotrygd, men saken beholdes i Spleis fordi det er utbetalt i Spleis tidligere.")
                return
            }
            aktivitetslogg.funksjonellFeil(RV_IT_16)
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
