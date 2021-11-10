package no.nav.helse.spleis.graphql

import no.nav.helse.person.Inntektskilde
import no.nav.helse.person.Periodetype
import no.nav.helse.serde.api.AktivitetDTO
import no.nav.helse.serde.api.BegrunnelseDTO
import no.nav.helse.serde.api.v2.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

data class GraphQLPerson(
    val aktorId: String,
    val fodselsnummer: String,
    val arbeidsgivere: List<GraphQLArbeidsgiver>,
    val inntektsgrunnlag: List<GraphQLInntektsgrunnlag>,
    // vilkarsgrunnlaghistorikk
    val dodsdato: LocalDate?,
    val versjon: Int
)

data class GraphQLArbeidsgiver(
    val organisasjonsnummer: String,
    val id: UUID,
    val generasjoner: List<GraphQLGenerasjon>
)

data class GraphQLGenerasjon(
    val id: UUID,
    val perioder: List<GraphQLTidslinjeperiode>
)

interface GraphQLTidslinjeperiode {
    val id: UUID
    val fom: LocalDate
    val tom: LocalDate
    val tidslinje: List<GraphQLDag>
    val behandlingstype: Behandlingstype
    val periodetype: Periodetype
    val inntektskilde: Inntektskilde
    val erForkastet: Boolean
    val opprettet: LocalDateTime
}

data class GraphQLUberegnetPeriode(
    override val fom: LocalDate,
    override val tom: LocalDate,
    override val tidslinje: List<GraphQLDag>,
    override val behandlingstype: Behandlingstype,
    override val periodetype: Periodetype,
    override val inntektskilde: Inntektskilde,
    override val erForkastet: Boolean,
    override val opprettet: LocalDateTime
) : GraphQLTidslinjeperiode {
    override val id: UUID = UUID.randomUUID()
}

enum class GraphQLHendelsetype {
    INNTEKTSMELDING,
    SENDT_SØKNAD_NAV,
    SENDT_SØKNAD_ARBEIDSGIVER,
    NY_SØKNAD,
    UKJENT
}

interface GraphQLHendelse {
    val id: String
    val type: GraphQLHendelsetype
}

data class GraphQLInntektsmelding(
    override val id: String,
    val mottattDato: LocalDateTime,
    val beregnetInntekt: Double
) : GraphQLHendelse {
    override val type = GraphQLHendelsetype.INNTEKTSMELDING
}

data class GraphQLSoknadNav(
    override val id: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertDato: LocalDateTime,
    val sendtNav: LocalDateTime
) : GraphQLHendelse {
    override val type = GraphQLHendelsetype.SENDT_SØKNAD_NAV
}

data class GraphQLSoknadArbeidsgiver(
    override val id: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertDato: LocalDateTime,
    val sendtArbeidsgiver: LocalDateTime
) : GraphQLHendelse {
    override val type = GraphQLHendelsetype.SENDT_SØKNAD_ARBEIDSGIVER
}

data class GraphQLSykmelding(
    override val id: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertDato: LocalDateTime
) : GraphQLHendelse {
    override val type = GraphQLHendelsetype.NY_SØKNAD
}

data class GraphQLSimulering(
    val totalbelop: Int,
    val perioder: List<Periode>
) {
    data class Periode(
        val fom: LocalDate,
        val tom: LocalDate,
        val utbetalinger: List<Utbetaling>
    )

    data class Utbetaling(
        val utbetalesTilId: String,
        val utbetalesTilNavn: String,
        val forfall: LocalDate,
        val feilkonto: Boolean,
        val detaljer: List<Detaljer>
    )

    data class Detaljer(
        val faktiskFom: LocalDate,
        val faktiskTom: LocalDate,
        val konto: String,
        val belop: Int,
        val tilbakeforing: Boolean,
        val sats: Int,
        val typeSats: String,
        val antallSats: Int,
        val uforegrad: Int,
        val klassekode: String,
        val klassekodeBeskrivelse: String,
        val utbetalingstype: String,
        val refunderesOrgNr: String
    )
}

data class GraphQLBeregnetPeriode(
    override val fom: LocalDate,
    override val tom: LocalDate,
    override val tidslinje: List<GraphQLDag>,
    override val behandlingstype: Behandlingstype,
    override val periodetype: Periodetype,
    override val inntektskilde: Inntektskilde,
    override val erForkastet: Boolean,
    override val opprettet: LocalDateTime,
    val beregningId: UUID,
    val gjenstaendeSykedager: Int?,
    val forbrukteSykedager: Int?,
    val skjaeringstidspunkt: LocalDate,
    val maksdato: LocalDate,
    val utbetaling: GraphQLUtbetaling,
    val hendelser: List<GraphQLHendelse>,
    val simulering: GraphQLSimulering?,
    val vilkarsgrunnlaghistorikkId: UUID,
    val periodevilkar: Vilkar,
    val aktivitetslogg: List<AktivitetDTO>
) : GraphQLTidslinjeperiode {
    override val id: UUID = UUID.randomUUID()

    data class Vilkar(
        val sykepengedager: Sykepengedager,
        val alder: Alder,
        val soknadsfrist: Soknadsfrist?
    )

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

    data class Soknadsfrist(
        val sendtNav: LocalDateTime,
        val soknadFom: LocalDate,
        val soknadTom: LocalDate,
        val oppfylt: Boolean
    )
}

data class GraphQLUtbetaling(
    val type: String,
    val status: String,
    val arbeidsgiverNettoBelop: Int,
    val personNettoBelop: Int,
    val arbeidsgiverFagsystemId: String,
    val personFagsystemId: String,
    val vurdering: Vurdering?
) {
    data class Vurdering(
        val godkjent: Boolean,
        val tidsstempel: LocalDateTime,
        val automatisk: Boolean,
        val ident: String
    )
}

data class GraphQLDag(
    val dato: LocalDate,
    val sykdomsdagtype: SykdomstidslinjedagType,
    val utbetalingsdagtype: UtbetalingstidslinjedagType,
    val kilde: Sykdomstidslinjedag.SykdomstidslinjedagKilde,
    val grad: Double?,
    val utbetalingsinfo: Utbetalingsinfo?,
    val begrunnelser: List<BegrunnelseDTO>?
)

data class GraphQLInntektsgrunnlag(
    val skjaeringstidspunkt: LocalDate,
    val sykepengegrunnlag: Double?,
    val omregnetArsinntekt: Double?,
    val sammenligningsgrunnlag: Double?,
    val avviksprosent: Double?,
    val maksUtbetalingPerDag: Double?,
    val inntekter: List<Arbeidsgiverinntekt>,
    val oppfyllerKravOmMinstelonn: Boolean?,
    val grunnbelop: Int
) {
    data class InntekterFraAOrdningen(
        val maned: YearMonth,
        val sum: Double
    )

    data class Arbeidsgiverinntekt(
        val arbeidsgiver: String,
        val omregnetArsinntekt: OmregnetArsinntekt?,
        val sammenligningsgrunnlag: Sammenligningsgrunnlag?
    ) {
        data class OmregnetArsinntekt(
            val kilde: Kilde,
            val belop: Double,
            val manedsbelop: Double,
            val inntekterFraAOrdningen: List<InntekterFraAOrdningen>?
        ) {
            enum class Kilde {
                Saksbehandler,
                Inntektsmelding,
                Infotrygd,
                AOrdningen
            }
        }

        data class Sammenligningsgrunnlag(
            val belop: Double,
            val inntekterFraAOrdningen: List<InntekterFraAOrdningen>
        )
    }
}
