package no.nav.helse.spleis.rest.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

enum class Inntektstype {
    EnArbeidsgiver
}

enum class Periodetype {
    Forstegangsbehandling,
    Forlengelse,
    OvergangFraIt,
    Infotrygdforlengelse
}

enum class Sykdomsdagtype {
    Arbeidsdag,
    Arbeidsgiverdag,
    MeldingTilNavDag,
    AvslattMeldingTilNavDag,
    Feriedag,
    ArbeidIkkeGjenopptattDag,
    ForeldetSykedag,
    FriskHelgedag,
    Permisjonsdag,
    Sykedag,
    SykedagNav,
    SykHelgedag,
    Ubestemtdag,
    AndreYtelserForeldrepenger,
    AndreYtelserAap,
    AndreYtelserOmsorgspenger,
    AndreYtelserPleiepenger,
    AndreYtelserSvangerskapspenger,
    AndreYtelserOpplaringspenger,
    AndreYtelserDagpenger
}

enum class UtbetalingsdagType {
    ArbeidsgiverperiodeDag,
    NavDag,
    NavHelgDag,
    Helgedag,
    Arbeidsdag,
    Feriedag,
    AvvistDag,
    UkjentDag,
    ForeldetDag,
    Ventetidsdag
}

enum class Sykdomsdagkildetype {
    Inntektsmelding,
    Soknad,
    Sykmelding,
    Saksbehandler,
    Ukjent
}

enum class Begrunnelse {
    SykepengedagerOppbrukt,
    SykepengedagerOppbruktOver67,
    MinimumInntekt,
    MinimumInntektOver67,
    EgenmeldingUtenforArbeidsgiverperiode,
    AvslattMeldingTilNavDag,
    MeldingTilNavDagUtenforVentetid,
    AndreYtelser,
    MinimumSykdomsgrad,
    EtterDodsdato,
    ManglerMedlemskap,
    ManglerOpptjening,
    Over70
}

enum class Periodetilstand {
    TilUtbetaling,
    TilAnnullering,
    AvventerAnnullering,
    Utbetalt,
    Annullert,
    AnnulleringFeilet,
    IngenUtbetaling,
    RevurderingFeilet,
    TilInfotrygd,
    ForberederGodkjenning,
    ManglerInformasjon,
    VenterPaAnnenPeriode,
    UtbetaltVenterPaAnnenPeriode,
    AvventerInntektsopplysninger,
    TilGodkjenning
}

enum class Utbetalingstatus {
    Annullert,
    GodkjentUtenUtbetaling,
    IkkeGodkjent,
    Overfort,
    Ubetalt,
    Utbetalt
}

enum class Utbetalingtype {
    UTBETALING,
    ETTERUTBETALING,
    ANNULLERING,
    REVURDERING,
    FERIEPENGER
}

data class Sykdomsdagkilde(
    val id: UUID,
    val type: Sykdomsdagkildetype
)

data class Utbetalingsinfo(
    val inntekt: Int?,
    val utbetaling: Int?,
    val personbelop: Int?,
    val arbeidsgiverbelop: Int?,
    val refusjonsbelop: Int?,
    val totalGrad: Double?
)

data class Vurdering(
    val godkjent: Boolean,
    val tidsstempel: LocalDateTime,
    val automatisk: Boolean,
    val ident: String
)

data class Utbetalingslinje(
    val fom: LocalDate,
    val tom: LocalDate,
    val dagsats: Int,
    val grad: Int
)

data class Oppdrag(
    val fagsystemId: String,
    val tidsstempel: LocalDateTime,
    val simulering: Simulering?,
    val utbetalingslinjer: List<Utbetalingslinje>
)

data class Utbetaling(
    val id: UUID,
    val typeEnum: Utbetalingtype,
    val statusEnum: Utbetalingstatus,
    val arbeidsgiverNettoBelop: Int,
    val personNettoBelop: Int,
    val arbeidsgiverFagsystemId: String,
    val personFagsystemId: String,
    val arbeidsgiveroppdrag: Oppdrag?,
    val personoppdrag: Oppdrag?,
    val vurdering: Vurdering?
)

