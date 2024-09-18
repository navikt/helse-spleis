package no.nav.helse.dto.serialisering

import no.nav.helse.dto.MaksdatobestemmelseDto
import java.time.LocalDate
import no.nav.helse.dto.PeriodeDto

data class MaksdatoresultatUtDto(
    val vurdertTilOgMed: LocalDate,
    val bestemmelse: MaksdatobestemmelseDto,
    val startdatoTreårsvindu: LocalDate,
    val startdatoSykepengerettighet: LocalDate?,
    val forbrukteDager: List<PeriodeDto>,
    val oppholdsdager: List<PeriodeDto>,
    val avslåtteDager: List<PeriodeDto>,
    val maksdato: LocalDate,
    val gjenståendeDager: Int,
    val grunnlag: UtbetalingstidslinjeUtDto
)