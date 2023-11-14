package no.nav.helse.serde.api.speil.builders

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.Grunnbeløp
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Subsumsjon
import no.nav.helse.person.Opptjening
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.person.VilkårsgrunnlagHistorikkVisitor
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysningForSammenligningsgrunnlag
import no.nav.helse.person.inntekt.IkkeRapportert
import no.nav.helse.person.inntekt.Infotrygd
import no.nav.helse.person.inntekt.Inntektsmelding
import no.nav.helse.person.inntekt.InntektsopplysningVisitor
import no.nav.helse.person.inntekt.Saksbehandler
import no.nav.helse.person.inntekt.Sammenligningsgrunnlag
import no.nav.helse.person.inntekt.SkattSykepengegrunnlag
import no.nav.helse.person.inntekt.Skatteopplysning
import no.nav.helse.person.inntekt.SkjønnsmessigFastsatt
import no.nav.helse.person.inntekt.Sykepengegrunnlag
import no.nav.helse.person.inntekt.Sykepengegrunnlag.FastsattEtterSkjønn
import no.nav.helse.serde.api.dto.GhostPeriodeDTO
import no.nav.helse.serde.api.dto.Refusjonselement
import no.nav.helse.serde.api.dto.SpleisVilkårsgrunnlag
import no.nav.helse.serde.api.dto.Vilkårsgrunnlag
import no.nav.helse.økonomi.Avviksprosent
import no.nav.helse.økonomi.Inntekt
import kotlin.properties.Delegates

internal class ISykepengegrunnlag(
    val inntekterPerArbeidsgiver: List<IArbeidsgiverinntekt>,
    val sykepengegrunnlag: Double,
    val oppfyllerMinsteinntektskrav: Boolean,
    val beregningsgrunnlag: Double,
    val omregnetÅrsinntekt: Double,
    val begrensning: SykepengegrunnlagsgrenseDTO,
    val refusjonsopplysningerPerArbeidsgiver: List<IArbeidsgiverrefusjon>,
    val avviksprosent: Double,
    val sammenligningsgrunnlagTotal: Double,
    val skjønnsmessigFastsattÅrlig: Double?
)

internal class IInntekt(
    val årlig: Double,
    val månedlig: Double,
    val daglig: Double
)

internal abstract class IVilkårsgrunnlag(
    val skjæringstidspunkt: LocalDate,
    val beregningsgrunnlag: Double,
    val sammenligningsgrunnlag: Double?,
    val sykepengegrunnlag: Double,
    val inntekter: List<IArbeidsgiverinntekt>,
    val refusjonsopplysningerPerArbeidsgiver: List<IArbeidsgiverrefusjon>,
    val id: UUID
) {
    private val arbeidsgivereMedInntekt = inntekter.filter { it.omregnetÅrsinntekt != null }
    abstract fun toDTO(): Vilkårsgrunnlag
    fun inngårIkkeISammenligningsgrunnlag(organisasjonsnummer: String) = inntekter.none { it.arbeidsgiver == organisasjonsnummer }
    open fun potensiellGhostperiode(
        organisasjonsnummer: String,
        sykefraværstilfeller: Map<LocalDate, List<ClosedRange<LocalDate>>>
    ): GhostPeriodeDTO? {
        if (arbeidsgivereMedInntekt.size < 2 || this.skjæringstidspunkt !in sykefraværstilfeller) return null
        val inntekten = inntekter.firstOrNull { it.arbeidsgiver == organisasjonsnummer }
        if (inntekten == null || inntekten.omregnetÅrsinntekt?.kilde == null) return null
        return GhostPeriodeDTO(
            id = UUID.randomUUID(),
            fom = skjæringstidspunkt,
            tom = sykefraværstilfeller.getValue(skjæringstidspunkt).maxOf { it.endInclusive },
            skjæringstidspunkt = skjæringstidspunkt,
            vilkårsgrunnlagId = this.id,
            deaktivert = inntekten.deaktivert
        )
    }

}

