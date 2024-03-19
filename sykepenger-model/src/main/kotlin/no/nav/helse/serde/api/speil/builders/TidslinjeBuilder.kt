package no.nav.helse.serde.api.speil.builders

import java.time.LocalDate
import no.nav.helse.dto.HendelseskildeDto
import no.nav.helse.dto.SykdomstidslinjeDagDto
import no.nav.helse.dto.SykdomstidslinjeDto
import no.nav.helse.dto.serialisering.UtbetalingsdagUtDto
import no.nav.helse.dto.serialisering.UtbetalingstidslinjeUtDto
import no.nav.helse.dto.serialisering.ØkonomiUtDto
import no.nav.helse.erHelg
import no.nav.helse.person.SykdomstidslinjeVisitor
import no.nav.helse.serde.api.dto.AvvistDag
import no.nav.helse.serde.api.dto.BegrunnelseDTO
import no.nav.helse.serde.api.dto.Sykdomstidslinjedag
import no.nav.helse.serde.api.dto.SykdomstidslinjedagKildetype
import no.nav.helse.serde.api.dto.SykdomstidslinjedagType
import no.nav.helse.serde.api.dto.UtbetalingsdagDTO
import no.nav.helse.serde.api.dto.UtbetalingstidslinjedagType
import no.nav.helse.serde.api.dto.UtbetalingstidslinjedagUtenGrad
import kotlin.math.roundToInt

