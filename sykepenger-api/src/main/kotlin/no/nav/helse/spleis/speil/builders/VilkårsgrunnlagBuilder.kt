package no.nav.helse.spleis.speil.builders

import java.time.LocalDate
import java.util.LinkedList
import java.util.UUID
import no.nav.helse.Grunnbeløp
import no.nav.helse.dto.InntektDto
import no.nav.helse.dto.MedlemskapsvurderingDto
import no.nav.helse.dto.serialisering.ArbeidsgiverInntektsopplysningUtDto
import no.nav.helse.dto.serialisering.InntektsopplysningUtDto
import no.nav.helse.dto.serialisering.SykepengegrunnlagUtDto
import no.nav.helse.dto.serialisering.VilkårsgrunnlagUtDto
import no.nav.helse.dto.serialisering.VilkårsgrunnlaghistorikkUtDto
import no.nav.helse.spleis.speil.dto.GhostPeriodeDTO
import no.nav.helse.spleis.speil.dto.InfotrygdVilkårsgrunnlag
import no.nav.helse.spleis.speil.dto.Refusjonselement
import no.nav.helse.spleis.speil.dto.SkjønnsmessigFastsattDTO
import no.nav.helse.spleis.speil.dto.SpleisVilkårsgrunnlag
import no.nav.helse.spleis.speil.dto.Vilkårsgrunnlag
import no.nav.helse.økonomi.Inntekt.Companion.årlig

internal abstract class IVilkårsgrunnlag(
    val skjæringstidspunkt: LocalDate,
    val beregningsgrunnlag: Double,
    val sykepengegrunnlag: Double,
    val inntekter: List<IArbeidsgiverinntekt>,
    val refusjonsopplysningerPerArbeidsgiver: List<IArbeidsgiverrefusjon>,
    val id: UUID
) {
    abstract fun toDTO(): Vilkårsgrunnlag
    fun inngårIkkeISammenligningsgrunnlag(organisasjonsnummer: String) = inntekter.none { it.arbeidsgiver == organisasjonsnummer }
    open fun potensiellGhostperiode(
        organisasjonsnummer: String,
        sykefraværstilfeller: Map<LocalDate, List<ClosedRange<LocalDate>>>
    ): GhostPeriodeDTO? {
        if (inntekter.size < 2 || this.skjæringstidspunkt !in sykefraværstilfeller) return null
        val inntekten = inntekter.firstOrNull { it.arbeidsgiver == organisasjonsnummer }
        if (inntekten == null) return null
        val sisteDag = minOf(inntekten.omregnetÅrsinntekt.tom, sykefraværstilfeller.getValue(skjæringstidspunkt).maxOf { it.endInclusive })
        return GhostPeriodeDTO(
            id = UUID.randomUUID(),
            fom = inntekten.omregnetÅrsinntekt.fom,
            tom = sisteDag,
            skjæringstidspunkt = skjæringstidspunkt,
            vilkårsgrunnlagId = this.id,
            deaktivert = inntekten.deaktivert
        )
    }

}