internal class ISpleisGrunnlag(
    skjæringstidspunkt: LocalDate,
    beregningsgrunnlag: Double,
    sammenligningsgrunnlag: Double,
    inntekter: List<IArbeidsgiverinntekt>,
    sykepengegrunnlag: Double,
    id: UUID,
    refusjonsopplysningerPerArbeidsgiver: List<IArbeidsgiverrefusjon>,
    val skjønnsmessigFastsattÅrlig: Double?,
    val omregnetÅrsinntekt: Double,
    val avviksprosent: Double?,
    val grunnbeløp: Int,
    val sykepengegrunnlagsgrense: SykepengegrunnlagsgrenseDTO,
    val meldingsreferanseId: UUID?,
    val antallOpptjeningsdagerErMinst: Int,
    val oppfyllerKravOmMinstelønn: Boolean,
    val oppfyllerKravOmOpptjening: Boolean,
    val oppfyllerKravOmMedlemskap: Boolean?
) : IVilkårsgrunnlag(skjæringstidspunkt, beregningsgrunnlag, sammenligningsgrunnlag, sykepengegrunnlag, inntekter, refusjonsopplysningerPerArbeidsgiver, id) {

    override fun toDTO(): Vilkårsgrunnlag {
        return SpleisVilkårsgrunnlag(
            skjæringstidspunkt = skjæringstidspunkt,
            beregningsgrunnlag = beregningsgrunnlag,
            omregnetÅrsinntekt = omregnetÅrsinntekt,
            sammenligningsgrunnlag = sammenligningsgrunnlag!!,
            sykepengegrunnlag = sykepengegrunnlag,
            inntekter = inntekter.map { it.toDTO() },
            arbeidsgiverrefusjoner = refusjonsopplysningerPerArbeidsgiver.map { it.toDTO() },
            skjønnsmessigFastsattÅrlig = skjønnsmessigFastsattÅrlig,
            avviksprosent = avviksprosent,
            grunnbeløp = grunnbeløp,
            sykepengegrunnlagsgrense = sykepengegrunnlagsgrense,
            antallOpptjeningsdagerErMinst = antallOpptjeningsdagerErMinst,
            opptjeningFra = skjæringstidspunkt.minusDays(antallOpptjeningsdagerErMinst.toLong()),
            oppfyllerKravOmMinstelønn = oppfyllerKravOmMinstelønn,
            oppfyllerKravOmOpptjening = oppfyllerKravOmOpptjening,
            oppfyllerKravOmMedlemskap = oppfyllerKravOmMedlemskap
        )
    }
}

class SykepengegrunnlagsgrenseDTO(
    val grunnbeløp: Int,
    val grense: Int,
    val virkningstidspunkt: LocalDate,
) {
    companion object {
        fun fra6GBegrensning(`6G`: Inntekt): SykepengegrunnlagsgrenseDTO {
            val `1G` = `6G` / 6
            return SykepengegrunnlagsgrenseDTO(
                grunnbeløp = InntektBuilder(`1G`).build().årlig.toInt(),
                grense = InntektBuilder(`6G`).build().årlig.toInt(),
                virkningstidspunkt = Grunnbeløp.virkningstidspunktFor(`1G`)
            )
        }
    }
}

internal class IInfotrygdGrunnlag(
    skjæringstidspunkt: LocalDate,
    beregningsgrunnlag: Double,
    sammenligningsgrunnlag: Double?,
    inntekter: List<IArbeidsgiverinntekt>,
    refusjonsopplysningerPerArbeidsgiver: List<IArbeidsgiverrefusjon>,
    sykepengegrunnlag: Double,
    id: UUID
) : IVilkårsgrunnlag(skjæringstidspunkt, beregningsgrunnlag, sammenligningsgrunnlag, sykepengegrunnlag, inntekter, refusjonsopplysningerPerArbeidsgiver, id) {

    override fun toDTO(): Vilkårsgrunnlag {
        return no.nav.helse.serde.api.dto.InfotrygdVilkårsgrunnlag(
            skjæringstidspunkt = skjæringstidspunkt,
            beregningsgrunnlag = beregningsgrunnlag,
            sammenligningsgrunnlag = sammenligningsgrunnlag,
            sykepengegrunnlag = sykepengegrunnlag,
            inntekter = inntekter.map { it.toDTO() },
            arbeidsgiverrefusjoner = refusjonsopplysningerPerArbeidsgiver.map { it.toDTO() },
        )
    }

    override fun potensiellGhostperiode(organisasjonsnummer: String, sykefraværstilfeller: Map<LocalDate, List<ClosedRange<LocalDate>>>) = null
}

