package no.nav.helse.dto

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.dto.SpannerPersonDto.ArbeidsgiverData.RefusjonservitørData
import no.nav.helse.dto.SpannerPersonDto.ArbeidsgiverData.SykdomstidslinjeData.DagData
import no.nav.helse.dto.SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.BehandlingData.ArbeidsgiverperiodeData
import no.nav.helse.dto.SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.BehandlingData.ArbeidssituasjonData
import no.nav.helse.dto.SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.BehandlingData.AvsenderData
import no.nav.helse.dto.SpannerPersonDto.UtbetalingData
import no.nav.helse.dto.SpannerPersonDto.UtbetalingstidslinjeData.UtbetalingsdagData
import no.nav.helse.dto.SpannerPersonDto.VilkårsgrunnlagElementData
import no.nav.helse.dto.SpannerPersonDto.VilkårsgrunnlagElementData.ArbeidsgiverInntektsopplysningData
import no.nav.helse.dto.SpannerPersonDto.VilkårsgrunnlagElementData.ArbeidsgiverInntektsopplysningData.InntektsopplysningData.InntektsopplysningstypeData
import no.nav.helse.dto.SpannerPersonDto.VilkårsgrunnlagElementData.SelvstendigInntektsopplysningData
import no.nav.helse.dto.SpannerPersonDto.VilkårsgrunnlagElementData.SelvstendigInntektsopplysningData.InntektsopplysningData.PensjonsgivendeInntektData
import no.nav.helse.dto.SpannerPersonDto.VilkårsgrunnlagInnslagData
import no.nav.helse.dto.deserialisering.ForberedendeVilkårsgrunnlagDto
import no.nav.helse.dto.deserialisering.YrkesaktivitetstypeDto
import no.nav.helse.dto.serialisering.ArbeidsgiverInntektsopplysningUtDto
import no.nav.helse.dto.serialisering.ArbeidsgiverUtDto
import no.nav.helse.dto.serialisering.ArbeidstakerFaktaavklartInntektUtDto
import no.nav.helse.dto.serialisering.ArbeidstakerinntektskildeUtDto
import no.nav.helse.dto.serialisering.BehandlingUtDto
import no.nav.helse.dto.serialisering.BehandlingendringUtDto
import no.nav.helse.dto.serialisering.FeriepengeUtDto
import no.nav.helse.dto.serialisering.FeriepengeoppdragUtDto
import no.nav.helse.dto.serialisering.FeriepengeutbetalingslinjeUtDto
import no.nav.helse.dto.serialisering.ForkastetVedtaksperiodeUtDto
import no.nav.helse.dto.serialisering.InfotrygdArbeidsgiverutbetalingsperiodeUtDto
import no.nav.helse.dto.serialisering.InfotrygdPersonutbetalingsperiodeUtDto
import no.nav.helse.dto.serialisering.InfotrygdhistorikkelementUtDto
import no.nav.helse.dto.serialisering.InntektsgrunnlagUtDto
import no.nav.helse.dto.serialisering.InntektsmeldingUtDto
import no.nav.helse.dto.serialisering.MaksdatoresultatUtDto
import no.nav.helse.dto.serialisering.OppdragUtDto
import no.nav.helse.dto.serialisering.OpptjeningUtDto
import no.nav.helse.dto.serialisering.PersonUtDto
import no.nav.helse.dto.serialisering.SaksbehandlerUtDto
import no.nav.helse.dto.serialisering.SelvstendigFaktaavklartInntektUtDto
import no.nav.helse.dto.serialisering.SkjønnsmessigFastsattUtDto
import no.nav.helse.dto.serialisering.UtbetalingUtDto
import no.nav.helse.dto.serialisering.UtbetalingsdagUtDto
import no.nav.helse.dto.serialisering.UtbetalingslinjeUtDto
import no.nav.helse.dto.serialisering.UtbetalingstidslinjeUtDto
import no.nav.helse.dto.serialisering.UtbetaltDagUtDto
import no.nav.helse.dto.serialisering.VedtaksperiodeUtDto
import no.nav.helse.dto.serialisering.VilkårsgrunnlagInnslagUtDto
import no.nav.helse.dto.serialisering.VilkårsgrunnlagUtDto
import no.nav.helse.nesteDag

