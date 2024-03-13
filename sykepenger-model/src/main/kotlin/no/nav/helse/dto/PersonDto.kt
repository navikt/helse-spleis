package no.nav.helse.dto

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.sykdomstidslinje.Dag

data class PersonDto(
    val aktørId: String,
    val fødselsnummer: String,
    val alder: AlderDto,
    val opprettet: LocalDateTime,
    val arbeidsgivere: List<ArbeidsgiverDto>,
    val infotrygdhistorikk: InfotrygdhistorikkDto,
    val vilkårsgrunnlagHistorikk: VilkårsgrunnlaghistorikkDto
)

data class AlderDto(val fødselsdato: LocalDate, val dødsdato: LocalDate?)

data class ArbeidsgiverDto(
    val id: UUID,
    val organisasjonsnummer: String,
    val inntektshistorikk: InntektshistorikkDto,
    val sykdomshistorikk: SykdomshistorikkDto,
    val sykmeldingsperioder: SykmeldingsperioderDto,
    val vedtaksperioder: List<VedtaksperiodeDto>,
    val forkastede: List<ForkastetVedtaksperiodeDto>,
    val utbetalinger: List<UtbetalingDto>,
    val feriepengeutbetalinger: List<FeriepengeDto>,
    val refusjonshistorikk: RefusjonshistorikkDto
)

data class SykmeldingsperioderDto(val perioder: List<PeriodeDto>)
data class RefusjonshistorikkDto(val refusjoner: List<RefusjonDto>)
data class RefusjonDto(
    val meldingsreferanseId: UUID,
    val førsteFraværsdag: LocalDate?,
    val arbeidsgiverperioder: List<PeriodeDto>,
    val beløp: InntektDto.MånedligDouble?,
    val sisteRefusjonsdag: LocalDate?,
    val endringerIRefusjon: List<EndringIRefusjonDto>,
    val tidsstempel: LocalDateTime
)
data class EndringIRefusjonDto(
    val beløp: InntektDto.MånedligDouble,
    val endringsdato: LocalDate
)
data class InntektshistorikkDto(val historikk: List<InntektsopplysningDto.InntektsmeldingDto>)

data class SykdomshistorikkDto(val elementer: List<SykdomshistorikkElementDto>)
data class SykdomshistorikkElementDto(
    val id: UUID,
    val hendelseId: UUID?,
    val tidsstempel: LocalDateTime,
    val hendelseSykdomstidslinje: SykdomstidslinjeDto,
    val beregnetSykdomstidslinje: SykdomstidslinjeDto,
)

data class SykdomstidslinjeDto(
    val dager: List<SykdomstidslinjeDagDto>,
    val periode: PeriodeDto?,
    val låstePerioder: List<PeriodeDto>
)

sealed class SykdomstidslinjeDagDto {
    abstract val dato: LocalDate
    abstract val kilde: HendelseskildeDto

    data class UkjentDagDto(override val dato: LocalDate, override val kilde: HendelseskildeDto) : SykdomstidslinjeDagDto()
    data class ArbeidsdagDto(override val dato: LocalDate, override val kilde: HendelseskildeDto) : SykdomstidslinjeDagDto()
    data class ArbeidsgiverdagDto(override val dato: LocalDate, override val kilde: HendelseskildeDto, val grad: ProsentdelDto) : SykdomstidslinjeDagDto()
    data class FeriedagDto(override val dato: LocalDate, override val kilde: HendelseskildeDto) : SykdomstidslinjeDagDto()
    data class ArbeidIkkeGjenopptattDagDto(override val dato: LocalDate, override val kilde: HendelseskildeDto) : SykdomstidslinjeDagDto()
    data class FriskHelgedagDto(override val dato: LocalDate, override val kilde: HendelseskildeDto) : SykdomstidslinjeDagDto()
    data class ArbeidsgiverHelgedagDto(override val dato: LocalDate, override val kilde: HendelseskildeDto, val grad: ProsentdelDto) : SykdomstidslinjeDagDto()
    data class SykedagDto(override val dato: LocalDate, override val kilde: HendelseskildeDto, val grad: ProsentdelDto) : SykdomstidslinjeDagDto()
    data class SykHelgedagDto(override val dato: LocalDate, override val kilde: HendelseskildeDto, val grad: ProsentdelDto) : SykdomstidslinjeDagDto()
    data class SykedagNavDto(override val dato: LocalDate, override val kilde: HendelseskildeDto, val grad: ProsentdelDto) : SykdomstidslinjeDagDto()
    data class ForeldetSykedagDto(override val dato: LocalDate, override val kilde: HendelseskildeDto, val grad: ProsentdelDto) : SykdomstidslinjeDagDto()
    data class PermisjonsdagDto(override val dato: LocalDate, override val kilde: HendelseskildeDto) : SykdomstidslinjeDagDto()
    data class ProblemDagDto(
        override val dato: LocalDate,
        override val kilde: HendelseskildeDto,
        val other: HendelseskildeDto,
        val melding: String
    ) : SykdomstidslinjeDagDto()
    data class AndreYtelserDto(override val dato: LocalDate, override val kilde: HendelseskildeDto, val ytelse: YtelseDto) : SykdomstidslinjeDagDto() {
        enum class YtelseDto {
            Foreldrepenger, AAP, Omsorgspenger, Pleiepenger, Svangerskapspenger, Opplæringspenger, Dagpenger
        }
    }