internal class InntektBuilder(private val inntekt: Inntekt) {
    internal fun build(): IInntekt {
        return inntekt.reflection { årlig, månedlig, daglig, _ ->
            IInntekt(årlig, månedlig, daglig)
        }
    }
}

internal class IVilkårsgrunnlagHistorikk(private val tilgjengeligeVilkårsgrunnlag: Map<Int, Map<UUID, IVilkårsgrunnlag>>) {
    private val vilkårsgrunnlagIBruk = mutableMapOf<UUID, IVilkårsgrunnlag>()

    internal fun inngårIkkeISammenligningsgrunnlag(organisasjonsnummer: String) =
        vilkårsgrunnlagIBruk.all { (_, a) -> a.inngårIkkeISammenligningsgrunnlag(organisasjonsnummer) }

    internal fun potensielleGhostsperioder(
        organisasjonsnummer: String,
        sykefraværstilfeller: Map<LocalDate, List<ClosedRange<LocalDate>>>
    ) =
        tilgjengeligeVilkårsgrunnlag[0]?.mapNotNull { (_, vilkårsgrunnlag) ->
            vilkårsgrunnlag.potensiellGhostperiode(organisasjonsnummer, sykefraværstilfeller)
        } ?: emptyList()

    internal fun toDTO(): Map<UUID, Vilkårsgrunnlag> {
        return vilkårsgrunnlagIBruk.mapValues { (_, vilkårsgrunnlag) -> vilkårsgrunnlag.toDTO() }
    }

    internal fun leggIBøtta(vilkårsgrunnlagId: UUID): IVilkårsgrunnlag {
        return vilkårsgrunnlagIBruk.getOrPut(vilkårsgrunnlagId) {
            tilgjengeligeVilkårsgrunnlag.entries.firstNotNullOf { (_, elementer) ->
                elementer[vilkårsgrunnlagId]
            }
        }
    }

    internal fun potensiellUBeregnetVilkårsprøvdPeriode(skjæringstidspunkt: LocalDate): UUID? {
        return tilgjengeligeVilkårsgrunnlag[0]?.filterValues {
            it.skjæringstidspunkt == skjæringstidspunkt
        }?.entries?.singleOrNull()?.key ?: return null
    }
}

internal class VilkårsgrunnlagBuilder(vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk) : VilkårsgrunnlagHistorikkVisitor {
    private var innslagIndex = 0
    private val historikk = mutableMapOf<Int, MutableMap<UUID, IVilkårsgrunnlag>>()

    init {
        vilkårsgrunnlagHistorikk.accept(this)
    }

    internal fun build() = IVilkårsgrunnlagHistorikk(historikk)

    override fun postVisitInnslag(innslag: VilkårsgrunnlagHistorikk.Innslag, id: UUID, opprettet: LocalDateTime) {
        innslagIndex += 1
    }

