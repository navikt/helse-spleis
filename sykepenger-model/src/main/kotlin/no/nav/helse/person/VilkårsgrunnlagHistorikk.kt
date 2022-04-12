package no.nav.helse.person

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.utbetalingstidslinje.Alder
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosent
import org.slf4j.LoggerFactory

internal class VilkårsgrunnlagHistorikk private constructor(private val historikk: MutableList<Innslag>) {

    internal constructor() : this(mutableListOf())

    private fun nyttInnslag() = (sisteInnlag()?.clone() ?: Innslag(UUID.randomUUID(), LocalDateTime.now()))
            .also { historikk.add(0, it) }

    private fun sisteInnlag() = historikk.firstOrNull()

    internal fun accept(visitor: VilkårsgrunnlagHistorikkVisitor) {
        visitor.preVisitVilkårsgrunnlagHistorikk()
        historikk.forEach { it.accept(visitor) }
        visitor.postVisitVilkårsgrunnlagHistorikk()
    }

    internal fun lagre(skjæringstidspunkt: LocalDate, vilkårsgrunnlag: Vilkårsgrunnlag) = lagre(skjæringstidspunkt, vilkårsgrunnlag.grunnlagsdata())

    internal fun lagre(skjæringstidspunkt: LocalDate, grunnlagselement: VilkårsgrunnlagElement) {
        nyttInnslag().add(skjæringstidspunkt, grunnlagselement)
    }

    internal fun lagre(grunnlagselementer: List<VilkårsgrunnlagElement>) {
        val nyeElementer = sisteInnlag()?.filtrerUtKjenteVilkårsgrunnlag(grunnlagselementer) ?: grunnlagselementer
        if (nyeElementer.isEmpty()) return
        val nyttInnslag = nyttInnslag()
        nyeElementer.forEach { nyttInnslag.add(it.skjæringstidspunkt(), it) }
    }

    internal fun oppdaterMinimumInntektsvurdering(skjæringstidspunkt: LocalDate, grunnlagsdata: Grunnlagsdata, oppfyllerKravTilMinimumInntekt: Boolean) {
        nyttInnslag().add(skjæringstidspunkt, grunnlagsdata.kopierMedMinimumInntektsvurdering(oppfyllerKravTilMinimumInntekt))
    }

    internal fun sisteId() = sisteInnlag()!!.id

    internal fun vilkårsgrunnlagFor(skjæringstidspunkt: LocalDate) = sisteInnlag()?.vilkårsgrunnlagFor(skjæringstidspunkt)

    internal fun avvisInngangsvilkår(tidslinjer: List<Utbetalingstidslinje>, alder: Alder) {
        sisteInnlag()?.avvis(tidslinjer, alder)
    }

    internal fun inntektsopplysningPerSkjæringstidspunktPerArbeidsgiver() = sisteInnlag()?.inntektsopplysningPerSkjæringstidspunktPerArbeidsgiver()

    internal fun skjæringstidspunkterFraSpleis() = sisteInnlag()?.skjæringstidspunkterFraSpleis() ?: emptySet()

    internal fun erRelevant(organisasjonsnummer: String, skjæringstidspunkter: List<LocalDate>) = sisteInnlag()?.erRelevant(organisasjonsnummer, skjæringstidspunkter) ?: false

    internal class Innslag(internal val id: UUID, private val opprettet: LocalDateTime) {
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

        internal fun harSykepengegrunnlag(organisasjonsnummer: String, skjæringstidspunkt: LocalDate) =
            vilkårsgrunnlag[skjæringstidspunkt]?.sykepengegrunnlag()?.inntektsopplysningPerArbeidsgiver()?.containsKey(organisasjonsnummer) ?: false

        internal fun avvis(tidslinjer: List<Utbetalingstidslinje>, alder: Alder) {
            vilkårsgrunnlag.forEach { (skjæringstidspunkt, element) ->
                element.avvis(tidslinjer, skjæringstidspunkt, alder)
            }
        }

        internal fun filtrerUtKjenteVilkårsgrunnlag(grunnlagselementer: List<VilkårsgrunnlagElement>) =
            grunnlagselementer.filter { nyttElement ->
                val originaltElementForSkjæringstidspunkt = vilkårsgrunnlag[nyttElement.skjæringstidspunkt()]
                originaltElementForSkjæringstidspunkt != nyttElement
            }
    }