    internal companion object {
        fun gjenopprett(dag: SykdomstidslinjeDagDto): Dag {
            return when (dag) {
                is AndreYtelserDto -> Dag.AndreYtelser.gjenopprett(dag)
                is ArbeidIkkeGjenopptattDagDto -> Dag.ArbeidIkkeGjenopptattDag.gjenopprett(dag)
                is ArbeidsdagDto -> Dag.Arbeidsdag.gjenopprett(dag)
                is ArbeidsgiverHelgedagDto -> Dag.ArbeidsgiverHelgedag.gjenopprett(dag)
                is ArbeidsgiverdagDto -> Dag.Arbeidsgiverdag.gjenopprett(dag)
                is FeriedagDto -> Dag.Feriedag.gjenopprett(dag)
                is ForeldetSykedagDto -> Dag.ForeldetSykedag.gjenopprett(dag)
                is FriskHelgedagDto -> Dag.FriskHelgedag.gjenopprett(dag)
                is PermisjonsdagDto -> Dag.Permisjonsdag.gjenopprett(dag)
                is ProblemDagDto -> Dag.ProblemDag.gjenopprett(dag)
                is SykHelgedagDto -> Dag.SykHelgedag.gjenopprett(dag)
                is SykedagDto -> Dag.Sykedag.gjenopprett(dag)
                is SykedagNavDto -> Dag.SykedagNav.gjenopprett(dag)
                is UkjentDagDto -> Dag.UkjentDag.gjenopprett(dag)
            }
        }
    }
}
data class HendelseskildeDto(
    val type: String,
    val meldingsreferanseId: UUID,
    val tidsstempel: LocalDateTime
)
data class ForkastetVedtaksperiodeDto(val vedtaksperiode: VedtaksperiodeDto)
data class VedtaksperiodeDto(
    val id: UUID,
    var tilstand: VedtaksperiodetilstandDto,
    val generasjoner: GenerasjonerDto,
    val opprettet: LocalDateTime,
    var oppdatert: LocalDateTime
)

sealed class VedtaksperiodetilstandDto {
    data object AVVENTER_HISTORIKK : VedtaksperiodetilstandDto()
    data object AVVENTER_GODKJENNING : VedtaksperiodetilstandDto()
    data object AVVENTER_SIMULERING : VedtaksperiodetilstandDto()
    data object TIL_UTBETALING : VedtaksperiodetilstandDto()
    data object TIL_INFOTRYGD : VedtaksperiodetilstandDto()
    data object AVSLUTTET : VedtaksperiodetilstandDto()
    data object AVSLUTTET_UTEN_UTBETALING : VedtaksperiodetilstandDto()
    data object REVURDERING_FEILET : VedtaksperiodetilstandDto()
    data object START : VedtaksperiodetilstandDto()
    data object AVVENTER_INFOTRYGDHISTORIKK : VedtaksperiodetilstandDto()
    data object AVVENTER_INNTEKTSMELDING : VedtaksperiodetilstandDto()
    data object AVVENTER_BLOKKERENDE_PERIODE : VedtaksperiodetilstandDto()
    data object AVVENTER_VILKÅRSPRØVING : VedtaksperiodetilstandDto()
    data object AVVENTER_REVURDERING : VedtaksperiodetilstandDto()
    data object AVVENTER_HISTORIKK_REVURDERING : VedtaksperiodetilstandDto()
    data object AVVENTER_VILKÅRSPRØVING_REVURDERING : VedtaksperiodetilstandDto()
    data object AVVENTER_SIMULERING_REVURDERING : VedtaksperiodetilstandDto()
    data object AVVENTER_GODKJENNING_REVURDERING : VedtaksperiodetilstandDto()
}