    override fun preVisitGrunnlagsdata(
        skjæringstidspunkt: LocalDate,
        grunnlagsdata: VilkårsgrunnlagHistorikk.Grunnlagsdata,
        sykepengegrunnlag: Sykepengegrunnlag,
        opptjening: Opptjening,
        vurdertOk: Boolean,
        meldingsreferanseId: UUID?,
        vilkårsgrunnlagId: UUID,
        medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus
    ) {
        historikk.getOrPut(innslagIndex) { mutableMapOf() }
            .getOrPut(vilkårsgrunnlagId) {
                val compositeSykepengegrunnlag = SykepengegrunnlagBuilder(sykepengegrunnlag).build()
                val oppfyllerKravOmMedlemskap = when (medlemskapstatus) {
                    Medlemskapsvurdering.Medlemskapstatus.Ja -> true
                    Medlemskapsvurdering.Medlemskapstatus.Nei -> false
                    else -> null
                }
                ISpleisGrunnlag(
                    skjæringstidspunkt = skjæringstidspunkt,
                    beregningsgrunnlag = compositeSykepengegrunnlag.beregningsgrunnlag,
                    omregnetÅrsinntekt = compositeSykepengegrunnlag.omregnetÅrsinntekt,
                    sammenligningsgrunnlag = compositeSykepengegrunnlag.sammenligningsgrunnlagTotal,
                    inntekter = compositeSykepengegrunnlag.inntekterPerArbeidsgiver,
                    refusjonsopplysningerPerArbeidsgiver = compositeSykepengegrunnlag.refusjonsopplysningerPerArbeidsgiver,
                    sykepengegrunnlag = compositeSykepengegrunnlag.sykepengegrunnlag,
                    skjønnsmessigFastsattÅrlig = compositeSykepengegrunnlag.skjønnsmessigFastsattÅrlig,
                    avviksprosent = compositeSykepengegrunnlag.avviksprosent,
                    grunnbeløp = compositeSykepengegrunnlag.begrensning.grunnbeløp,
                    sykepengegrunnlagsgrense = compositeSykepengegrunnlag.begrensning,
                    meldingsreferanseId = meldingsreferanseId,
                    antallOpptjeningsdagerErMinst = opptjening.opptjeningsdager(),
                    oppfyllerKravOmMinstelønn = compositeSykepengegrunnlag.oppfyllerMinsteinntektskrav,
                    oppfyllerKravOmOpptjening = opptjening.erOppfylt(),
                    oppfyllerKravOmMedlemskap = oppfyllerKravOmMedlemskap,
                    id = vilkårsgrunnlagId
                )
            }
    }

    override fun preVisitInfotrygdVilkårsgrunnlag(infotrygdVilkårsgrunnlag: VilkårsgrunnlagHistorikk.InfotrygdVilkårsgrunnlag, skjæringstidspunkt: LocalDate, sykepengegrunnlag: Sykepengegrunnlag, vilkårsgrunnlagId: UUID) {
        historikk
            .getOrPut(innslagIndex) { mutableMapOf() }
            .getOrPut(vilkårsgrunnlagId) {
                val byggetSykepengegrunnlag = SykepengegrunnlagBuilder(
                    sykepengegrunnlag
                ).build()
                IInfotrygdGrunnlag(
                    skjæringstidspunkt = skjæringstidspunkt,
                    beregningsgrunnlag = byggetSykepengegrunnlag.beregningsgrunnlag,
                    sammenligningsgrunnlag = null,
                    inntekter = byggetSykepengegrunnlag.inntekterPerArbeidsgiver,
                    refusjonsopplysningerPerArbeidsgiver = byggetSykepengegrunnlag.refusjonsopplysningerPerArbeidsgiver,
                    sykepengegrunnlag = byggetSykepengegrunnlag.sykepengegrunnlag,
                    id = vilkårsgrunnlagId
                )
            }
    }

