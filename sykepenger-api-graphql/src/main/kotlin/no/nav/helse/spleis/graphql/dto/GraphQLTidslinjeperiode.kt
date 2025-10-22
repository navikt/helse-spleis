package no.nav.helse.spleis.graphql.dto

import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

enum class GraphQLInntektstype {
    EnArbeidsgiver,
    FlereArbeidsgivere
}

enum class GraphQLPeriodetype {
    Forstegangsbehandling,
    Forlengelse,
    OvergangFraIt,
    Infotrygdforlengelse;
}

enum class GraphQLSykdomsdagtype {
    Arbeidsdag,
    Arbeidsgiverdag,
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
    AndreYtelserDagpenger,
    Avslatt
}

enum class GraphQLUtbetalingsdagType {
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

enum class GraphQLSykdomsdagkildetype {
    Inntektsmelding,
    Soknad,
    Sykmelding,
    Saksbehandler,
    Ukjent
}

enum class GraphQLBegrunnelse {
    SykepengedagerOppbrukt,
    SykepengedagerOppbruktOver67,
    MinimumInntekt,
    MinimumInntektOver67,
    EgenmeldingUtenforArbeidsgiverperiode,
    AndreYtelser,
    MinimumSykdomsgrad,
    EtterDodsdato,
    ManglerMedlemskap,
    ManglerOpptjening,
    Over70
}

enum class GraphQLPeriodetilstand {
    TilUtbetaling,
    TilAnnullering,
    AvventerAnnullering,
    Utbetalt,
    Annullert,
    AnnulleringFeilet,
    IngenUtbetaling,
    RevurderingFeilet,
    TilInfotrygd,
    UtbetalingFeilet,
    ForberederGodkjenning,
    ManglerInformasjon,
    VenterPaAnnenPeriode,
    UtbetaltVenterPaAnnenPeriode,
    AvventerInntektsopplysninger,
    TilSkjonnsfastsettelse,
    TilGodkjenning;
}

enum class GraphQLUtbetalingstatus {
    Annullert,
    Forkastet,
    // todo: ikke i bruk, men jeg tør ikke gjøre ting som påvirker graphql
    Godkjent,
    GodkjentUtenUtbetaling,
    IkkeGodkjent,
    Overfort,

    @Deprecated("skal slettes")
    Sendt,
    Ubetalt,