data class SpannerPersonDto(
    val fødselsnummer: String,
    val fødselsdato: LocalDate,
    val arbeidsgivere: List<ArbeidsgiverData>,
    val opprettet: LocalDateTime,
    val infotrygdhistorikk: List<InfotrygdhistorikkElementData>,
    val vilkårsgrunnlagHistorikk: List<VilkårsgrunnlagInnslagData>,
    val skjæringstidspunkter: List<ArbeidsgiverData.PeriodeData>,
    val minimumSykdomsgradVurdering: List<MinimumSykdomsgradVurderingPeriode>,
    val dødsdato: LocalDate?
) {
    data class MinimumSykdomsgradVurderingPeriode(val fom: LocalDate, val tom: LocalDate)
    data class InfotrygdhistorikkElementData(
        val id: UUID,
        val tidsstempel: LocalDateTime,
        val hendelseId: UUID,
        val ferieperioder: List<FerieperiodeData>,
        val arbeidsgiverutbetalingsperioder: List<ArbeidsgiverutbetalingsperiodeData>,
        val personutbetalingsperioder: List<PersonutbetalingsperiodeData>,
        val oppdatert: LocalDateTime
    ) {
        data class FerieperiodeData(
            val fom: LocalDate,
            val tom: LocalDate
        )

        data class PersonutbetalingsperiodeData(
            val orgnr: String,
            val fom: LocalDate,
            val tom: LocalDate
        )

        data class ArbeidsgiverutbetalingsperiodeData(
            val orgnr: String,
            val fom: LocalDate,
            val tom: LocalDate
        )
    }

    data class VilkårsgrunnlagInnslagData(
        val id: UUID,
        val opprettet: LocalDateTime,
        val vilkårsgrunnlag: List<VilkårsgrunnlagElementData>
    )

    data class VilkårsgrunnlagElementData(
        val skjæringstidspunkt: LocalDate,
        val type: GrunnlagsdataType,
        val inntektsgrunnlag: InntektsgrunnlagData,
        val opptjening: OpptjeningData?,
        val medlemskapstatus: MedlemskapstatusDto?,
        val meldingsreferanseId: UUID?,
        val vilkårsgrunnlagId: UUID
    ) {
        enum class MedlemskapstatusDto { JA, VET_IKKE, NEI, UAVKLART_MED_BRUKERSPØRSMÅL }
        enum class GrunnlagsdataType { Infotrygd, Vilkårsprøving }

        data class InntektsgrunnlagData(
            val grunnbeløp: Double?,
            val arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysningData>,
            val deaktiverteArbeidsforhold: List<ArbeidsgiverInntektsopplysningData>,
            val vurdertInfotrygd: Boolean,
            val totalOmregnetÅrsinntekt: InntektbeløpDto.Årlig,
            val beregningsgrunnlag: InntektbeløpDto.Årlig,
            val er6GBegrenset: Boolean
        )

        data class ArbeidsgiverInntektsopplysningData(
            val orgnummer: String,
            val faktaavklartInntekt: InntektsopplysningData,
            val korrigertInntekt: KorrigertInntektsopplysningData?,
            val skjønnsmessigFastsatt: SkjønnsmessigFastsattData?
        ) {
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
            }

            data class InntektsopplysningData(
                val id: UUID,
                val dato: LocalDate,
                val hendelseId: UUID,
                val beløp: InntektDto?,
                val kilde: String?,
                val type: InntektsopplysningstypeData,
                val tidsstempel: LocalDateTime,
                val skatteopplysninger: List<SkatteopplysningData>?,
                val pensjonsgivendeInntekter: List<PensjonsgivendeInntektData>?,
                val anvendtGrunnbeløp: InntektDto?
            ) {
                enum class InntektsopplysningstypeData {
                    ARBEIDSTAKER,
                    SELVSTENDIG
                }

                data class PensjonsgivendeInntektData(val årstall: Year, val beløp: InntektDto)
            }

            data class KorrigertInntektsopplysningData(
                val id: UUID,
                val dato: LocalDate,
                val hendelseId: UUID,
                val beløp: InntektDto?,
                val tidsstempel: LocalDateTime
            )

            data class SkjønnsmessigFastsattData(
                val id: UUID,
                val dato: LocalDate,
                val hendelseId: UUID,
                val beløp: InntektDto?,
                val tidsstempel: LocalDateTime
            )
        }

        data class OpptjeningData(
            val opptjeningFom: LocalDate,
            val opptjeningTom: LocalDate,
            val arbeidsforhold: List<ArbeidsgiverOpptjeningsgrunnlagData>
        ) {
            data class ArbeidsgiverOpptjeningsgrunnlagData(
                val orgnummer: String,
                val ansattPerioder: List<ArbeidsforholdData>
            ) {
                data class ArbeidsforholdData(
                    val ansattFom: LocalDate,
                    val ansattTom: LocalDate?,
                    val deaktivert: Boolean
                )
            }
        }

        data class SelvstendigInntektsopplysningData(
            val faktaavklartInntekt: InntektsopplysningData,
            val skjønnsmessigFastsatt: SkjønnsmessigFastsattData?
        ) {
            data class InntektsopplysningData(
                val id: UUID,
                val dato: LocalDate,
                val hendelseId: UUID,
                val beløp: InntektDto?,
                val tidsstempel: LocalDateTime,
                val pensjonsgivendeInntekter: List<PensjonsgivendeInntektData>,
                val anvendtGrunnbeløp: InntektDto
            ) {
                data class PensjonsgivendeInntektData(val årstall: Year, val beløp: InntektDto)
            }

            data class SkjønnsmessigFastsattData( // holder denne separat fra ArbeidsgiverInntektsopplysningData for vi har foreløpig ingen god grunn til å slå dem sammen
                val id: UUID,
                val dato: LocalDate,
                val hendelseId: UUID,
                val beløp: InntektDto?,
                val tidsstempel: LocalDateTime
            )
        }
    }

    data class ForberedendeVilkårsgrunnlagData(val erOpptjeningVurdertOk: Boolean)

    data class InntektDto(
        val årlig: Double,
        val månedligDouble: Double,
        val dagligDouble: Double,
        val dagligInt: Int
    )

    data class ArbeidsgiverData(
        val organisasjonsnummer: String,
        val yrkesaktivitetstype: YrkesaktivitetstypeData,
        val id: UUID,
        val inntektshistorikk: List<InntektsmeldingData>,
        val sykdomshistorikk: List<SykdomshistorikkData>,
        val sykmeldingsperioder: List<SykmeldingsperiodeData>,
        val arbeidsgiverperioder: List<ArbeidsgiverperioderesultatData>,
        val vedtaksperioder: List<VedtaksperiodeData>,
        val forkastede: List<ForkastetVedtaksperiodeData>,
        val utbetalinger: List<UtbetalingData>,
        val feriepengeutbetalinger: List<FeriepengeutbetalingData>,
        val ubrukteRefusjonsopplysninger: RefusjonservitørData
    ) {
        enum class YrkesaktivitetstypeData {
            ARBEIDSTAKER,
            ARBEIDSLEDIG,
            FRILANS,
            SELVSTENDIG
        }

        data class InntektsmeldingData(
            val id: UUID,
            val dato: LocalDate,
            val hendelseId: UUID,
            val beløp: InntektDto,
            val tidsstempel: LocalDateTime,
            val kilde: KildeData
        ) {
            enum class KildeData {
                Arbeidsgiver, AOrdningen
            }
        }

        data class PeriodeData(val fom: LocalDate, val tom: LocalDate)
        data class SykdomstidslinjeData(
            val dager: List<DagData>,
            val periode: PeriodeData?,
            val låstePerioder: List<PeriodeData>?
        ) {
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
                val gradSomProsent = grad * 100

                init {
                    check(dato != null || (fom != null && tom != null)) {
                        "enten må dato være satt eller så må både fom og tom være satt"
                    }
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
                SYKEDAG_NAV,
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
            )
        }

        data class ForkastetVedtaksperiodeData(
            val vedtaksperiode: VedtaksperiodeData
        )

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
            data class UtbetaltDagData(
                val type: String,
                val orgnummer: String,
                val dato: LocalDate,
                val beløp: Int,
            )

            data class OppdragData(
                val mottaker: String,
                val fagområde: String,
                val linjer: List<UtbetalingslinjeData>,
                val fagsystemId: String,
                val endringskode: String,
                val tidsstempel: LocalDateTime
            ) {
                data class UtbetalingslinjeData(
                    val fom: LocalDate,
                    val tom: LocalDate,
                    val sats: Int,
                    val refFagsystemId: String?,
                    val delytelseId: Int,
                    val refDelytelseId: Int?,
                    val endringskode: String,
                    val klassekode: String,
                    val datoStatusFom: LocalDate?,
                    val statuskode: String?
                )
            }
        }

        data class ArbeidsgiverperioderesultatData(
            val omsluttendePeriode: PeriodeData,
            val arbeidsgiverperiode: List<PeriodeData>,
            val ferdigAvklart: Boolean
        )

        data class SykmeldingsperiodeData(
            val fom: LocalDate,
            val tom: LocalDate
        )

        data class Gjeldende(
            val skjæringstidspunkt: LocalDate?,
            val sykdomstidslinje: List<DagData>,
            val refusjonstidslinje: List<BeløpstidslinjeperiodeData>,
            val inntektsendringer: List<BeløpstidslinjeperiodeData>,
            val refusjonstidslinjeHensyntattUbrukteRefusjonsopplysninger: List<BeløpstidslinjeperiodeData>,
            val dagerNavOvertarAnsvar: List<PeriodeData>,
            val egenmeldingsdager: List<PeriodeData>,
            val utbetalingstidslinje: List<Any>,
            val arbeidsgiverperiode: ArbeidsgiverperiodeData,
            val ventetid: PeriodeData?,
            val arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysningData>,
            val forbrukteDager: Long,
            val gjenståendeDager: Int,
            val maksdato: LocalDate,
            val arbeidsgiverOppdrag: OppdragData?,
            val personOppdrag: OppdragData?,
            val inntektjusteringer: Map<String, BeløpstidslinjeData>
        )

        data class VedtaksperiodeData(
            val id: UUID,
            val tilstand: TilstandType,
            val skjæringstidspunkt: LocalDate,
            val fom: LocalDate,
            val tom: LocalDate,
            val sykmeldingFom: LocalDate,
            val sykmeldingTom: LocalDate,
            val behandlinger: List<BehandlingData>,
            val venteårsak: VedtaksperiodeVenterDto?,
            val opprettet: LocalDateTime,
            val oppdatert: LocalDateTime,
            private val vilkårsgrunnlag: VilkårsgrunnlagElementData?,
            private val utbetaling: UtbetalingData?,
            private val sisteBehandlingId: UUID?,
            private val sisteRefusjonstidslinje: BeløpstidslinjeData?,
            val annulleringskandidater: List<AnnulleringskandidatDto>
        ) {
            @Suppress("unused", "Denne vises i Spanner sånn at man slipper å gå på siste behandling på siste endring")
            val gjeldende = behandlinger.last().let { behandling ->
                behandling.endringer.last().let { gjeldendeEndring ->
                    Gjeldende(
                        skjæringstidspunkt = skjæringstidspunkt,
                        arbeidsgiverperiode = gjeldendeEndring.arbeidsgiverperiode,
                        ventetid = gjeldendeEndring.ventetid,
                        refusjonstidslinje = gjeldendeEndring.refusjonstidslinje.perioder,
                        inntektsendringer = gjeldendeEndring.inntektsendringer.perioder,
                        refusjonstidslinjeHensyntattUbrukteRefusjonsopplysninger = if (behandling.id == sisteBehandlingId) sisteRefusjonstidslinje!!.perioder else emptyList(),
                        utbetalingstidslinje = gjeldendeEndring.utbetalingstidslinje.dager,
                        forbrukteDager = gjeldendeEndring.maksdatoresultat.forbrukteDagerAntall,
                        gjenståendeDager = gjeldendeEndring.maksdatoresultat.gjenståendeDager,
                        maksdato = gjeldendeEndring.maksdatoresultat.maksdato,
                        sykdomstidslinje = gjeldendeEndring.sykdomstidslinje.dager,
                        arbeidsgiverInntektsopplysninger = vilkårsgrunnlag?.inntektsgrunnlag?.arbeidsgiverInntektsopplysninger ?: emptyList(),
                        personOppdrag = utbetaling?.personOppdrag?.takeUnless { it.linjer.isEmpty() },
                        arbeidsgiverOppdrag = utbetaling?.arbeidsgiverOppdrag?.takeUnless { it.linjer.isEmpty() },
                        dagerNavOvertarAnsvar = gjeldendeEndring.dagerNavOvertarAnsvar,
                        egenmeldingsdager = gjeldendeEndring.egenmeldingsdager,
                        inntektjusteringer = gjeldendeEndring.inntektjusteringer
                    )
                }
            }

            enum class TilstandType {
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
                SELVSTENDIG_AVSLUTTET
            }

            data class AnnulleringskandidatDto(
                val vedtaksperiodeId: UUID,
                val organisasjonsnummer: String,
                val fom: LocalDate,
                val tom: LocalDate
            )

            data class VedtaksperiodeVenterDto(
                val ventetSiden: LocalDateTime,
                val venterTil: LocalDateTime,
                val venterPå: VenterPåDto,
            )

            data class VenterPåDto(
                val vedtaksperiodeId: UUID,
                val organisasjonsnummer: String,
                val venteårsak: VenteårsakDto
            )

            data class VenteårsakDto(
                val hva: String,
                val hvorfor: String?
            )

            enum class MaksdatobestemmelseDto {
                IKKE_VURDERT, ORDINÆR_RETT, BEGRENSET_RETT, SYTTI_ÅR
            }

            data class MaksdatoresultatData(
                val vurdertTilOgMed: LocalDate,
                val bestemmelse: MaksdatobestemmelseDto,
                val startdatoTreårsvindu: LocalDate,
                val startdatoSykepengerettighet: LocalDate?,
                val forbrukteDager: List<PeriodeData>,
                val oppholdsdager: List<PeriodeData>,
                val avslåtteDager: List<PeriodeData>,
                val maksdato: LocalDate,
                val gjenståendeDager: Int
            ) {
                val forbrukteDagerAntall = forbrukteDager.sumOf { it.fom.datesUntil(it.tom).count() + 1 }
            }

            data class DokumentsporingData(
                val dokumentId: UUID,
                val dokumenttype: DokumentTypeData
            )

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
                AndreYtelser,
                System
            }

            data class BehandlingData(
                val id: UUID,
                val tilstand: TilstandData,
                val vedtakFattet: LocalDateTime?,
                val avsluttet: LocalDateTime?,
                val kilde: KildeData,
                val endringer: List<EndringData>,
            ) {
                enum class TilstandData {
                    UBEREGNET, UBEREGNET_OMGJØRING, UBEREGNET_REVURDERING, BEREGNET, BEREGNET_OMGJØRING, BEREGNET_REVURDERING,
                    VEDTAK_FATTET, REVURDERT_VEDTAK_AVVIST, VEDTAK_IVERKSATT, AVSLUTTET_UTEN_VEDTAK, ANNULLERT_PERIODE, TIL_INFOTRYGD,
                    UBEREGNET_ANNULLERING, BEREGNET_ANNULLERING, OVERFØRT_ANNULLERING
                }

                enum class AvsenderData {
                    SYKMELDT, ARBEIDSGIVER, SAKSBEHANDLER, SYSTEM
                }

                data class KildeData(
                    val meldingsreferanseId: UUID,
                    val innsendt: LocalDateTime,
                    val registrert: LocalDateTime,
                    val avsender: AvsenderData
                )

                data class EndringData(
                    val id: UUID,
                    val tidsstempel: LocalDateTime,
                    val arbeidssituasjon: ArbeidssituasjonData,
                    val sykmeldingsperiodeFom: LocalDate,
                    val sykmeldingsperiodeTom: LocalDate,
                    val fom: LocalDate,
                    val tom: LocalDate,
                    val utbetalingId: UUID?,
                    val skjæringstidspunkt: LocalDate?,
                    val skjæringstidspunkter: List<LocalDate>,
                    val utbetalingstatus: UtbetalingData.UtbetalingstatusData?,
                    val vilkårsgrunnlagId: UUID?,
                    val sykdomstidslinje: SykdomstidslinjeData,
                    val utbetalingstidslinje: UtbetalingstidslinjeData,
                    val refusjonstidslinje: BeløpstidslinjeData,
                    val inntektsendringer: BeløpstidslinjeData,
                    val dokumentsporing: DokumentsporingData,
                    val arbeidsgiverperiode: ArbeidsgiverperiodeData,
                    val ventetid: PeriodeData?,
                    val dagerNavOvertarAnsvar: List<PeriodeData>,
                    val egenmeldingsdager: List<PeriodeData>,
                    val maksdatoresultat: MaksdatoresultatData,
                    val inntektjusteringer: Map<String, BeløpstidslinjeData>,
                    val faktaavklartInntekt: SelvstendigInntektsopplysningData.InntektsopplysningData?,
                    val forberedendeVilkårsgrunnlag: ForberedendeVilkårsgrunnlagData?
                )

                data class ArbeidsgiverperiodeData(
                    val ferdigAvklart: Boolean,
                    val dager: List<PeriodeData>
                )
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
            }

            data class DataForSimuleringData(
                val totalbeløp: Int,
                val perioder: List<SimulertPeriode>
            ) {
                data class SimulertPeriode(
                    val fom: LocalDate,
                    val tom: LocalDate,
                    val utbetalinger: List<SimulertUtbetaling>
                )

                data class SimulertUtbetaling(
                    val forfallsdato: LocalDate,
                    val utbetalesTil: Mottaker,
                    val feilkonto: Boolean,
                    val detaljer: List<Detaljer>
                )

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
                )

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

        data class RefusjonservitørData(val refusjonstidslinjer: Map<LocalDate, BeløpstidslinjeData>)
    }

    data class SykdomshistorikkData(
        val tidsstempel: LocalDateTime,
        val id: UUID,
        val hendelseId: UUID?,
        val hendelseSykdomstidslinje: ArbeidsgiverData.SykdomstidslinjeData,
        val beregnetSykdomstidslinje: ArbeidsgiverData.SykdomstidslinjeData
    )

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

        data class VurderingData(
            val godkjent: Boolean,
            val ident: String,
            val epost: String,
            val tidspunkt: LocalDateTime,
            val automatiskBehandling: Boolean
        )
    }

    data class OppdragData(
        val mottaker: String,
        val fagområde: String,
        val linjer: List<UtbetalingslinjeData>,
        val fagsystemId: String,
        val endringskode: String,
        val tidsstempel: LocalDateTime,
        val nettoBeløp: Int,
        val stønadsdager: Int,
        val totalbeløp: Int,
        val avstemmingsnøkkel: Long?,
        val status: OppdragstatusData?,
        val overføringstidspunkt: LocalDateTime?,
        val erSimulert: Boolean,
        val simuleringsResultat: ArbeidsgiverData.VedtaksperiodeData.DataForSimuleringData?
    ) {
        enum class OppdragstatusData { OVERFØRT, AKSEPTERT, AKSEPTERT_MED_FEIL, AVVIST, FEIL }
    }

    data class UtbetalingslinjeData(
        val fom: LocalDate,
        val tom: LocalDate,
        val sats: Int,
        val grad: Int,
        val stønadsdager: Int,
        val totalbeløp: Int,
        val refFagsystemId: String?,
        val delytelseId: Int,
        val refDelytelseId: Int?,
        val endringskode: String,
        val klassekode: String,
        val datoStatusFom: LocalDate?,
        val statuskode: String?
    )

    data class UtbetalingstidslinjeData(
        val dager: List<UtbetalingsdagData>
    ) {
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
            NyVilkårsprøvingNødvendig
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
            val dekingsgrad: Double,
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
            val gradSomProsent = grad * 100
            val totalGradSomProsent = totalGrad * 100
            val utbetalingsgradSomProsent = utbetalingsgrad * 100
            val dekingsgradSomProsent = dekingsgrad * 100
        }
    }

    data class BeløpstidslinjeData(val perioder: List<BeløpstidslinjeperiodeData>)
    data class BeløpstidslinjeperiodeData(val fom: LocalDate, val tom: LocalDate, val dagligBeløp: Double, val meldingsreferanseId: UUID, val avsender: AvsenderData, val tidsstempel: LocalDateTime)
}