data class Dag(
    val dato: LocalDate,
    val sykdomsdagtype: Sykdomsdagtype,
    val utbetalingsdagtype: UtbetalingsdagType,
    val kilde: Sykdomsdagkilde,
    val grad: Double?,
    val utbetalingsinfo: Utbetalingsinfo?,
    val begrunnelser: List<Begrunnelse>?
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = BeregnetPeriode::class, name = "BeregnetPeriode"),
    JsonSubTypes.Type(value = UberegnetPeriode::class, name = "UberegnetPeriode")
)
sealed interface Tidslinjeperiode {
    val behandlingId: UUID
    val kilde: UUID
    val fom: LocalDate
    val tom: LocalDate
    val tidslinje: List<Dag>
    val periodetype: Periodetype
    val hendelser: List<Hendelse>

    // 040423: dette feltet virker ikke å være i bruk i Speil, men spesialist nullsjekker det
    val inntektstype: Inntektstype
    val erForkastet: Boolean
    val opprettet: LocalDateTime
    val vedtaksperiodeId: UUID
    val periodetilstand: Periodetilstand
    val skjaeringstidspunkt: LocalDate
    val pensjonsgivendeInntekter: List<PensjonsgivendeInntekt>
}

data class UberegnetPeriode(
    override val behandlingId: UUID,
    override val kilde: UUID,
    override val fom: LocalDate,
    override val tom: LocalDate,
    override val tidslinje: List<Dag>,
    override val periodetype: Periodetype,
    override val erForkastet: Boolean,
    override val opprettet: LocalDateTime,
    override val vedtaksperiodeId: UUID,
    override val periodetilstand: Periodetilstand,
    override val skjaeringstidspunkt: LocalDate,
    override val hendelser: List<Hendelse>,
    override val pensjonsgivendeInntekter: List<PensjonsgivendeInntekt>
) : Tidslinjeperiode {
    override val inntektstype: Inntektstype get() = Inntektstype.EnArbeidsgiver
}

data class BeregnetPeriode(
    override val behandlingId: UUID,
    override val kilde: UUID,
    override val fom: LocalDate,
    override val tom: LocalDate,
    override val tidslinje: List<Dag>,
    override val periodetype: Periodetype,
    override val erForkastet: Boolean,
    override val opprettet: LocalDateTime,
    override val vedtaksperiodeId: UUID,
    override val periodetilstand: Periodetilstand,
    override val skjaeringstidspunkt: LocalDate,
    override val hendelser: List<Hendelse>,
    override val pensjonsgivendeInntekter: List<PensjonsgivendeInntekt>,
    val beregningId: UUID,
    val gjenstaendeSykedager: Int?,
    val forbrukteSykedager: Int?,
    val maksdato: LocalDate,
    val utbetaling: Utbetaling,
    val periodevilkar: Periodevilkar,
    val vilkarsgrunnlagId: UUID?,
    val annulleringskandidater: List<Annulleringskandidat>
) : Tidslinjeperiode {
    override val inntektstype: Inntektstype get() = Inntektstype.EnArbeidsgiver
}

data class Periodevilkar(
    val sykepengedager: Sykepengedager,
    val alder: Alder
) {
    data class Sykepengedager(
        val skjaeringstidspunkt: LocalDate,
        val maksdato: LocalDate,
        val forbrukteSykedager: Int?,
        val gjenstaendeSykedager: Int?,
        val oppfylt: Boolean
    )

    data class Alder(
        val alderSisteSykedag: Int,
        val oppfylt: Boolean
    )
}

data class PensjonsgivendeInntekt(
    val inntektsar: Int,
    val arligBelop: Double
)

data class Annulleringskandidat(
    val vedtaksperiodeId: UUID,
    val organisasjonsnummer: String,
    val fom: LocalDate,
    val tom: LocalDate
)
