package no.nav.helse.serde

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import java.time.YearMonth
import java.util.UUID
import kotlin.streams.asSequence
import no.nav.helse.dto.AlderDto
import no.nav.helse.dto.ArbeidsforholdDto
import no.nav.helse.dto.ArbeidsgiverOpptjeningsgrunnlagDto
import no.nav.helse.dto.ArbeidsgiverperiodeavklaringDto
import no.nav.helse.dto.ArbeidssituasjonDto
import no.nav.helse.dto.AvsenderDto
import no.nav.helse.dto.BegrunnelseDto
import no.nav.helse.dto.BehandlingkildeDto
import no.nav.helse.dto.BehandlingtilstandDto
import no.nav.helse.dto.BeløpstidslinjeDto
import no.nav.helse.dto.DokumentsporingDto
import no.nav.helse.dto.DokumenttypeDto
import no.nav.helse.dto.EndringskodeDto
import no.nav.helse.dto.FagområdeDto
import no.nav.helse.dto.FeriepengerendringskodeDto
import no.nav.helse.dto.FeriepengerfagområdeDto
import no.nav.helse.dto.FeriepengerklassekodeDto
import no.nav.helse.dto.HendelseskildeDto
import no.nav.helse.dto.InfotrygdFerieperiodeDto
import no.nav.helse.dto.InntektbeløpDto
import no.nav.helse.dto.InntektskildeDto
import no.nav.helse.dto.InntekttypeDto
import no.nav.helse.dto.KlassekodeDto
import no.nav.helse.dto.MaksdatobestemmelseDto
import no.nav.helse.dto.MeldingsreferanseDto
import no.nav.helse.dto.OppdragstatusDto
import no.nav.helse.dto.PeriodeDto
import no.nav.helse.dto.ProsentdelDto
import no.nav.helse.dto.RefusjonsservitørDto
import no.nav.helse.dto.SimuleringResultatDto
import no.nav.helse.dto.SkatteopplysningDto
import no.nav.helse.dto.SykdomshistorikkDto
import no.nav.helse.dto.SykdomshistorikkElementDto
import no.nav.helse.dto.SykdomstidslinjeDagDto
import no.nav.helse.dto.SykdomstidslinjeDagDto.AndreYtelserDto
import no.nav.helse.dto.SykdomstidslinjeDagDto.ArbeidIkkeGjenopptattDagDto
import no.nav.helse.dto.SykdomstidslinjeDagDto.ArbeidsdagDto
import no.nav.helse.dto.SykdomstidslinjeDagDto.ArbeidsgiverHelgedagDto
import no.nav.helse.dto.SykdomstidslinjeDagDto.ArbeidsgiverdagDto
import no.nav.helse.dto.SykdomstidslinjeDagDto.FeriedagDto
import no.nav.helse.dto.SykdomstidslinjeDagDto.ForeldetSykedagDto
import no.nav.helse.dto.SykdomstidslinjeDagDto.FriskHelgedagDto
import no.nav.helse.dto.SykdomstidslinjeDagDto.PermisjonsdagDto
import no.nav.helse.dto.SykdomstidslinjeDagDto.ProblemDagDto
import no.nav.helse.dto.SykdomstidslinjeDagDto.SykHelgedagDto
import no.nav.helse.dto.SykdomstidslinjeDagDto.SykedagDto
import no.nav.helse.dto.SykdomstidslinjeDagDto.UkjentDagDto
import no.nav.helse.dto.SykdomstidslinjeDto
import no.nav.helse.dto.SykmeldingsperioderDto
import no.nav.helse.dto.UtbetalingTilstandDto
import no.nav.helse.dto.UtbetalingVurderingDto
import no.nav.helse.dto.UtbetalingtypeDto
import no.nav.helse.dto.VedtaksperiodetilstandDto
import no.nav.helse.dto.deserialisering.ArbeidsgiverInnDto
import no.nav.helse.dto.deserialisering.ArbeidsgiverInntektsopplysningInnDto
import no.nav.helse.dto.deserialisering.ArbeidstakerFaktaavklartInntektInnDto
import no.nav.helse.dto.deserialisering.ArbeidstakerinntektskildeInnDto
import no.nav.helse.dto.deserialisering.ArbeidstakerinntektskildeInnDto.AOrdningenDto
import no.nav.helse.dto.deserialisering.BehandlingInnDto
import no.nav.helse.dto.deserialisering.BehandlingendringInnDto
import no.nav.helse.dto.deserialisering.BehandlingerInnDto
import no.nav.helse.dto.deserialisering.FeriepengeInnDto
import no.nav.helse.dto.deserialisering.FeriepengeoppdragInnDto
import no.nav.helse.dto.deserialisering.FeriepengeutbetalinggrunnlagInnDto
import no.nav.helse.dto.deserialisering.FeriepengeutbetalinggrunnlagInnDto.UtbetaltDagInnDto
import no.nav.helse.dto.deserialisering.FeriepengeutbetalingslinjeInnDto
import no.nav.helse.dto.deserialisering.ForberedendeVilkårsgrunnlagDto
import no.nav.helse.dto.deserialisering.ForkastetVedtaksperiodeInnDto
import no.nav.helse.dto.deserialisering.InfotrygdArbeidsgiverutbetalingsperiodeInnDto
import no.nav.helse.dto.deserialisering.InfotrygdPersonutbetalingsperiodeInnDto
import no.nav.helse.dto.deserialisering.InfotrygdhistorikkInnDto
import no.nav.helse.dto.deserialisering.InfotrygdhistorikkelementInnDto
import no.nav.helse.dto.deserialisering.InntektsdataInnDto
import no.nav.helse.dto.deserialisering.InntektsgrunnlagInnDto
import no.nav.helse.dto.deserialisering.InntektshistorikkInnDto
import no.nav.helse.dto.deserialisering.InntektsmeldingInnDto
import no.nav.helse.dto.deserialisering.MaksdatoresultatInnDto
import no.nav.helse.dto.deserialisering.MinimumSykdomsgradVurderingInnDto
import no.nav.helse.dto.deserialisering.OppdragInnDto
import no.nav.helse.dto.deserialisering.OpptjeningInnDto
import no.nav.helse.dto.deserialisering.PersonInnDto
import no.nav.helse.dto.deserialisering.SaksbehandlerInnDto
import no.nav.helse.dto.deserialisering.SelvstendigFaktaavklartInntektInnDto
import no.nav.helse.dto.deserialisering.SelvstendigInntektsopplysningInnDto
import no.nav.helse.dto.deserialisering.SkjønnsmessigFastsattInnDto
import no.nav.helse.dto.deserialisering.UtbetalingInnDto
import no.nav.helse.dto.deserialisering.UtbetalingsdagInnDto
import no.nav.helse.dto.deserialisering.UtbetalingsdagInnDto.*
import no.nav.helse.dto.deserialisering.UtbetalingslinjeInnDto
import no.nav.helse.dto.deserialisering.UtbetalingstidslinjeInnDto
import no.nav.helse.dto.deserialisering.VedtaksperiodeInnDto
import no.nav.helse.dto.deserialisering.VilkårsgrunnlagInnDto
import no.nav.helse.dto.deserialisering.VilkårsgrunnlagInnslagInnDto
import no.nav.helse.dto.deserialisering.VilkårsgrunnlaghistorikkInnDto
import no.nav.helse.dto.deserialisering.YrkesaktivitetstypeDto
import no.nav.helse.dto.deserialisering.ØkonomiInnDto
import no.nav.helse.serde.PersonData.ArbeidsgiverData.VedtaksperiodeData.BehandlingData.AvsenderData
import no.nav.helse.serde.mapping.JsonMedlemskapstatus

