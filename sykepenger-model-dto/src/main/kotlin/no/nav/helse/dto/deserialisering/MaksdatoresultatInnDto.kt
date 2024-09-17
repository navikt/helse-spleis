package no.nav.helse.dto.deserialisering

import no.nav.helse.dto.MaksdatobestemmelseDto
import java.time.LocalDate

data class MaksdatoresultatInnDto(
    val vurdertTilOgMed: LocalDate,
    val bestemmelse: MaksdatobestemmelseDto,
    val startdatoTreårsvindu: LocalDate,
    val forbrukteDager: Set<LocalDate>,
    val maksdato: LocalDate,
    val gjenståendeDager: Int,
    val grunnlag: UtbetalingstidslinjeInnDto
)