package no.nav.helse.spleis.rest.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

enum class ApiInntektstype {
    EnArbeidsgiver
}

enum class ApiPeriodetype {
    Forstegangsbehandling,
    Forlengelse,
    OvergangFraIt,
    Infotrygdforlengelse
}

enum class ApiSykdomsdagtype {
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

enum class ApiUtbetalingsdagType {
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

enum class ApiSykdomsdagkildetype {
    Inntektsmelding,
    Soknad,
    Sykmelding,
    Saksbehandler,
    Ukjent
}

enum class ApiBegrunnelse {
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

enum class ApiPeriodetilstand {
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

enum class ApiUtbetalingstatus {
    Annullert,
    GodkjentUtenUtbetaling,
    IkkeGodkjent,
    Overfort,
    Ubetalt,
    Utbetalt
}

enum class ApiUtbetalingtype {
    UTBETALING,
    ETTERUTBETALING,
    ANNULLERING,
    REVURDERING,
    FERIEPENGER
}

data class ApiSykdomsdagkilde(
    val id: UUID,
    val type: ApiSykdomsdagkildetype
)

data class ApiUtbetalingsinfo(
    val inntekt: Int?,
    val utbetaling: Int?,
    val personbelop: Int?,
    val arbeidsgiverbelop: Int?,
    val refusjonsbelop: Int?,
    val totalGrad: Double?
)

data class ApiVurdering(
    val godkjent: Boolean,
    val tidsstempel: LocalDateTime,
    val automatisk: Boolean,
    val ident: String
)

data class ApiUtbetalingslinje(
    val fom: LocalDate,
    val tom: LocalDate,
    val dagsats: Int,
    val grad: Int
)

data class ApiOppdrag(
    val fagsystemId: String,
    val tidsstempel: LocalDateTime,
    val simulering: ApiSimulering?,
    val utbetalingslinjer: List<ApiUtbetalingslinje>
)

data class ApiUtbetaling(
    val id: UUID,
    val typeEnum: ApiUtbetalingtype,
    val statusEnum: ApiUtbetalingstatus,
    val arbeidsgiverNettoBelop: Int,
    val personNettoBelop: Int,
    val arbeidsgiverFagsystemId: String,
    val personFagsystemId: String,
    val arbeidsgiveroppdrag: ApiOppdrag?,
    val personoppdrag: ApiOppdrag?,
    val vurdering: ApiVurdering?
)

data class ApiDag(
    val dato: LocalDate,
    val sykdomsdagtype: ApiSykdomsdagtype,
    val utbetalingsdagtype: ApiUtbetalingsdagType,
    val kilde: ApiSykdomsdagkilde,
    val grad: Double?,
    val utbetalingsinfo: ApiUtbetalingsinfo?,
    val begrunnelser: List<ApiBegrunnelse>?
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = ApiBeregnetPeriode::class, name = "BeregnetPeriode"),
    JsonSubTypes.Type(value = ApiUberegnetPeriode::class, name = "UberegnetPeriode")
)
sealed interface ApiTidslinjeperiode {
    val behandlingId: UUID
    val kilde: UUID
    val fom: LocalDate
    val tom: LocalDate
    val tidslinje: List<ApiDag>
    val periodetype: ApiPeriodetype
    val hendelser: List<ApiHendelse>

    // 040423: dette feltet virker ikke å være i bruk i Speil, men spesialist nullsjekker det
    val inntektstype: ApiInntektstype
    val erForkastet: Boolean
    val opprettet: LocalDateTime
    val vedtaksperiodeId: UUID
    val periodetilstand: ApiPeriodetilstand
    val skjaeringstidspunkt: LocalDate
    val pensjonsgivendeInntekter: List<ApiPensjonsgivendeInntekt>
}

data class ApiUberegnetPeriode(
    override val behandlingId: UUID,
    override val kilde: UUID,
    override val fom: LocalDate,
    override val tom: LocalDate,
    override val tidslinje: List<ApiDag>,
    override val periodetype: ApiPeriodetype,
    override val erForkastet: Boolean,
    override val opprettet: LocalDateTime,
    override val vedtaksperiodeId: UUID,
    override val periodetilstand: ApiPeriodetilstand,
    override val skjaeringstidspunkt: LocalDate,
    override val hendelser: List<ApiHendelse>,
    override val pensjonsgivendeInntekter: List<ApiPensjonsgivendeInntekt>
) : ApiTidslinjeperiode {
    override val inntektstype: ApiInntektstype get() = ApiInntektstype.EnArbeidsgiver
}

data class ApiBeregnetPeriode(
    override val behandlingId: UUID,
    override val kilde: UUID,
    override val fom: LocalDate,
    override val tom: LocalDate,
    override val tidslinje: List<ApiDag>,
    override val periodetype: ApiPeriodetype,
    override val erForkastet: Boolean,
    override val opprettet: LocalDateTime,
    override val vedtaksperiodeId: UUID,
    override val periodetilstand: ApiPeriodetilstand,
    override val skjaeringstidspunkt: LocalDate,
    override val hendelser: List<ApiHendelse>,
    override val pensjonsgivendeInntekter: List<ApiPensjonsgivendeInntekt>,
    val beregningId: UUID,
    val gjenstaendeSykedager: Int?,
    val forbrukteSykedager: Int?,
    val maksdato: LocalDate,
    val utbetaling: ApiUtbetaling,
    val periodevilkar: ApiPeriodevilkar,
    val vilkarsgrunnlagId: UUID?,
    val annulleringskandidater: List<ApiAnnulleringskandidat>
) : ApiTidslinjeperiode {
    override val inntektstype: ApiInntektstype get() = ApiInntektstype.EnArbeidsgiver
}

data class ApiPeriodevilkar(
    val sykepengedager: ApiSykepengedager,
    val alder: ApiAlder
) {
    data class ApiSykepengedager(
        val skjaeringstidspunkt: LocalDate,
        val maksdato: LocalDate,
        val forbrukteSykedager: Int?,
        val gjenstaendeSykedager: Int?,
        val oppfylt: Boolean
    )

    data class ApiAlder(
        val alderSisteSykedag: Int,
        val oppfylt: Boolean
    )
}

data class ApiPensjonsgivendeInntekt(
    val inntektsar: Int,
    val arligBelop: Double
)

data class ApiAnnulleringskandidat(
    val vedtaksperiodeId: UUID,
    val organisasjonsnummer: String,
    val fom: LocalDate,
    val tom: LocalDate
)