fun PersonUtDto.tilSpannerPersonDto(): SpannerPersonDto {
    val vilkårsgrunnlagHistorikkDto = vilkårsgrunnlagHistorikk.historikk.map { it.tilPersonData() }
    return SpannerPersonDto(
        fødselsdato = this.alder.fødselsdato,
        fødselsnummer = this.fødselsnummer,
        opprettet = this.opprettet,
        arbeidsgivere = this.arbeidsgivere.map { it.tilPersonData(vilkårsgrunnlagHistorikkDto) },
        infotrygdhistorikk = this.infotrygdhistorikk.elementer.map { it.tilPersonData() },
        vilkårsgrunnlagHistorikk = vilkårsgrunnlagHistorikkDto,
        skjæringstidspunkter = this.skjæringstidspunkter.map { SpannerPersonDto.ArbeidsgiverData.PeriodeData(it.fom, it.tom) },
        minimumSykdomsgradVurdering = minimumSykdomsgradVurdering.perioder.map { SpannerPersonDto.MinimumSykdomsgradVurderingPeriode(it.fom, it.tom) },
        dødsdato = this.alder.dødsdato
    )
}

private fun ArbeidsgiverUtDto.tilPersonData(vilkårsgrunnlagHistorikk: List<VilkårsgrunnlagInnslagData>): SpannerPersonDto.ArbeidsgiverData {
    val utbetalingerDto = this.utbetalinger.map { it.tilPersonData() }
    return SpannerPersonDto.ArbeidsgiverData(
        id = this.id,
        organisasjonsnummer = this.organisasjonsnummer,
        yrkesaktivitetstype = when (this.yrkesaktivitetstype) {
            YrkesaktivitetstypeDto.ARBEIDSTAKER -> SpannerPersonDto.ArbeidsgiverData.YrkesaktivitetstypeData.ARBEIDSTAKER
            YrkesaktivitetstypeDto.ARBEIDSLEDIG -> SpannerPersonDto.ArbeidsgiverData.YrkesaktivitetstypeData.ARBEIDSLEDIG
            YrkesaktivitetstypeDto.FRILANS -> SpannerPersonDto.ArbeidsgiverData.YrkesaktivitetstypeData.FRILANS
            YrkesaktivitetstypeDto.SELVSTENDIG -> SpannerPersonDto.ArbeidsgiverData.YrkesaktivitetstypeData.SELVSTENDIG
        },
        inntektshistorikk = this.inntektshistorikk.historikk.map { it.tilPersonData() },
        sykdomshistorikk = this.sykdomshistorikk.elementer.map { it.tilPersonData() },
        sykmeldingsperioder = this.sykmeldingsperioder.tilPersonData(),
        arbeidsgiverperioder = this.arbeidsgiverperioder.map { it.tilPersonData() },
        vedtaksperioder = this.vedtaksperioder.map { vedtaksperiode ->
            val gjeldendeEndring = vedtaksperiode.behandlinger.behandlinger.last().endringer.last()
            val vilkårsgrunnlag = gjeldendeEndring.vilkårsgrunnlagId?.let { vilkårsgrunnlagId -> vilkårsgrunnlagHistorikk.flatMap { it.vilkårsgrunnlag }.first { it.vilkårsgrunnlagId == vilkårsgrunnlagId } }
            val utbetaling = gjeldendeEndring.utbetalingId?.let { utbetalingId -> utbetalingerDto.first { it.id == utbetalingId } }
            vedtaksperiode.tilPersonData(vilkårsgrunnlag, utbetaling, ubrukteRefusjonsopplysninger.sisteBehandlingId, ubrukteRefusjonsopplysninger.sisteRefusjonstidslinje?.tilPersonData())
        },
        forkastede = this.forkastede.map { it.tilPersonData() },
        utbetalinger = utbetalingerDto,
        feriepengeutbetalinger = this.feriepengeutbetalinger.map { it.tilPersonData() },
        ubrukteRefusjonsopplysninger = RefusjonservitørData(this.ubrukteRefusjonsopplysninger.ubrukteRefusjonsopplysninger.refusjonstidslinjer.mapValues { (_, dto) -> dto.tilPersonData() })
    )
}

private fun ArbeidsgiverperioderesultatDto.tilPersonData() = SpannerPersonDto.ArbeidsgiverData.ArbeidsgiverperioderesultatData(
    omsluttendePeriode = SpannerPersonDto.ArbeidsgiverData.PeriodeData(this.omsluttendePeriode.fom, this.omsluttendePeriode.tom),
    arbeidsgiverperiode = this.arbeidsgiverperiode.map { SpannerPersonDto.ArbeidsgiverData.PeriodeData(it.fom, it.tom) },
    ferdigAvklart = this.ferdigAvklart
)

private fun InntektDto.tilPersonData() = SpannerPersonDto.InntektDto(
    årlig = this.årlig.beløp,
    månedligDouble = this.månedligDouble.beløp,
    dagligDouble = this.dagligDouble.beløp,
    dagligInt = this.dagligInt.beløp
)

private fun InntektsmeldingUtDto.tilPersonData() =
    SpannerPersonDto.ArbeidsgiverData.InntektsmeldingData(
        id = this.id,
        dato = this.inntektsdata.dato,
        hendelseId = this.inntektsdata.hendelseId.id,
        beløp = this.inntektsdata.beløp.tilPersonData(),
        tidsstempel = this.inntektsdata.tidsstempel,
        kilde = when (this.kilde) {
            InntektsmeldingUtDto.KildeDto.Arbeidsgiver -> SpannerPersonDto.ArbeidsgiverData.InntektsmeldingData.KildeData.Arbeidsgiver
            InntektsmeldingUtDto.KildeDto.AOrdningen -> SpannerPersonDto.ArbeidsgiverData.InntektsmeldingData.KildeData.AOrdningen
        }
    )

private fun SykdomshistorikkElementDto.tilPersonData() = SpannerPersonDto.SykdomshistorikkData(
    id = this.id,
    tidsstempel = this.tidsstempel,
    hendelseId = this.hendelseId?.id,
    hendelseSykdomstidslinje = this.hendelseSykdomstidslinje.tilPersonData(),
    beregnetSykdomstidslinje = this.beregnetSykdomstidslinje.tilPersonData(),
)

private fun SykdomstidslinjeDto.tilPersonData() =
    SpannerPersonDto.ArbeidsgiverData.SykdomstidslinjeData(
        dager = dager.map { it.tilPersonData() }.forkortSykdomstidslinje(),
        låstePerioder = this.låstePerioder.map {
            SpannerPersonDto.ArbeidsgiverData.PeriodeData(
                it.fom,
                it.tom
            )
        },
        periode = this.periode?.let { SpannerPersonDto.ArbeidsgiverData.PeriodeData(it.fom, it.tom) }
    )

