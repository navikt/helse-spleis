package no.nav.helse.memento

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import java.time.YearMonth
import java.util.UUID

data class PersonMemento(
    val aktørId: String,
    val fødselsnummer: String,
    val alder: AlderMemento,
    val opprettet: LocalDateTime,
    val arbeidsgivere: List<ArbeidsgiverMemento>,
    val infotrygdhistorikk: InfotrygdhistorikkMemento,
    val vilkårsgrunnlagHistorikk: VilkårsgrunnlaghistorikkMemento
) {
}

data class AlderMemento(val fødselsdato: LocalDate, val dødsdato: LocalDate?)

data class ArbeidsgiverMemento(
    val id: UUID,
    val organisasjonsnummer: String,
    val inntektshistorikk: InntektshistorikkMemento,
    val sykdomshistorikk: SykdomshistorikkMemento,
    val sykmeldingsperioder: SykmeldingsperioderMemento,
    val vedtaksperioder: List<VedtaksperiodeMemento>,
    val forkastede: List<ForkastetVedtaksperiodeMemento>,
    val utbetalinger: List<UtbetalingMemento>,
    val feriepengeutbetalinger: List<FeriepengeMemento>,
    val refusjonshistorikk: RefusjonshistorikkMemento
)

data class SykmeldingsperioderMemento(val perioder: List<PeriodeMemento>)
data class RefusjonshistorikkMemento(val refusjoner: List<RefusjonMemento>)
data class RefusjonMemento(
    val meldingsreferanseId: UUID,
    val førsteFraværsdag: LocalDate?,
    val arbeidsgiverperioder: List<PeriodeMemento>,
    val beløp: InntektMemento?,
    val sisteRefusjonsdag: LocalDate?,
    val endringerIRefusjon: List<EndringIRefusjonMemento>,
    val tidsstempel: LocalDateTime
)
data class EndringIRefusjonMemento(
    val beløp: InntektMemento,
    val endringsdato: LocalDate
)
data class InntektshistorikkMemento(val historikk: List<InntektsopplysningMemento.InntektsmeldingMemento>)

data class SykdomshistorikkMemento(val elementer: List<SykdomshistorikkElementMemento>)
data class SykdomshistorikkElementMemento(
    val id: UUID,
    val hendelseId: UUID?,
    val tidsstempel: LocalDateTime,
    val hendelseSykdomstidslinje: SykdomstidslinjeMemento,
    val beregnetSykdomstidslinje: SykdomstidslinjeMemento,
)

data class SykdomstidslinjeMemento(
    val dager: List<DagMemento>,
    val periode: PeriodeMemento?,
    val låstePerioder: List<PeriodeMemento>
)

sealed class DagMemento {
    abstract val dato: LocalDate
    abstract val kilde: HendelseskildeMemento

