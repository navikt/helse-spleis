package no.nav.helse.dto.serialisering

import no.nav.helse.dto.MaksdatobestemmelseDto
import no.nav.helse.dto.PeriodeDto
import java.time.LocalDate

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
)