private fun List<DagData>.forkortSykdomstidslinje(): List<DagData> {
    return this.fold(emptyList()) { result, neste ->
        val slåttSammen = result.lastOrNull()?.utvideMed(neste) ?: return@fold result + neste
        result.dropLast(1) + slåttSammen
    }
}

private fun DagData.utvideMed(other: DagData): DagData? {
    if (!kanUtvidesMed(other)) return null
    val otherDato = checkNotNull(other.dato) { "dato må være satt" }
    if (this.dato != null) {
        return this.copy(dato = null, fom = dato, tom = otherDato)
    }
    return this.copy(tom = other.dato)
}

private fun DagData.kanUtvidesMed(other: DagData): Boolean {
    // alle verdier må være like (untatt datoene)
    val utenDatoer = { dag: DagData -> dag.copy(fom = null, tom = null, dato = LocalDate.EPOCH) }
    return utenDatoer(this) == utenDatoer(other) && (dato ?: tom!!).nesteDag == other.dato
}

private fun SykdomstidslinjeDagDto.tilPersonData() = when (this) {
    is SykdomstidslinjeDagDto.UkjentDagDto -> DagData(
        type = SpannerPersonDto.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.UKJENT_DAG,
        kilde = this.kilde.tilPersonData(),
        grad = 0.0,
        other = null,
        melding = null,
        dato = dato,
        fom = null,
        tom = null
    )

    is SykdomstidslinjeDagDto.AndreYtelserDto -> DagData(
        type = when (this.ytelse) {
            SykdomstidslinjeDagDto.AndreYtelserDto.YtelseDto.AAP -> SpannerPersonDto.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.ANDRE_YTELSER_AAP
            SykdomstidslinjeDagDto.AndreYtelserDto.YtelseDto.Foreldrepenger -> SpannerPersonDto.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.ANDRE_YTELSER_FORELDREPENGER
            SykdomstidslinjeDagDto.AndreYtelserDto.YtelseDto.Omsorgspenger -> SpannerPersonDto.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.ANDRE_YTELSER_OMSORGSPENGER
            SykdomstidslinjeDagDto.AndreYtelserDto.YtelseDto.Pleiepenger -> SpannerPersonDto.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.ANDRE_YTELSER_PLEIEPENGER
            SykdomstidslinjeDagDto.AndreYtelserDto.YtelseDto.Svangerskapspenger -> SpannerPersonDto.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.ANDRE_YTELSER_SVANGERSKAPSPENGER
            SykdomstidslinjeDagDto.AndreYtelserDto.YtelseDto.Opplæringspenger -> SpannerPersonDto.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.ANDRE_YTELSER_OPPLÆRINGSPENGER
            SykdomstidslinjeDagDto.AndreYtelserDto.YtelseDto.Dagpenger -> SpannerPersonDto.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.ANDRE_YTELSER_DAGPENGER
        },
        kilde = this.kilde.tilPersonData(),
        grad = 0.0,
        other = null,
        melding = null,
        dato = dato,
        fom = null,
        tom = null
    )

    is SykdomstidslinjeDagDto.ArbeidIkkeGjenopptattDagDto -> DagData(
        type = SpannerPersonDto.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.ARBEID_IKKE_GJENOPPTATT_DAG,
        kilde = this.kilde.tilPersonData(),
        grad = 0.0,
        other = null,
        melding = null,
        dato = dato,
        fom = null,
        tom = null
    )

    is SykdomstidslinjeDagDto.ArbeidsdagDto -> DagData(
        type = SpannerPersonDto.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.ARBEIDSDAG,
        kilde = this.kilde.tilPersonData(),
        grad = 0.0,
        other = null,
        melding = null,
        dato = dato,
        fom = null,
        tom = null
    )

    is SykdomstidslinjeDagDto.ArbeidsgiverHelgedagDto -> DagData(
        type = SpannerPersonDto.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.ARBEIDSGIVERDAG,
        kilde = this.kilde.tilPersonData(),
        grad = this.grad.prosentDesimal,
        other = null,
        melding = null,
        dato = dato,
        fom = null,
        tom = null
    )

    is SykdomstidslinjeDagDto.ArbeidsgiverdagDto -> DagData(
        type = SpannerPersonDto.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.ARBEIDSGIVERDAG,
        kilde = this.kilde.tilPersonData(),
        grad = this.grad.prosentDesimal,
        other = null,
        melding = null,
        dato = dato,
        fom = null,
        tom = null
    )

    is SykdomstidslinjeDagDto.FeriedagDto -> DagData(
        type = SpannerPersonDto.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.FERIEDAG,
        kilde = this.kilde.tilPersonData(),
        grad = 0.0,
        other = null,
        melding = null,
        dato = dato,
        fom = null,
        tom = null
    )

    is SykdomstidslinjeDagDto.ForeldetSykedagDto -> DagData(
        type = SpannerPersonDto.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.FORELDET_SYKEDAG,
        kilde = this.kilde.tilPersonData(),
        grad = this.grad.prosentDesimal,
        other = null,
        melding = null,
        dato = dato,
        fom = null,
        tom = null
    )

    is SykdomstidslinjeDagDto.FriskHelgedagDto -> DagData(
        type = SpannerPersonDto.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.FRISK_HELGEDAG,
        kilde = this.kilde.tilPersonData(),
        grad = 0.0,
        other = null,
        melding = null,
        dato = dato,
        fom = null,
        tom = null
    )

    is SykdomstidslinjeDagDto.PermisjonsdagDto -> DagData(
        type = SpannerPersonDto.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.PERMISJONSDAG,
        kilde = this.kilde.tilPersonData(),
        grad = 0.0,
        other = null,
        melding = null,
        dato = dato,
        fom = null,
        tom = null
    )

    is SykdomstidslinjeDagDto.ProblemDagDto -> DagData(
        type = SpannerPersonDto.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.PROBLEMDAG,
        kilde = this.kilde.tilPersonData(),
        grad = 0.0,
        other = this.other.tilPersonData(),
        melding = this.melding,
        dato = dato,
        fom = null,
        tom = null
    )

    is SykdomstidslinjeDagDto.SykHelgedagDto -> DagData(
        type = SpannerPersonDto.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.SYKEDAG,
        kilde = this.kilde.tilPersonData(),
        grad = this.grad.prosentDesimal,
        other = null,
        melding = null,
        dato = dato,
        fom = null,
        tom = null
    )

    is SykdomstidslinjeDagDto.SykedagDto -> DagData(
        type = SpannerPersonDto.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.SYKEDAG,
        kilde = this.kilde.tilPersonData(),
        grad = this.grad.prosentDesimal,
        other = null,
        melding = null,
        dato = dato,
        fom = null,
        tom = null
    )
}

private fun HendelseskildeDto.tilPersonData() =
    SpannerPersonDto.ArbeidsgiverData.SykdomstidslinjeData.KildeData(
        type = this.type,
        id = this.meldingsreferanseId.id,
        tidsstempel = this.tidsstempel
    )

private fun SykmeldingsperioderDto.tilPersonData() = perioder.map {
    SpannerPersonDto.ArbeidsgiverData.SykmeldingsperiodeData(it.fom, it.tom)
}

private fun ForkastetVedtaksperiodeUtDto.tilPersonData() =
    SpannerPersonDto.ArbeidsgiverData.ForkastetVedtaksperiodeData(
        vedtaksperiode = this.vedtaksperiode.tilPersonData()
    )

private fun VedtaksperiodeUtDto.tilPersonData(
    vilkårsgrunnlag: VilkårsgrunnlagElementData? = null,
    utbetaling: UtbetalingData? = null,
    sisteBehandlingId: UUID? = null,
    sisteRefusjonstidslinje: SpannerPersonDto.BeløpstidslinjeData? = null
) = SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData(
    id = id,
    tilstand = when (tilstand) {
        VedtaksperiodetilstandDto.AVSLUTTET -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.TilstandType.AVSLUTTET
        VedtaksperiodetilstandDto.AVSLUTTET_UTEN_UTBETALING -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.TilstandType.AVSLUTTET_UTEN_UTBETALING
        VedtaksperiodetilstandDto.AVVENTER_BLOKKERENDE_PERIODE -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
        VedtaksperiodetilstandDto.AVVENTER_A_ORDNINGEN -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.TilstandType.AVVENTER_A_ORDNINGEN
        VedtaksperiodetilstandDto.AVVENTER_GODKJENNING -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.TilstandType.AVVENTER_GODKJENNING
        VedtaksperiodetilstandDto.AVVENTER_GODKJENNING_REVURDERING -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.TilstandType.AVVENTER_GODKJENNING_REVURDERING
        VedtaksperiodetilstandDto.AVVENTER_HISTORIKK -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.TilstandType.AVVENTER_HISTORIKK
        VedtaksperiodetilstandDto.AVVENTER_HISTORIKK_REVURDERING -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.TilstandType.AVVENTER_HISTORIKK_REVURDERING
        VedtaksperiodetilstandDto.AVVENTER_INFOTRYGDHISTORIKK -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.TilstandType.AVVENTER_INFOTRYGDHISTORIKK
        VedtaksperiodetilstandDto.AVVENTER_INNTEKTSMELDING -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.TilstandType.AVVENTER_INNTEKTSMELDING
        VedtaksperiodetilstandDto.AVVENTER_REVURDERING -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.TilstandType.AVVENTER_REVURDERING
        VedtaksperiodetilstandDto.AVVENTER_SIMULERING -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.TilstandType.AVVENTER_SIMULERING
        VedtaksperiodetilstandDto.AVVENTER_SIMULERING_REVURDERING -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.TilstandType.AVVENTER_SIMULERING_REVURDERING
        VedtaksperiodetilstandDto.AVVENTER_VILKÅRSPRØVING -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.TilstandType.AVVENTER_VILKÅRSPRØVING
        VedtaksperiodetilstandDto.AVVENTER_VILKÅRSPRØVING_REVURDERING -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.TilstandType.AVVENTER_VILKÅRSPRØVING_REVURDERING
        VedtaksperiodetilstandDto.REVURDERING_FEILET -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.TilstandType.REVURDERING_FEILET
        VedtaksperiodetilstandDto.START -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.TilstandType.START
        VedtaksperiodetilstandDto.TIL_INFOTRYGD -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.TilstandType.TIL_INFOTRYGD
        VedtaksperiodetilstandDto.TIL_UTBETALING -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.TilstandType.TIL_UTBETALING
        VedtaksperiodetilstandDto.AVVENTER_ANNULLERING -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.TilstandType.AVVENTER_ANNULLERING
        VedtaksperiodetilstandDto.TIL_ANNULLERING -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.TilstandType.TIL_ANNULLERING

        VedtaksperiodetilstandDto.SELVSTENDIG_AVSLUTTET -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.TilstandType.SELVSTENDIG_AVSLUTTET
        VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_BLOKKERENDE_PERIODE -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.TilstandType.SELVSTENDIG_AVVENTER_BLOKKERENDE_PERIODE
        VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_GODKJENNING -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.TilstandType.SELVSTENDIG_AVVENTER_GODKJENNING
        VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_HISTORIKK -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.TilstandType.SELVSTENDIG_AVVENTER_HISTORIKK
        VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_INFOTRYGDHISTORIKK -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.TilstandType.SELVSTENDIG_AVVENTER_INFOTRYGDHISTORIKK
        VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_SIMULERING -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.TilstandType.SELVSTENDIG_AVVENTER_SIMULERING
        VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_VILKÅRSPRØVING -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.TilstandType.SELVSTENDIG_AVVENTER_VILKÅRSPRØVING
        VedtaksperiodetilstandDto.SELVSTENDIG_START -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.TilstandType.SELVSTENDIG_START
        VedtaksperiodetilstandDto.SELVSTENDIG_TIL_UTBETALING -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.TilstandType.SELVSTENDIG_TIL_UTBETALING
    },
    skjæringstidspunkt = skjæringstidspunkt,
    fom = fom,
    tom = tom,
    sykmeldingFom = sykmeldingFom,
    sykmeldingTom = sykmeldingTom,
    behandlinger = behandlinger.behandlinger.map { it.tilPersonData() },
    venteårsak = utledVenteårsak(venteårsak),
    opprettet = opprettet,
    oppdatert = oppdatert,
    vilkårsgrunnlag = vilkårsgrunnlag,
    utbetaling = utbetaling,
    sisteBehandlingId,
    sisteRefusjonstidslinje,
    annulleringskandidater = annulleringskandidater.map { SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.AnnulleringskandidatDto(it.vedtaksperiodeId, it.organisasjonsnummer, it.fom, it.tom) }
)

