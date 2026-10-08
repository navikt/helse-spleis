package no.nav.helse.dto.serialisering

import no.nav.helse.dto.BegrunnelseDto
import java.time.LocalDate

sealed class UtbetalingsdagUtDto {
    abstract val dato: LocalDate
    abstract val økonomi: ØkonomiUtDto

    data class ArbeidsgiverperiodeDagDto(
        override val dato: LocalDate,
        override val økonomi: ØkonomiUtDto,
    ) : UtbetalingsdagUtDto()

    data class ArbeidsgiverperiodeDagNavDto(
        override val dato: LocalDate,
        override val økonomi: ØkonomiUtDto,
    ) : UtbetalingsdagUtDto()

    data class NavDagDto(
        override val dato: LocalDate,
        override val økonomi: ØkonomiUtDto,
    ) : UtbetalingsdagUtDto()

    data class NavHelgDagDto(
        override val dato: LocalDate,
        override val økonomi: ØkonomiUtDto,
    ) : UtbetalingsdagUtDto()

    data class FridagDto(
        override val dato: LocalDate,
        override val økonomi: ØkonomiUtDto,
    ) : UtbetalingsdagUtDto()

    data class ArbeidsdagDto(
        override val dato: LocalDate,
        override val økonomi: ØkonomiUtDto,
    ) : UtbetalingsdagUtDto()

    data class AvvistDagDto(
        override val dato: LocalDate,
        override val økonomi: ØkonomiUtDto,
        val begrunnelser: List<BegrunnelseDto>,
    ) : UtbetalingsdagUtDto()

    data class ForeldetDagDto(
        override val dato: LocalDate,
        override val økonomi: ØkonomiUtDto,
    ) : UtbetalingsdagUtDto()

    data class UkjentDagDto(
        override val dato: LocalDate,
        override val økonomi: ØkonomiUtDto,
    ) : UtbetalingsdagUtDto()

    data class VentetidsdagDto(
        override val dato: LocalDate,
        override val økonomi: ØkonomiUtDto,
    ) : UtbetalingsdagUtDto()
}