internal class SykdomstidslinjeBuilder(private val dto: SykdomstidslinjeDto): SykdomstidslinjeVisitor {
    private val tidslinje by lazy {
        dto.dager.map {
            when (it) {
                is SykdomstidslinjeDagDto.AndreYtelserDto -> when (it.ytelse) {
                    SykdomstidslinjeDagDto.AndreYtelserDto.YtelseDto.Foreldrepenger -> Sykdomstidslinjedag(
                        dagen = it.dato,
                        type = SykdomstidslinjedagType.ANDRE_YTELSER_FORELDREPENGER,
                        kilde = it.kilde.tilKildeDTO(),
                        grad = null
                    )
                    SykdomstidslinjeDagDto.AndreYtelserDto.YtelseDto.AAP -> Sykdomstidslinjedag(
                        dagen = it.dato,
                        type = SykdomstidslinjedagType.ANDRE_YTELSER_AAP,
                        kilde = it.kilde.tilKildeDTO(),
                        grad = null
                    )
                    SykdomstidslinjeDagDto.AndreYtelserDto.YtelseDto.Omsorgspenger -> Sykdomstidslinjedag(
                        dagen = it.dato,
                        type = SykdomstidslinjedagType.ANDRE_YTELSER_OMSORGSPENGER,
                        kilde = it.kilde.tilKildeDTO(),
                        grad = null
                    )
                    SykdomstidslinjeDagDto.AndreYtelserDto.YtelseDto.Pleiepenger -> Sykdomstidslinjedag(
                        dagen = it.dato,
                        type = SykdomstidslinjedagType.ANDRE_YTELSER_PLEIEPENGER,
                        kilde = it.kilde.tilKildeDTO(),
                        grad = null
                    )
                    SykdomstidslinjeDagDto.AndreYtelserDto.YtelseDto.Svangerskapspenger -> Sykdomstidslinjedag(
                        dagen = it.dato,
                        type = SykdomstidslinjedagType.ANDRE_YTELSER_SVANGERSKAPSPENGER,
                        kilde = it.kilde.tilKildeDTO(),
                        grad = null
                    )
                    SykdomstidslinjeDagDto.AndreYtelserDto.YtelseDto.Opplæringspenger -> Sykdomstidslinjedag(
                        dagen = it.dato,
                        type = SykdomstidslinjedagType.ANDRE_YTELSER_OPPLÆRINGSPENGER,
                        kilde = it.kilde.tilKildeDTO(),
                        grad = null
                    )
                    SykdomstidslinjeDagDto.AndreYtelserDto.YtelseDto.Dagpenger -> Sykdomstidslinjedag(
                        dagen = it.dato,
                        type = SykdomstidslinjedagType.ANDRE_YTELSER_DAGPENGER,
                        kilde = it.kilde.tilKildeDTO(),
                        grad = null
                    )
                }
                is SykdomstidslinjeDagDto.ArbeidIkkeGjenopptattDagDto -> Sykdomstidslinjedag(
                    dagen = it.dato,
                    type = SykdomstidslinjedagType.ARBEID_IKKE_GJENOPPTATT_DAG,
                    kilde = it.kilde.tilKildeDTO(),
                    grad = null
                )
                is SykdomstidslinjeDagDto.ArbeidsdagDto -> Sykdomstidslinjedag(
                    dagen = it.dato,
                    type = SykdomstidslinjedagType.ARBEIDSDAG,
                    kilde = it.kilde.tilKildeDTO(),
                    grad = null
                )
                is SykdomstidslinjeDagDto.ArbeidsgiverHelgedagDto -> Sykdomstidslinjedag(
                    dagen = it.dato,
                    type = SykdomstidslinjedagType.ARBEIDSGIVERDAG,
                    kilde = it.kilde.tilKildeDTO(),
                    grad = it.grad.prosent.roundToInt()
                )
                is SykdomstidslinjeDagDto.ArbeidsgiverdagDto -> Sykdomstidslinjedag(
                    dagen = it.dato,
                    type = SykdomstidslinjedagType.ARBEIDSGIVERDAG,
                    kilde = it.kilde.tilKildeDTO(),
                    grad = it.grad.prosent.roundToInt()
                )
                is SykdomstidslinjeDagDto.FeriedagDto -> Sykdomstidslinjedag(
                    dagen = it.dato,
                    type = SykdomstidslinjedagType.FERIEDAG,
                    kilde = it.kilde.tilKildeDTO(),
                    grad = null
                )
                is SykdomstidslinjeDagDto.ForeldetSykedagDto -> Sykdomstidslinjedag(
                    dagen = it.dato,
                    type = SykdomstidslinjedagType.FORELDET_SYKEDAG,
                    kilde = it.kilde.tilKildeDTO(),
                    grad = it.grad.prosent.roundToInt()
                )
                is SykdomstidslinjeDagDto.FriskHelgedagDto -> Sykdomstidslinjedag(
                    dagen = it.dato,
                    type = SykdomstidslinjedagType.FRISK_HELGEDAG,
                    kilde = it.kilde.tilKildeDTO(),
                    grad = null
                )
                is SykdomstidslinjeDagDto.PermisjonsdagDto -> Sykdomstidslinjedag(
                    dagen = it.dato,
                    type = SykdomstidslinjedagType.PERMISJONSDAG,
                    kilde = it.kilde.tilKildeDTO(),
                    grad = null
                )
                is SykdomstidslinjeDagDto.ProblemDagDto -> Sykdomstidslinjedag(
                    dagen = it.dato,
                    type = SykdomstidslinjedagType.UBESTEMTDAG,
                    kilde = it.kilde.tilKildeDTO(),
                    grad = null
                )
                is SykdomstidslinjeDagDto.SykHelgedagDto -> Sykdomstidslinjedag(
                    dagen = it.dato,
                    type = SykdomstidslinjedagType.SYK_HELGEDAG,
                    kilde = it.kilde.tilKildeDTO(),
                    grad = it.grad.prosent.roundToInt()
                )
                is SykdomstidslinjeDagDto.SykedagDto -> Sykdomstidslinjedag(
                    dagen = it.dato,
                    type = SykdomstidslinjedagType.SYKEDAG,
                    kilde = it.kilde.tilKildeDTO(),
                    grad = it.grad.prosent.roundToInt()
                )
                is SykdomstidslinjeDagDto.SykedagNavDto -> Sykdomstidslinjedag(
                    dagen = it.dato,
                    type = SykdomstidslinjedagType.SYKEDAG_NAV,
                    kilde = it.kilde.tilKildeDTO(),
                    grad = it.grad.prosent.roundToInt()
                )
                is SykdomstidslinjeDagDto.UkjentDagDto -> Sykdomstidslinjedag(
                    dagen = it.dato,
                    type = SykdomstidslinjedagType.ARBEIDSDAG,
                    kilde = it.kilde.tilKildeDTO(),
                    grad = null
                )
            }
        }
    }