private fun utledVenteårsak(venteårsak: VedtaksperiodeVenterDto?): SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.VedtaksperiodeVenterDto? {
    try {
        return venteårsak?.tilPersonData()
    } catch (_: Exception) {
        return SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.VedtaksperiodeVenterDto(
            ventetSiden = LocalDateTime.now(),
            venterTil = LocalDateTime.MAX,
            venterPå = SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.VenterPåDto(
                vedtaksperiodeId = "00000000-0000-0000-0000-000000000000".let { UUID.fromString(it) },
                organisasjonsnummer = "ORGNUMMER",
                venteårsak = SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.VenteårsakDto(
                    hva = "Hjelp - kræsj",
                    hvorfor = "Det kastes en feil et sted når venteårsak evalueres, se i loggene"
                )
            )
        )
    }
}

private fun VedtaksperiodeVenterDto.tilPersonData() =
    SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.VedtaksperiodeVenterDto(
        ventetSiden = ventetSiden,
        venterTil = venterTil,
        venterPå = venterPå.tilPersonData()
    )

private fun VenterPåDto.tilPersonData() =
    SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.VenterPåDto(
        vedtaksperiodeId = vedtaksperiodeId,
        organisasjonsnummer = organisasjonsnummer,
        venteårsak = venteårsak.tilPersonData()
    )

private fun VenteårsakDto.tilPersonData() =
    SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.VenteårsakDto(
        hva = hva,
        hvorfor = hvorfor
    )

private fun BehandlingUtDto.tilPersonData() =
    SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.BehandlingData(
        id = this.id,
        tilstand = when (this.tilstand) {
            BehandlingtilstandDto.ANNULLERT_PERIODE -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.BehandlingData.TilstandData.ANNULLERT_PERIODE
            BehandlingtilstandDto.AVSLUTTET_UTEN_VEDTAK -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.BehandlingData.TilstandData.AVSLUTTET_UTEN_VEDTAK
            BehandlingtilstandDto.BEREGNET -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.BehandlingData.TilstandData.BEREGNET
            BehandlingtilstandDto.BEREGNET_OMGJØRING -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.BehandlingData.TilstandData.BEREGNET_OMGJØRING
            BehandlingtilstandDto.BEREGNET_REVURDERING -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.BehandlingData.TilstandData.BEREGNET_REVURDERING
            BehandlingtilstandDto.REVURDERT_VEDTAK_AVVIST -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.BehandlingData.TilstandData.REVURDERT_VEDTAK_AVVIST
            BehandlingtilstandDto.TIL_INFOTRYGD -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.BehandlingData.TilstandData.TIL_INFOTRYGD
            BehandlingtilstandDto.UBEREGNET -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.BehandlingData.TilstandData.UBEREGNET
            BehandlingtilstandDto.UBEREGNET_OMGJØRING -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.BehandlingData.TilstandData.UBEREGNET_OMGJØRING
            BehandlingtilstandDto.UBEREGNET_REVURDERING -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.BehandlingData.TilstandData.UBEREGNET_REVURDERING
            BehandlingtilstandDto.VEDTAK_FATTET -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.BehandlingData.TilstandData.VEDTAK_FATTET
            BehandlingtilstandDto.VEDTAK_IVERKSATT -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.BehandlingData.TilstandData.VEDTAK_IVERKSATT
            BehandlingtilstandDto.UBEREGNET_ANNULLERING -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.BehandlingData.TilstandData.UBEREGNET_ANNULLERING
            BehandlingtilstandDto.BEREGNET_ANNULLERING -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.BehandlingData.TilstandData.BEREGNET_ANNULLERING
            BehandlingtilstandDto.OVERFØRT_ANNULLERING -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.BehandlingData.TilstandData.OVERFØRT_ANNULLERING
        },
        vedtakFattet = this.vedtakFattet,
        avsluttet = this.avsluttet,
        kilde = this.kilde.tilPersonData(),
        endringer = this.endringer.map { it.tilPersonData() },
    )

private fun BehandlingkildeDto.tilPersonData() =
    SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.BehandlingData.KildeData(
        meldingsreferanseId = this.meldingsreferanseId.id,
        innsendt = this.innsendt,
        registrert = this.registert,
        avsender = this.avsender.tilPersonData()
    )

private fun AvsenderDto.tilPersonData() = when (this) {
    AvsenderDto.ARBEIDSGIVER -> AvsenderData.ARBEIDSGIVER
    AvsenderDto.SAKSBEHANDLER -> AvsenderData.SAKSBEHANDLER
    AvsenderDto.SYKMELDT -> AvsenderData.SYKMELDT
    AvsenderDto.SYSTEM -> AvsenderData.SYSTEM
}

private fun BehandlingendringUtDto.tilPersonData() =
    SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.BehandlingData.EndringData(
        id = id,
        tidsstempel = tidsstempel,
        arbeidssituasjon = when (arbeidssituasjon) {
            ArbeidssituasjonDto.ARBEIDSTAKER -> ArbeidssituasjonData.ARBEIDSTAKER
            ArbeidssituasjonDto.ARBEIDSLEDIG -> ArbeidssituasjonData.ARBEIDSLEDIG
            ArbeidssituasjonDto.FRILANSER -> ArbeidssituasjonData.FRILANSER
            ArbeidssituasjonDto.SELVSTENDIG_NÆRINGSDRIVENDE -> ArbeidssituasjonData.SELVSTENDIG_NÆRINGSDRIVENDE
            ArbeidssituasjonDto.BARNEPASSER -> ArbeidssituasjonData.BARNEPASSER
            ArbeidssituasjonDto.JORDBRUKER -> ArbeidssituasjonData.JORDBRUKER
            ArbeidssituasjonDto.FISKER -> ArbeidssituasjonData.FISKER
            ArbeidssituasjonDto.ANNET -> ArbeidssituasjonData.ANNET
        },
        sykmeldingsperiodeFom = sykmeldingsperiode.fom,
        sykmeldingsperiodeTom = sykmeldingsperiode.tom,
        fom = periode.fom,
        tom = periode.tom,
        utbetalingId = utbetalingId,
        utbetalingstatus = this.utbetalingstatus?.tilPersonData(),
        skjæringstidspunkt = this.skjæringstidspunkt,
        skjæringstidspunkter = this.skjæringstidspunkter,
        vilkårsgrunnlagId = vilkårsgrunnlagId,
        sykdomstidslinje = sykdomstidslinje.tilPersonData(),
        utbetalingstidslinje = utbetalingstidslinje.tilPersonData(),
        refusjonstidslinje = refusjonstidslinje.tilPersonData(),
        inntektsendringer = inntektsendringer.tilPersonData(),
        dokumentsporing = dokumentsporing.tilPersonData(),
        arbeidsgiverperiode = arbeidsgiverperiode.tilPersonData(),
        dagerNavOvertarAnsvar = dagerNavOvertarAnsvar.map {
            SpannerPersonDto.ArbeidsgiverData.PeriodeData(
                it.fom,
                it.tom
            )
        },
        egenmeldingsdager = egenmeldingsdager.map {
            SpannerPersonDto.ArbeidsgiverData.PeriodeData(
                it.fom,
                it.tom
            )
        },
        maksdatoresultat = maksdatoresultat.tilPersonData(),
        inntektjusteringer = inntektjusteringer.map { (inntektskilde, beløpstidslinje) ->
            inntektskilde.id to beløpstidslinje.tilPersonData()
        }.toMap(),
        faktaavklartInntekt = faktaavklartInntekt?.tilPersonData(),
        ventetid = ventetid?.let { SpannerPersonDto.ArbeidsgiverData.PeriodeData(it.fom, it.tom) },
        forberedendeVilkårsgrunnlag = forberedendeVilkårsgrunnlag?.tilPersonData()
    )

private fun ArbeidsgiverperiodeavklaringDto.tilPersonData() = ArbeidsgiverperiodeData(
    ferdigAvklart = this.ferdigAvklart,
    dager = this.dager.map { SpannerPersonDto.ArbeidsgiverData.PeriodeData(it.fom, it.tom) }
)