data class GenerasjonerDto(val generasjoner: List<GenerasjonDto>)
data class GenerasjonDto(
    val id: UUID,
    val tilstand: GenerasjonTilstandDto,
    val endringer: List<GenerasjonEndringDto>,
    val vedtakFattet: LocalDateTime?,
    val avsluttet: LocalDateTime?,
    val kilde: GenerasjonkildeDto,
)

sealed class GenerasjonTilstandDto {
    data object UBEREGNET : GenerasjonTilstandDto()
    data object UBEREGNET_OMGJØRING : GenerasjonTilstandDto()
    data object UBEREGNET_REVURDERING : GenerasjonTilstandDto()
    data object BEREGNET : GenerasjonTilstandDto()
    data object BEREGNET_OMGJØRING : GenerasjonTilstandDto()
    data object BEREGNET_REVURDERING : GenerasjonTilstandDto()
    data object VEDTAK_FATTET : GenerasjonTilstandDto()
    data object REVURDERT_VEDTAK_AVVIST : GenerasjonTilstandDto()
    data object VEDTAK_IVERKSATT : GenerasjonTilstandDto()
    data object AVSLUTTET_UTEN_VEDTAK : GenerasjonTilstandDto()
    data object ANNULLERT_PERIODE : GenerasjonTilstandDto()
    data object TIL_INFOTRYGD : GenerasjonTilstandDto()
}

data class GenerasjonEndringDto(
    val id: UUID,
    val tidsstempel: LocalDateTime,
    val sykmeldingsperiode: PeriodeDto,
    val periode: PeriodeDto,
    val vilkårsgrunnlagId: UUID?,
    val utbetalingId: UUID?,
    val dokumentsporing: DokumentsporingDto,
    val sykdomstidslinje: SykdomstidslinjeDto
)

data class DokumentsporingDto(
    val id: UUID,
    val type: DokumenttypeDto
)

sealed class DokumenttypeDto {
    data object Sykmelding : DokumenttypeDto()
    data object Søknad : DokumenttypeDto()
    data object InntektsmeldingInntekt : DokumenttypeDto()
    data object InntektsmeldingDager : DokumenttypeDto()
    data object OverstyrTidslinje : DokumenttypeDto()
    data object OverstyrInntekt : DokumenttypeDto()
    data object OverstyrRefusjon : DokumenttypeDto()
    data object OverstyrArbeidsgiveropplysninger : DokumenttypeDto()
    data object OverstyrArbeidsforhold : DokumenttypeDto()
    data object SkjønnsmessigFastsettelse : DokumenttypeDto()
}

data class GenerasjonkildeDto(
    val meldingsreferanseId: UUID,
    val innsendt: LocalDateTime,
    val registert: LocalDateTime,
    val avsender: AvsenderDto
)

data class VilkårsgrunnlaghistorikkDto(
    val historikk: List<VilkårsgrunnlagInnslagDto>
)

data class VilkårsgrunnlagInnslagDto(
    val id: UUID,
    val opprettet: LocalDateTime,
    val vilkårsgrunnlag: List<VilkårsgrunnlagDto>
)

sealed class VilkårsgrunnlagDto {
    abstract val vilkårsgrunnlagId: UUID
    abstract val skjæringstidspunkt: LocalDate
    abstract val sykepengegrunnlag: SykepengegrunnlagDto

    data class Spleis(
        override val vilkårsgrunnlagId: UUID,
        override val skjæringstidspunkt: LocalDate,
        override val sykepengegrunnlag: SykepengegrunnlagDto,
        val opptjening: OpptjeningDto,
        val medlemskapstatus: MedlemskapsvurderingDto,
        val vurdertOk: Boolean,
        val meldingsreferanseId: UUID?
    ) : VilkårsgrunnlagDto()
    data class Infotrygd(
        override val vilkårsgrunnlagId: UUID,
        override val skjæringstidspunkt: LocalDate,
        override val sykepengegrunnlag: SykepengegrunnlagDto
    ) : VilkårsgrunnlagDto()
}

