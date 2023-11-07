package no.nav.helse.serde.api.dto

import java.time.LocalDate
import java.util.UUID

data class SammenslåttDag(
    val dagen: LocalDate,
    val sykdomstidslinjedagtype: SykdomstidslinjedagType,
    val utbetalingstidslinjedagtype: UtbetalingstidslinjedagType,
    val kilde: Sykdomstidslinjedag.SykdomstidslinjedagKilde,
    val grad: Int? = null,
    val utbetalingsinfo: Utbetalingsinfo? = null,
    val begrunnelser: List<BegrunnelseDTO>? = null,
) {
    /*
        sammenligner ikke utbetalingsinfo (siden endring av beløp dekkes av sjekk på vilkårsgrunnlagId),
        ei heller utbetalingstidslinjedagtypen siden den reflekterer både endring av sykdomstidslinje+vilkårsgrunnlag (og dekkes dermed fra før)
     */
    fun sammeGrunnlag(other: SammenslåttDag) =
        this.dagen == other.dagen
                && this.sykdomstidslinjedagtype == other.sykdomstidslinjedagtype
                && this.kilde == other.kilde
                && this.grad == grad
}

enum class SykdomstidslinjedagType {
    ARBEIDSDAG,
    ARBEIDSGIVERDAG,
    FERIEDAG,
    ARBEID_IKKE_GJENOPPTATT_DAG,
    FORELDET_SYKEDAG,
    FRISK_HELGEDAG,
    PERMISJONSDAG,
    SYKEDAG,
    SYKEDAG_NAV,
    SYK_HELGEDAG,
    UBESTEMTDAG,
    ANDRE_YTELSER_FORELDREPENGER,
    ANDRE_YTELSER_AAP,
    ANDRE_YTELSER_OMSORGSPENGER,
    ANDRE_YTELSER_PLEIEPENGER,
    ANDRE_YTELSER_SVANGERSKAPSPENGER,
    ANDRE_YTELSER_OPPLÆRINGSPENGER,
    ANDRE_YTELSER_DAGPENGER,
}

enum class SykdomstidslinjedagKildetype {
    Inntektsmelding,
    Søknad,
    Sykmelding,
    Saksbehandler,
    Ukjent
}

data class Sykdomstidslinjedag(
    val dagen: LocalDate,
    val type: SykdomstidslinjedagType,
    val kilde: SykdomstidslinjedagKilde,
    val grad: Int? = null
) {
    data class SykdomstidslinjedagKilde(
        val type: SykdomstidslinjedagKildetype,
        val id: UUID
    )
}

enum class UtbetalingstidslinjedagType {
    ArbeidsgiverperiodeDag,
    NavDag,
    NavHelgDag,
    Helgedag,   // SpeilBuilder only code breakout of Fridag
    Arbeidsdag,
    Feriedag,   // SpeilBuilder only code breakout of Fridag
    AvvistDag,
    UkjentDag,
    ForeldetDag
}

interface Utbetalingstidslinjedag {
    val type: UtbetalingstidslinjedagType
    val dato: LocalDate

    fun utbetalingsinfo(): Utbetalingsinfo? = null
}

data class UtbetalingsdagDTO(
    override val type: UtbetalingstidslinjedagType = UtbetalingstidslinjedagType.NavDag,
    override val dato: LocalDate,
    val personbeløp: Int,
    val arbeidsgiverbeløp: Int,
    val totalGrad: Int
) : Utbetalingstidslinjedag {
    override fun utbetalingsinfo() = Utbetalingsinfo(personbeløp, arbeidsgiverbeløp, totalGrad)
}

data class AvvistDag(
    override val type: UtbetalingstidslinjedagType = UtbetalingstidslinjedagType.AvvistDag,
    override val dato: LocalDate,
    val begrunnelser: List<BegrunnelseDTO>,
    val totalGrad: Int
) : Utbetalingstidslinjedag {
    override fun utbetalingsinfo() = Utbetalingsinfo(null, null, totalGrad)
}

data class UtbetalingstidslinjedagUtenGrad(
    override val type: UtbetalingstidslinjedagType,
    override val dato: LocalDate
) : Utbetalingstidslinjedag
