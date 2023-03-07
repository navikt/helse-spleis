package no.nav.helse.serde.api.speil.builders

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.Grunnbeløp
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.Subsumsjon
import no.nav.helse.person.inntekt.InntektsopplysningVisitor
import no.nav.helse.person.Opptjening
import no.nav.helse.person.SammenligningsgrunnlagVisitor
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.person.VilkårsgrunnlagHistorikk.Grunnlagsdata
import no.nav.helse.person.VilkårsgrunnlagHistorikk.InfotrygdVilkårsgrunnlag
import no.nav.helse.person.VilkårsgrunnlagHistorikkVisitor
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysningForSammenligningsgrunnlag
import no.nav.helse.person.inntekt.Infotrygd
import no.nav.helse.person.inntekt.Inntektsmelding
import no.nav.helse.person.inntekt.Saksbehandler
import no.nav.helse.person.inntekt.Sammenligningsgrunnlag
import no.nav.helse.person.inntekt.SkattSykepengegrunnlag
import no.nav.helse.person.inntekt.Skatteopplysning
import no.nav.helse.person.inntekt.Sykepengegrunnlag
import no.nav.helse.serde.api.dto.GhostPeriodeDTO
import no.nav.helse.serde.api.dto.Refusjonselement
import no.nav.helse.serde.api.dto.SpleisVilkårsgrunnlag
import no.nav.helse.serde.api.dto.Vilkårsgrunnlag
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosent
import kotlin.properties.Delegates

private typealias VilkårsgrunnlagHistorikkId = UUID

internal class IInnslag(
    private val innslag: Map<LocalDate, IVilkårsgrunnlag>
) {
    internal fun toDTO() = innslag.mapValues { (_, vilkårsgrunnlag) -> vilkårsgrunnlag.toDTO() }
    internal fun finn(skjæringstidspunkt: LocalDate) = innslag[skjæringstidspunkt]?.toDTO()
    internal fun finn(id: UUID) = innslag.entries.first { (_, vilkårsgrunnlag) -> vilkårsgrunnlag.id == id }.value.toDTO()
    internal fun inngårIkkeISammenligningsgrunnlag(organisasjonsnummer: String) =
        innslag.all { (_, vilkårsgrunnlag) -> vilkårsgrunnlag.inngårIkkeISammenligningsgrunnlag(organisasjonsnummer) }

    internal fun potensielleGhostsperioder(
        organisasjonsnummer: String,
        nyesteInnslagId: UUID?,
        sykefraværstilfeller: Map<LocalDate, List<ClosedRange<LocalDate>>>
    ) = innslag
        .mapNotNull { (_, innslag) -> innslag.potensiellGhostperiode(organisasjonsnummer, nyesteInnslagId!!, sykefraværstilfeller) }
}

internal class ISykepengegrunnlag(
    val inntekterPerArbeidsgiver: List<IArbeidsgiverinntekt>,
    val sykepengegrunnlag: Double,
    val oppfyllerMinsteinntektskrav: Boolean,
    val omregnetÅrsinntekt: Double,
    val begrensning: SykepengegrunnlagsgrenseDTO,
    val refusjonsopplysningerPerArbeidsgiver: List<IArbeidsgiverrefusjon>
)

internal class IInntekt(
    val årlig: Double,
    val månedlig: Double,
    val daglig: Double
)

internal interface IVilkårsgrunnlag {
    val skjæringstidspunkt: LocalDate
    val omregnetÅrsinntekt: Double
    val sammenligningsgrunnlag: Double?
    val sykepengegrunnlag: Double
    val inntekter: List<IArbeidsgiverinntekt>
    val refusjonsopplysningerPerArbeidsgiver: List<IArbeidsgiverrefusjon>
    val id: UUID
    fun toDTO(): Vilkårsgrunnlag
    fun inngårIkkeISammenligningsgrunnlag(organisasjonsnummer: String) = inntekter.none { it.arbeidsgiver == organisasjonsnummer }
    fun potensiellGhostperiode(
        organisasjonsnummer: String,
        innslagId: UUID,
        sykefraværstilfeller: Map<LocalDate, List<ClosedRange<LocalDate>>>
    ): GhostPeriodeDTO? {
        if (this.skjæringstidspunkt !in sykefraværstilfeller) return null
        val inntekten = inntekter.firstOrNull { it.arbeidsgiver == organisasjonsnummer }
        if (inntekten == null || inntekten.omregnetÅrsinntekt?.kilde == null) return null
        return GhostPeriodeDTO(
            id = UUID.randomUUID(),
            fom = skjæringstidspunkt,
            tom = sykefraværstilfeller.getValue(skjæringstidspunkt).maxOf { it.endInclusive },
            skjæringstidspunkt = skjæringstidspunkt,
            vilkårsgrunnlagHistorikkInnslagId = innslagId,
            vilkårsgrunnlagId = this.id,
            deaktivert = inntekten.deaktivert
        )
    }
}