sealed class MedlemskapsvurderingDto {
    data object Ja : MedlemskapsvurderingDto()
    data object Nei : MedlemskapsvurderingDto()
    data object VetIkke : MedlemskapsvurderingDto()
    data object UavklartMedBrukerspørsmål : MedlemskapsvurderingDto()
}

data class SykepengegrunnlagDto(
    val arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysningDto>,
    val deaktiverteArbeidsforhold: List<ArbeidsgiverInntektsopplysningDto>,
    val vurdertInfotrygd: Boolean,
    val sammenligningsgrunnlag: SammenligningsgrunnlagDto,
    val `6G`: InntektDto.Årlig
)
data class ArbeidsgiverInntektsopplysningDto(
    val orgnummer: String,
    val gjelder: PeriodeDto,
    val inntektsopplysning: InntektsopplysningDto,
    val refusjonsopplysninger: RefusjonsopplysningerDto
)
sealed class InntektsopplysningDto {
    abstract val id: UUID
    abstract val hendelseId: UUID
    abstract val dato: LocalDate
    abstract val beløp: InntektDto.MånedligDouble?
    abstract val tidsstempel: LocalDateTime

    data class IkkeRapportertDto(
        override val id: UUID,
        override val hendelseId: UUID,
        override val dato: LocalDate,
        override val tidsstempel: LocalDateTime
    ) : InntektsopplysningDto() {
        override val beløp: InntektDto.MånedligDouble? = null
    }

    data class InfotrygdDto(
        override val id: UUID,
        override val hendelseId: UUID,
        override val dato: LocalDate,
        override val beløp: InntektDto.MånedligDouble,
        override val tidsstempel: LocalDateTime
    ) : InntektsopplysningDto()

    data class SaksbehandlerDto(
        override val id: UUID,
        override val hendelseId: UUID,
        override val dato: LocalDate,
        override val beløp: InntektDto.MånedligDouble,
        override val tidsstempel: LocalDateTime,
        val forklaring: String?,
        val subsumsjon: SubsumsjonDto?,
        val overstyrtInntekt: UUID,
    ) : InntektsopplysningDto()

    data class SkjønnsmessigFastsattDto(
        override val id: UUID,
        override val hendelseId: UUID,
        override val dato: LocalDate,
        override val beløp: InntektDto.MånedligDouble,
        override val tidsstempel: LocalDateTime,
        val overstyrtInntekt: UUID,
    ) : InntektsopplysningDto()

    data class InntektsmeldingDto(
        override val id: UUID,
        override val hendelseId: UUID,
        override val dato: LocalDate,
        override val beløp: InntektDto.MånedligDouble,
        override val tidsstempel: LocalDateTime
    ) : InntektsopplysningDto()
    data class SkattSykepengegrunnlagDto(
        override val id: UUID,
        override val hendelseId: UUID,
        override val dato: LocalDate,
        override val tidsstempel: LocalDateTime,
        val inntektsopplysninger: List<SkatteopplysningDto>,
        val ansattPerioder: List<AnsattPeriodeDto>
    ) : InntektsopplysningDto() {
        override val beløp: InntektDto.MånedligDouble? = null
    }
}
data class AnsattPeriodeDto(val fom: LocalDate, val tom: LocalDate?)
data class SubsumsjonDto(
    val paragraf: String,
    val ledd: Int?,
    val bokstav: String?,
)
data class SammenligningsgrunnlagDto(
    val sammenligningsgrunnlag: InntektDto.Årlig,
    val arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysningForSammenligningsgrunnlagDto>,
)
data class ArbeidsgiverInntektsopplysningForSammenligningsgrunnlagDto(
    val orgnummer: String,
    val inntektsopplysninger: List<SkatteopplysningDto>
)
data class SkatteopplysningDto(
    val hendelseId: UUID,
    val beløp: InntektDto.MånedligDouble,
    val måned: YearMonth,
    val type: InntekttypeDto,
    val fordel: String,
    val beskrivelse: String,
    val tidsstempel: LocalDateTime
)
sealed class InntekttypeDto {
    data object LØNNSINNTEKT : InntekttypeDto()
    data object NÆRINGSINNTEKT : InntekttypeDto()
    data object PENSJON_ELLER_TRYGD : InntekttypeDto()
    data object YTELSE_FRA_OFFENTLIGE : InntekttypeDto()
}
data class RefusjonsopplysningerDto(val opplysninger: List<RefusjonsopplysningDto>)
data class RefusjonsopplysningDto(
    val meldingsreferanseId: UUID,
    val fom: LocalDate,
    val tom: LocalDate?,
    val beløp: InntektDto.MånedligDouble
)
data class OpptjeningDto(
    val arbeidsforhold: List<ArbeidsgiverOpptjeningsgrunnlagDto>,
    val opptjeningsperiode: PeriodeDto
)

