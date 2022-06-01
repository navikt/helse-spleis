package no.nav.helse.person

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosent

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

    internal fun vilkårsgrunnlagFor(skjæringstidspunkt: LocalDate) = sisteInnlag()?.vilkårsgrunnlagFor(skjæringstidspunkt)

    internal fun avvisInngangsvilkår(tidslinjer: List<Utbetalingstidslinje>) {
        sisteInnlag()?.avvis(tidslinjer)
    }

    internal fun inntektsopplysninger() = sisteInnlag()?.inntektsopplysningPerSkjæringstidspunktPerArbeidsgiver()

    internal fun skjæringstidspunkterFraSpleis() = sisteInnlag()?.skjæringstidspunkterFraSpleis() ?: emptySet()

    internal fun erRelevant(organisasjonsnummer: String, skjæringstidspunkter: List<LocalDate>) = sisteInnlag()?.erRelevant(organisasjonsnummer, skjæringstidspunkter) ?: false

    internal class Innslag private constructor(
        internal val id: UUID,
        private val opprettet: LocalDateTime,
        private val vilkårsgrunnlag: MutableMap<LocalDate, VilkårsgrunnlagElement> = mutableMapOf()
    ) {
        internal constructor(other: Innslag?, elementer: List<VilkårsgrunnlagElement>) : this(UUID.randomUUID(), LocalDateTime.now()) {
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

        internal fun inntektsopplysningPerSkjæringstidspunktPerArbeidsgiver() =
            vilkårsgrunnlag.mapValues { (_, vilkårsgrunnlagElement) ->
                vilkårsgrunnlagElement.sykepengegrunnlag().inntektsopplysningPerArbeidsgiver()
            }

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

        override fun hashCode(): Int {
            return this.vilkårsgrunnlag.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            if (other !is Innslag) return false
            return this.vilkårsgrunnlag == other.vilkårsgrunnlag
        }

        internal companion object {
            fun gjenopprett(id: UUID, opprettet: LocalDateTime, elementer: Map<LocalDate, VilkårsgrunnlagElement>): Innslag {
                return Innslag(id, opprettet).also {
                    it.vilkårsgrunnlag.putAll(elementer)
                }
            }
        }
    }

    internal interface VilkårsgrunnlagElement: Aktivitetskontekst {
        fun add(innslag: Innslag)
        fun valider(aktivitetslogg: Aktivitetslogg)
        fun accept(vilkårsgrunnlagHistorikkVisitor: VilkårsgrunnlagHistorikkVisitor)
        fun sykepengegrunnlag(): Sykepengegrunnlag
        fun sammenligningsgrunnlagPerArbeidsgiver(): Map<String, Inntektshistorikk.Inntektsopplysning>
        fun grunnlagsBegrensning(): Sykepengegrunnlag.Begrensning
        fun inntektsgrunnlag(): Inntekt // TODO: fjerne denne
        fun gjelderFlereArbeidsgivere(): Boolean
        fun sjekkAvviksprosent(aktivitetslogg: IAktivitetslogg): Boolean = true
        fun avvis(tidslinjer: List<Utbetalingstidslinje>, skjæringstidspunkt: LocalDate) {}
    }

    internal class Grunnlagsdata(
        private val skjæringstidspunkt: LocalDate,
        private val sykepengegrunnlag: Sykepengegrunnlag,
        private val sammenligningsgrunnlag: Sammenligningsgrunnlag,
        private val avviksprosent: Prosent?,
        internal val opptjening: Opptjening,
        private val medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus,
        private val vurdertOk: Boolean,
        private val meldingsreferanseId: UUID?,
        private val vilkårsgrunnlagId: UUID
    ) : VilkårsgrunnlagElement {
        override fun add(innslag: Innslag) {
            innslag.add(skjæringstidspunkt, this)
        }

        override fun valider(aktivitetslogg: Aktivitetslogg) {}

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

        override fun sykepengegrunnlag() = sykepengegrunnlag
        override fun grunnlagsBegrensning() = sykepengegrunnlag.begrensning
        override fun inntektsgrunnlag() = sykepengegrunnlag.inntektsgrunnlag
        override fun sammenligningsgrunnlagPerArbeidsgiver() = sammenligningsgrunnlag.inntektsopplysningPerArbeidsgiver()
        override fun gjelderFlereArbeidsgivere() = sykepengegrunnlag.inntektsopplysningPerArbeidsgiver().size > 1

        override fun avvis(tidslinjer: List<Utbetalingstidslinje>, skjæringstidspunkt: LocalDate) {
            val begrunnelser = mutableListOf<Begrunnelse>()
            if (medlemskapstatus == Medlemskapsvurdering.Medlemskapstatus.Nei) begrunnelser.add(Begrunnelse.ManglerMedlemskap)
            sykepengegrunnlag.begrunnelse(begrunnelser)
            if (!opptjening.erOppfylt()) begrunnelser.add(Begrunnelse.ManglerOpptjening)
            if (begrunnelser.isEmpty()) return
            Utbetalingstidslinje.avvis(tidslinjer, setOf(skjæringstidspunkt), begrunnelser)
        }

        override fun toSpesifikkKontekst() = SpesifikkKontekst(
            kontekstType = "vilkårsgrunnlag",
            kontekstMap = mapOf(
                "vilkårsgrunnlagId" to vilkårsgrunnlagId.toString(),
                "skjæringstidspunkt" to skjæringstidspunkt.toString(),
                "vilkårsgrunnlagtype" to "Spleis"
            )
        )

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
        private val skjæringstidspunkt: LocalDate,
        private val sykepengegrunnlag: Sykepengegrunnlag,
        private val vilkårsgrunnlagId: UUID = UUID.randomUUID()
    ) : VilkårsgrunnlagElement {
        override fun add(innslag: Innslag) {
            innslag.add(skjæringstidspunkt, this)
        }

        override fun valider(aktivitetslogg: Aktivitetslogg) {
        }

        override fun accept(vilkårsgrunnlagHistorikkVisitor: VilkårsgrunnlagHistorikkVisitor) {
            vilkårsgrunnlagHistorikkVisitor.preVisitInfotrygdVilkårsgrunnlag(this, skjæringstidspunkt, sykepengegrunnlag, vilkårsgrunnlagId)
            sykepengegrunnlag.accept(vilkårsgrunnlagHistorikkVisitor)
            vilkårsgrunnlagHistorikkVisitor.postVisitInfotrygdVilkårsgrunnlag(this, skjæringstidspunkt, sykepengegrunnlag, vilkårsgrunnlagId)
        }

        override fun sykepengegrunnlag() = sykepengegrunnlag
        override fun grunnlagsBegrensning() = sykepengegrunnlag.begrensning
        override fun sammenligningsgrunnlagPerArbeidsgiver() = emptyMap<String, Inntektshistorikk.Inntektsopplysning>()
        override fun inntektsgrunnlag() = sykepengegrunnlag.inntektsgrunnlag
        override fun gjelderFlereArbeidsgivere() = sykepengegrunnlag.inntektsopplysningPerArbeidsgiver().size > 1

        override fun toSpesifikkKontekst() = SpesifikkKontekst(
            kontekstType = "vilkårsgrunnlag",
            kontekstMap = mapOf(
                "vilkårsgrunnlagId" to vilkårsgrunnlagId.toString(),
                "skjæringstidspunkt" to skjæringstidspunkt.toString(),
                "vilkårsgrunnlagtype" to "Infotrygd"
            )
        )

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

    companion object {
        fun ferdigVilkårsgrunnlagHistorikk(parseVilkårsgrunnlag: List<Innslag>) =
            VilkårsgrunnlagHistorikk(parseVilkårsgrunnlag.map { it }.toMutableList())
    }
}