internal class ISpleisGrunnlag(
    override val skjæringstidspunkt: LocalDate,
    override val omregnetÅrsinntekt: Double,
    override val sammenligningsgrunnlag: Double,
    override val inntekter: List<IArbeidsgiverinntekt>,
    override val sykepengegrunnlag: Double,
    override val id: UUID,
    override val refusjonsopplysningerPerArbeidsgiver: List<IArbeidsgiverrefusjon>,
    val avviksprosent: Double?,
    val grunnbeløp: Int,
    val sykepengegrunnlagsgrense: SykepengegrunnlagsgrenseDTO,
    val meldingsreferanseId: UUID?,
    val antallOpptjeningsdagerErMinst: Int,
    val oppfyllerKravOmMinstelønn: Boolean,
    val oppfyllerKravOmOpptjening: Boolean,
    val oppfyllerKravOmMedlemskap: Boolean?
) : IVilkårsgrunnlag {
    override fun toDTO(): Vilkårsgrunnlag {
        return SpleisVilkårsgrunnlag(
            skjæringstidspunkt = skjæringstidspunkt,
            omregnetÅrsinntekt = omregnetÅrsinntekt,
            sammenligningsgrunnlag = sammenligningsgrunnlag,
            sykepengegrunnlag = sykepengegrunnlag,
            inntekter = inntekter.map { it.toDTO() },
            arbeidsgiverrefusjoner = refusjonsopplysningerPerArbeidsgiver.map { it.toDTO() },
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
    override val skjæringstidspunkt: LocalDate,
    override val omregnetÅrsinntekt: Double,
    override val sammenligningsgrunnlag: Double?,
    override val inntekter: List<IArbeidsgiverinntekt>,
    override val refusjonsopplysningerPerArbeidsgiver: List<IArbeidsgiverrefusjon>,
    override val sykepengegrunnlag: Double,
    override val id: UUID
) : IVilkårsgrunnlag {
    override fun toDTO(): Vilkårsgrunnlag {
        return no.nav.helse.serde.api.dto.InfotrygdVilkårsgrunnlag(
            skjæringstidspunkt = skjæringstidspunkt,
            omregnetÅrsinntekt = omregnetÅrsinntekt,
            sammenligningsgrunnlag = sammenligningsgrunnlag,
            sykepengegrunnlag = sykepengegrunnlag,
            inntekter = inntekter.map { it.toDTO() },
            arbeidsgiverrefusjoner = refusjonsopplysningerPerArbeidsgiver.map { it.toDTO() },
        )
    }

    override fun potensiellGhostperiode(
        organisasjonsnummer: String,
        innslagId: UUID,
        sykefraværstilfeller: Map<LocalDate, List<ClosedRange<LocalDate>>>
    ) = null
}

internal class InntektBuilder(private val inntekt: Inntekt) {
    internal fun build(): IInntekt {
        return inntekt.reflection { årlig, månedlig, daglig, _ ->
            IInntekt(årlig, månedlig, daglig)
        }
    }
}

internal class IVilkårsgrunnlagHistorikk {
    private var nyesteInnslagId: UUID? = null
    private val historikk = mutableMapOf<VilkårsgrunnlagHistorikkId, IInnslag>()
    private val nyesteInnslag get() = historikk[nyesteInnslagId]
    private val vilkårsgrunnlagIBruk = mutableMapOf<UUID, Vilkårsgrunnlag>()

    internal fun leggTil(vilkårsgrunnlagHistorikkId: UUID, innslag: IInnslag) {
        if (nyesteInnslagId == null) nyesteInnslagId = vilkårsgrunnlagHistorikkId
        historikk.putIfAbsent(vilkårsgrunnlagHistorikkId, innslag)
    }

    internal fun inngårIkkeISammenligningsgrunnlag(organisasjonsnummer: String) =
        nyesteInnslag?.inngårIkkeISammenligningsgrunnlag(organisasjonsnummer) ?: true

    internal fun toDTO() = historikk.mapValues { (_, innslag) -> innslag.toDTO() }.toMap()

    internal fun potensielleGhostsperioder(
        organisasjonsnummer: String,
        sykefraværstilfeller: Map<LocalDate, List<ClosedRange<LocalDate>>>
    ) =
        nyesteInnslag?.potensielleGhostsperioder(organisasjonsnummer, nyesteInnslagId, sykefraværstilfeller) ?: emptyList()

    internal fun vilkårsgrunnlagSomBlirPektPå(): Map<UUID, Vilkårsgrunnlag> {
        return vilkårsgrunnlagIBruk.toMap()
    }

    internal fun leggIBøtta(vilkårsgrunnlag: IVilkårsgrunnlag?) {
        if (vilkårsgrunnlag == null) return
        vilkårsgrunnlagIBruk[vilkårsgrunnlag.id] = vilkårsgrunnlag.toDTO()
    }
    internal fun leggIBøtta(innslagId: UUID?, vilkårsgrunnlagId: UUID?) {
        if (innslagId == null || vilkårsgrunnlagId == null) return
        vilkårsgrunnlagIBruk[vilkårsgrunnlagId] = historikk.getValue(innslagId).finn(vilkårsgrunnlagId)
    }

    internal fun finn(vilkårsgrunnlagHistorikkInnslagId: UUID?, vilkårsgrunnlagId: UUID?, skjæringstidspunkt: LocalDate,): Vilkårsgrunnlag? {
        if (vilkårsgrunnlagHistorikkInnslagId == null || vilkårsgrunnlagId == null) return null
        return historikk[vilkårsgrunnlagHistorikkInnslagId]?.finn(skjæringstidspunkt)
    }
}

internal class VilkårsgrunnlagBuilder(vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk) : VilkårsgrunnlagHistorikkVisitor {
    private val historikk = IVilkårsgrunnlagHistorikk()

    init {
        vilkårsgrunnlagHistorikk.accept(this)
    }

    internal fun build() = historikk

    override fun preVisitInnslag(innslag: VilkårsgrunnlagHistorikk.Innslag, id: UUID, opprettet: LocalDateTime) {
        historikk.leggTil(id, InnslagBuilder(innslag).build())
    }

    internal class InnslagBuilder(innslag: VilkårsgrunnlagHistorikk.Innslag) : VilkårsgrunnlagHistorikkVisitor {
        private val vilkårsgrunnlag = mutableMapOf<LocalDate, IVilkårsgrunnlag>()

        init {
            innslag.accept(this)
        }

        internal fun build() = IInnslag(vilkårsgrunnlag.toMap())

        override fun preVisitGrunnlagsdata(
            skjæringstidspunkt: LocalDate,
            grunnlagsdata: Grunnlagsdata,
            sykepengegrunnlag: Sykepengegrunnlag,
            sammenligningsgrunnlag: Sammenligningsgrunnlag,
            avviksprosent: Prosent?,
            opptjening: Opptjening,
            vurdertOk: Boolean,
            meldingsreferanseId: UUID?,
            vilkårsgrunnlagId: UUID,
            medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus
        ) {
            val sammenligningsgrunnlagBuilder = SammenligningsgrunnlagBuilder(sammenligningsgrunnlag)

            val compositeSykepengegrunnlag =
                SykepengegrunnlagBuilder(sykepengegrunnlag, sammenligningsgrunnlagBuilder).build()
            val oppfyllerKravOmMedlemskap = when (medlemskapstatus) {
                Medlemskapsvurdering.Medlemskapstatus.Ja -> true
                Medlemskapsvurdering.Medlemskapstatus.Nei -> false
                else -> null
            }

            vilkårsgrunnlag[skjæringstidspunkt] = ISpleisGrunnlag(
                skjæringstidspunkt = skjæringstidspunkt,
                omregnetÅrsinntekt = compositeSykepengegrunnlag.omregnetÅrsinntekt,
                sammenligningsgrunnlag = sammenligningsgrunnlagBuilder.total(),
                inntekter = compositeSykepengegrunnlag.inntekterPerArbeidsgiver,
                refusjonsopplysningerPerArbeidsgiver = compositeSykepengegrunnlag.refusjonsopplysningerPerArbeidsgiver,
                sykepengegrunnlag = compositeSykepengegrunnlag.sykepengegrunnlag,
                avviksprosent = avviksprosent?.prosent(),
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

        override fun preVisitInfotrygdVilkårsgrunnlag(
            infotrygdVilkårsgrunnlag: InfotrygdVilkårsgrunnlag,
            skjæringstidspunkt: LocalDate,
            sykepengegrunnlag: Sykepengegrunnlag,
            vilkårsgrunnlagId: UUID
        ) {
            val byggetSykepengegrunnlag = SykepengegrunnlagBuilder(sykepengegrunnlag, null /* vi har ikke noe sammenligningsgrunnlag for Infotrygd-saker */)
                .build()
            vilkårsgrunnlag[skjæringstidspunkt] = IInfotrygdGrunnlag(
                skjæringstidspunkt = skjæringstidspunkt,
                omregnetÅrsinntekt = byggetSykepengegrunnlag.omregnetÅrsinntekt,
                sammenligningsgrunnlag = null,
                inntekter = byggetSykepengegrunnlag.inntekterPerArbeidsgiver,
                refusjonsopplysningerPerArbeidsgiver = byggetSykepengegrunnlag.refusjonsopplysningerPerArbeidsgiver,
                sykepengegrunnlag = byggetSykepengegrunnlag.sykepengegrunnlag,
                id = vilkårsgrunnlagId
            )
        }

        internal class SammenligningsgrunnlagBuilder(sammenligningsgrunnlag: Sammenligningsgrunnlag) : SammenligningsgrunnlagVisitor {
            private lateinit var total: Inntekt
            private val beløp = mutableMapOf<String, Double>()

            init {
                sammenligningsgrunnlag.accept(this)
            }

            internal fun total() = InntektBuilder(total).build().årlig
            internal fun totalFor(orgnummer: String) = beløp[orgnummer]

            internal fun inntekter() = beløp.map { (orgnummer, beløp) ->
                IArbeidsgiverinntekt(
                    arbeidsgiver = orgnummer,
                    omregnetÅrsinntekt = null,
                    sammenligningsgrunnlag = beløp,
                    deaktivert = false
                )
            }

            override fun preVisitSammenligningsgrunnlag(
                sammenligningsgrunnlag1: Sammenligningsgrunnlag,
                sammenligningsgrunnlag: Inntekt
            ) {
                this.total = sammenligningsgrunnlag
            }

            override fun preVisitArbeidsgiverInntektsopplysningForSammenligningsgrunnlag(
                arbeidsgiverInntektsopplysning: ArbeidsgiverInntektsopplysningForSammenligningsgrunnlag,
                orgnummer: String,
                rapportertInntekt: Inntekt
            ) {
                beløp[orgnummer] = InntektBuilder(rapportertInntekt).build().årlig
            }
        }

        internal class SykepengegrunnlagBuilder(
            sykepengegrunnlag: Sykepengegrunnlag,
            private val sammenligningsgrunnlagBuilder: SammenligningsgrunnlagBuilder?
        ) : VilkårsgrunnlagHistorikkVisitor {
            private lateinit var `6G`: Inntekt
            private val inntekterPerArbeidsgiver = mutableListOf<IArbeidsgiverinntekt>()
            private val refusjonsopplysningerPerArbeidsgiver = mutableListOf<IArbeidsgiverrefusjon>()
            private lateinit var sykepengegrunnlag: IInntekt
            private var omregnetÅrsinntekt by Delegates.notNull<Double>()
            private var oppfyllerMinsteinntektskrav by Delegates.notNull<Boolean>()

            init {
                sykepengegrunnlag.accept(this)
            }

            fun build(): ISykepengegrunnlag {
                return ISykepengegrunnlag(
                    inntekterPerArbeidsgiver = inntekterPerArbeidsgiver.toList() + arbeidsgivereUtenSykepengegrunnlag(),
                    sykepengegrunnlag = sykepengegrunnlag.årlig,
                    oppfyllerMinsteinntektskrav = oppfyllerMinsteinntektskrav,
                    omregnetÅrsinntekt = omregnetÅrsinntekt,
                    begrensning = SykepengegrunnlagsgrenseDTO.fra6GBegrensning(this.`6G`),
                    refusjonsopplysningerPerArbeidsgiver = refusjonsopplysningerPerArbeidsgiver.toList()
                )
            }

            private fun arbeidsgivereUtenSykepengegrunnlag() = sammenligningsgrunnlagBuilder?.inntekter()?.filterNot { inntekt ->
                inntekterPerArbeidsgiver.any { other -> other.arbeidsgiver == inntekt.arbeidsgiver }
            } ?: emptyList()

            override fun preVisitSykepengegrunnlag(
                sykepengegrunnlag1: Sykepengegrunnlag,
                skjæringstidspunkt: LocalDate,
                sykepengegrunnlag: Inntekt,
                skjønnsmessigFastsattÅrsinntekt: Inntekt?,
                beregningsgrunnlag: Inntekt,
                `6G`: Inntekt,
                begrensning: Sykepengegrunnlag.Begrensning,
                vurdertInfotrygd: Boolean,
                minsteinntekt: Inntekt,
                oppfyllerMinsteinntektskrav: Boolean
            ) {
                this.sykepengegrunnlag = InntektBuilder(sykepengegrunnlag).build()
                this.oppfyllerMinsteinntektskrav = oppfyllerMinsteinntektskrav
                this.omregnetÅrsinntekt = InntektBuilder(beregningsgrunnlag).build().årlig
                this.`6G` = `6G`
            }

            private var deaktivert = false
            override fun preVisitDeaktiverteArbeidsgiverInntektsopplysninger(arbeidsgiverInntektopplysninger: List<ArbeidsgiverInntektsopplysning>) {
                deaktivert = true
            }

            override fun postVisitDeaktiverteArbeidsgiverInntektsopplysninger(arbeidsgiverInntektopplysninger: List<ArbeidsgiverInntektsopplysning>) {
                deaktivert = false
            }

            override fun preVisitArbeidsgiverInntektsopplysning(arbeidsgiverInntektsopplysning: ArbeidsgiverInntektsopplysning, orgnummer: String) {
                val inntektsopplysningBuilder = InntektsopplysningBuilder(
                    organisasjonsnummer = orgnummer,
                    inntektsopplysning = arbeidsgiverInntektsopplysning,
                    sammenligningsgrunnlagBuilder = sammenligningsgrunnlagBuilder,
                    deaktivert = deaktivert
                )
                inntekterPerArbeidsgiver.add(inntektsopplysningBuilder.build())
                refusjonsopplysningerPerArbeidsgiver.add(
                    inntektsopplysningBuilder.buildRefusjonsopplysninger()
                )
            }
        }


        private class InntektsopplysningBuilder(
            private val organisasjonsnummer: String,
            inntektsopplysning: ArbeidsgiverInntektsopplysning,
            private val sammenligningsgrunnlagBuilder: SammenligningsgrunnlagBuilder?,
            private val deaktivert: Boolean,
        ) : VilkårsgrunnlagHistorikkVisitor {
            private lateinit var inntekt: IArbeidsgiverinntekt
            private val refusjonsopplysninger = mutableListOf<Refusjonselement>()

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
                sammenligningsgrunnlag = sammenligningsgrunnlagBuilder?.totalFor(organisasjonsnummer),
                deaktivert = deaktivert
            )

            override fun visitInfotrygd(infotrygd: Infotrygd, id: UUID, dato: LocalDate, hendelseId: UUID, beløp: Inntekt, tidsstempel: LocalDateTime) {
                val inntekt = InntektBuilder(beløp).build()
                this.inntekt = nyArbeidsgiverInntekt(IInntektkilde.Infotrygd, inntekt)
            }

            override fun visitSaksbehandler(
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
                this.inntekt = nyArbeidsgiverInntekt(IInntektkilde.Saksbehandler, inntekt)
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
                this.inntekt = nyArbeidsgiverInntekt(IInntektkilde.Inntektsmelding, inntekt)
            }

            override fun visitIkkeRapportert(id: UUID, dato: LocalDate, tidsstempel: LocalDateTime) {
                val inntekt = IInntekt(0.0, 0.0, 0.0)
                this.inntekt = nyArbeidsgiverInntekt(IInntektkilde.IkkeRapportert, inntekt)
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
                this.inntekt = nyArbeidsgiverInntekt(IInntektkilde.AOrdningen, inntekt, inntekterFraAOrdningen)
            }

            class SkattBuilder(skattComposite: SkattSykepengegrunnlag) : InntektsopplysningVisitor {
                private val inntekt = InntektBuilder(skattComposite.omregnetÅrsinntekt()).build()
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
}