    internal interface VilkårsgrunnlagElement: Aktivitetskontekst {
        fun skjæringstidspunkt(): LocalDate
        fun valider(aktivitetslogg: Aktivitetslogg)
        fun accept(skjæringstidspunkt: LocalDate, vilkårsgrunnlagHistorikkVisitor: VilkårsgrunnlagHistorikkVisitor)
        fun sykepengegrunnlag(): Sykepengegrunnlag
        fun sammenligningsgrunnlag(): Inntekt?
        fun sammenligningsgrunnlagPerArbeidsgiver(): Map<String, Inntektshistorikk.Inntektsopplysning>
        fun grunnlagsBegrensning(): Sykepengegrunnlag.Begrensning
        fun grunnlagForSykepengegrunnlag(): Inntekt
        fun gjelderFlereArbeidsgivere(): Boolean
        fun oppdaterManglendeMinimumInntekt(person: Person, skjæringstidspunkt: LocalDate) {}
        fun sjekkAvviksprosent(aktivitetslogg: IAktivitetslogg): Boolean = true
        fun avvis(tidslinjer: List<Utbetalingstidslinje>, skjæringstidspunkt: LocalDate, alder: Alder) {}
    }

    internal class Grunnlagsdata(
        private val skjæringstidspunkt: LocalDate,
        private val sykepengegrunnlag: Sykepengegrunnlag,
        private val sammenligningsgrunnlag: Sammenligningsgrunnlag,
        private val avviksprosent: Prosent?,
        internal val opptjening: Opptjening,
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
                sammenligningsgrunnlag.sammenligningsgrunnlag,
                avviksprosent,
                opptjening,
                harMinimumInntekt,
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
                harMinimumInntekt,
                vurdertOk,
                meldingsreferanseId,
                vilkårsgrunnlagId
            )
        }

        override fun sykepengegrunnlag() = sykepengegrunnlag
        override fun grunnlagsBegrensning() = sykepengegrunnlag.begrensning
        override fun grunnlagForSykepengegrunnlag() = sykepengegrunnlag.grunnlagForSykepengegrunnlag
        override fun sammenligningsgrunnlag() = sammenligningsgrunnlag.sammenligningsgrunnlag
        override fun sammenligningsgrunnlagPerArbeidsgiver() = sammenligningsgrunnlag.inntektsopplysningPerArbeidsgiver()

        override fun gjelderFlereArbeidsgivere() = sykepengegrunnlag.inntektsopplysningPerArbeidsgiver().size > 1

        override fun avvis(tidslinjer: List<Utbetalingstidslinje>, skjæringstidspunkt: LocalDate, alder: Alder) {
            if (vurdertOk) return
            val begrunnelser = mutableListOf<Begrunnelse>()
            if (medlemskapstatus == Medlemskapsvurdering.Medlemskapstatus.Nei) begrunnelser.add(Begrunnelse.ManglerMedlemskap)
            if (harMinimumInntekt == false) begrunnelser.add(alder.begrunnelseForMinimumInntekt(skjæringstidspunkt))
            if (!opptjening.erOppfylt()) begrunnelser.add(Begrunnelse.ManglerOpptjening)
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

        internal fun kopierMedMinimumInntektsvurdering(minimumInntektVurdering: Boolean) = Grunnlagsdata(
            skjæringstidspunkt = skjæringstidspunkt,
            sykepengegrunnlag = sykepengegrunnlag,
            sammenligningsgrunnlag = sammenligningsgrunnlag,
            avviksprosent = avviksprosent,
            opptjening = opptjening, // TODO: Må sjekkes på nytt
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
            sammenligningsgrunnlag: Sammenligningsgrunnlag,
            sammenligningsgrunnlagVurdering: Boolean,
            avviksprosent: Prosent,
            nyOpptjening: Opptjening,
            minimumInntektVurdering: Boolean,
            meldingsreferanseId: UUID
        ) = Grunnlagsdata(
            skjæringstidspunkt = skjæringstidspunkt,
            sykepengegrunnlag = sykepengegrunnlag,
            sammenligningsgrunnlag = sammenligningsgrunnlag,
            avviksprosent = avviksprosent,
            opptjening = nyOpptjening,
            medlemskapstatus = medlemskapstatus,
            harMinimumInntekt = minimumInntektVurdering,
            vurdertOk = nyOpptjening.erOppfylt() && minimumInntektVurdering && sammenligningsgrunnlagVurdering,
            meldingsreferanseId = meldingsreferanseId,
            vilkårsgrunnlagId = UUID.randomUUID()
        )

        fun harInntektFraAOrdningen(): Boolean = sykepengegrunnlag.inntektsopplysningPerArbeidsgiver().values
            .any { it is Inntektshistorikk.SkattComposite || it is Inntektshistorikk.IkkeRapportert }
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

        override fun sykepengegrunnlag() = sykepengegrunnlag
        override fun grunnlagsBegrensning() = sykepengegrunnlag.begrensning
        override fun sammenligningsgrunnlag() = null
        override fun sammenligningsgrunnlagPerArbeidsgiver() = emptyMap<String, Inntektshistorikk.Inntektsopplysning>()

        override fun grunnlagForSykepengegrunnlag() = sykepengegrunnlag.grunnlagForSykepengegrunnlag

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
}