internal class ISpleisGrunnlag(
    skjæringstidspunkt: LocalDate,
    beregningsgrunnlag: Double,
    inntekter: List<IArbeidsgiverinntekt>,
    sykepengegrunnlag: Double,
    id: UUID,
    refusjonsopplysningerPerArbeidsgiver: List<IArbeidsgiverrefusjon>,
    val overstyringer: Set<UUID>,
    val omregnetÅrsinntekt: Double,
    val grunnbeløp: Int,
    val sykepengegrunnlagsgrense: SykepengegrunnlagsgrenseDTO,
    val meldingsreferanseId: UUID?,
    val antallOpptjeningsdagerErMinst: Int,
    val oppfyllerKravOmMinstelønn: Boolean,
    val oppfyllerKravOmOpptjening: Boolean,
    val oppfyllerKravOmMedlemskap: Boolean?
) : IVilkårsgrunnlag(skjæringstidspunkt, beregningsgrunnlag, sykepengegrunnlag, inntekter, refusjonsopplysningerPerArbeidsgiver, id) {

    override fun toDTO(): Vilkårsgrunnlag {
        return SpleisVilkårsgrunnlag(
            skjæringstidspunkt = skjæringstidspunkt,
            beregningsgrunnlag = beregningsgrunnlag,
            omregnetÅrsinntekt = omregnetÅrsinntekt,
            sykepengegrunnlag = sykepengegrunnlag,
            inntekter = inntekter.map { it.toDTO() },
            arbeidsgiverrefusjoner = refusjonsopplysningerPerArbeidsgiver.map { it.toDTO() },
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
        fun fra6GBegrensning(`6G`: InntektDto): SykepengegrunnlagsgrenseDTO {
            val `1G` = `6G`.årlig.beløp / 6
            return SykepengegrunnlagsgrenseDTO(
                grunnbeløp = `1G`.toInt(),
                grense = `6G`.årlig.beløp.toInt(),
                virkningstidspunkt = Grunnbeløp.virkningstidspunktFor(`1G`.årlig)
            )
        }
    }
}

internal class IInfotrygdGrunnlag(
    skjæringstidspunkt: LocalDate,
    beregningsgrunnlag: Double,
    inntekter: List<IArbeidsgiverinntekt>,
    refusjonsopplysningerPerArbeidsgiver: List<IArbeidsgiverrefusjon>,
    sykepengegrunnlag: Double,
    id: UUID
) : IVilkårsgrunnlag(skjæringstidspunkt, beregningsgrunnlag, sykepengegrunnlag, inntekter, refusjonsopplysningerPerArbeidsgiver, id) {

    override fun toDTO(): Vilkårsgrunnlag {
        return InfotrygdVilkårsgrunnlag(
            skjæringstidspunkt = skjæringstidspunkt,
            beregningsgrunnlag = beregningsgrunnlag,
            sykepengegrunnlag = sykepengegrunnlag,
            inntekter = inntekter.map { it.toDTO() },
            arbeidsgiverrefusjoner = refusjonsopplysningerPerArbeidsgiver.map { it.toDTO() },
        )
    }

    override fun potensiellGhostperiode(organisasjonsnummer: String, sykefraværstilfeller: Map<LocalDate, List<ClosedRange<LocalDate>>>) = null
}

internal class IVilkårsgrunnlagHistorikk(private val tilgjengeligeVilkårsgrunnlag: List<Map<UUID, IVilkårsgrunnlag>>) {
    private val vilkårsgrunnlagIBruk = mutableMapOf<UUID, IVilkårsgrunnlag>()

    internal fun inngårIkkeISammenligningsgrunnlag(organisasjonsnummer: String) =
        vilkårsgrunnlagIBruk.all { (_, a) -> a.inngårIkkeISammenligningsgrunnlag(organisasjonsnummer) }

    internal fun potensielleGhostsperioder(
        organisasjonsnummer: String,
        sykefraværstilfeller: Map<LocalDate, List<ClosedRange<LocalDate>>>
    ) =
        tilgjengeligeVilkårsgrunnlag.firstOrNull()?.mapNotNull { (_, vilkårsgrunnlag) ->
            vilkårsgrunnlag.potensiellGhostperiode(organisasjonsnummer, sykefraværstilfeller)
        } ?: emptyList()

    internal fun toDTO(): Map<UUID, Vilkårsgrunnlag> {
        return vilkårsgrunnlagIBruk.mapValues { (_, vilkårsgrunnlag) -> vilkårsgrunnlag.toDTO() }
    }

    internal fun leggIBøtta(vilkårsgrunnlagId: UUID): IVilkårsgrunnlag {
        return vilkårsgrunnlagIBruk.getOrPut(vilkårsgrunnlagId) {
            tilgjengeligeVilkårsgrunnlag.firstNotNullOf { elementer ->
                elementer[vilkårsgrunnlagId]
            }
        }
    }
}

internal class VilkårsgrunnlagBuilder(vilkårsgrunnlagHistorikk: VilkårsgrunnlaghistorikkUtDto) {
    private val inntekter = mutableMapOf<UUID, IOmregnetÅrsinntekt>()
    private val historikk = LinkedList<Map<UUID, IVilkårsgrunnlag>>()

    init {
        vilkårsgrunnlagHistorikk.historikk.asReversed().forEach {
            historikk.addFirst(it.vilkårsgrunnlag.associate {
                it.vilkårsgrunnlagId to when (it) {
                    is VilkårsgrunnlagUtDto.Infotrygd -> mapInfotrygd(it)
                    is VilkårsgrunnlagUtDto.Spleis -> mapSpleis(it)
                }
            })
        }
    }

    internal fun build() = IVilkårsgrunnlagHistorikk(historikk)

    private fun mapSpleis(grunnlagsdata: VilkårsgrunnlagUtDto.Spleis): IVilkårsgrunnlag {
        val oppfyllerKravOmMedlemskap = when (grunnlagsdata.medlemskapstatus) {
            MedlemskapsvurderingDto.Ja -> true
            MedlemskapsvurderingDto.Nei -> false
            MedlemskapsvurderingDto.UavklartMedBrukerspørsmål -> null
            MedlemskapsvurderingDto.VetIkke -> null
        }

        val begrensning = SykepengegrunnlagsgrenseDTO.fra6GBegrensning(grunnlagsdata.sykepengegrunnlag.`6G`)
        val overstyringer = grunnlagsdata.sykepengegrunnlag.arbeidsgiverInntektsopplysninger.mapNotNull {
            when (it.inntektsopplysning) {
                is InntektsopplysningUtDto.IkkeRapportertDto -> null
                is InntektsopplysningUtDto.InfotrygdDto -> null
                is InntektsopplysningUtDto.InntektsmeldingDto -> null
                is InntektsopplysningUtDto.SaksbehandlerDto -> it.inntektsopplysning.hendelseId
                is InntektsopplysningUtDto.SkattSykepengegrunnlagDto -> null
                is InntektsopplysningUtDto.SkjønnsmessigFastsattDto -> it.inntektsopplysning.hendelseId
            }
        }.toSet()

        return ISpleisGrunnlag(
            skjæringstidspunkt = grunnlagsdata.skjæringstidspunkt,
            overstyringer = overstyringer,
            beregningsgrunnlag = grunnlagsdata.sykepengegrunnlag.beregningsgrunnlag.årlig.beløp,
            omregnetÅrsinntekt = grunnlagsdata.sykepengegrunnlag.totalOmregnetÅrsinntekt.årlig.beløp,
            inntekter = inntekter(grunnlagsdata.sykepengegrunnlag),
            refusjonsopplysningerPerArbeidsgiver = refusjonsopplysninger(grunnlagsdata.sykepengegrunnlag),
            sykepengegrunnlag = grunnlagsdata.sykepengegrunnlag.sykepengegrunnlag.årlig.beløp,
            grunnbeløp = begrensning.grunnbeløp,
            sykepengegrunnlagsgrense = begrensning,
            meldingsreferanseId = grunnlagsdata.meldingsreferanseId,
            antallOpptjeningsdagerErMinst = grunnlagsdata.opptjening.opptjeningsdager,
            oppfyllerKravOmMinstelønn = grunnlagsdata.sykepengegrunnlag.oppfyllerMinsteinntektskrav,
            oppfyllerKravOmOpptjening = grunnlagsdata.opptjening.erOppfylt,
            oppfyllerKravOmMedlemskap = oppfyllerKravOmMedlemskap,
            id = grunnlagsdata.vilkårsgrunnlagId
        )
    }

    private fun inntekter(dto: SykepengegrunnlagUtDto): List<IArbeidsgiverinntekt> {
        return dto.arbeidsgiverInntektsopplysninger.map { mapInntekt(it) } + dto.deaktiverteArbeidsforhold.map { mapInntekt(it, true) }
    }

    private fun mapInntekt(dto: ArbeidsgiverInntektsopplysningUtDto, deaktivert: Boolean = false): IArbeidsgiverinntekt {
        return mapInntekt(dto.orgnummer, dto.gjelder.fom, dto.gjelder.tom, dto.inntektsopplysning, deaktivert)
    }

    private fun mapInntekt(orgnummer: String, fom: LocalDate, tom: LocalDate, io: InntektsopplysningUtDto, deaktivert: Boolean): IArbeidsgiverinntekt {
        val omregnetÅrsinntekt = when (io) {
            is InntektsopplysningUtDto.IkkeRapportertDto -> IOmregnetÅrsinntekt(IInntektkilde.IkkeRapportert, fom, tom, 0.0, 0.0, null)
            is InntektsopplysningUtDto.InfotrygdDto -> IOmregnetÅrsinntekt(IInntektkilde.Infotrygd, fom, tom, io.beløp.årlig.beløp, io.beløp.månedligDouble.beløp, null)
            is InntektsopplysningUtDto.InntektsmeldingDto -> IOmregnetÅrsinntekt(IInntektkilde.Inntektsmelding, fom, tom, io.beløp.årlig.beløp, io.beløp.månedligDouble.beløp, null)
            is InntektsopplysningUtDto.SaksbehandlerDto -> IOmregnetÅrsinntekt(IInntektkilde.Saksbehandler, fom, tom, io.beløp.årlig.beløp, io.beløp.månedligDouble.beløp, null)
            is InntektsopplysningUtDto.SkattSykepengegrunnlagDto -> IOmregnetÅrsinntekt(
                IInntektkilde.AOrdningen, fom, tom, io.beløp.årlig.beløp, io.beløp.månedligDouble.beløp, io.inntektsopplysninger
                .groupBy { it.måned }
                .mapValues { (_, verdier) -> verdier.sumOf { it.beløp.beløp } }
                .map { (måned, månedligSum) ->
                    IInntekterFraAOrdningen(
                        måned = måned,
                        sum = månedligSum
                    )
                }
            )
            is InntektsopplysningUtDto.SkjønnsmessigFastsattDto -> inntekter.getValue(io.overstyrtInntekt)
        }.also {
            inntekter[io.id] = it
        }
        return IArbeidsgiverinntekt(
            arbeidsgiver = orgnummer,
            omregnetÅrsinntekt = omregnetÅrsinntekt,
            skjønnsmessigFastsatt = when (io) {
                is InntektsopplysningUtDto.SkjønnsmessigFastsattDto -> SkjønnsmessigFastsattDTO(
                    årlig = io.beløp.årlig.beløp,
                    månedlig = io.beløp.månedligDouble.beløp
                )
                else -> null
            },
            deaktivert = deaktivert
        )
    }

    private fun refusjonsopplysninger(dto: SykepengegrunnlagUtDto) =
        dto.arbeidsgiverInntektsopplysninger.map {
            IArbeidsgiverrefusjon(
                arbeidsgiver = it.orgnummer,
                refusjonsopplysninger = it.refusjonsopplysninger.opplysninger.map {
                    Refusjonselement(
                        fom = it.fom,
                        tom = it.tom,
                        beløp = it.beløp.månedligDouble.beløp,
                        meldingsreferanseId = it.meldingsreferanseId
                    )
                }
            )
        }

    private fun mapInfotrygd(infotrygdVilkårsgrunnlag: VilkårsgrunnlagUtDto.Infotrygd): IVilkårsgrunnlag {
        return IInfotrygdGrunnlag(
            skjæringstidspunkt = infotrygdVilkårsgrunnlag.skjæringstidspunkt,
            beregningsgrunnlag = infotrygdVilkårsgrunnlag.sykepengegrunnlag.beregningsgrunnlag.årlig.beløp,
            inntekter = inntekter(infotrygdVilkårsgrunnlag.sykepengegrunnlag),
            refusjonsopplysningerPerArbeidsgiver = refusjonsopplysninger(infotrygdVilkårsgrunnlag.sykepengegrunnlag),
            sykepengegrunnlag = infotrygdVilkårsgrunnlag.sykepengegrunnlag.sykepengegrunnlag.årlig.beløp,
            id = infotrygdVilkårsgrunnlag.vilkårsgrunnlagId
        )
    }
}