    @Deprecated("skal slettes")
    UtbetalingFeilet,
    Utbetalt
}

data class GraphQLSykdomsdagkilde(
    val id: UUID,
    val type: GraphQLSykdomsdagkildetype
)

data class GraphQLUtbetalingsinfo(
    val inntekt: Int?,
    val utbetaling: Int?,
    val personbelop: Int?,
    val arbeidsgiverbelop: Int?,
    val refusjonsbelop: Int?,
    val totalGrad: Double?
)

data class GraphQLVurdering(
    val godkjent: Boolean,
    val tidsstempel: LocalDateTime,
    val automatisk: Boolean,
    val ident: String
)

data class GraphQLUtbetalingslinje(
    val fom: LocalDate,
    val tom: LocalDate,
    val dagsats: Int,
    val grad: Int
)

data class GraphQLOppdrag(
    val fagsystemId: String,
    val tidsstempel: LocalDateTime,
    val simulering: GraphQLSimulering?,
    val utbetalingslinjer: List<GraphQLUtbetalingslinje>
)

data class GraphQLUtbetaling(
    val id: UUID,
    val type: String,
    val typeEnum: Utbetalingtype,
    val status: String,
    val statusEnum: GraphQLUtbetalingstatus,
    val arbeidsgiverNettoBelop: Int,
    val personNettoBelop: Int,
    val arbeidsgiverFagsystemId: String,
    val personFagsystemId: String,
    val arbeidsgiveroppdrag: GraphQLOppdrag?,
    val personoppdrag: GraphQLOppdrag?,
    val vurdering: GraphQLVurdering?
)

enum class Utbetalingtype {
    UTBETALING,
    ETTERUTBETALING,
    ANNULLERING,
    REVURDERING,
    FERIEPENGER
}

data class GraphQLDag(
    val dato: LocalDate,
    val sykdomsdagtype: GraphQLSykdomsdagtype,
    val utbetalingsdagtype: GraphQLUtbetalingsdagType,
    val kilde: GraphQLSykdomsdagkilde,
    val grad: Double?,
    val utbetalingsinfo: GraphQLUtbetalingsinfo?,
    val begrunnelser: List<GraphQLBegrunnelse>?
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "__typename")
interface GraphQLTidslinjeperiode {
    val behandlingId: UUID
    val kilde: UUID
    val fom: LocalDate
    val tom: LocalDate
    val tidslinje: List<GraphQLDag>
    val periodetype: GraphQLPeriodetype
    val hendelser: List<GraphQLHendelse>
    val inntektstype: GraphQLInntektstype // 040423: dette feltet virker ikke å være i bruk i Speil
    val erForkastet: Boolean
    val opprettet: LocalDateTime
    val vedtaksperiodeId: UUID
    val periodetilstand: GraphQLPeriodetilstand
    val skjaeringstidspunkt: LocalDate
    val pensjonsgivendeInntekter: List<GraphQLPensjonsgivendeInntekt>
}

data class GraphQLUberegnetPeriode(
    override val behandlingId: UUID,
    override val kilde: UUID,
    override val fom: LocalDate,
    override val tom: LocalDate,
    override val tidslinje: List<GraphQLDag>,
    override val periodetype: GraphQLPeriodetype,
    override val erForkastet: Boolean,
    override val opprettet: LocalDateTime,
    override val vedtaksperiodeId: UUID,
    override val periodetilstand: GraphQLPeriodetilstand,
    override val skjaeringstidspunkt: LocalDate,
    override val hendelser: List<GraphQLHendelse>,
    override val pensjonsgivendeInntekter: List<GraphQLPensjonsgivendeInntekt>
) : GraphQLTidslinjeperiode {
    @Suppress("unused", "verdien av feltet brukes ikke i speil (brukes bare for å sjekke !=null i speil)")
    override val inntektstype: GraphQLInntektstype get() = GraphQLInntektstype.EnArbeidsgiver
}

data class GraphQLPeriodevilkar(
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

data class GraphQLBeregnetPeriode(
    override val behandlingId: UUID,
    override val kilde: UUID,
    override val fom: LocalDate,
    override val tom: LocalDate,
    override val tidslinje: List<GraphQLDag>,
    override val periodetype: GraphQLPeriodetype,
    override val erForkastet: Boolean,
    override val opprettet: LocalDateTime,
    override val vedtaksperiodeId: UUID,
    override val periodetilstand: GraphQLPeriodetilstand,
    override val skjaeringstidspunkt: LocalDate,
    override val hendelser: List<GraphQLHendelse>,
    override val pensjonsgivendeInntekter: List<GraphQLPensjonsgivendeInntekt>,
    val beregningId: UUID,
    val gjenstaendeSykedager: Int?,
    val forbrukteSykedager: Int?,
    val maksdato: LocalDate,
    val utbetaling: GraphQLUtbetaling,
    val periodevilkar: GraphQLPeriodevilkar,
    val vilkarsgrunnlagId: UUID?,
    val annulleringskandidater: List<GraphQLAnnulleringskandidat>
) : GraphQLTidslinjeperiode {
    @Suppress("unused", "verdien av feltet brukes ikke i speil (brukes bare for å sjekke !=null i speil)")
    override val inntektstype: GraphQLInntektstype get() = GraphQLInntektstype.EnArbeidsgiver
}

data class GraphQLInntekt(
    val inntektskilde: String,
    val periodiserteInntekter: List<GraphQLPeriodisertInntekt>
)

data class GraphQLPeriodisertInntekt(
    val fom: LocalDate,
    val tom: LocalDate,
    val dagligBelop: Double
)

data class GraphQLPensjonsgivendeInntekt(
    val inntektsar: Int,
    val arligBelop: Double
)

data class GraphQLAnnulleringskandidat(
    val vedtaksperiodeId: UUID,
    val organisasjonsnummer: String,
    val fom: LocalDate,
    val tom: LocalDate
)