data class PersonData(
    val fødselsnummer: String,
    val fødselsdato: LocalDate,
    val arbeidsgivere: List<ArbeidsgiverData>,
    val opprettet: LocalDateTime,
    val infotrygdhistorikk: List<InfotrygdhistorikkElementData>,
    val vilkårsgrunnlagHistorikk: List<VilkårsgrunnlagInnslagData>,
    val minimumSykdomsgradVurdering: List<MinimumSykdomsgradVurderingPeriodeData>?,
    val dødsdato: LocalDate?
) {
    fun tilPersonDto() = PersonInnDto(
        fødselsnummer = this.fødselsnummer,
        alder = AlderDto(fødselsdato = this.fødselsdato, dødsdato = this.dødsdato),
        opprettet = this.opprettet,
        arbeidsgivere = this.arbeidsgivere.map { it.tilDto() },
        infotrygdhistorikk = InfotrygdhistorikkInnDto(this.infotrygdhistorikk.map { it.tilDto() }),
        vilkårsgrunnlagHistorikk = VilkårsgrunnlaghistorikkInnDto(vilkårsgrunnlagHistorikk.map { it.tilDto() }),
        minimumSykdomsgradVurdering = MinimumSykdomsgradVurderingInnDto(
            perioder = minimumSykdomsgradVurdering?.map { PeriodeDto(it.fom, it.tom) } ?: emptyList()
        )
    )

    data class MinimumSykdomsgradVurderingPeriodeData(val fom: LocalDate, val tom: LocalDate)
    data class InfotrygdhistorikkElementData(
        val id: UUID,
        val tidsstempel: LocalDateTime,
        val hendelseId: UUID,
        val ferieperioder: List<FerieperiodeData>,
        val arbeidsgiverutbetalingsperioder: List<ArbeidsgiverutbetalingsperiodeData>,
        val personutbetalingsperioder: List<PersonutbetalingsperiodeData>,
        val oppdatert: LocalDateTime
    ) {
        fun tilDto() = InfotrygdhistorikkelementInnDto(
            id = this.id,
            tidsstempel = this.tidsstempel,
            hendelseId = MeldingsreferanseDto(this.hendelseId),
            ferieperioder = this.ferieperioder.map { it.tilDto() },
            arbeidsgiverutbetalingsperioder = this.arbeidsgiverutbetalingsperioder.map { it.tilDto() },
            personutbetalingsperioder = this.personutbetalingsperioder.map { it.tilDto() },
            oppdatert = this.oppdatert
        )

        data class FerieperiodeData(
            val fom: LocalDate,
            val tom: LocalDate
        ) {
            fun tilDto() = InfotrygdFerieperiodeDto(
                periode = PeriodeDto(
                    fom = this.fom,
                    tom = this.tom
                )
            )
        }

        data class PersonutbetalingsperiodeData(
            val orgnr: String,
            val fom: LocalDate,
            val tom: LocalDate
        ) {
            fun tilDto() = InfotrygdPersonutbetalingsperiodeInnDto(
                orgnr = this.orgnr,
                periode = PeriodeDto(
                    fom = this.fom,
                    tom = this.tom
                )
            )
        }

        data class ArbeidsgiverutbetalingsperiodeData(
            val orgnr: String,
            val fom: LocalDate,
            val tom: LocalDate
        ) {
            fun tilDto() = InfotrygdArbeidsgiverutbetalingsperiodeInnDto(
                orgnr = this.orgnr,
                periode = PeriodeDto(fom = this.fom, tom = this.tom)
            )
        }
    }

    data class VilkårsgrunnlagInnslagData(
        val id: UUID,
        val opprettet: LocalDateTime,
        val vilkårsgrunnlag: List<VilkårsgrunnlagElementData>
    ) {
        fun tilDto() = VilkårsgrunnlagInnslagInnDto(
            id = this.id,
            opprettet = this.opprettet,
            vilkårsgrunnlag = this.vilkårsgrunnlag.map { it.tilDto() }
        )
    }

    data class VilkårsgrunnlagElementData(
        val skjæringstidspunkt: LocalDate,
        val type: GrunnlagsdataType,
        val inntektsgrunnlag: InntektsgrunnlagData,
        val opptjening: OpptjeningData?,
        val medlemskapstatus: JsonMedlemskapstatus?,
        val vurdertOk: Boolean?,
        val meldingsreferanseId: UUID?,
        val vilkårsgrunnlagId: UUID
    ) {
        fun tilDto() = when (type) {
            GrunnlagsdataType.Infotrygd -> VilkårsgrunnlagInnDto.Infotrygd(
                vilkårsgrunnlagId = this.vilkårsgrunnlagId,
                skjæringstidspunkt = this.skjæringstidspunkt,
                inntektsgrunnlag = inntektsgrunnlag.tilInfotrygdDto()
            )

            GrunnlagsdataType.Vilkårsprøving -> VilkårsgrunnlagInnDto.Spleis(
                vilkårsgrunnlagId = this.vilkårsgrunnlagId,
                skjæringstidspunkt = this.skjæringstidspunkt,
                inntektsgrunnlag = this.inntektsgrunnlag.tilSpleisDto(),
                opptjening = this.opptjening?.tilDto(),
                medlemskapstatus = this.medlemskapstatus!!.tilDto(),
                vurdertOk = this.vurdertOk!!,
                meldingsreferanseId = this.meldingsreferanseId?.let { MeldingsreferanseDto(it) }
            )
        }

        enum class GrunnlagsdataType {
            Infotrygd,
            Vilkårsprøving
        }

        data class InntektsgrunnlagData(
            val grunnbeløp: Double?,
            val arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysningData>,
            val selvstendigInntektsopplysninger: SelvstendigInntektsopplysningData?,
            val deaktiverteArbeidsforhold: List<ArbeidsgiverInntektsopplysningData>,
            val vurdertInfotrygd: Boolean,
        ) {
            fun tilSpleisDto() = InntektsgrunnlagInnDto(
                arbeidsgiverInntektsopplysninger = this.arbeidsgiverInntektsopplysninger.map { it.tilDto() },
                selvstendigInntektsopplysning = selvstendigInntektsopplysninger?.tilDto(),
                deaktiverteArbeidsforhold = this.deaktiverteArbeidsforhold.map { it.tilDto() },
                vurdertInfotrygd = this.vurdertInfotrygd,
                `6G` = InntektbeløpDto.Årlig(grunnbeløp!!)
            )

            fun tilInfotrygdDto() = InntektsgrunnlagInnDto(
                arbeidsgiverInntektsopplysninger = this.arbeidsgiverInntektsopplysninger.map { it.tilDto() },
                selvstendigInntektsopplysning = selvstendigInntektsopplysninger?.tilDto(),
                deaktiverteArbeidsforhold = this.deaktiverteArbeidsforhold.map { it.tilDto() },
                vurdertInfotrygd = this.vurdertInfotrygd,
                `6G` = InntektbeløpDto.Årlig(grunnbeløp!!)
            )
        }

        data class ArbeidsgiverInntektsopplysningData(
            val orgnummer: String,
            val inntektsopplysning: InntektsopplysningData,
            val korrigertInntekt: KorrigertInntektsopplysningData?,
            val skjønnsmessigFastsatt: SkjønnsmessigFastsattData?
        ) {
            fun tilDto() = ArbeidsgiverInntektsopplysningInnDto(
                orgnummer = this.orgnummer,
                faktaavklartInntekt = inntektsopplysning.tilDto(),
                korrigertInntekt = korrigertInntekt?.tilDto(),
                skjønnsmessigFastsatt = skjønnsmessigFastsatt?.tilDto()
            )

            data class SkatteopplysningData(
                val hendelseId: UUID,
                val beløp: Double,
                val måned: YearMonth,
                val type: InntekttypeData,
                val fordel: String,
                val beskrivelse: String,
                val tidsstempel: LocalDateTime
            ) {
                enum class InntekttypeData {
                    LØNNSINNTEKT,
                    NÆRINGSINNTEKT,
                    PENSJON_ELLER_TRYGD,
                    YTELSE_FRA_OFFENTLIGE
                }

                fun tilDto() = SkatteopplysningDto(
                    hendelseId = MeldingsreferanseDto(this.hendelseId),
                    beløp = InntektbeløpDto.MånedligDouble(beløp = beløp),
                    måned = this.måned,
                    type = when (type) {
                        InntekttypeData.LØNNSINNTEKT -> InntekttypeDto.LØNNSINNTEKT
                        InntekttypeData.NÆRINGSINNTEKT -> InntekttypeDto.NÆRINGSINNTEKT
                        InntekttypeData.PENSJON_ELLER_TRYGD -> InntekttypeDto.PENSJON_ELLER_TRYGD
                        InntekttypeData.YTELSE_FRA_OFFENTLIGE -> InntekttypeDto.YTELSE_FRA_OFFENTLIGE
                    },
                    fordel = fordel,
                    beskrivelse = beskrivelse,
                    tidsstempel = tidsstempel
                )
            }

            data class InntektsopplysningData(
                val id: UUID,
                val dato: LocalDate,
                val hendelseId: UUID,
                val beløp: Double,
                val tidsstempel: LocalDateTime,
                val kilde: InntektsopplysningskildeData?,
                val type: InntektsopplysningstypeData,
                val pensjonsgivendeInntekter: List<PensjonsgivendeInntektData>?,
                val anvendtÅrligGrunnbeløp: Double?,
                val skatteopplysninger: List<SkatteopplysningData>?
            ) {
                enum class InntektsopplysningstypeData {
                    ARBEIDSTAKER,
                    SELVSTENDIG
                }

                enum class InntektsopplysningskildeData {
                    SKATT_SYKEPENGEGRUNNLAG,
                    INFOTRYGD,
                    INNTEKTSMELDING
                }

                data class PensjonsgivendeInntektData(val årstall: Int, val årligBeløp: Double)

                fun tilDto(): ArbeidstakerFaktaavklartInntektInnDto {
                    val inntektsdata = InntektsdataInnDto(
                        hendelseId = MeldingsreferanseDto(this.hendelseId),
                        dato = this.dato,
                        beløp = InntektbeløpDto.MånedligDouble(beløp = beløp),
                        tidsstempel = this.tidsstempel
                    )
                    return ArbeidstakerFaktaavklartInntektInnDto(
                        id = this.id,
                        inntektsdata = inntektsdata,
                        inntektsopplysningskilde = when (kilde!!) {
                            InntektsopplysningskildeData.SKATT_SYKEPENGEGRUNNLAG -> AOrdningenDto(
                                inntektsopplysninger = this.skatteopplysninger?.map { it.tilDto() } ?: emptyList()
                            )

                            InntektsopplysningskildeData.INFOTRYGD -> ArbeidstakerinntektskildeInnDto.InfotrygdDto
                            InntektsopplysningskildeData.INNTEKTSMELDING -> ArbeidstakerinntektskildeInnDto.ArbeidsgiverDto
                        }

                    )
                }
            }

            data class KorrigertInntektsopplysningData(
                val id: UUID,
                val dato: LocalDate,
                val hendelseId: UUID,
                val beløp: Double,
                val tidsstempel: LocalDateTime
            ) {
                fun tilDto() = SaksbehandlerInnDto(
                    id = this.id,
                    inntektsdata = InntektsdataInnDto(
                        hendelseId = MeldingsreferanseDto(this.hendelseId),
                        dato = this.dato,
                        beløp = InntektbeløpDto.MånedligDouble(beløp = beløp),
                        tidsstempel = this.tidsstempel
                    )
                )
            }

            data class SkjønnsmessigFastsattData(
                val id: UUID,
                val dato: LocalDate,
                val hendelseId: UUID,
                val beløp: Double,
                val tidsstempel: LocalDateTime
            ) {
                fun tilDto() = SkjønnsmessigFastsattInnDto(
                    id = this.id,
                    inntektsdata = InntektsdataInnDto(
                        hendelseId = MeldingsreferanseDto(this.hendelseId),
                        dato = this.dato,
                        beløp = InntektbeløpDto.MånedligDouble(beløp = beløp),
                        tidsstempel = this.tidsstempel
                    )
                )
            }
        }

        data class SelvstendigInntektsopplysningData(
            val inntektsopplysning: InntektsopplysningData,
            val skjønnsmessigFastsatt: SkjønnsmessigFastsattData?
        ) {
            fun tilDto() = SelvstendigInntektsopplysningInnDto(
                faktaavklartInntekt = inntektsopplysning.tilDto(),
                skjønnsmessigFastsatt = skjønnsmessigFastsatt?.tilDto()
            )

            data class SkatteopplysningData(
                val hendelseId: UUID,
                val beløp: Double,
                val måned: YearMonth,
                val type: InntekttypeData,
                val fordel: String,
                val beskrivelse: String,
                val tidsstempel: LocalDateTime
            ) {
                enum class InntekttypeData {
                    LØNNSINNTEKT,
                    NÆRINGSINNTEKT,
                    PENSJON_ELLER_TRYGD,
                    YTELSE_FRA_OFFENTLIGE
                }

                fun tilDto() = SkatteopplysningDto(
                    hendelseId = MeldingsreferanseDto(this.hendelseId),
                    beløp = InntektbeløpDto.MånedligDouble(beløp = beløp),
                    måned = this.måned,
                    type = when (type) {
                        InntekttypeData.LØNNSINNTEKT -> InntekttypeDto.LØNNSINNTEKT
                        InntekttypeData.NÆRINGSINNTEKT -> InntekttypeDto.NÆRINGSINNTEKT
                        InntekttypeData.PENSJON_ELLER_TRYGD -> InntekttypeDto.PENSJON_ELLER_TRYGD
                        InntekttypeData.YTELSE_FRA_OFFENTLIGE -> InntekttypeDto.YTELSE_FRA_OFFENTLIGE
                    },
                    fordel = fordel,
                    beskrivelse = beskrivelse,
                    tidsstempel = tidsstempel
                )
            }

            data class InntektsopplysningData(
                val id: UUID,
                val dato: LocalDate,
                val hendelseId: UUID,
                val beløp: Double,
                val tidsstempel: LocalDateTime,
                val pensjonsgivendeInntekter: List<PensjonsgivendeInntektData>?,
                val anvendtÅrligGrunnbeløp: Double?,
                val skatteopplysninger: List<SkatteopplysningData>?
            ) {
                data class PensjonsgivendeInntektData(val årstall: Int, val årligBeløp: Double)

                fun tilDto(): SelvstendigFaktaavklartInntektInnDto {
                    val inntektsdata = InntektsdataInnDto(
                        hendelseId = MeldingsreferanseDto(this.hendelseId),
                        dato = this.dato,
                        beløp = InntektbeløpDto.MånedligDouble(beløp = beløp),
                        tidsstempel = this.tidsstempel
                    )
                    return SelvstendigFaktaavklartInntektInnDto(
                        id = this.id,
                        inntektsdata = inntektsdata,
                        pensjonsgivendeInntekter = this.pensjonsgivendeInntekter!!.map { SelvstendigFaktaavklartInntektInnDto.PensjonsgivendeInntektDto(Year.of(it.årstall), InntektbeløpDto.Årlig(it.årligBeløp)) },
                        anvendtGrunnbeløp = InntektbeløpDto.Årlig(this.anvendtÅrligGrunnbeløp!!),
                    )
                }
            }

            data class SkjønnsmessigFastsattData(
                val id: UUID,
                val dato: LocalDate,
                val hendelseId: UUID,
                val beløp: Double,
                val tidsstempel: LocalDateTime
            ) {
                fun tilDto() = SkjønnsmessigFastsattInnDto(
                    id = this.id,
                    inntektsdata = InntektsdataInnDto(
                        hendelseId = MeldingsreferanseDto(this.hendelseId),
                        dato = this.dato,
                        beløp = InntektbeløpDto.MånedligDouble(beløp = beløp),
                        tidsstempel = this.tidsstempel
                    )
                )
            }
        }

        data class OpptjeningData(
            val opptjeningFom: LocalDate,
            val opptjeningTom: LocalDate,
            val arbeidsforhold: List<ArbeidsgiverOpptjeningsgrunnlagData>
        ) {
            fun tilDto() = OpptjeningInnDto(
                arbeidsforhold = this.arbeidsforhold.map { it.tilDto() },
                opptjeningsperiode = PeriodeDto(fom = this.opptjeningFom, tom = this.opptjeningTom)
            )

            data class ArbeidsgiverOpptjeningsgrunnlagData(
                val orgnummer: String,
                val ansattPerioder: List<ArbeidsforholdData>
            ) {
                fun tilDto() = ArbeidsgiverOpptjeningsgrunnlagDto(
                    orgnummer = this.orgnummer,
                    ansattPerioder = this.ansattPerioder.map { it.tilDto() }
                )

                data class ArbeidsforholdData(
                    val ansattFom: LocalDate,
                    val ansattTom: LocalDate?,
                    val deaktivert: Boolean
                ) {
                    fun tilDto() = ArbeidsforholdDto(
                        ansattFom = ansattFom, ansattTom = ansattTom, deaktivert = deaktivert
                    )
                }
            }
        }
    }

    data class ArbeidsgiverData(
        val organisasjonsnummer: String,
        val yrkesaktivitetstype: YrkesaktivitetTypeData,
        val id: UUID,
        val inntektshistorikk: List<InntektsmeldingData>,
        val sykdomshistorikk: List<SykdomshistorikkData>,
        val sykmeldingsperioder: List<SykmeldingsperiodeData>,
        val vedtaksperioder: List<VedtaksperiodeData>,
        val forkastede: List<ForkastetVedtaksperiodeData>,
        val utbetalinger: List<UtbetalingData>,
        val feriepengeutbetalinger: List<FeriepengeutbetalingData>,
        val ubrukteRefusjonsopplysninger: Map<LocalDate, BeløpstidslinjeData>
    ) {
        fun tilDto() = ArbeidsgiverInnDto(
            id = this.id,
            organisasjonsnummer = this.organisasjonsnummer,
            yrkesaktivitetstype = when (this.yrkesaktivitetstype) {
                YrkesaktivitetTypeData.ARBEIDSTAKER -> YrkesaktivitetstypeDto.ARBEIDSTAKER
                YrkesaktivitetTypeData.ARBEIDSLEDIG -> YrkesaktivitetstypeDto.ARBEIDSLEDIG
                YrkesaktivitetTypeData.FRILANS -> YrkesaktivitetstypeDto.FRILANS
                YrkesaktivitetTypeData.SELVSTENDIG -> YrkesaktivitetstypeDto.SELVSTENDIG
            },
            inntektshistorikk = InntektshistorikkInnDto(this.inntektshistorikk.map { it.tilDto() }),
            sykdomshistorikk = SykdomshistorikkDto(this.sykdomshistorikk.map { it.tilDto() }),
            sykmeldingsperioder = SykmeldingsperioderDto(this.sykmeldingsperioder.map { it.tilDto() }),
            vedtaksperioder = this.vedtaksperioder.map { it.tilDto() },
            forkastede = this.forkastede.map { it.tilDto() },
            utbetalinger = this.utbetalinger.map { it.tilDto() },
            feriepengeutbetalinger = this.feriepengeutbetalinger.map { it.tilDto() },
            ubrukteRefusjonsopplysninger = RefusjonsservitørDto(this.ubrukteRefusjonsopplysninger.mapValues { (_, beløpstidslinje) -> beløpstidslinje.tilDto() })
        )

        data class InntektsmeldingData(
            val id: UUID,
            val dato: LocalDate,
            val hendelseId: UUID,
            val beløp: Double,
            val kilde: InntektsmeldingKildeDto,
            val tidsstempel: LocalDateTime
        ) {
            enum class InntektsmeldingKildeDto {
                Arbeidsgiver,
                AOrdningen
            }

            fun tilDto() = InntektsmeldingInnDto(
                id = this.id,
                inntektsdata = InntektsdataInnDto(
                    hendelseId = MeldingsreferanseDto(this.hendelseId),
                    dato = this.dato,
                    beløp = InntektbeløpDto.MånedligDouble(beløp),
                    tidsstempel = this.tidsstempel
                ),
                kilde = when (kilde) {
                    InntektsmeldingKildeDto.Arbeidsgiver -> InntektsmeldingInnDto.KildeDto.Arbeidsgiver
                    InntektsmeldingKildeDto.AOrdningen -> InntektsmeldingInnDto.KildeDto.AOrdningen
                }
            )
        }

        data class PeriodeData(val fom: LocalDate, val tom: LocalDate) {
            fun tilDto() = PeriodeDto(fom = this.fom, tom = this.tom)
        }

        data class SykdomstidslinjeData(
            val dager: List<DagData>,
            val periode: PeriodeData?,
            val låstePerioder: List<PeriodeData>?
        ) {
            fun tilDto() = SykdomstidslinjeDto(
                dager = this.dager.flatMap { it.tilDto() },
                periode = this.periode?.tilDto(),
                låstePerioder = this.låstePerioder?.map { it.tilDto() } ?: emptyList()
            )

            data class DagData(
                val type: JsonDagType,
                val kilde: KildeData,
                val grad: Double,
                val other: KildeData?,
                val melding: String?,
                val fom: LocalDate?,
                val tom: LocalDate?,
                val dato: LocalDate?
            ) {
                init {
                    check(dato != null || (fom != null && tom != null)) {
                        "enten må dato være satt eller så må både fom og tom være satt"
                    }
                }

                private val datoer = datosekvens(dato, fom, tom)

                fun tilDto(): Sequence<SykdomstidslinjeDagDto> {
                    val kilde = this.kilde.tilDto()
                    return datoer.map { tilDto(it, kilde) }
                }

                private fun tilDto(dagen: LocalDate, kilde: HendelseskildeDto) = when (type) {
                    JsonDagType.ARBEIDSDAG -> ArbeidsdagDto(dato = dagen, kilde = kilde)
                    JsonDagType.ARBEIDSGIVERDAG -> if (dagen.erHelg())
                        ArbeidsgiverHelgedagDto(dato = dagen, kilde = kilde, grad = ProsentdelDto(grad))
                    else
                        ArbeidsgiverdagDto(dato = dagen, kilde = kilde, grad = ProsentdelDto(grad))

                    JsonDagType.FERIEDAG -> FeriedagDto(dato = dagen, kilde = kilde)
                    JsonDagType.ARBEID_IKKE_GJENOPPTATT_DAG -> ArbeidIkkeGjenopptattDagDto(dato = dagen, kilde = kilde)
                    JsonDagType.FRISK_HELGEDAG -> FriskHelgedagDto(dato = dagen, kilde = kilde)
                    JsonDagType.FORELDET_SYKEDAG -> ForeldetSykedagDto(dato = dagen, kilde = kilde, grad = ProsentdelDto(grad))
                    JsonDagType.PERMISJONSDAG -> PermisjonsdagDto(dato = dagen, kilde = kilde)
                    JsonDagType.PROBLEMDAG -> ProblemDagDto(dato = dagen, kilde = kilde, other = this.other!!.tilDto(), melding = this.melding!!)
                    JsonDagType.SYKEDAG -> if (dagen.erHelg())
                        SykHelgedagDto(dato = dagen, kilde = kilde, grad = ProsentdelDto(grad))
                    else
                        SykedagDto(dato = dagen, kilde = kilde, grad = ProsentdelDto(grad))

                    JsonDagType.ANDRE_YTELSER_FORELDREPENGER -> AndreYtelserDto(dato = dagen, kilde = kilde, AndreYtelserDto.YtelseDto.Foreldrepenger)
                    JsonDagType.ANDRE_YTELSER_AAP -> AndreYtelserDto(dato = dagen, kilde = kilde, ytelse = AndreYtelserDto.YtelseDto.AAP)
                    JsonDagType.ANDRE_YTELSER_OMSORGSPENGER -> AndreYtelserDto(dato = dagen, kilde = kilde, ytelse = AndreYtelserDto.YtelseDto.Omsorgspenger)
                    JsonDagType.ANDRE_YTELSER_PLEIEPENGER -> AndreYtelserDto(dato = dagen, kilde = kilde, ytelse = AndreYtelserDto.YtelseDto.Pleiepenger)
                    JsonDagType.ANDRE_YTELSER_SVANGERSKAPSPENGER -> AndreYtelserDto(dato = dagen, kilde = kilde, ytelse = AndreYtelserDto.YtelseDto.Svangerskapspenger)
                    JsonDagType.ANDRE_YTELSER_OPPLÆRINGSPENGER -> AndreYtelserDto(dato = dagen, kilde = kilde, ytelse = AndreYtelserDto.YtelseDto.Opplæringspenger)
                    JsonDagType.ANDRE_YTELSER_DAGPENGER -> AndreYtelserDto(dato = dagen, kilde = kilde, ytelse = AndreYtelserDto.YtelseDto.Dagpenger)
                    JsonDagType.UKJENT_DAG -> UkjentDagDto(dato = dagen, kilde = kilde)
                }
            }

            enum class JsonDagType {
                ARBEIDSDAG,
                ARBEIDSGIVERDAG,

                FERIEDAG,
                ARBEID_IKKE_GJENOPPTATT_DAG,
                FRISK_HELGEDAG,
                FORELDET_SYKEDAG,
                PERMISJONSDAG,
                PROBLEMDAG,
                SYKEDAG,

                ANDRE_YTELSER_FORELDREPENGER,
                ANDRE_YTELSER_AAP,
                ANDRE_YTELSER_OMSORGSPENGER,
                ANDRE_YTELSER_PLEIEPENGER,
                ANDRE_YTELSER_SVANGERSKAPSPENGER,
                ANDRE_YTELSER_OPPLÆRINGSPENGER,
                ANDRE_YTELSER_DAGPENGER,

                UKJENT_DAG
            }

            data class KildeData(
                val type: String,
                val id: UUID,
                val tidsstempel: LocalDateTime
            ) {
                fun tilDto() = HendelseskildeDto(
                    type = this.type,
                    meldingsreferanseId = MeldingsreferanseDto(this.id),
                    tidsstempel = this.tidsstempel
                )
            }
        }

        data class ForkastetVedtaksperiodeData(
            val vedtaksperiode: VedtaksperiodeData
        ) {
            fun tilDto() = ForkastetVedtaksperiodeInnDto(vedtaksperiode = this.vedtaksperiode.tilDto())
        }

        data class FeriepengeutbetalingData(
            val infotrygdFeriepengebeløpPerson: Double,
            val infotrygdFeriepengebeløpArbeidsgiver: Double,
            val spleisFeriepengebeløpArbeidsgiver: Double,
            val spleisFeriepengebeløpPerson: Double,
            val oppdrag: OppdragData,
            val personoppdrag: OppdragData,
            val opptjeningsår: Year,
            val utbetalteDager: List<UtbetaltDagData>,
            val feriepengedager: List<UtbetaltDagData>,
            val utbetalingId: UUID,
            val sendTilOppdrag: Boolean,
            val sendPersonoppdragTilOS: Boolean,
        ) {
            fun tilDto() = FeriepengeInnDto(
                feriepengeberegner = FeriepengeutbetalinggrunnlagInnDto(
                    opptjeningsår = this.opptjeningsår,
                    utbetalteDager = this.utbetalteDager.map { it.tilDto() },
                    feriepengedager = this.feriepengedager.map { it.tilDto() }
                ),
                infotrygdFeriepengebeløpPerson = this.infotrygdFeriepengebeløpPerson,
                infotrygdFeriepengebeløpArbeidsgiver = this.infotrygdFeriepengebeløpArbeidsgiver,
                spleisFeriepengebeløpPerson = this.spleisFeriepengebeløpPerson,
                spleisFeriepengebeløpArbeidsgiver = this.spleisFeriepengebeløpArbeidsgiver,
                oppdrag = this.oppdrag.tilDto(),
                personoppdrag = this.personoppdrag.tilDto(),
                utbetalingId = this.utbetalingId,
                sendTilOppdrag = this.sendTilOppdrag,
                sendPersonoppdragTilOS = this.sendPersonoppdragTilOS
            )

            data class UtbetaltDagData(
                val type: String,
                val orgnummer: String,
                val dato: LocalDate,
                val beløp: Int,
            ) {
                fun tilDto(): UtbetaltDagInnDto = when (type) {
                    "InfotrygdPersonDag" -> UtbetaltDagInnDto.InfotrygdPerson(
                        orgnummer = orgnummer,
                        dato = dato,
                        beløp = beløp
                    )

                    "InfotrygdArbeidsgiverDag" -> UtbetaltDagInnDto.InfotrygdArbeidsgiver(
                        orgnummer = orgnummer,
                        dato = dato,
                        beløp = beløp
                    )

                    "SpleisArbeidsgiverDag" -> UtbetaltDagInnDto.SpleisArbeidsgiver(
                        orgnummer = orgnummer,
                        dato = dato,
                        beløp = beløp
                    )

                    "SpleisPersonDag" -> UtbetaltDagInnDto.SpleisPerson(
                        orgnummer = orgnummer,
                        dato = dato,
                        beløp = beløp
                    )

                    else -> error("Ukjent utbetaltdag-type: $type")
                }
            }

            data class OppdragData(
                val mottaker: String,
                val fagområde: String,
                val linjer: List<UtbetalingslinjeData>,
                val fagsystemId: String,
                val endringskode: String,
                val tidsstempel: LocalDateTime
            ) {
                fun tilDto() = FeriepengeoppdragInnDto(
                    mottaker = this.mottaker,
                    fagområde = when (fagområde) {
                        "SPREF" -> FeriepengerfagområdeDto.SPREF
                        "SP" -> FeriepengerfagområdeDto.SP
                        else -> error("Ukjent fagområde: $fagområde")
                    },
                    linjer = this.linjer.map { it.tilDto() },
                    fagsystemId = this.fagsystemId,
                    endringskode = when (endringskode) {
                        "NY" -> FeriepengerendringskodeDto.NY
                        "ENDR" -> FeriepengerendringskodeDto.ENDR
                        "UEND" -> FeriepengerendringskodeDto.UEND
                        else -> error("Ukjent endringskode: $endringskode")
                    },
                    tidsstempel = this.tidsstempel
                )

                data class UtbetalingslinjeData(
                    val fom: LocalDate,
                    val tom: LocalDate,
                    val sats: Int,
                    val refFagsystemId: String?,
                    val delytelseId: Int,
                    val refDelytelseId: Int?,
                    val endringskode: String,
                    val klassekode: String,
                    val datoStatusFom: LocalDate?
                ) {
                    fun tilDto() = FeriepengeutbetalingslinjeInnDto(
                        fom = this.fom,
                        tom = this.tom,
                        beløp = this.sats,
                        refFagsystemId = this.refFagsystemId,
                        delytelseId = this.delytelseId,
                        refDelytelseId = this.refDelytelseId,
                        endringskode = when (this.endringskode) {
                            "NY" -> FeriepengerendringskodeDto.NY
                            "ENDR" -> FeriepengerendringskodeDto.ENDR
                            "UEND" -> FeriepengerendringskodeDto.UEND
                            else -> error("Ukjent endringskode: $endringskode")
                        },
                        klassekode = when (this.klassekode) {
                            "SPREFAGFER-IOP" -> FeriepengerklassekodeDto.RefusjonFeriepengerIkkeOpplysningspliktig
                            "SPATFER" -> FeriepengerklassekodeDto.SykepengerArbeidstakerFeriepenger
                            else -> error("Ukjent klassekode: ${this.klassekode}")
                        },
                        datoStatusFom = this.datoStatusFom
                    )
                }
            }
        }

        data class SykmeldingsperiodeData(
            val fom: LocalDate,
            val tom: LocalDate
        ) {
            fun tilDto() = PeriodeDto(fom = fom, tom = tom)
        }

        data class VedtaksperiodeData(
            val id: UUID,
            val tilstand: TilstandTypeData,
            val skjæringstidspunkt: LocalDate,
            val behandlinger: List<BehandlingData>,
            val opprettet: LocalDateTime,
            val oppdatert: LocalDateTime
        ) {
            enum class TilstandTypeData {
                AVVENTER_HISTORIKK,
                AVVENTER_GODKJENNING,
                AVVENTER_SIMULERING,
                TIL_UTBETALING,
                TIL_INFOTRYGD,
                AVSLUTTET,
                AVSLUTTET_UTEN_UTBETALING,
                REVURDERING_FEILET,
                START,
                AVVENTER_INFOTRYGDHISTORIKK,
                AVVENTER_INNTEKTSMELDING,
                AVVENTER_BLOKKERENDE_PERIODE,
                AVVENTER_A_ORDNINGEN,
                AVVENTER_VILKÅRSPRØVING,
                AVVENTER_REVURDERING,
                AVVENTER_HISTORIKK_REVURDERING,
                AVVENTER_VILKÅRSPRØVING_REVURDERING,
                AVVENTER_SIMULERING_REVURDERING,
                AVVENTER_GODKJENNING_REVURDERING,

                AVVENTER_ANNULLERING,
                TIL_ANNULLERING,

                SELVSTENDIG_START,
                SELVSTENDIG_AVVENTER_INFOTRYGDHISTORIKK,
                SELVSTENDIG_AVVENTER_BLOKKERENDE_PERIODE,
                SELVSTENDIG_AVVENTER_VILKÅRSPRØVING,
                SELVSTENDIG_AVVENTER_HISTORIKK,
                SELVSTENDIG_AVVENTER_SIMULERING,
                SELVSTENDIG_AVVENTER_GODKJENNING,

                SELVSTENDIG_TIL_UTBETALING,
                SELVSTENDIG_AVSLUTTET,
            }

            fun tilDto() = VedtaksperiodeInnDto(
                id = this.id,
                tilstand = when (tilstand) {
                    TilstandTypeData.AVVENTER_A_ORDNINGEN -> VedtaksperiodetilstandDto.AVVENTER_A_ORDNINGEN
                    TilstandTypeData.AVVENTER_HISTORIKK -> VedtaksperiodetilstandDto.AVVENTER_HISTORIKK
                    TilstandTypeData.AVVENTER_GODKJENNING -> VedtaksperiodetilstandDto.AVVENTER_GODKJENNING
                    TilstandTypeData.AVVENTER_SIMULERING -> VedtaksperiodetilstandDto.AVVENTER_SIMULERING
                    TilstandTypeData.TIL_UTBETALING -> VedtaksperiodetilstandDto.TIL_UTBETALING
                    TilstandTypeData.TIL_INFOTRYGD -> VedtaksperiodetilstandDto.TIL_INFOTRYGD
                    TilstandTypeData.AVSLUTTET -> VedtaksperiodetilstandDto.AVSLUTTET
                    TilstandTypeData.AVSLUTTET_UTEN_UTBETALING -> VedtaksperiodetilstandDto.AVSLUTTET_UTEN_UTBETALING
                    TilstandTypeData.REVURDERING_FEILET -> VedtaksperiodetilstandDto.REVURDERING_FEILET
                    TilstandTypeData.START -> VedtaksperiodetilstandDto.START
                    TilstandTypeData.AVVENTER_INFOTRYGDHISTORIKK -> VedtaksperiodetilstandDto.AVVENTER_INFOTRYGDHISTORIKK
                    TilstandTypeData.AVVENTER_INNTEKTSMELDING -> VedtaksperiodetilstandDto.AVVENTER_INNTEKTSMELDING
                    TilstandTypeData.AVVENTER_BLOKKERENDE_PERIODE -> VedtaksperiodetilstandDto.AVVENTER_BLOKKERENDE_PERIODE
                    TilstandTypeData.AVVENTER_VILKÅRSPRØVING -> VedtaksperiodetilstandDto.AVVENTER_VILKÅRSPRØVING
                    TilstandTypeData.AVVENTER_REVURDERING -> VedtaksperiodetilstandDto.AVVENTER_REVURDERING
                    TilstandTypeData.AVVENTER_HISTORIKK_REVURDERING -> VedtaksperiodetilstandDto.AVVENTER_HISTORIKK_REVURDERING
                    TilstandTypeData.AVVENTER_VILKÅRSPRØVING_REVURDERING -> VedtaksperiodetilstandDto.AVVENTER_VILKÅRSPRØVING_REVURDERING
                    TilstandTypeData.AVVENTER_SIMULERING_REVURDERING -> VedtaksperiodetilstandDto.AVVENTER_SIMULERING_REVURDERING
                    TilstandTypeData.AVVENTER_GODKJENNING_REVURDERING -> VedtaksperiodetilstandDto.AVVENTER_GODKJENNING_REVURDERING
                    TilstandTypeData.AVVENTER_ANNULLERING -> VedtaksperiodetilstandDto.AVVENTER_ANNULLERING
                    TilstandTypeData.TIL_ANNULLERING -> VedtaksperiodetilstandDto.TIL_ANNULLERING

                    TilstandTypeData.SELVSTENDIG_START -> VedtaksperiodetilstandDto.SELVSTENDIG_START
                    TilstandTypeData.SELVSTENDIG_AVVENTER_INFOTRYGDHISTORIKK -> VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_INFOTRYGDHISTORIKK
                    TilstandTypeData.SELVSTENDIG_AVVENTER_BLOKKERENDE_PERIODE -> VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_BLOKKERENDE_PERIODE
                    TilstandTypeData.SELVSTENDIG_AVVENTER_VILKÅRSPRØVING -> VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_VILKÅRSPRØVING
                    TilstandTypeData.SELVSTENDIG_AVVENTER_HISTORIKK -> VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_HISTORIKK
                    TilstandTypeData.SELVSTENDIG_AVVENTER_SIMULERING -> VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_SIMULERING
                    TilstandTypeData.SELVSTENDIG_AVVENTER_GODKJENNING -> VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_GODKJENNING
                    TilstandTypeData.SELVSTENDIG_TIL_UTBETALING -> VedtaksperiodetilstandDto.SELVSTENDIG_TIL_UTBETALING
                    TilstandTypeData.SELVSTENDIG_AVSLUTTET -> VedtaksperiodetilstandDto.SELVSTENDIG_AVSLUTTET
                },
                behandlinger = BehandlingerInnDto(this.behandlinger.map { it.tilDto() }),
                opprettet = opprettet,
                oppdatert = oppdatert
            )

            enum class MaksdatobestemmelseData {
                IKKE_VURDERT, ORDINÆR_RETT, BEGRENSET_RETT, SYTTI_ÅR
            }

            data class MaksdatoresultatData(
                val vurdertTilOgMed: LocalDate,
                val bestemmelse: MaksdatobestemmelseData,
                val startdatoTreårsvindu: LocalDate,
                val startdatoSykepengerettighet: LocalDate?,
                val forbrukteDager: List<PeriodeData>,
                val oppholdsdager: List<PeriodeData>,
                val avslåtteDager: List<PeriodeData>,
                val maksdato: LocalDate,
                val gjenståendeDager: Int
            ) {
                fun tilDto() = MaksdatoresultatInnDto(
                    vurdertTilOgMed = vurdertTilOgMed,
                    bestemmelse = when (bestemmelse) {
                        MaksdatobestemmelseData.IKKE_VURDERT -> MaksdatobestemmelseDto.IKKE_VURDERT
                        MaksdatobestemmelseData.ORDINÆR_RETT -> MaksdatobestemmelseDto.ORDINÆR_RETT
                        MaksdatobestemmelseData.BEGRENSET_RETT -> MaksdatobestemmelseDto.BEGRENSET_RETT
                        MaksdatobestemmelseData.SYTTI_ÅR -> MaksdatobestemmelseDto.SYTTI_ÅR
                    },
                    startdatoTreårsvindu = startdatoTreårsvindu,
                    startdatoSykepengerettighet = startdatoSykepengerettighet,
                    forbrukteDager = forbrukteDager.map { PeriodeDto(fom = it.fom, tom = it.tom) },
                    oppholdsdager = oppholdsdager.map { PeriodeDto(fom = it.fom, tom = it.tom) },
                    avslåtteDager = avslåtteDager.map { PeriodeDto(fom = it.fom, tom = it.tom) },
                    maksdato = maksdato,
                    gjenståendeDager = gjenståendeDager
                )
            }

            data class DokumentsporingData(
                val dokumentId: UUID,
                val dokumenttype: DokumentTypeData
            ) {
                fun tilDto() = DokumentsporingDto(
                    id = MeldingsreferanseDto(this.dokumentId),
                    type = when (dokumenttype) {
                        DokumentTypeData.Sykmelding -> DokumenttypeDto.Sykmelding
                        DokumentTypeData.Søknad -> DokumenttypeDto.Søknad
                        DokumentTypeData.InntektsmeldingInntekt -> DokumenttypeDto.InntektsmeldingInntekt
                        DokumentTypeData.InntektsmeldingRefusjon -> DokumenttypeDto.InntektsmeldingRefusjon
                        DokumentTypeData.InntektsmeldingDager -> DokumenttypeDto.InntektsmeldingDager
                        DokumentTypeData.InntektFraAOrdningen -> DokumenttypeDto.InntektFraAOrdningen
                        DokumentTypeData.OverstyrTidslinje -> DokumenttypeDto.OverstyrTidslinje
                        DokumentTypeData.OverstyrInntekt -> DokumenttypeDto.OverstyrInntekt
                        DokumentTypeData.OverstyrRefusjon -> DokumenttypeDto.OverstyrRefusjon
                        DokumentTypeData.OverstyrArbeidsgiveropplysninger -> DokumenttypeDto.OverstyrArbeidsgiveropplysninger
                        DokumentTypeData.OverstyrArbeidsforhold -> DokumenttypeDto.OverstyrArbeidsforhold
                        DokumentTypeData.SkjønnsmessigFastsettelse -> DokumenttypeDto.SkjønnsmessigFastsettelse
                        DokumentTypeData.AndreYtelser -> DokumenttypeDto.AndreYtelser
                    }
                )
            }

            enum class DokumentTypeData {
                Sykmelding,
                Søknad,
                InntektsmeldingInntekt,
                InntektsmeldingRefusjon,
                InntektsmeldingDager,
                InntektFraAOrdningen,
                OverstyrTidslinje,
                OverstyrInntekt,
                OverstyrRefusjon,
                OverstyrArbeidsgiveropplysninger,
                OverstyrArbeidsforhold,
                SkjønnsmessigFastsettelse,
                AndreYtelser
            }

            data class BehandlingData(
                val id: UUID,
                val tilstand: TilstandData,
                val vedtakFattet: LocalDateTime?,
                val avsluttet: LocalDateTime?,
                val kilde: KildeData,
                val endringer: List<EndringData>,
            ) {
                fun tilDto() = BehandlingInnDto(
                    id = this.id,
                    tilstand = when (tilstand) {
                        TilstandData.UBEREGNET -> BehandlingtilstandDto.UBEREGNET
                        TilstandData.UBEREGNET_OMGJØRING -> BehandlingtilstandDto.UBEREGNET_OMGJØRING
                        TilstandData.UBEREGNET_REVURDERING -> BehandlingtilstandDto.UBEREGNET_REVURDERING
                        TilstandData.BEREGNET -> BehandlingtilstandDto.BEREGNET
                        TilstandData.BEREGNET_OMGJØRING -> BehandlingtilstandDto.BEREGNET_OMGJØRING
                        TilstandData.BEREGNET_REVURDERING -> BehandlingtilstandDto.BEREGNET_REVURDERING
                        TilstandData.VEDTAK_FATTET -> BehandlingtilstandDto.VEDTAK_FATTET
                        TilstandData.REVURDERT_VEDTAK_AVVIST -> BehandlingtilstandDto.REVURDERT_VEDTAK_AVVIST
                        TilstandData.VEDTAK_IVERKSATT -> BehandlingtilstandDto.VEDTAK_IVERKSATT
                        TilstandData.AVSLUTTET_UTEN_VEDTAK -> BehandlingtilstandDto.AVSLUTTET_UTEN_VEDTAK
                        TilstandData.ANNULLERT_PERIODE -> BehandlingtilstandDto.ANNULLERT_PERIODE
                        TilstandData.TIL_INFOTRYGD -> BehandlingtilstandDto.TIL_INFOTRYGD
                        TilstandData.UBEREGNET_ANNULLERING -> BehandlingtilstandDto.UBEREGNET_ANNULLERING
                        TilstandData.BEREGNET_ANNULLERING -> BehandlingtilstandDto.BEREGNET_ANNULLERING
                        TilstandData.OVERFØRT_ANNULLERING -> BehandlingtilstandDto.OVERFØRT_ANNULLERING
                    },
                    vedtakFattet = this.vedtakFattet,
                    avsluttet = this.avsluttet,
                    kilde = this.kilde.tilDto(),
                    endringer = this.endringer.map { it.tilDto() },
                )

                enum class TilstandData {
                    UBEREGNET, UBEREGNET_OMGJØRING, UBEREGNET_REVURDERING, BEREGNET, BEREGNET_OMGJØRING, BEREGNET_REVURDERING,
                    VEDTAK_FATTET, REVURDERT_VEDTAK_AVVIST, VEDTAK_IVERKSATT, AVSLUTTET_UTEN_VEDTAK, ANNULLERT_PERIODE, TIL_INFOTRYGD,
                    UBEREGNET_ANNULLERING, BEREGNET_ANNULLERING, OVERFØRT_ANNULLERING
                }

                enum class AvsenderData {
                    SYKMELDT, ARBEIDSGIVER, SAKSBEHANDLER, SYSTEM;

                    fun tilDto() = when (this) {
                        SYKMELDT -> AvsenderDto.SYKMELDT
                        ARBEIDSGIVER -> AvsenderDto.ARBEIDSGIVER
                        SAKSBEHANDLER -> AvsenderDto.SAKSBEHANDLER
                        SYSTEM -> AvsenderDto.SYSTEM
                    }
                }

                data class KildeData(
                    val meldingsreferanseId: UUID,
                    val innsendt: LocalDateTime,
                    val registrert: LocalDateTime,
                    val avsender: AvsenderData
                ) {
                    fun tilDto() = BehandlingkildeDto(
                        meldingsreferanseId = MeldingsreferanseDto(this.meldingsreferanseId),
                        innsendt = this.innsendt,
                        registert = this.registrert,
                        avsender = this.avsender.tilDto()
                    )
                }

                data class EndringData(
                    val id: UUID,
                    val tidsstempel: LocalDateTime,
                    val sykmeldingsperiodeFom: LocalDate,
                    val sykmeldingsperiodeTom: LocalDate,
                    val fom: LocalDate,
                    val tom: LocalDate,
                    val arbeidssituasjon: ArbeidssituasjonData,
                    val skjæringstidspunkt: LocalDate,
                    val skjæringstidspunkter: List<LocalDate>,
                    val utbetalingId: UUID?,
                    val vilkårsgrunnlagId: UUID?,
                    val sykdomstidslinje: SykdomstidslinjeData,
                    val utbetalingstidslinje: UtbetalingstidslinjeData,
                    val refusjonstidslinje: BeløpstidslinjeData,
                    val inntektsendringer: BeløpstidslinjeData,
                    val dokumentsporing: DokumentsporingData,
                    val arbeidsgiverperiode: ArbeidsgiverperiodeData,
                    val dagerNavOvertarAnsvar: List<PeriodeData>,
                    val egenmeldingsdager: List<PeriodeData>,
                    val maksdatoresultat: MaksdatoresultatData,
                    val inntektjusteringer: Map<String, BeløpstidslinjeData>,
                    val faktaavklartInntekt: VilkårsgrunnlagElementData.SelvstendigInntektsopplysningData.InntektsopplysningData?,
                    val ventetid: PeriodeData?,
                    val forberedendeVilkårsgrunnlag: ForberedendeVilkårsgrunnlagData?
                ) {
                    fun tilDto() = BehandlingendringInnDto(
                        id = this.id,
                        tidsstempel = this.tidsstempel,
                        sykmeldingsperiode = PeriodeDto(fom = sykmeldingsperiodeFom, tom = sykmeldingsperiodeTom),
                        periode = PeriodeDto(fom = this.fom, tom = this.tom),
                        arbeidssituasjon = when (this.arbeidssituasjon) {
                            ArbeidssituasjonData.ARBEIDSTAKER -> ArbeidssituasjonDto.ARBEIDSTAKER
                            ArbeidssituasjonData.ARBEIDSLEDIG -> ArbeidssituasjonDto.ARBEIDSLEDIG
                            ArbeidssituasjonData.SELVSTENDIG_NÆRINGSDRIVENDE -> ArbeidssituasjonDto.SELVSTENDIG_NÆRINGSDRIVENDE
                            ArbeidssituasjonData.BARNEPASSER -> ArbeidssituasjonDto.BARNEPASSER
                            ArbeidssituasjonData.FRILANSER -> ArbeidssituasjonDto.FRILANSER
                            ArbeidssituasjonData.JORDBRUKER -> ArbeidssituasjonDto.JORDBRUKER
                            ArbeidssituasjonData.FISKER -> ArbeidssituasjonDto.FISKER
                            ArbeidssituasjonData.ANNET -> ArbeidssituasjonDto.ANNET
                        },
                        vilkårsgrunnlagId = this.vilkårsgrunnlagId,
                        utbetalingId = this.utbetalingId,
                        dokumentsporing = this.dokumentsporing.tilDto(),
                        sykdomstidslinje = this.sykdomstidslinje.tilDto(),
                        utbetalingstidslinje = this.utbetalingstidslinje.tilDto(),
                        refusjonstidslinje = this.refusjonstidslinje.tilDto(),
                        inntektsendringer = this.inntektsendringer.tilDto(),
                        skjæringstidspunkt = skjæringstidspunkt,
                        skjæringstidspunkter = skjæringstidspunkter,
                        arbeidsgiverperiode = arbeidsgiverperiode.tilDto(),
                        dagerNavOvertarAnsvar = dagerNavOvertarAnsvar.map { it.tilDto() },
                        egenmeldingsdager = egenmeldingsdager.map { it.tilDto() },
                        maksdatoresultat = maksdatoresultat.tilDto(),
                        inntektjusteringer = inntektjusteringer.map { (inntektskilde, beløpstidslinje) ->
                            InntektskildeDto(inntektskilde) to beløpstidslinje.tilDto()
                        }.toMap(),
                        faktaavklartInntekt = faktaavklartInntekt?.tilDto(),
                        ventetid = ventetid?.tilDto(),
                        forberedendeVilkårsgrunnlag = forberedendeVilkårsgrunnlag?.tilDto()
                    )

                    data class ArbeidsgiverperiodeData(
                        val ferdigAvklart: Boolean,
                        val dager: List<PeriodeData>
                    ) {
                        fun tilDto() = ArbeidsgiverperiodeavklaringDto(
                            ferdigAvklart = this.ferdigAvklart,
                            dager = this.dager.map { it.tilDto() }
                        )
                    }

                    enum class ArbeidssituasjonData {
                        ARBEIDSTAKER,
                        ARBEIDSLEDIG,
                        FRILANSER,
                        SELVSTENDIG_NÆRINGSDRIVENDE,
                        BARNEPASSER,
                        JORDBRUKER,
                        FISKER,
                        ANNET
                    }

                    data class ForberedendeVilkårsgrunnlagData(val erOpptjeningVurdertOk: Boolean) {
                        fun tilDto() = ForberedendeVilkårsgrunnlagDto(erOpptjeningVurdertOk)
                    }
                }
            }

            data class DataForSimuleringData(
                val totalbeløp: Int,
                val perioder: List<SimulertPeriode>
            ) {
                internal fun tilDto() = SimuleringResultatDto(
                    totalbeløp = totalbeløp,
                    perioder = perioder.map { it.tilDto() }
                )

                data class SimulertPeriode(
                    val fom: LocalDate,
                    val tom: LocalDate,
                    val utbetalinger: List<SimulertUtbetaling>
                ) {

                    internal fun tilDto(): SimuleringResultatDto.SimulertPeriode {
                        return SimuleringResultatDto.SimulertPeriode(
                            fom = fom,
                            tom = tom,
                            utbetalinger = utbetalinger.map { it.tilDto() }
                        )
                    }
                }

                data class SimulertUtbetaling(
                    val forfallsdato: LocalDate,
                    val utbetalesTil: Mottaker,
                    val feilkonto: Boolean,
                    val detaljer: List<Detaljer>
                ) {
                    internal fun tilDto(): SimuleringResultatDto.SimulertUtbetaling {
                        return SimuleringResultatDto.SimulertUtbetaling(
                            forfallsdato = forfallsdato,
                            utbetalesTil = SimuleringResultatDto.Mottaker(
                                id = utbetalesTil.id,
                                navn = utbetalesTil.navn
                            ),
                            feilkonto = feilkonto,
                            detaljer = detaljer.map { it.tilDto() }
                        )
                    }
                }

                data class Detaljer(
                    val fom: LocalDate,
                    val tom: LocalDate,
                    val konto: String,
                    val beløp: Int,
                    val klassekode: Klassekode,
                    val uføregrad: Int,
                    val utbetalingstype: String,
                    val tilbakeføring: Boolean,
                    val sats: Sats,
                    val refunderesOrgnummer: String
                ) {
                    internal fun tilDto(): SimuleringResultatDto.Detaljer {
                        return SimuleringResultatDto.Detaljer(
                            fom = fom,
                            tom = tom,
                            konto = konto,
                            beløp = beløp,
                            klassekode = SimuleringResultatDto.Klassekode(
                                kode = klassekode.kode,
                                beskrivelse = klassekode.beskrivelse
                            ),
                            uføregrad = uføregrad,
                            utbetalingstype = utbetalingstype,
                            tilbakeføring = tilbakeføring,
                            sats = SimuleringResultatDto.Sats(
                                sats = sats.sats,
                                antall = sats.antall,
                                type = sats.type
                            ),
                            refunderesOrgnummer = refunderesOrgnummer
                        )
                    }
                }

                data class Sats(
                    val sats: Double,
                    val antall: Int,
                    val type: String
                )

                data class Klassekode(
                    val kode: String,
                    val beskrivelse: String
                )

                data class Mottaker(
                    val id: String,
                    val navn: String
                )
            }
        }

        enum class YrkesaktivitetTypeData {
            ARBEIDSTAKER,
            ARBEIDSLEDIG,
            FRILANS,
            SELVSTENDIG
        }
    }

    data class SykdomshistorikkData(
        val tidsstempel: LocalDateTime,
        val id: UUID,
        val hendelseId: UUID?,
        val hendelseSykdomstidslinje: ArbeidsgiverData.SykdomstidslinjeData,
        val beregnetSykdomstidslinje: ArbeidsgiverData.SykdomstidslinjeData
    ) {
        fun tilDto() = SykdomshistorikkElementDto(
            id = this.id,
            hendelseId = this.hendelseId?.let { MeldingsreferanseDto(it) },
            tidsstempel = this.tidsstempel,
            hendelseSykdomstidslinje = hendelseSykdomstidslinje.tilDto(),
            beregnetSykdomstidslinje = beregnetSykdomstidslinje.tilDto()
        )
    }

    data class UtbetalingData(
        val id: UUID,
        val korrelasjonsId: UUID,
        val fom: LocalDate,
        val tom: LocalDate,
        val annulleringer: List<UUID>?,
        val utbetalingstidslinje: UtbetalingstidslinjeData,
        val arbeidsgiverOppdrag: OppdragData,
        val personOppdrag: OppdragData,
        val tidsstempel: LocalDateTime,
        val type: UtbetalingtypeData,
        val status: UtbetalingstatusData,
        val maksdato: LocalDate,
        val forbrukteSykedager: Int?,
        val gjenståendeSykedager: Int?,
        val vurdering: VurderingData?,
        val overføringstidspunkt: LocalDateTime?,
        val avstemmingsnøkkel: Long?,
        val avsluttet: LocalDateTime?,
        val oppdatert: LocalDateTime
    ) {
        enum class UtbetalingtypeData { UTBETALING, ETTERUTBETALING, ANNULLERING, REVURDERING }
        enum class UtbetalingstatusData {
            NY,
            IKKE_UTBETALT,
            IKKE_GODKJENT,
            OVERFØRT,
            UTBETALT,
            GODKJENT,
            GODKJENT_UTEN_UTBETALING,
            ANNULLERT,
            FORKASTET
        }

        fun tilDto() = UtbetalingInnDto(
            id = this.id,
            korrelasjonsId = this.korrelasjonsId,
            periode = PeriodeDto(fom = this.fom, tom = this.tom),
            utbetalingstidslinje = this.utbetalingstidslinje.tilDto(),
            arbeidsgiverOppdrag = this.arbeidsgiverOppdrag.tilDto(),
            personOppdrag = this.personOppdrag.tilDto(),
            tidsstempel = this.tidsstempel,
            tilstand = when (status) {
                UtbetalingstatusData.NY -> UtbetalingTilstandDto.NY
                UtbetalingstatusData.IKKE_UTBETALT -> UtbetalingTilstandDto.IKKE_UTBETALT
                UtbetalingstatusData.IKKE_GODKJENT -> UtbetalingTilstandDto.IKKE_GODKJENT
                UtbetalingstatusData.OVERFØRT -> UtbetalingTilstandDto.OVERFØRT
                UtbetalingstatusData.UTBETALT -> UtbetalingTilstandDto.UTBETALT
                UtbetalingstatusData.GODKJENT -> UtbetalingTilstandDto.GODKJENT
                UtbetalingstatusData.GODKJENT_UTEN_UTBETALING -> UtbetalingTilstandDto.GODKJENT_UTEN_UTBETALING
                UtbetalingstatusData.ANNULLERT -> UtbetalingTilstandDto.ANNULLERT
                UtbetalingstatusData.FORKASTET -> UtbetalingTilstandDto.FORKASTET
            },
            type = when (type) {
                UtbetalingtypeData.UTBETALING -> UtbetalingtypeDto.UTBETALING
                UtbetalingtypeData.ETTERUTBETALING -> UtbetalingtypeDto.ETTERUTBETALING
                UtbetalingtypeData.ANNULLERING -> UtbetalingtypeDto.ANNULLERING
                UtbetalingtypeData.REVURDERING -> UtbetalingtypeDto.REVURDERING
            },
            maksdato = this.maksdato,
            forbrukteSykedager = this.forbrukteSykedager,
            gjenståendeSykedager = this.gjenståendeSykedager,
            annulleringer = this.annulleringer ?: emptyList(),
            vurdering = this.vurdering?.tilDto(),
            overføringstidspunkt = overføringstidspunkt,
            avstemmingsnøkkel = avstemmingsnøkkel,
            avsluttet = avsluttet,
            oppdatert = oppdatert
        )

        data class VurderingData(
            val godkjent: Boolean,
            val ident: String,
            val epost: String,
            val tidspunkt: LocalDateTime,
            val automatiskBehandling: Boolean
        ) {
            fun tilDto() = UtbetalingVurderingDto(
                godkjent = godkjent,
                ident = ident,
                epost = epost,
                tidspunkt = tidspunkt,
                automatiskBehandling = automatiskBehandling
            )
        }
    }

    data class OppdragData(
        val mottaker: String,
        val fagområde: String,
        val linjer: List<UtbetalingslinjeData>,
        val fagsystemId: String,
        val endringskode: String,
        val tidsstempel: LocalDateTime,
        val nettoBeløp: Int,
        val avstemmingsnøkkel: Long?,
        val status: OppdragstatusData?,
        val overføringstidspunkt: LocalDateTime?,
        val erSimulert: Boolean,
        val simuleringsResultat: ArbeidsgiverData.VedtaksperiodeData.DataForSimuleringData?
    ) {
        enum class OppdragstatusData { OVERFØRT, AKSEPTERT, AKSEPTERT_MED_FEIL, AVVIST, FEIL }

        fun tilDto() = OppdragInnDto(
            mottaker = this.mottaker,
            fagområde = when (fagområde) {
                "SPREF" -> FagområdeDto.SPREF
                "SP" -> FagområdeDto.SP
                else -> error("Ukjent fagområde: $fagområde")
            },
            linjer = this.linjer.map { it.tilDto() },
            fagsystemId = this.fagsystemId,
            endringskode = when (endringskode) {
                "NY" -> EndringskodeDto.NY
                "ENDR" -> EndringskodeDto.ENDR
                "UEND" -> EndringskodeDto.UEND
                else -> error("Ukjent endringskode: $endringskode")
            },
            nettoBeløp = this.nettoBeløp,
            overføringstidspunkt = this.overføringstidspunkt,
            avstemmingsnøkkel = this.avstemmingsnøkkel,
            status = when (status) {
                OppdragstatusData.OVERFØRT -> OppdragstatusDto.OVERFØRT
                OppdragstatusData.AKSEPTERT -> OppdragstatusDto.AKSEPTERT
                OppdragstatusData.AKSEPTERT_MED_FEIL -> OppdragstatusDto.AKSEPTERT_MED_FEIL
                OppdragstatusData.AVVIST -> OppdragstatusDto.AVVIST
                OppdragstatusData.FEIL -> OppdragstatusDto.FEIL
                null -> null
            },
            tidsstempel = this.tidsstempel,
            erSimulert = this.erSimulert,
            simuleringsResultat = this.simuleringsResultat?.tilDto()
        )
    }

    data class UtbetalingslinjeData(
        val fom: LocalDate,
        val tom: LocalDate,
        val sats: Int,
        val grad: Int,
        val refFagsystemId: String?,
        val delytelseId: Int,
        val refDelytelseId: Int?,
        val endringskode: String,
        val klassekode: String,
        val datoStatusFom: LocalDate?
    ) {
        fun tilDto() = UtbetalingslinjeInnDto(
            fom = this.fom,
            tom = this.tom,
            beløp = this.sats,
            grad = this.grad,
            refFagsystemId = this.refFagsystemId,
            delytelseId = this.delytelseId,
            refDelytelseId = this.refDelytelseId,
            endringskode = when (this.endringskode) {
                "NY" -> EndringskodeDto.NY
                "ENDR" -> EndringskodeDto.ENDR
                "UEND" -> EndringskodeDto.UEND
                else -> error("Ukjent endringskode: $endringskode")
            },
            klassekode = when (this.klassekode) {
                "SPREFAG-IOP" -> KlassekodeDto.RefusjonIkkeOpplysningspliktig
                "SPATORD" -> KlassekodeDto.SykepengerArbeidstakerOrdinær
                "SPSND-OP" -> KlassekodeDto.SelvstendigNæringsdrivendeOppgavepliktig
                "SPSNDDM-OP" -> KlassekodeDto.SelvstendigNæringsdrivendeBarnepasserOppgavepliktig
                else -> error("Ukjent klassekode: ${this.klassekode}")
            },
            datoStatusFom = this.datoStatusFom
        )
    }

    data class UtbetalingstidslinjeData(
        val dager: List<UtbetalingsdagData>
    ) {
        fun tilDto() = UtbetalingstidslinjeInnDto(dager = this.dager.flatMap { it.tilDto() })

        enum class BegrunnelseData {
            SykepengedagerOppbrukt,
            SykepengedagerOppbruktOver67,
            MinimumInntekt,
            MinimumInntektOver67,
            EgenmeldingUtenforArbeidsgiverperiode,
            MinimumSykdomsgrad,
            AndreYtelserAap,
            AndreYtelserDagpenger,
            AndreYtelserForeldrepenger,
            AndreYtelserOmsorgspenger,
            AndreYtelserOpplaringspenger,
            AndreYtelserPleiepenger,
            AndreYtelserSvangerskapspenger,
            EtterDødsdato,
            ManglerMedlemskap,
            ManglerOpptjening,
            Over70,
            NyVilkårsprøvingNødvendig;

            fun tilDto() = when (this) {
                SykepengedagerOppbrukt -> BegrunnelseDto.SykepengedagerOppbrukt
                SykepengedagerOppbruktOver67 -> BegrunnelseDto.SykepengedagerOppbruktOver67
                MinimumInntekt -> BegrunnelseDto.MinimumInntekt
                MinimumInntektOver67 -> BegrunnelseDto.MinimumInntektOver67
                EgenmeldingUtenforArbeidsgiverperiode -> BegrunnelseDto.EgenmeldingUtenforArbeidsgiverperiode
                MinimumSykdomsgrad -> BegrunnelseDto.MinimumSykdomsgrad
                AndreYtelserAap -> BegrunnelseDto.AndreYtelserAap
                AndreYtelserDagpenger -> BegrunnelseDto.AndreYtelserDagpenger
                AndreYtelserForeldrepenger -> BegrunnelseDto.AndreYtelserForeldrepenger
                AndreYtelserOmsorgspenger -> BegrunnelseDto.AndreYtelserOmsorgspenger
                AndreYtelserOpplaringspenger -> BegrunnelseDto.AndreYtelserOpplaringspenger
                AndreYtelserPleiepenger -> BegrunnelseDto.AndreYtelserPleiepenger
                AndreYtelserSvangerskapspenger -> BegrunnelseDto.AndreYtelserSvangerskapspenger
                EtterDødsdato -> BegrunnelseDto.EtterDødsdato
                ManglerMedlemskap -> BegrunnelseDto.ManglerMedlemskap
                ManglerOpptjening -> BegrunnelseDto.ManglerOpptjening
                Over70 -> BegrunnelseDto.Over70
                NyVilkårsprøvingNødvendig -> BegrunnelseDto.NyVilkårsprøvingNødvendig
            }
        }

        enum class TypeData {
            ArbeidsgiverperiodeDag,
            NavDag,
            NavHelgDag,
            Arbeidsdag,
            Fridag,
            AvvistDag,
            UkjentDag,
            ForeldetDag,
            ArbeidsgiverperiodedagNav,
            Ventetidsdag
        }

        data class UtbetalingsdagData(
            val type: TypeData,
            val aktuellDagsinntekt: Double,
            val inntektjustering: Double,
            val dekningsgrad: Double,
            val begrunnelser: List<BegrunnelseData>?,
            val grad: Double,
            val totalGrad: Double,
            val utbetalingsgrad: Double,
            val arbeidsgiverRefusjonsbeløp: Double,
            val arbeidsgiverbeløp: Double?,
            val personbeløp: Double?,
            val reservertArbeidsgiverbeløp: Double?,
            val reservertPersonbeløp: Double?,
            val dato: LocalDate?,
            val fom: LocalDate?,
            val tom: LocalDate?
        ) {
            private val datoer = datosekvens(dato, fom, tom)
            private val økonomiDto by lazy {
                val lagretArbeidsgiverbeløp = this.arbeidsgiverbeløp?.let { InntektbeløpDto.DagligDouble(it) }
                val lagretPersonbeløp = this.personbeløp?.let { InntektbeløpDto.DagligDouble(it) }
                ØkonomiInnDto(
                    grad = ProsentdelDto(grad),
                    totalGrad = ProsentdelDto(totalGrad),
                    utbetalingsgrad = ProsentdelDto(utbetalingsgrad),
                    arbeidsgiverRefusjonsbeløp = InntektbeløpDto.DagligDouble(this.arbeidsgiverRefusjonsbeløp),
                    aktuellDagsinntekt = InntektbeløpDto.DagligDouble(this.aktuellDagsinntekt),
                    inntektjustering = InntektbeløpDto.DagligDouble(this.inntektjustering),
                    dekningsgrad = ProsentdelDto(dekningsgrad),
                    arbeidsgiverbeløp = lagretArbeidsgiverbeløp,
                    personbeløp = lagretPersonbeløp,
                    // Disse elvisene kan fjernes når alle personer har blitt oppdatert i databasen
                    // Feltene må være nullable (om ingen finner ut at arbeidsigverbeløp & personbeløp aldri blir lagret ned som null)
                    reservertArbeidsgiverbeløp = this.reservertArbeidsgiverbeløp?.let { InntektbeløpDto.DagligDouble(it) } ?: lagretArbeidsgiverbeløp,
                    reservertPersonbeløp = this.reservertPersonbeløp?.let { InntektbeløpDto.DagligDouble(it) } ?: lagretPersonbeløp
                )
            }

            fun tilDto() = datoer.map { tilDto(it) }
            private fun tilDto(dato: LocalDate): UtbetalingsdagInnDto {
                return when (type) {
                    TypeData.ArbeidsgiverperiodeDag -> ArbeidsgiverperiodeDagDto(dato = dato, økonomi = økonomiDto)
                    TypeData.NavDag -> NavDagDto(dato = dato, økonomi = økonomiDto)
                    TypeData.NavHelgDag -> NavHelgDagDto(dato = dato, økonomi = økonomiDto)
                    TypeData.Arbeidsdag -> ArbeidsdagDto(dato = dato, økonomi = økonomiDto)
                    TypeData.Fridag -> FridagDto(dato = dato, økonomi = økonomiDto)
                    TypeData.AvvistDag -> AvvistDagDto(dato = dato, økonomi = økonomiDto, begrunnelser = begrunnelser!!.map { it.tilDto() })
                    TypeData.UkjentDag -> UkjentDagDto(dato = dato, økonomi = økonomiDto)
                    TypeData.ForeldetDag -> ForeldetDagDto(dato = dato, økonomi = økonomiDto)
                    TypeData.ArbeidsgiverperiodedagNav -> ArbeidsgiverperiodeDagNavDto(dato = dato, økonomi = økonomiDto)
                    TypeData.Ventetidsdag -> VentetidsdagDto(dato = dato, økonomi = økonomiDto)
                }
            }
        }
    }

    data class BeløpstidslinjeData(val perioder: List<BeløpstidslinjeperiodeData>) {
        fun tilDto() = BeløpstidslinjeDto(perioder.map {
            BeløpstidslinjeDto.BeløpstidslinjeperiodeDto(
                fom = it.fom,
                tom = it.tom,
                dagligBeløp = it.dagligBeløp,
                kilde = BeløpstidslinjeDto.BeløpstidslinjedagKildeDto(
                    meldingsreferanseId = it.meldingsreferanseId,
                    avsender = it.avsender.tilDto(),
                    tidsstempel = it.tidsstempel
                )
            )
        })
    }

    data class BeløpstidslinjeperiodeData(val fom: LocalDate, val tom: LocalDate, val dagligBeløp: Double, val meldingsreferanseId: UUID, val avsender: AvsenderData, val tidsstempel: LocalDateTime)
}

private fun LocalDate.erHelg() = dayOfWeek in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

private fun datosekvens(dato: LocalDate?, fom: LocalDate?, tom: LocalDate?): Sequence<LocalDate> {
    check(dato != null || (fom != null && tom != null)) {
        "må ha <dato> eller både <fom> og <tom>. Fikk dato=$dato, fom = $fom, tom = $tom"
    }
    return when {
        dato != null -> sequenceOf(dato)
        else -> fom!!.datesUntil(tom!!.plusDays(1)).asSequence()
    }
}