    internal class SykepengegrunnlagBuilder(
        sykepengegrunnlag: Sykepengegrunnlag,
    ) : VilkårsgrunnlagHistorikkVisitor {
        private lateinit var `6G`: Inntekt
        private val inntekterPerArbeidsgiver = mutableListOf<IArbeidsgiverinntekt>()
        private val refusjonsopplysningerPerArbeidsgiver = mutableListOf<IArbeidsgiverrefusjon>()
        private lateinit var sykepengegrunnlag: IInntekt
        private lateinit var tilstand: Sykepengegrunnlag.Tilstand
        private var beregningsgrunnlag by Delegates.notNull<Double>()
        private var omregnetÅrsinntekt by Delegates.notNull<Double>()
        private var avviksprosent by Delegates.notNull<Double>()
        private var sammenligningsgrunnlagTotal by Delegates.notNull<Double>()
        private var oppfyllerMinsteinntektskrav by Delegates.notNull<Boolean>()

        init {
            sykepengegrunnlag.accept(this)
        }

        fun build(): ISykepengegrunnlag {
            return ISykepengegrunnlag(
                inntekterPerArbeidsgiver = inntekterPerArbeidsgiver.toList(),
                sykepengegrunnlag = sykepengegrunnlag.årlig,
                oppfyllerMinsteinntektskrav = oppfyllerMinsteinntektskrav,
                beregningsgrunnlag = beregningsgrunnlag,
                omregnetÅrsinntekt = omregnetÅrsinntekt,
                begrensning = SykepengegrunnlagsgrenseDTO.fra6GBegrensning(this.`6G`),
                refusjonsopplysningerPerArbeidsgiver = refusjonsopplysningerPerArbeidsgiver.toList(),
                avviksprosent = avviksprosent,
                sammenligningsgrunnlagTotal = sammenligningsgrunnlagTotal,
                skjønnsmessigFastsattÅrlig = if (tilstand == FastsattEtterSkjønn) beregningsgrunnlag else null
            )
        }

        override fun preVisitSammenligningsgrunnlag(
            sammenligningsgrunnlag1: Sammenligningsgrunnlag,
            sammenligningsgrunnlag: Inntekt
        ) {
            this.sammenligningsgrunnlagTotal = InntektBuilder(sammenligningsgrunnlag).build().årlig
        }

        override fun preVisitSykepengegrunnlag(
            sykepengegrunnlag1: Sykepengegrunnlag,
            skjæringstidspunkt: LocalDate,
            sykepengegrunnlag: Inntekt,
            avviksprosent: Avviksprosent,
            totalOmregnetÅrsinntekt: Inntekt,
            beregningsgrunnlag: Inntekt,
            `6G`: Inntekt,
            begrensning: Sykepengegrunnlag.Begrensning,
            vurdertInfotrygd: Boolean,
            minsteinntekt: Inntekt,
            oppfyllerMinsteinntektskrav: Boolean,
            tilstand: Sykepengegrunnlag.Tilstand
        ) {
            this.sykepengegrunnlag = InntektBuilder(sykepengegrunnlag).build()
            this.oppfyllerMinsteinntektskrav = oppfyllerMinsteinntektskrav
            this.beregningsgrunnlag = InntektBuilder(beregningsgrunnlag).build().årlig
            this.omregnetÅrsinntekt = InntektBuilder(totalOmregnetÅrsinntekt).build().årlig
            this.`6G` = `6G`
            this.avviksprosent = avviksprosent.prosent()
            this.tilstand = tilstand
        }

        private var deaktivert = false
        override fun preVisitDeaktiverteArbeidsgiverInntektsopplysninger(arbeidsgiverInntektopplysninger: List<ArbeidsgiverInntektsopplysning>) {
            deaktivert = true
        }

        override fun postVisitDeaktiverteArbeidsgiverInntektsopplysninger(arbeidsgiverInntektopplysninger: List<ArbeidsgiverInntektsopplysning>) {
            deaktivert = false
        }

        override fun preVisitArbeidsgiverInntektsopplysning(arbeidsgiverInntektsopplysning: ArbeidsgiverInntektsopplysning, orgnummer: String, gjelder: Periode) {
            val inntektsopplysningBuilder = InntektsopplysningBuilder(
                organisasjonsnummer = orgnummer,
                inntektsopplysning = arbeidsgiverInntektsopplysning,
                deaktivert = deaktivert
            )
            leggTilInntekt(inntektsopplysningBuilder.build())
            refusjonsopplysningerPerArbeidsgiver.add(
                inntektsopplysningBuilder.buildRefusjonsopplysninger()
            )
        }

        override fun preVisitArbeidsgiverInntektsopplysningForSammenligningsgrunnlag(
            arbeidsgiverInntektsopplysning: ArbeidsgiverInntektsopplysningForSammenligningsgrunnlag,
            orgnummer: String,
            rapportertInntekt: Inntekt
        ) {
             leggTilInntekt(IArbeidsgiverinntekt(
                arbeidsgiver = orgnummer,
                omregnetÅrsinntekt = null,
                sammenligningsgrunnlag = InntektBuilder(rapportertInntekt).build().årlig,
                skjønnsmessigFastsatt = null,
                 deaktivert = false
            ))
        }

        private fun leggTilInntekt(inntekt: IArbeidsgiverinntekt) {
            val eksisterende = inntekterPerArbeidsgiver.firstOrNull { it.arbeidsgiver == inntekt.arbeidsgiver }
            if (eksisterende == null) {
                inntekterPerArbeidsgiver.add(inntekt)
                return
            }
            val ny = eksisterende.copy(
                omregnetÅrsinntekt = eksisterende.omregnetÅrsinntekt ?: inntekt.omregnetÅrsinntekt,
                sammenligningsgrunnlag = eksisterende.sammenligningsgrunnlag ?: inntekt.sammenligningsgrunnlag,
                deaktivert = eksisterende.deaktivert || inntekt.deaktivert
            )
            inntekterPerArbeidsgiver.remove(eksisterende)
            inntekterPerArbeidsgiver.add(ny)
        }
    }