    data class UkjentDagMemento(override val dato: LocalDate, override val kilde: HendelseskildeMemento) : DagMemento()
    data class ArbeidsdagMemento(override val dato: LocalDate, override val kilde: HendelseskildeMemento) : DagMemento()
    data class ArbeidsgiverdagMemento(override val dato: LocalDate, override val kilde: HendelseskildeMemento, val økonomi: ØkonomiMemento) : DagMemento()
    data class FeriedagMemento(override val dato: LocalDate, override val kilde: HendelseskildeMemento) : DagMemento()
    data class ArbeidIkkeGjenopptattDagMemento(override val dato: LocalDate, override val kilde: HendelseskildeMemento) : DagMemento()
    data class FriskHelgedagMemento(override val dato: LocalDate, override val kilde: HendelseskildeMemento) : DagMemento()
    data class ArbeidsgiverHelgedagMemento(override val dato: LocalDate, override val kilde: HendelseskildeMemento, val økonomi: ØkonomiMemento) : DagMemento()
    data class SykedagMemento(override val dato: LocalDate, override val kilde: HendelseskildeMemento, val økonomi: ØkonomiMemento) : DagMemento()
    data class SykHelgedagMemento(override val dato: LocalDate, override val kilde: HendelseskildeMemento, val økonomi: ØkonomiMemento) : DagMemento()
    data class SykedagNavMemento(override val dato: LocalDate, override val kilde: HendelseskildeMemento, val økonomi: ØkonomiMemento) : DagMemento()
    data class ForeldetSykedagMemento(override val dato: LocalDate, override val kilde: HendelseskildeMemento, val økonomi: ØkonomiMemento) : DagMemento()
    data class PermisjonsdagMemento(override val dato: LocalDate, override val kilde: HendelseskildeMemento) : DagMemento()
    data class ProblemDagMemento(
        override val dato: LocalDate,
        override val kilde: HendelseskildeMemento,
        val other: HendelseskildeMemento,
        val melding: String
    ) : DagMemento()
    data class AndreYtelserMemento(override val dato: LocalDate, override val kilde: HendelseskildeMemento, val ytelse: YtelseMemento) : DagMemento() {
        enum class YtelseMemento {
            Foreldrepenger, AAP, Omsorgspenger, Pleiepenger, Svangerskapspenger, Opplæringspenger, Dagpenger
        }
    }
}
data class HendelseskildeMemento(
    val type: String,
    val meldingsreferanseId: UUID,
    val tidsstempel: LocalDateTime
)
data class ForkastetVedtaksperiodeMemento(val vedtaksperiode: VedtaksperiodeMemento)
data class VedtaksperiodeMemento(
    val id: UUID,
    var tilstand: VedtaksperiodetilstandMemento,
    val generasjoner: GenerasjonerMemento,
    val opprettet: LocalDateTime,
    var oppdatert: LocalDateTime
)

sealed class VedtaksperiodetilstandMemento {
    data object AVVENTER_HISTORIKK : VedtaksperiodetilstandMemento()
    data object AVVENTER_GODKJENNING : VedtaksperiodetilstandMemento()
    data object AVVENTER_SIMULERING : VedtaksperiodetilstandMemento()
    data object TIL_UTBETALING : VedtaksperiodetilstandMemento()
    data object TIL_INFOTRYGD : VedtaksperiodetilstandMemento()
    data object AVSLUTTET : VedtaksperiodetilstandMemento()
    data object AVSLUTTET_UTEN_UTBETALING : VedtaksperiodetilstandMemento()
    data object REVURDERING_FEILET : VedtaksperiodetilstandMemento()
    data object START : VedtaksperiodetilstandMemento()
    data object AVVENTER_INFOTRYGDHISTORIKK : VedtaksperiodetilstandMemento()
    data object AVVENTER_INNTEKTSMELDING : VedtaksperiodetilstandMemento()
    data object AVVENTER_BLOKKERENDE_PERIODE : VedtaksperiodetilstandMemento()
    data object AVVENTER_VILKÅRSPRØVING : VedtaksperiodetilstandMemento()
    data object AVVENTER_REVURDERING : VedtaksperiodetilstandMemento()
    data object AVVENTER_HISTORIKK_REVURDERING : VedtaksperiodetilstandMemento()
    data object AVVENTER_VILKÅRSPRØVING_REVURDERING : VedtaksperiodetilstandMemento()
    data object AVVENTER_SIMULERING_REVURDERING : VedtaksperiodetilstandMemento()
    data object AVVENTER_GODKJENNING_REVURDERING : VedtaksperiodetilstandMemento()
}

data class GenerasjonerMemento(val generasjoner: List<GenerasjonMemento>)
data class GenerasjonMemento(
    val id: UUID,
    val tilstand: GenerasjonTilstandMemento,
    val endringer: List<GenerasjonEndringMemento>,
    val vedtakFattet: LocalDateTime?,
    val avsluttet: LocalDateTime?,
    val kilde: GenerasjonkildeMemento,
)

