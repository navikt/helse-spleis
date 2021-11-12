package no.nav.helse.serde.api.v2

import no.nav.helse.serde.api.BegrunnelseDTO
import java.time.LocalDate
import java.util.*

data class SammenslåttDag(
    val dagen: LocalDate,
    val sykdomstidslinjedagtype: SykdomstidslinjedagType,
    val utbetalingstidslinjedagtype: UtbetalingstidslinjedagType,
    val kilde: Sykdomstidslinjedag.SykdomstidslinjedagKilde,
    val grad: Double? = null,
    val utbetalingsinfo: Utbetalingsinfo? = null,
    val begrunnelser: List<BegrunnelseDTO>? = null,
)

enum class SykdomstidslinjedagType {
    ARBEIDSDAG,
    ARBEIDSGIVERDAG,
    FERIEDAG,
    FORELDET_SYKEDAG,
    FRISK_HELGEDAG,
    PERMISJONSDAG,
    SYKEDAG,
    SYK_HELGEDAG,
    UBESTEMTDAG,
    AVSLÅTT
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
    val grad: Double? = null
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
    val inntekt: Int
    val dato: LocalDate

    fun utbetalingsinfo(): Utbetalingsinfo? = null
}

data class NavDag(
    override val type: UtbetalingstidslinjedagType = UtbetalingstidslinjedagType.NavDag,
    override val inntekt: Int,
    override val dato: LocalDate,
    val utbetaling: Int,
    val personbeløp: Int,
    val arbeidsgiverbeløp: Int,
    val refusjonsbeløp: Int,
    val grad: Double,
    val totalGrad: Double?
) : Utbetalingstidslinjedag {
    override fun utbetalingsinfo() = Utbetalingsinfo(inntekt, utbetaling, personbeløp, arbeidsgiverbeløp, refusjonsbeløp, totalGrad)
}

data class AvvistDag(
    override val type: UtbetalingstidslinjedagType = UtbetalingstidslinjedagType.AvvistDag,
    override val inntekt: Int,
    override val dato: LocalDate,
    val begrunnelser: List<BegrunnelseDTO>,
    val grad: Double,
    val totalGrad: Double?
) : Utbetalingstidslinjedag {
    override fun utbetalingsinfo() = Utbetalingsinfo(inntekt, null, null, null, null, totalGrad)
}

data class UtbetalingstidslinjedagUtenGrad(
    override val type: UtbetalingstidslinjedagType,
    override val inntekt: Int,
    override val dato: LocalDate
) : Utbetalingstidslinjedag

data class UtbetalingstidslinjedagMedGrad(
    override val type: UtbetalingstidslinjedagType,
    override val inntekt: Int,
    override val dato: LocalDate,
    val grad: Double
) : Utbetalingstidslinjedag