    private class InntektsopplysningBuilder(
        private val organisasjonsnummer: String,
        inntektsopplysning: ArbeidsgiverInntektsopplysning,
        private val deaktivert: Boolean,
    ) : VilkårsgrunnlagHistorikkVisitor {
        private lateinit var inntekt: IArbeidsgiverinntekt
        private val refusjonsopplysninger = mutableListOf<Refusjonselement>()
        private var tilstand: Tilstand = Tilstand.FangeInntekt

        init {
            inntektsopplysning.accept(this)
        }

        fun build() = inntekt

        fun buildRefusjonsopplysninger() = IArbeidsgiverrefusjon(organisasjonsnummer, refusjonsopplysninger.toList())

        override fun visitRefusjonsopplysning(
            meldingsreferanseId: UUID,
            fom: LocalDate,
            tom: LocalDate?,
            beløp: Inntekt
        ) {
            refusjonsopplysninger.add(
                Refusjonselement(
                    fom = fom,
                    tom = tom,
                    beløp = beløp.reflection { _, månedlig, _, _ -> månedlig },
                    meldingsreferanseId = meldingsreferanseId
                )
            )
        }

        private fun nyArbeidsgiverInntekt(
            kilde: IInntektkilde,
            inntekt: IInntekt,
            inntekterFraAOrdningen: List<IInntekterFraAOrdningen>? = null,
        ) = IArbeidsgiverinntekt(
            organisasjonsnummer,
            IOmregnetÅrsinntekt(kilde, inntekt.årlig, inntekt.månedlig, inntekterFraAOrdningen),
            sammenligningsgrunnlag = null,
            deaktivert = deaktivert,
            skjønnsmessigFastsatt = null
        )

        private fun nyArbeidsgiverInntektSkjønnsmessigfastsatt(
            omregnetÅrsinntektKilde: IInntektkilde,
            skjønnsmessigFastsattInntekt: IInntekt,
            omregnetÅrsinntekt: IInntekt,
            inntekterFraAOrdningen: List<IInntekterFraAOrdningen>? = null,
        ) = IArbeidsgiverinntekt(
            organisasjonsnummer,
            omregnetÅrsinntekt = IOmregnetÅrsinntekt(omregnetÅrsinntektKilde, omregnetÅrsinntekt.årlig, omregnetÅrsinntekt.månedlig, inntekterFraAOrdningen),
            sammenligningsgrunnlag = null,
            deaktivert = deaktivert,
            skjønnsmessigFastsatt = IOmregnetÅrsinntekt(IInntektkilde.SkjønnsmessigFastsatt, skjønnsmessigFastsattInntekt.årlig, skjønnsmessigFastsattInntekt.månedlig, inntekterFraAOrdningen)
        )

        override fun visitInfotrygd(infotrygd: Infotrygd, id: UUID, dato: LocalDate, hendelseId: UUID, beløp: Inntekt, tidsstempel: LocalDateTime) {
            val inntekt = InntektBuilder(beløp).build()
            this.tilstand.lagreInntekt(this, nyArbeidsgiverInntekt(IInntektkilde.Infotrygd, inntekt))
        }

        override fun preVisitSkjønnsmessigFastsatt(
            saksbehandler: SkjønnsmessigFastsatt,
            id: UUID,
            dato: LocalDate,
            hendelseId: UUID,
            beløp: Inntekt,
            tidsstempel: LocalDateTime
        ) {
            val skjønnsmessigFastsattInntekt = InntektBuilder(beløp).build()
            val foregående = saksbehandler.omregnetÅrsinntekt()
            val omregnetÅrsinntekt = InntektBuilder(foregående.fastsattÅrsinntekt()).build()
            val omregnetÅrsinntektKilde = when (foregående) {
                is Saksbehandler -> IInntektkilde.Saksbehandler
                is Infotrygd -> IInntektkilde.Infotrygd
                is SkattSykepengegrunnlag -> IInntektkilde.AOrdningen
                is IkkeRapportert -> IInntektkilde.IkkeRapportert
                else -> IInntektkilde.Inntektsmelding
            }
            this.tilstand.lagreInntekt(this, nyArbeidsgiverInntektSkjønnsmessigfastsatt(
                omregnetÅrsinntektKilde,
                skjønnsmessigFastsattInntekt,
                omregnetÅrsinntekt
            ))
        }

        override fun preVisitSaksbehandler(
            saksbehandler: Saksbehandler,
            id: UUID,
            dato: LocalDate,
            hendelseId: UUID,
            beløp: Inntekt,
            forklaring: String?,
            subsumsjon: Subsumsjon?,
            tidsstempel: LocalDateTime
        ) {
            val inntekt = InntektBuilder(beløp).build()
            this.tilstand.lagreInntekt(this, nyArbeidsgiverInntekt(IInntektkilde.Saksbehandler, inntekt))
        }

        override fun visitInntektsmelding(
            inntektsmelding: Inntektsmelding,
            id: UUID,
            dato: LocalDate,
            hendelseId: UUID,
            beløp: Inntekt,
            tidsstempel: LocalDateTime
        ) {
            val inntekt = InntektBuilder(beløp).build()
            this.tilstand.lagreInntekt(this, nyArbeidsgiverInntekt(IInntektkilde.Inntektsmelding, inntekt))
        }

        override fun visitIkkeRapportert(
            ikkeRapportert: IkkeRapportert,
            id: UUID,
            hendelseId: UUID,
            dato: LocalDate,
            tidsstempel: LocalDateTime
        ) {
            val inntekt = IInntekt(0.0, 0.0, 0.0)
            this.tilstand.lagreInntekt(this, nyArbeidsgiverInntekt(IInntektkilde.IkkeRapportert, inntekt))
        }

        override fun preVisitSkattSykepengegrunnlag(
            skattSykepengegrunnlag: SkattSykepengegrunnlag,
            id: UUID,
            hendelseId: UUID,
            dato: LocalDate,
            beløp: Inntekt,
            tidsstempel: LocalDateTime
        ) {
            val (inntekt, inntekterFraAOrdningen) = SkattBuilder(skattSykepengegrunnlag).build()
            this.tilstand.lagreInntekt(this, nyArbeidsgiverInntekt(IInntektkilde.AOrdningen, inntekt, inntekterFraAOrdningen))
        }

        private sealed interface Tilstand {
            fun lagreInntekt(builder: InntektsopplysningBuilder, inntekt: IArbeidsgiverinntekt)
            object FangeInntekt : Tilstand {
                override fun lagreInntekt(builder: InntektsopplysningBuilder, inntekt: IArbeidsgiverinntekt) {
                    builder.inntekt = inntekt
                    builder.tilstand = HarFangetInntekt
                }
            }
            object HarFangetInntekt : Tilstand {
                override fun lagreInntekt(builder: InntektsopplysningBuilder, inntekt: IArbeidsgiverinntekt) {}
            }
        }

        class SkattBuilder(skattComposite: SkattSykepengegrunnlag) : InntektsopplysningVisitor {
            private val inntekt = InntektBuilder(skattComposite.fastsattÅrsinntekt()).build()
            private val inntekterFraAOrdningen = mutableMapOf<YearMonth, Double>()

            init {
                skattComposite.accept(this)
            }

            fun build() = inntekt to inntekterFraAOrdningen.map { (måned, sum) -> IInntekterFraAOrdningen(måned, sum) }

            override fun visitSkatteopplysning(
                skatteopplysning: Skatteopplysning,
                hendelseId: UUID,
                beløp: Inntekt,
                måned: YearMonth,
                type: Skatteopplysning.Inntekttype,
                fordel: String,
                beskrivelse: String,
                tidsstempel: LocalDateTime
            ) {
                val inntekt = InntektBuilder(beløp).build()
                inntekterFraAOrdningen.merge(måned, inntekt.månedlig, Double::plus)
            }
        }
    }
}