sealed class GenerasjonTilstandMemento {
    data object UBEREGNET : GenerasjonTilstandMemento()
    data object UBEREGNET_OMGJØRING : GenerasjonTilstandMemento()
    data object UBEREGNET_REVURDERING : GenerasjonTilstandMemento()
    data object BEREGNET : GenerasjonTilstandMemento()
    data object BEREGNET_OMGJØRING : GenerasjonTilstandMemento()
    data object BEREGNET_REVURDERING : GenerasjonTilstandMemento()
    data object VEDTAK_FATTET : GenerasjonTilstandMemento()
    data object REVURDERT_VEDTAK_AVVIST : GenerasjonTilstandMemento()
    data object VEDTAK_IVERKSATT : GenerasjonTilstandMemento()
    data object AVSLUTTET_UTEN_VEDTAK : GenerasjonTilstandMemento()
    data object ANNULLERT_PERIODE : GenerasjonTilstandMemento()
    data object TIL_INFOTRYGD : GenerasjonTilstandMemento()
}

data class GenerasjonEndringMemento(
    val id: UUID,
    val tidsstempel: LocalDateTime,
    val sykmeldingsperiode: PeriodeMemento,
    val periode: PeriodeMemento,
    val vilkårsgrunnlagId: UUID?,
    val utbetalingId: UUID?,
    val dokumentsporing: DokumentsporingMemento,
    val sykdomstidslinje: SykdomstidslinjeMemento
)

data class DokumentsporingMemento(
    val id: UUID,
    val type: DokumenttypeMemento
)

sealed class DokumenttypeMemento {
    data object Sykmelding : DokumenttypeMemento()
    data object Søknad : DokumenttypeMemento()
    data object InntektsmeldingInntekt : DokumenttypeMemento()
    data object InntektsmeldingDager : DokumenttypeMemento()
    data object OverstyrTidslinje : DokumenttypeMemento()
    data object OverstyrInntekt : DokumenttypeMemento()
    data object OverstyrRefusjon : DokumenttypeMemento()
    data object OverstyrArbeidsgiveropplysninger : DokumenttypeMemento()
    data object OverstyrArbeidsforhold : DokumenttypeMemento()
    data object SkjønnsmessigFastsettelse : DokumenttypeMemento()
}

data class GenerasjonkildeMemento(
    val meldingsreferanseId: UUID,
    val innsendt: LocalDateTime,
    val registert: LocalDateTime,
    val avsender: AvsenderMemento
)
sealed class AvsenderMemento {
    data object SYKMELDT : AvsenderMemento()
    data object ARBEIDSGIVER : AvsenderMemento()
    data object SAKSBEHANDLER : AvsenderMemento()
    data object SYSTEM : AvsenderMemento()
}

data class VilkårsgrunnlaghistorikkMemento(
    val historikk: List<VilkårsgrunnlagInnslagMemento>
)

data class VilkårsgrunnlagInnslagMemento(
    val id: UUID,
    val opprettet: LocalDateTime,
    val vilkårsgrunnlag: List<VilkårsgrunnlagMemento>
)

sealed class VilkårsgrunnlagMemento {
    abstract val vilkårsgrunnlagId: UUID
    abstract val skjæringstidspunkt: LocalDate
    abstract val sykepengegrunnlag: SykepengegrunnlagMemento
    abstract val opptjening: OpptjeningMemento?

    data class Spleis(
        override val vilkårsgrunnlagId: UUID,
        override val skjæringstidspunkt: LocalDate,
        override val sykepengegrunnlag: SykepengegrunnlagMemento,
        override val opptjening: OpptjeningMemento?,
        val medlemskapstatus: MedlemskapsvurderingMemento,
        val vurdertOk: Boolean,
        val meldingsreferanseId: UUID?
    ) : VilkårsgrunnlagMemento()
    data class Infotrygd(
        override val vilkårsgrunnlagId: UUID,
        override val skjæringstidspunkt: LocalDate,
        override val sykepengegrunnlag: SykepengegrunnlagMemento,
        override val opptjening: OpptjeningMemento?
    ) : VilkårsgrunnlagMemento()
}

sealed class MedlemskapsvurderingMemento {
    data object Ja : MedlemskapsvurderingMemento()
    data object Nei : MedlemskapsvurderingMemento()
    data object VetIkke : MedlemskapsvurderingMemento()
    data object UavklartMedBrukerspørsmål : MedlemskapsvurderingMemento()
}