private fun MaksdatoresultatUtDto.tilPersonData() = SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.MaksdatoresultatData(
    vurdertTilOgMed = vurdertTilOgMed,
    bestemmelse = when (bestemmelse) {
        MaksdatobestemmelseDto.IKKE_VURDERT -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.MaksdatobestemmelseDto.IKKE_VURDERT
        MaksdatobestemmelseDto.ORDINÆR_RETT -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.MaksdatobestemmelseDto.ORDINÆR_RETT
        MaksdatobestemmelseDto.BEGRENSET_RETT -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.MaksdatobestemmelseDto.BEGRENSET_RETT
        MaksdatobestemmelseDto.SYTTI_ÅR -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.MaksdatobestemmelseDto.SYTTI_ÅR
    },
    startdatoTreårsvindu = startdatoTreårsvindu,
    startdatoSykepengerettighet = startdatoSykepengerettighet,
    forbrukteDager = forbrukteDager.map { SpannerPersonDto.ArbeidsgiverData.PeriodeData(it.fom, it.tom) },
    oppholdsdager = oppholdsdager.map { SpannerPersonDto.ArbeidsgiverData.PeriodeData(it.fom, it.tom) },
    avslåtteDager = avslåtteDager.map { SpannerPersonDto.ArbeidsgiverData.PeriodeData(it.fom, it.tom) },
    maksdato = maksdato,
    gjenståendeDager = gjenståendeDager
)

private fun DokumentsporingDto.tilPersonData() =
    SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.DokumentsporingData(
        dokumentId = this.id.id,
        dokumenttype = when (type) {
            DokumenttypeDto.InntektsmeldingDager -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.DokumentTypeData.InntektsmeldingDager
            DokumenttypeDto.InntektsmeldingInntekt -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.DokumentTypeData.InntektsmeldingInntekt
            DokumenttypeDto.InntektsmeldingRefusjon -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.DokumentTypeData.InntektsmeldingRefusjon
            DokumenttypeDto.InntektFraAOrdningen -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.DokumentTypeData.InntektFraAOrdningen
            DokumenttypeDto.OverstyrArbeidsforhold -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.DokumentTypeData.OverstyrArbeidsforhold
            DokumenttypeDto.OverstyrArbeidsgiveropplysninger -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.DokumentTypeData.OverstyrArbeidsgiveropplysninger
            DokumenttypeDto.OverstyrInntekt -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.DokumentTypeData.OverstyrInntekt
            DokumenttypeDto.OverstyrRefusjon -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.DokumentTypeData.OverstyrRefusjon
            DokumenttypeDto.OverstyrTidslinje -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.DokumentTypeData.OverstyrTidslinje
            DokumenttypeDto.SkjønnsmessigFastsettelse -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.DokumentTypeData.SkjønnsmessigFastsettelse
            DokumenttypeDto.Sykmelding -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.DokumentTypeData.Sykmelding
            DokumenttypeDto.Søknad -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.DokumentTypeData.Søknad
            DokumenttypeDto.AndreYtelser -> SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.DokumentTypeData.AndreYtelser
        }
    )

private fun UtbetalingUtDto.tilPersonData() = UtbetalingData(
    id = this.id,
    korrelasjonsId = this.korrelasjonsId,
    fom = this.periode.fom,
    tom = this.periode.tom,
    annulleringer = this.annulleringer,
    utbetalingstidslinje = this.utbetalingstidslinje.tilPersonData(),
    arbeidsgiverOppdrag = this.arbeidsgiverOppdrag.tilPersonData(),
    personOppdrag = this.personOppdrag.tilPersonData(),
    tidsstempel = this.tidsstempel,
    type = when (this.type) {
        UtbetalingtypeDto.ANNULLERING -> UtbetalingData.UtbetalingtypeData.ANNULLERING
        UtbetalingtypeDto.ETTERUTBETALING -> UtbetalingData.UtbetalingtypeData.ETTERUTBETALING
        UtbetalingtypeDto.REVURDERING -> UtbetalingData.UtbetalingtypeData.REVURDERING
        UtbetalingtypeDto.UTBETALING -> UtbetalingData.UtbetalingtypeData.UTBETALING
    },
    status = this.tilstand.tilPersonData(),
    maksdato = this.maksdato,
    forbrukteSykedager = this.forbrukteSykedager,
    gjenståendeSykedager = this.gjenståendeSykedager,
    vurdering = this.vurdering?.tilPersonData(),
    overføringstidspunkt = overføringstidspunkt,
    avstemmingsnøkkel = avstemmingsnøkkel,
    avsluttet = avsluttet,
    oppdatert = oppdatert
)

private fun UtbetalingTilstandDto.tilPersonData() = when (this) {
    UtbetalingTilstandDto.ANNULLERT -> UtbetalingData.UtbetalingstatusData.ANNULLERT
    UtbetalingTilstandDto.FORKASTET -> UtbetalingData.UtbetalingstatusData.FORKASTET
    UtbetalingTilstandDto.GODKJENT -> UtbetalingData.UtbetalingstatusData.GODKJENT
    UtbetalingTilstandDto.GODKJENT_UTEN_UTBETALING -> UtbetalingData.UtbetalingstatusData.GODKJENT_UTEN_UTBETALING
    UtbetalingTilstandDto.IKKE_GODKJENT -> UtbetalingData.UtbetalingstatusData.IKKE_GODKJENT
    UtbetalingTilstandDto.IKKE_UTBETALT -> UtbetalingData.UtbetalingstatusData.IKKE_UTBETALT
    UtbetalingTilstandDto.NY -> UtbetalingData.UtbetalingstatusData.NY
    UtbetalingTilstandDto.OVERFØRT -> UtbetalingData.UtbetalingstatusData.OVERFØRT
    UtbetalingTilstandDto.UTBETALT -> UtbetalingData.UtbetalingstatusData.UTBETALT
}

private fun UtbetalingstidslinjeUtDto.tilPersonData() = SpannerPersonDto.UtbetalingstidslinjeData(
    dager = this.dager.map { it.tilPersonData() }.forkortUtbetalingstidslinje()
)

private fun List<UtbetalingsdagData>.forkortUtbetalingstidslinje(): List<UtbetalingsdagData> {
    return this.fold(emptyList()) { result, neste ->
        val slåttSammen = result.lastOrNull()?.utvideMed(neste) ?: return@fold result + neste
        result.dropLast(1) + slåttSammen
    }
}

private fun UtbetalingsdagData.utvideMed(other: UtbetalingsdagData): UtbetalingsdagData? {
    if (!kanUtvidesMed(other)) return null
    val otherDato = checkNotNull(other.dato) { "dato må være satt" }
    if (this.dato != null) {
        return this.copy(dato = null, fom = dato, tom = otherDato)
    }
    return this.copy(tom = other.dato)
}

private fun UtbetalingsdagData.kanUtvidesMed(other: UtbetalingsdagData): Boolean {
    // alle verdier må være like (untatt datoene)
    val utenDatoer = { dag: UtbetalingsdagData -> dag.copy(fom = null, tom = null, dato = LocalDate.EPOCH) }
    return utenDatoer(this) == utenDatoer(other) && (dato ?: tom!!).nesteDag == other.dato
}

private fun UtbetalingsdagUtDto.tilPersonData() =
    UtbetalingsdagData(
        type = when (this) {
            is UtbetalingsdagUtDto.ArbeidsdagDto -> SpannerPersonDto.UtbetalingstidslinjeData.TypeData.Arbeidsdag
            is UtbetalingsdagUtDto.ArbeidsgiverperiodeDagDto -> SpannerPersonDto.UtbetalingstidslinjeData.TypeData.ArbeidsgiverperiodeDag
            is UtbetalingsdagUtDto.ArbeidsgiverperiodeDagNavDto -> SpannerPersonDto.UtbetalingstidslinjeData.TypeData.ArbeidsgiverperiodedagNav
            is UtbetalingsdagUtDto.AvvistDagDto -> SpannerPersonDto.UtbetalingstidslinjeData.TypeData.AvvistDag
            is UtbetalingsdagUtDto.ForeldetDagDto -> SpannerPersonDto.UtbetalingstidslinjeData.TypeData.ForeldetDag
            is UtbetalingsdagUtDto.FridagDto -> SpannerPersonDto.UtbetalingstidslinjeData.TypeData.Fridag
            is UtbetalingsdagUtDto.NavDagDto -> SpannerPersonDto.UtbetalingstidslinjeData.TypeData.NavDag
            is UtbetalingsdagUtDto.NavHelgDagDto -> SpannerPersonDto.UtbetalingstidslinjeData.TypeData.NavHelgDag
            is UtbetalingsdagUtDto.UkjentDagDto -> SpannerPersonDto.UtbetalingstidslinjeData.TypeData.UkjentDag
            is UtbetalingsdagUtDto.VentetidsdagDto -> SpannerPersonDto.UtbetalingstidslinjeData.TypeData.Ventetidsdag
        },
        aktuellDagsinntekt = this.økonomi.aktuellDagsinntekt.dagligDouble.beløp,
        dekingsgrad = this.økonomi.dekningsgrad.prosentDesimal,
        begrunnelser = when (this) {
            is UtbetalingsdagUtDto.AvvistDagDto -> this.begrunnelser.map { it.tilPersonData() }
            else -> null
        },
        grad = this.økonomi.grad.prosentDesimal,
        totalGrad = this.økonomi.totalGrad.prosentDesimal,
        utbetalingsgrad = this.økonomi.utbetalingsgrad.prosentDesimal,
        arbeidsgiverRefusjonsbeløp = økonomi.arbeidsgiverRefusjonsbeløp.dagligDouble.beløp,
        arbeidsgiverbeløp = this.økonomi.arbeidsgiverbeløp?.dagligDouble?.beløp,
        personbeløp = this.økonomi.personbeløp?.dagligDouble?.beløp,
        reservertArbeidsgiverbeløp = this.økonomi.reservertArbeidsgiverbeløp?.dagligDouble?.beløp,
        reservertPersonbeløp = this.økonomi.reservertPersonbeløp?.dagligDouble?.beløp,
        dato = this.dato,
        fom = null,
        tom = null
    )

