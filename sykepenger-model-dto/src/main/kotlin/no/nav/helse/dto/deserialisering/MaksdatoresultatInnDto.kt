package no.nav.helse.dto.deserialisering

import no.nav.helse.dto.MaksdatobestemmelseDto
import java.time.LocalDate
import no.nav.helse.dto.PeriodeDto

data class MaksdatoresultatInnDto(
    val vurdertTilOgMed: LocalDate,
    val bestemmelse: MaksdatobestemmelseDto,
    val startdatoTreårsvindu: LocalDate,
    val startdatoSykepengerettighet: LocalDate?,
    val forbrukteDager: List<PeriodeDto>,
    val oppholdsdager: List<PeriodeDto>,
    val avslåtteDager: List<PeriodeDto>,
    val maksdato: LocalDate,
    val gjenståendeDager: Int,
    val grunnlag: UtbetalingstidslinjeInnDto
)