data class ArbeidsgiverOpptjeningsgrunnlagDto(
    val orgnummer: String,
    val ansattPerioder: List<ArbeidsforholdDto>
)

data class ArbeidsforholdDto(
    val ansattFom: LocalDate,
    val ansattTom: LocalDate?,
    val deaktivert: Boolean
)

data class FeriepengeDto(
    val feriepengeberegner: FeriepengeberegnerDto,
    val infotrygdFeriepengebeløpPerson: Double,
    val infotrygdFeriepengebeløpArbeidsgiver: Double,
    val spleisFeriepengebeløpArbeidsgiver: Double,
    val spleisFeriepengebeløpPerson: Double,
    val oppdrag: OppdragDto,
    val personoppdrag: OppdragDto,
    val utbetalingId: UUID,
    val sendTilOppdrag: Boolean,
    val sendPersonoppdragTilOS: Boolean,
)

data class FeriepengeberegnerDto(
    val opptjeningsår: Year,
    val utbetalteDager: List<UtbetaltDagDto>,
    val feriepengedager: List<UtbetaltDagDto>
)
sealed class UtbetaltDagDto {
    abstract val orgnummer: String
    abstract val dato: LocalDate
    abstract val beløp: Int

    data class InfotrygdArbeidsgiver(
        override val orgnummer: String,
        override val dato: LocalDate,
        override val beløp: Int
    ) : UtbetaltDagDto()
    data class InfotrygdPerson(
        override val orgnummer: String,
        override val dato: LocalDate,
        override val beløp: Int
    ) : UtbetaltDagDto()
    data class SpleisArbeidsgiver(
        override val orgnummer: String,
        override val dato: LocalDate,
        override val beløp: Int
    ) : UtbetaltDagDto()
    data class SpleisPerson(
        override val orgnummer: String,
        override val dato: LocalDate,
        override val beløp: Int
    ) : UtbetaltDagDto()
}

data class InfotrygdhistorikkDto(
    val elementer: List<InfotrygdhistorikkelementDto>
)

data class InfotrygdhistorikkelementDto(
    val id: UUID,
    val tidsstempel: LocalDateTime,
    val hendelseId: UUID?,
    val ferieperioder: List<InfotrygdFerieperiodeDto>,
    val arbeidsgiverutbetalingsperioder: List<InfotrygdArbeidsgiverutbetalingsperiodeDto>,
    val personutbetalingsperioder: List<InfotrygdPersonutbetalingsperiodeDto>,
    val inntekter: List<InfotrygdInntektsopplysningDto>,
    val arbeidskategorikoder: Map<String, LocalDate>,
    val oppdatert: LocalDateTime
)

data class InfotrygdFerieperiodeDto(val periode: PeriodeDto)
data class InfotrygdArbeidsgiverutbetalingsperiodeDto(
    val orgnr: String,
    val periode: PeriodeDto,
    val grad: ProsentdelDto,
    val inntekt: InntektDto.DagligInt
)
data class InfotrygdPersonutbetalingsperiodeDto(
    val orgnr: String,
    val periode: PeriodeDto,
    val grad: ProsentdelDto,
    val inntekt: InntektDto.DagligInt
)
data class InfotrygdInntektsopplysningDto(
    val orgnummer: String,
    val sykepengerFom: LocalDate,
    val inntekt: InntektDto.MånedligDouble,
    val refusjonTilArbeidsgiver: Boolean,
    val refusjonTom: LocalDate?,
    val lagret: LocalDateTime?
)