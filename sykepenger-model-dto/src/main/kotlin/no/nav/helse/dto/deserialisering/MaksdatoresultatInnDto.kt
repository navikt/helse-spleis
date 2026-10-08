package no.nav.helse.dto.deserialisering

import no.nav.helse.dto.MaksdatobestemmelseDto
import no.nav.helse.dto.PeriodeDto
import java.time.LocalDate

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
)
