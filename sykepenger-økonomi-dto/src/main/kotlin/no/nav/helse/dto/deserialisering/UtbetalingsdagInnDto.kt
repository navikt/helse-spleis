package no.nav.helse.dto.deserialisering

import no.nav.helse.dto.BegrunnelseDto
import java.time.LocalDate

sealed class UtbetalingsdagInnDto {
    abstract val dato: LocalDate
    abstract val økonomi: ØkonomiInnDto

    data class ArbeidsgiverperiodeDagDto(
        override val dato: LocalDate,
        override val økonomi: ØkonomiInnDto,
    ) : UtbetalingsdagInnDto()

    data class ArbeidsgiverperiodeDagNavDto(
        override val dato: LocalDate,
        override val økonomi: ØkonomiInnDto,
    ) : UtbetalingsdagInnDto()

    data class NavDagDto(
        override val dato: LocalDate,
        override val økonomi: ØkonomiInnDto,
    ) : UtbetalingsdagInnDto()

    data class NavHelgDagDto(
        override val dato: LocalDate,
        override val økonomi: ØkonomiInnDto,
    ) : UtbetalingsdagInnDto()

    data class FridagDto(
        override val dato: LocalDate,
        override val økonomi: ØkonomiInnDto,
    ) : UtbetalingsdagInnDto()

    data class ArbeidsdagDto(
        override val dato: LocalDate,
        override val økonomi: ØkonomiInnDto,
    ) : UtbetalingsdagInnDto()

    data class AvvistDagDto(
        override val dato: LocalDate,
        override val økonomi: ØkonomiInnDto,
        val begrunnelser: List<BegrunnelseDto>,
    ) : UtbetalingsdagInnDto()

    data class ForeldetDagDto(
        override val dato: LocalDate,
        override val økonomi: ØkonomiInnDto,
    ) : UtbetalingsdagInnDto()

    data class UkjentDagDto(
        override val dato: LocalDate,
        override val økonomi: ØkonomiInnDto,
    ) : UtbetalingsdagInnDto()

    data class VentetidsdagDto(
        override val dato: LocalDate,
        override val økonomi: ØkonomiInnDto,
    ) : UtbetalingsdagInnDto()
}
