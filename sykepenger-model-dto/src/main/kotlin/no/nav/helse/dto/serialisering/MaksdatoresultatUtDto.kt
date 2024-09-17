package no.nav.helse.dto.serialisering

import no.nav.helse.dto.MaksdatobestemmelseDto
import java.time.LocalDate

data class MaksdatoresultatUtDto(
    val vurdertTilOgMed: LocalDate,
    val bestemmelse: MaksdatobestemmelseDto,
    val startdatoTreårsvindu: LocalDate,
    val forbrukteDager: Set<LocalDate>,
    val maksdato: LocalDate,
    val gjenståendeDager: Int,
    val grunnlag: UtbetalingstidslinjeUtDto
)