private fun BegrunnelseDto.tilPersonData() = when (this) {
    BegrunnelseDto.AndreYtelserAap -> SpannerPersonDto.UtbetalingstidslinjeData.BegrunnelseData.AndreYtelserAap
    BegrunnelseDto.AndreYtelserDagpenger -> SpannerPersonDto.UtbetalingstidslinjeData.BegrunnelseData.AndreYtelserDagpenger
    BegrunnelseDto.AndreYtelserForeldrepenger -> SpannerPersonDto.UtbetalingstidslinjeData.BegrunnelseData.AndreYtelserForeldrepenger
    BegrunnelseDto.AndreYtelserOmsorgspenger -> SpannerPersonDto.UtbetalingstidslinjeData.BegrunnelseData.AndreYtelserOmsorgspenger
    BegrunnelseDto.AndreYtelserOpplaringspenger -> SpannerPersonDto.UtbetalingstidslinjeData.BegrunnelseData.AndreYtelserOpplaringspenger
    BegrunnelseDto.AndreYtelserPleiepenger -> SpannerPersonDto.UtbetalingstidslinjeData.BegrunnelseData.AndreYtelserPleiepenger
    BegrunnelseDto.AndreYtelserSvangerskapspenger -> SpannerPersonDto.UtbetalingstidslinjeData.BegrunnelseData.AndreYtelserSvangerskapspenger
    BegrunnelseDto.EgenmeldingUtenforArbeidsgiverperiode -> SpannerPersonDto.UtbetalingstidslinjeData.BegrunnelseData.EgenmeldingUtenforArbeidsgiverperiode
    BegrunnelseDto.EtterDødsdato -> SpannerPersonDto.UtbetalingstidslinjeData.BegrunnelseData.EtterDødsdato
    BegrunnelseDto.ManglerMedlemskap -> SpannerPersonDto.UtbetalingstidslinjeData.BegrunnelseData.ManglerMedlemskap
    BegrunnelseDto.ManglerOpptjening -> SpannerPersonDto.UtbetalingstidslinjeData.BegrunnelseData.ManglerOpptjening
    BegrunnelseDto.MinimumInntekt -> SpannerPersonDto.UtbetalingstidslinjeData.BegrunnelseData.MinimumInntekt
    BegrunnelseDto.MinimumInntektOver67 -> SpannerPersonDto.UtbetalingstidslinjeData.BegrunnelseData.MinimumInntektOver67
    BegrunnelseDto.MinimumSykdomsgrad -> SpannerPersonDto.UtbetalingstidslinjeData.BegrunnelseData.MinimumSykdomsgrad
    BegrunnelseDto.NyVilkårsprøvingNødvendig -> SpannerPersonDto.UtbetalingstidslinjeData.BegrunnelseData.NyVilkårsprøvingNødvendig
    BegrunnelseDto.Over70 -> SpannerPersonDto.UtbetalingstidslinjeData.BegrunnelseData.Over70
    BegrunnelseDto.SykepengedagerOppbrukt -> SpannerPersonDto.UtbetalingstidslinjeData.BegrunnelseData.SykepengedagerOppbrukt
    BegrunnelseDto.SykepengedagerOppbruktOver67 -> SpannerPersonDto.UtbetalingstidslinjeData.BegrunnelseData.SykepengedagerOppbruktOver67
}

private fun UtbetalingVurderingDto.tilPersonData() = UtbetalingData.VurderingData(
    godkjent = godkjent,
    ident = ident,
    epost = epost,
    tidspunkt = tidspunkt,
    automatiskBehandling = automatiskBehandling
)

private fun OppdragUtDto.tilPersonData() = SpannerPersonDto.OppdragData(
    mottaker = this.mottaker,
    fagområde = when (this.fagområde) {
        FagområdeDto.SP -> "SP"
        FagområdeDto.SPREF -> "SPREF"
    },
    linjer = this.linjer.map { it.tilPersonData() },
    fagsystemId = this.fagsystemId,
    endringskode = this.endringskode.tilPersonData(),
    tidsstempel = this.tidsstempel,
    nettoBeløp = this.nettoBeløp,
    totalbeløp = this.totalbeløp,
    stønadsdager = this.stønadsdager,
    avstemmingsnøkkel = this.avstemmingsnøkkel,
    status = when (this.status) {
        OppdragstatusDto.AKSEPTERT -> SpannerPersonDto.OppdragData.OppdragstatusData.AKSEPTERT
        OppdragstatusDto.AKSEPTERT_MED_FEIL -> SpannerPersonDto.OppdragData.OppdragstatusData.AKSEPTERT_MED_FEIL
        OppdragstatusDto.AVVIST -> SpannerPersonDto.OppdragData.OppdragstatusData.AVVIST
        OppdragstatusDto.FEIL -> SpannerPersonDto.OppdragData.OppdragstatusData.FEIL
        OppdragstatusDto.OVERFØRT -> SpannerPersonDto.OppdragData.OppdragstatusData.OVERFØRT
        null -> null
    },
    overføringstidspunkt = this.overføringstidspunkt,
    erSimulert = this.erSimulert,
    simuleringsResultat = this.simuleringsResultat?.tilPersonData()
)

private fun EndringskodeDto.tilPersonData() = when (this) {
    EndringskodeDto.ENDR -> "ENDR"
    EndringskodeDto.NY -> "NY"
    EndringskodeDto.UEND -> "UEND"
}

private fun UtbetalingslinjeUtDto.tilPersonData() = SpannerPersonDto.UtbetalingslinjeData(
    fom = this.fom,
    tom = this.tom,
    sats = this.beløp,
    grad = this.grad,
    totalbeløp = this.totalbeløp,
    stønadsdager = this.stønadsdager,
    refFagsystemId = this.refFagsystemId,
    delytelseId = this.delytelseId,
    refDelytelseId = this.refDelytelseId,
    endringskode = this.endringskode.tilPersonData(),
    klassekode = this.klassekode.tilPersonData(),
    datoStatusFom = this.datoStatusFom,
    statuskode = this.statuskode
)

private fun KlassekodeDto.tilPersonData() = when (this) {
    KlassekodeDto.RefusjonIkkeOpplysningspliktig -> "SPREFAG-IOP"
    KlassekodeDto.SykepengerArbeidstakerOrdinær -> "SPATORD"
    KlassekodeDto.SelvstendigNæringsdrivendeOppgavepliktig -> "SPSND-OP"
    KlassekodeDto.SelvstendigNæringsdrivendeBarnepasserOppgavepliktig -> "SPSNDDM-OP"
    KlassekodeDto.SelvstendigNæringsdrivendeFisker -> "SPSNDFISK"
    KlassekodeDto.SelvstendigNæringsdrivendeJordbrukOgSkogbruk -> "SPSNDJORD"
}

private fun SimuleringResultatDto.tilPersonData() =
    SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.DataForSimuleringData(
        totalbeløp = this.totalbeløp,
        perioder = this.perioder.map {
            SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.DataForSimuleringData.SimulertPeriode(
                fom = it.fom,
                tom = it.tom,

                utbetalinger = it.utbetalinger.map {
                    SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.DataForSimuleringData.SimulertUtbetaling(
                        forfallsdato = it.forfallsdato,
                        utbetalesTil = SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.DataForSimuleringData.Mottaker(
                            id = it.utbetalesTil.id,
                            navn = it.utbetalesTil.navn
                        ),
                        feilkonto = it.feilkonto,
                        detaljer = it.detaljer.map {
                            SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.DataForSimuleringData.Detaljer(
                                fom = it.fom,
                                tom = it.tom,
                                konto = it.konto,
                                beløp = it.beløp,
                                klassekode = SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.DataForSimuleringData.Klassekode(
                                    kode = it.klassekode.kode,
                                    beskrivelse = it.klassekode.beskrivelse
                                ),
                                uføregrad = it.uføregrad,
                                utbetalingstype = it.utbetalingstype,
                                tilbakeføring = it.tilbakeføring,
                                sats = SpannerPersonDto.ArbeidsgiverData.VedtaksperiodeData.DataForSimuleringData.Sats(
                                    sats = it.sats.sats,
                                    antall = it.sats.antall,
                                    type = it.sats.type
                                ),
                                refunderesOrgnummer = it.refunderesOrgnummer
                            )
                        }
                    )
                }
            )
        }
    )

private fun FeriepengeUtDto.tilPersonData() =
    SpannerPersonDto.ArbeidsgiverData.FeriepengeutbetalingData(
        infotrygdFeriepengebeløpPerson = this.infotrygdFeriepengebeløpPerson,
        infotrygdFeriepengebeløpArbeidsgiver = this.infotrygdFeriepengebeløpArbeidsgiver,
        spleisFeriepengebeløpArbeidsgiver = this.spleisFeriepengebeløpArbeidsgiver,
        spleisFeriepengebeløpPerson = this.spleisFeriepengebeløpPerson,
        oppdrag = this.oppdrag.tilPersonData(),
        personoppdrag = this.personoppdrag.tilPersonData(),
        opptjeningsår = this.feriepengeberegner.opptjeningsår,
        utbetalteDager = this.feriepengeberegner.utbetalteDager.map { it.tilPersonData() },
        feriepengedager = this.feriepengeberegner.feriepengedager.map { it.tilPersonData() },
        utbetalingId = utbetalingId,
        sendTilOppdrag = sendTilOppdrag,
        sendPersonoppdragTilOS = sendPersonoppdragTilOS
    )

private fun FeriepengeoppdragUtDto.tilPersonData() = SpannerPersonDto.ArbeidsgiverData.FeriepengeutbetalingData.OppdragData(
    mottaker = this.mottaker,
    fagområde = when (this.fagområde) {
        FeriepengerfagområdeDto.SP -> "SP"
        FeriepengerfagområdeDto.SPREF -> "SPREF"
    },
    linjer = this.linjer.map { it.tilPersonData() },
    fagsystemId = this.fagsystemId,
    endringskode = this.endringskode.tilPersonData(),
    tidsstempel = this.tidsstempel
)

private fun FeriepengerendringskodeDto.tilPersonData() = when (this) {
    FeriepengerendringskodeDto.ENDR -> "ENDR"
    FeriepengerendringskodeDto.NY -> "NY"
    FeriepengerendringskodeDto.UEND -> "UEND"
}

private fun FeriepengeutbetalingslinjeUtDto.tilPersonData() = SpannerPersonDto.ArbeidsgiverData.FeriepengeutbetalingData.OppdragData.UtbetalingslinjeData(
    fom = this.fom,
    tom = this.tom,
    sats = this.beløp,
    refFagsystemId = this.refFagsystemId,
    delytelseId = this.delytelseId,
    refDelytelseId = this.refDelytelseId,
    endringskode = this.endringskode.tilPersonData(),
    klassekode = this.klassekode.tilPersonData(),
    datoStatusFom = this.datoStatusFom,
    statuskode = this.statuskode
)

private fun FeriepengerklassekodeDto.tilPersonData() = when (this) {
    FeriepengerklassekodeDto.RefusjonFeriepengerIkkeOpplysningspliktig -> "SPREFAGFER-IOP"
    FeriepengerklassekodeDto.SykepengerArbeidstakerFeriepenger -> "SPATFER"
}

private fun UtbetaltDagUtDto.tilPersonData() =
    SpannerPersonDto.ArbeidsgiverData.FeriepengeutbetalingData.UtbetaltDagData(
        type = when (this) {
            is UtbetaltDagUtDto.InfotrygdArbeidsgiver -> "InfotrygdArbeidsgiverDag"
            is UtbetaltDagUtDto.InfotrygdPerson -> "InfotrygdPersonDag"
            is UtbetaltDagUtDto.SpleisArbeidsgiver -> "SpleisArbeidsgiverDag"
            is UtbetaltDagUtDto.SpleisPerson -> "SpleisPersonDag"
        },
        orgnummer = orgnummer,
        dato = dato,
        beløp = beløp
    )