data class SykepengegrunnlagMemento(
    val arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysningMemento>,
    val deaktiverteArbeidsforhold: List<ArbeidsgiverInntektsopplysningMemento>,
    val vurdertInfotrygd: Boolean,
    val sammenligningsgrunnlag: SammenligningsgrunnlagMemento,
    val `6G`: InntektMemento?
)
data class ArbeidsgiverInntektsopplysningMemento(
    val orgnummer: String,
    val gjelder: PeriodeMemento,
    val inntektsopplysning: InntektsopplysningMemento,
    val refusjonsopplysninger: RefusjonsopplysningerMemento
)
sealed class InntektsopplysningMemento {
    abstract val id: UUID
    abstract val hendelseId: UUID
    abstract val dato: LocalDate
    abstract val beløp: InntektMemento
    abstract val tidsstempel: LocalDateTime

    data class IkkeRapportertMemento(
        override val id: UUID,
        override val hendelseId: UUID,
        override val dato: LocalDate,
        override val beløp: InntektMemento,
        override val tidsstempel: LocalDateTime
    ) : InntektsopplysningMemento()

    data class InfotrygdMemento(
        override val id: UUID,
        override val hendelseId: UUID,
        override val dato: LocalDate,
        override val beløp: InntektMemento,
        override val tidsstempel: LocalDateTime
    ) : InntektsopplysningMemento()

    data class SaksbehandlerMemento(
        override val id: UUID,
        override val hendelseId: UUID,
        override val dato: LocalDate,
        override val beløp: InntektMemento,
        override val tidsstempel: LocalDateTime,
        val forklaring: String?,
        val subsumsjon: SubsumsjonMemento?,
        val overstyrtInntekt: InntektsopplysningMemento,
    ) : InntektsopplysningMemento()

    data class SkjønnsmessigFastsattMemento(
        override val id: UUID,
        override val hendelseId: UUID,
        override val dato: LocalDate,
        override val beløp: InntektMemento,
        override val tidsstempel: LocalDateTime,
        val overstyrtInntekt: InntektsopplysningMemento,
    ) : InntektsopplysningMemento()

    data class InntektsmeldingMemento(
        override val id: UUID,
        override val hendelseId: UUID,
        override val dato: LocalDate,
        override val beløp: InntektMemento,
        override val tidsstempel: LocalDateTime
    ) : InntektsopplysningMemento()
    data class SkattSykepengegrunnlagMemento(
        override val id: UUID,
        override val hendelseId: UUID,
        override val dato: LocalDate,
        override val beløp: InntektMemento,
        override val tidsstempel: LocalDateTime,
        val inntektsopplysninger: List<SkatteopplysningMemento>,
        val ansattPerioder: List<AnsattPeriodeMemento>
    ) : InntektsopplysningMemento()
}
data class AnsattPeriodeMemento(val fom: LocalDate, val tom: LocalDate?)
data class SubsumsjonMemento(
    val paragraf: String,
    val ledd: Int?,
    val bokstav: String?,
)
data class SammenligningsgrunnlagMemento(
    val sammenligningsgrunnlag: InntektMemento,
    val arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysningForSammenligningsgrunnlagMemento>,
)
data class ArbeidsgiverInntektsopplysningForSammenligningsgrunnlagMemento(
    val orgnummer: String,
    val inntektsopplysninger: List<SkatteopplysningMemento>
)
data class SkatteopplysningMemento(
    val hendelseId: UUID,
    val beløp: InntektMemento,
    val måned: YearMonth,
    val type: InntekttypeMemento,
    val fordel: String,
    val beskrivelse: String,
    val tidsstempel: LocalDateTime
)
sealed class InntekttypeMemento {
    data object LØNNSINNTEKT : InntekttypeMemento()
    data object NÆRINGSINNTEKT : InntekttypeMemento()
    data object PENSJON_ELLER_TRYGD : InntekttypeMemento()
    data object YTELSE_FRA_OFFENTLIGE : InntekttypeMemento()
}
data class RefusjonsopplysningerMemento(val opplysninger: List<RefusjonsopplysningMemento>)
data class RefusjonsopplysningMemento(
    val meldingsreferanseId: UUID,
    val fom: LocalDate,
    val tom: LocalDate?,
    val beløp: InntektMemento
)
data class OpptjeningMemento(
    val arbeidsforhold: List<ArbeidsgiverOpptjeningsgrunnlagMemento>,
    val opptjeningsperiode: PeriodeMemento
)