    private fun HendelseskildeDto.tilKildeDTO() =
        Sykdomstidslinjedag.SykdomstidslinjedagKilde(
            type = when (type) {
                "Inntektsmelding" -> SykdomstidslinjedagKildetype.Inntektsmelding
                "Søknad" -> SykdomstidslinjedagKildetype.Søknad
                "OverstyrTidslinje" -> SykdomstidslinjedagKildetype.Saksbehandler
                else -> SykdomstidslinjedagKildetype.Ukjent
            },
            id = meldingsreferanseId
        )

    fun build() = tidslinje.toList()
}

internal class UtbetalingstidslinjeBuilder(private val dto: UtbetalingstidslinjeUtDto) {
    private val utbetalingstidslinje by lazy {
        dto.dager.map {
            when (it) {
                is UtbetalingsdagUtDto.ArbeidsdagDto -> UtbetalingstidslinjedagUtenGrad(type = UtbetalingstidslinjedagType.Arbeidsdag, dato = it.dato)
                is UtbetalingsdagUtDto.ArbeidsgiverperiodeDagDto -> UtbetalingstidslinjedagUtenGrad(type = UtbetalingstidslinjedagType.ArbeidsgiverperiodeDag, dato = it.dato)
                is UtbetalingsdagUtDto.ArbeidsgiverperiodeDagNavDto -> mapUtbetalingsdag(it.dato, UtbetalingstidslinjedagType.ArbeidsgiverperiodeDag, it.økonomi)
                is UtbetalingsdagUtDto.AvvistDagDto -> AvvistDag(
                    type = UtbetalingstidslinjedagType.AvvistDag,
                    dato = it.dato,
                    begrunnelser = it.begrunnelser.map { BegrunnelseDTO.fraBegrunnelse(it) },
                    totalGrad = it.økonomi.totalGrad.prosent.toInt()
                )

                is UtbetalingsdagUtDto.ForeldetDagDto -> UtbetalingstidslinjedagUtenGrad(type = UtbetalingstidslinjedagType.ForeldetDag, dato = it.dato)
                is UtbetalingsdagUtDto.FridagDto -> UtbetalingstidslinjedagUtenGrad(
                    type = if (it.dato.erHelg()) UtbetalingstidslinjedagType.Helgedag else UtbetalingstidslinjedagType.Feriedag,
                    dato = it.dato
                )
                is UtbetalingsdagUtDto.NavDagDto -> mapUtbetalingsdag(it.dato, UtbetalingstidslinjedagType.NavDag, it.økonomi)
                is UtbetalingsdagUtDto.NavHelgDagDto -> UtbetalingstidslinjedagUtenGrad(type = UtbetalingstidslinjedagType.NavHelgDag, dato = it.dato)
                is UtbetalingsdagUtDto.UkjentDagDto -> UtbetalingstidslinjedagUtenGrad(
                    type = UtbetalingstidslinjedagType.UkjentDag,
                    dato = it.dato
                )

            }
        }
    }

    private fun mapUtbetalingsdag(dato: LocalDate, type: UtbetalingstidslinjedagType, økonomi: ØkonomiUtDto): UtbetalingsdagDTO {
        return UtbetalingsdagDTO(
            type = type,
            dato = dato,
            arbeidsgiverbeløp = økonomi.arbeidsgiverbeløp!!.dagligInt.beløp,
            personbeløp = økonomi.personbeløp!!.dagligInt.beløp,
            totalGrad = økonomi.totalGrad.prosent.roundToInt()
        )
    }

    internal fun build() = utbetalingstidslinje.toList()
}