private fun InfotrygdhistorikkelementUtDto.tilPersonData() =
    SpannerPersonDto.InfotrygdhistorikkElementData(
        id = this.id,
        tidsstempel = this.tidsstempel,
        hendelseId = this.hendelseId.id,
        ferieperioder = this.ferieperioder.map { it.tilPersonData() },
        arbeidsgiverutbetalingsperioder = this.arbeidsgiverutbetalingsperioder.map { it.tilPersonData() },
        personutbetalingsperioder = this.personutbetalingsperioder.map { it.tilPersonData() },
        oppdatert = oppdatert
    )

private fun InfotrygdFerieperiodeDto.tilPersonData() =
    SpannerPersonDto.InfotrygdhistorikkElementData.FerieperiodeData(
        fom = this.periode.fom,
        tom = this.periode.tom
    )

private fun InfotrygdArbeidsgiverutbetalingsperiodeUtDto.tilPersonData() =
    SpannerPersonDto.InfotrygdhistorikkElementData.ArbeidsgiverutbetalingsperiodeData(
        orgnr = this.orgnr,
        fom = this.periode.fom,
        tom = this.periode.tom
    )

private fun InfotrygdPersonutbetalingsperiodeUtDto.tilPersonData() =
    SpannerPersonDto.InfotrygdhistorikkElementData.PersonutbetalingsperiodeData(
        orgnr = this.orgnr,
        fom = this.periode.fom,
        tom = this.periode.tom
    )

private fun VilkårsgrunnlagInnslagUtDto.tilPersonData() = VilkårsgrunnlagInnslagData(
    id = this.id,
    opprettet = this.opprettet,
    vilkårsgrunnlag = this.vilkårsgrunnlag.map { it.tilPersonData() }
)

private fun VilkårsgrunnlagUtDto.tilPersonData() = VilkårsgrunnlagElementData(
    skjæringstidspunkt = this.skjæringstidspunkt,
    type = when (this) {
        is VilkårsgrunnlagUtDto.Infotrygd -> VilkårsgrunnlagElementData.GrunnlagsdataType.Infotrygd
        is VilkårsgrunnlagUtDto.Spleis -> VilkårsgrunnlagElementData.GrunnlagsdataType.Vilkårsprøving
    },
    inntektsgrunnlag = this.inntektsgrunnlag.tilPersonData(),
    opptjening = when (this) {
        is VilkårsgrunnlagUtDto.Spleis -> this.opptjening?.tilPersonData()
        is VilkårsgrunnlagUtDto.Infotrygd -> null
    },
    medlemskapstatus = when (this) {
        is VilkårsgrunnlagUtDto.Spleis -> when (this.medlemskapstatus) {
            MedlemskapsvurderingDto.Ja -> VilkårsgrunnlagElementData.MedlemskapstatusDto.JA
            MedlemskapsvurderingDto.Nei -> VilkårsgrunnlagElementData.MedlemskapstatusDto.NEI
            MedlemskapsvurderingDto.UavklartMedBrukerspørsmål -> VilkårsgrunnlagElementData.MedlemskapstatusDto.UAVKLART_MED_BRUKERSPØRSMÅL
            MedlemskapsvurderingDto.VetIkke -> VilkårsgrunnlagElementData.MedlemskapstatusDto.VET_IKKE
        }

        else -> null
    },
    meldingsreferanseId = when (this) {
        is VilkårsgrunnlagUtDto.Spleis -> this.meldingsreferanseId?.id
        else -> null
    },
    vilkårsgrunnlagId = this.vilkårsgrunnlagId
)

private fun OpptjeningUtDto.tilPersonData() =
    VilkårsgrunnlagElementData.OpptjeningData(
        opptjeningFom = this.opptjeningsperiode.fom,
        opptjeningTom = this.opptjeningsperiode.tom,
        arbeidsforhold = this.arbeidsforhold.map {
            VilkårsgrunnlagElementData.OpptjeningData.ArbeidsgiverOpptjeningsgrunnlagData(
                orgnummer = it.orgnummer,
                ansattPerioder = it.ansattPerioder.map {
                    VilkårsgrunnlagElementData.OpptjeningData.ArbeidsgiverOpptjeningsgrunnlagData.ArbeidsforholdData(
                        ansattFom = it.ansattFom,
                        ansattTom = it.ansattTom,
                        deaktivert = it.deaktivert
                    )
                }
            )
        }
    )

private fun InntektsgrunnlagUtDto.tilPersonData() =
    VilkårsgrunnlagElementData.InntektsgrunnlagData(
        grunnbeløp = this.`6G`.årlig.beløp,
        arbeidsgiverInntektsopplysninger = this.arbeidsgiverInntektsopplysninger.map { it.tilPersonData() },
        deaktiverteArbeidsforhold = this.deaktiverteArbeidsforhold.map { it.tilPersonData() },
        vurdertInfotrygd = this.vurdertInfotrygd,
        totalOmregnetÅrsinntekt = totalOmregnetÅrsinntekt.årlig,
        beregningsgrunnlag = beregningsgrunnlag.årlig,
        er6GBegrenset = er6GBegrenset
    )

private fun ArbeidsgiverInntektsopplysningUtDto.tilPersonData() =
    ArbeidsgiverInntektsopplysningData(
        orgnummer = this.orgnummer,
        faktaavklartInntekt = this.faktaavklartInntekt.tilPersonData(),
        korrigertInntekt = this.korrigertInntekt?.tilPersonData(),
        skjønnsmessigFastsatt = this.skjønnsmessigFastsatt?.tilPersonData()
    )

private fun ArbeidstakerFaktaavklartInntektUtDto.tilPersonData() =
    ArbeidsgiverInntektsopplysningData.InntektsopplysningData(
        id = this.id,
        dato = this.inntektsdata.dato,
        hendelseId = this.inntektsdata.hendelseId.id,
        beløp = this.inntektsdata.beløp.tilPersonData(),
        tidsstempel = this.inntektsdata.tidsstempel,
        type = InntektsopplysningstypeData.ARBEIDSTAKER,
        kilde = when (this.inntektsopplysningskilde) {
            is ArbeidstakerinntektskildeUtDto.InfotrygdDto -> "INFOTRYGD"
            is ArbeidstakerinntektskildeUtDto.ArbeidsgiverDto -> "INNTEKTSMELDING"
            is ArbeidstakerinntektskildeUtDto.AOrdningenDto -> "SKATT_SYKEPENGEGRUNNLAG"
        },
        skatteopplysninger = when (val kilde = this.inntektsopplysningskilde) {
            is ArbeidstakerinntektskildeUtDto.AOrdningenDto -> kilde.inntektsopplysninger.map { it.tilPersonDataSkattopplysning() }
            else -> null
        },
        pensjonsgivendeInntekter = null,
        anvendtGrunnbeløp = null
    )

private fun SelvstendigFaktaavklartInntektUtDto.tilPersonData() =
    SelvstendigInntektsopplysningData.InntektsopplysningData(
        id = this.id,
        dato = this.inntektsdata.dato,
        hendelseId = this.inntektsdata.hendelseId.id,
        beløp = this.inntektsdata.beløp.tilPersonData(),
        tidsstempel = this.inntektsdata.tidsstempel,
        pensjonsgivendeInntekter = this.pensjonsgivendeInntekter.map {
            PensjonsgivendeInntektData(
                årstall = it.årstall,
                beløp = it.beløp.tilPersonData()
            )
        },
        anvendtGrunnbeløp = this.anvendtGrunnbeløp.tilPersonData()
    )

private fun ForberedendeVilkårsgrunnlagDto.tilPersonData() = SpannerPersonDto.ForberedendeVilkårsgrunnlagData(erOpptjeningVurdertOk)

private fun SaksbehandlerUtDto.tilPersonData() =
    ArbeidsgiverInntektsopplysningData.KorrigertInntektsopplysningData(
        id = this.id,
        dato = this.inntektsdata.dato,
        hendelseId = this.inntektsdata.hendelseId.id,
        beløp = this.inntektsdata.beløp.tilPersonData(),
        tidsstempel = this.inntektsdata.tidsstempel
    )

private fun SkjønnsmessigFastsattUtDto.tilPersonData() =
    ArbeidsgiverInntektsopplysningData.SkjønnsmessigFastsattData(
        id = this.id,
        dato = this.inntektsdata.dato,
        hendelseId = this.inntektsdata.hendelseId.id,
        beløp = this.inntektsdata.beløp.tilPersonData(),
        tidsstempel = this.inntektsdata.tidsstempel
    )

private fun SkatteopplysningDto.tilPersonDataSkattopplysning() =
    ArbeidsgiverInntektsopplysningData.SkatteopplysningData(
        hendelseId = this.hendelseId.id,
        beløp = this.beløp.beløp,
        måned = this.måned,
        type = when (this.type) {
            InntekttypeDto.LØNNSINNTEKT -> ArbeidsgiverInntektsopplysningData.SkatteopplysningData.InntekttypeData.LØNNSINNTEKT
            InntekttypeDto.NÆRINGSINNTEKT -> ArbeidsgiverInntektsopplysningData.SkatteopplysningData.InntekttypeData.NÆRINGSINNTEKT
            InntekttypeDto.PENSJON_ELLER_TRYGD -> ArbeidsgiverInntektsopplysningData.SkatteopplysningData.InntekttypeData.PENSJON_ELLER_TRYGD
            InntekttypeDto.YTELSE_FRA_OFFENTLIGE -> ArbeidsgiverInntektsopplysningData.SkatteopplysningData.InntekttypeData.YTELSE_FRA_OFFENTLIGE
        },
        fordel = fordel,
        beskrivelse = beskrivelse,
        tidsstempel = tidsstempel
    )

private fun BeløpstidslinjeDto.tilPersonData() = SpannerPersonDto.BeløpstidslinjeData(
    perioder = this.perioder.map {
        SpannerPersonDto.BeløpstidslinjeperiodeData(
            fom = it.fom,
            tom = it.tom,
            dagligBeløp = it.dagligBeløp,
            meldingsreferanseId = it.kilde.meldingsreferanseId.id,
            avsender = it.kilde.avsender.tilPersonData(),
            tidsstempel = it.kilde.tidsstempel
        )
    }
)