data class ArbeidsgiverOpptjeningsgrunnlagMemento(
    val orgnummer: String,
    val ansattPerioder: List<ArbeidsforholdMemento>
)

data class ArbeidsforholdMemento(
    val ansattFom: LocalDate,
    val ansattTom: LocalDate?,
    val deaktivert: Boolean
)

data class FeriepengeMemento(
    val feriepengeberegner: FeriepengeberegnerMemento,
    val infotrygdFeriepengebeløpPerson: Double,
    val infotrygdFeriepengebeløpArbeidsgiver: Double,
    val spleisFeriepengebeløpArbeidsgiver: Double,
    val spleisFeriepengebeløpPerson: Double,
    val oppdrag: OppdragMemento,
    val personoppdrag: OppdragMemento,
    val utbetalingId: UUID,
    val sendTilOppdrag: Boolean,
    val sendPersonoppdragTilOS: Boolean,
)

data class FeriepengeberegnerMemento(
    val opptjeningsår: Year,
    val utbetalteDager: List<UtbetaltDagMemento>
)
sealed class UtbetaltDagMemento {
    abstract val orgnummer: String
    abstract val dato: LocalDate
    abstract val beløp: Int

    data class InfotrygdArbeidsgiver(
        override val orgnummer: String,
        override val dato: LocalDate,
        override val beløp: Int
    ) : UtbetaltDagMemento()
    data class InfotrygdPerson(
        override val orgnummer: String,
        override val dato: LocalDate,
        override val beløp: Int
    ) : UtbetaltDagMemento()
    data class SpleisArbeidsgiver(
        override val orgnummer: String,
        override val dato: LocalDate,
        override val beløp: Int
    ) : UtbetaltDagMemento()
    data class SpleisPerson(
        override val orgnummer: String,
        override val dato: LocalDate,
        override val beløp: Int
    ) : UtbetaltDagMemento()
}

data class InfotrygdhistorikkMemento(
    val elementer: List<InfotrygdhistorikkelementMemento>
)

data class InfotrygdhistorikkelementMemento(
    val id: UUID,
    val tidsstempel: LocalDateTime,
    val hendelseId: UUID?,
    val ferieperioder: List<InfotrygdFerieperiodeMemento>,
    val arbeidsgiverutbetalingsperioder: List<InfotrygdArbeidsgiverutbetalingsperiodeMemento>,
    val personutbetalingsperioder: List<InfotrygdPersonutbetalingsperiodeMemento>,
    val inntekter: List<InfotrygdInntektsopplysningMemento>,
    val arbeidskategorikoder: Map<String, LocalDate>,
    val oppdatert: LocalDateTime
)

data class InfotrygdFerieperiodeMemento(val periode: PeriodeMemento)
data class InfotrygdArbeidsgiverutbetalingsperiodeMemento(
    val orgnr: String,
    val periode: PeriodeMemento,
    val grad: ProsentdelMemento,
    val inntekt: InntektMemento
)
data class InfotrygdPersonutbetalingsperiodeMemento(
    val orgnr: String,
    val periode: PeriodeMemento,
    val grad: ProsentdelMemento,
    val inntekt: InntektMemento
)
data class InfotrygdInntektsopplysningMemento(
    val orgnummer: String,
    val sykepengerFom: LocalDate,
    val inntekt: InntektMemento,
    val refusjonTilArbeidsgiver: Boolean,
    val refusjonTom: LocalDate?,
    val lagret: LocalDateTime?
)