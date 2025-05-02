package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.dto.MaksdatobestemmelseDto
import no.nav.helse.dto.deserialisering.MaksdatoresultatInnDto
import no.nav.helse.dto.serialisering.MaksdatoresultatUtDto
import no.nav.helse.hendelser.Periode

data class Maksdatoresultat(
    val vurdertTilOgMed: LocalDate,
    val bestemmelse: Bestemmelse,
    val startdatoTreårsvindu: LocalDate,
    val startdatoSykepengerettighet: LocalDate?,
    val forbrukteDager: List<Periode>,
    val oppholdsdager: List<Periode>,
    val avslåtteDager: List<Periode>,
    val maksdato: LocalDate,
    val gjenståendeDager: Int
) {
    val antallForbrukteDager = forbrukteDager.sumOf { it.count() }

    enum class Bestemmelse { IKKE_VURDERT, ORDINÆR_RETT, BEGRENSET_RETT, SYTTI_ÅR }
    companion object {
        val IkkeVurdert = Maksdatoresultat(
            vurdertTilOgMed = LocalDate.MIN,
            bestemmelse = Bestemmelse.IKKE_VURDERT,
            startdatoSykepengerettighet = null,
            startdatoTreårsvindu = LocalDate.MIN,
            forbrukteDager = emptyList(),
            oppholdsdager = emptyList(),
            avslåtteDager = emptyList(),
            maksdato = LocalDate.MIN,
            gjenståendeDager = 0
        )

        fun gjenopprett(dto: MaksdatoresultatInnDto) = Maksdatoresultat(
            vurdertTilOgMed = dto.vurdertTilOgMed,
            bestemmelse = when (dto.bestemmelse) {
                MaksdatobestemmelseDto.IKKE_VURDERT -> Bestemmelse.IKKE_VURDERT
                MaksdatobestemmelseDto.ORDINÆR_RETT -> Bestemmelse.ORDINÆR_RETT
                MaksdatobestemmelseDto.BEGRENSET_RETT -> Bestemmelse.BEGRENSET_RETT
                MaksdatobestemmelseDto.SYTTI_ÅR -> Bestemmelse.SYTTI_ÅR
            },
            startdatoSykepengerettighet = dto.startdatoSykepengerettighet,
            startdatoTreårsvindu = dto.startdatoTreårsvindu,
            forbrukteDager = dto.forbrukteDager.map { Periode.gjenopprett(it) },
            oppholdsdager = dto.oppholdsdager.map { Periode.gjenopprett(it) },
            avslåtteDager = dto.avslåtteDager.map { Periode.gjenopprett(it) },
            maksdato = dto.maksdato,
            gjenståendeDager = dto.gjenståendeDager
        )
    }

    fun dto() = MaksdatoresultatUtDto(
        vurdertTilOgMed = vurdertTilOgMed,
        bestemmelse = when (bestemmelse) {
            Bestemmelse.IKKE_VURDERT -> MaksdatobestemmelseDto.IKKE_VURDERT
            Bestemmelse.ORDINÆR_RETT -> MaksdatobestemmelseDto.ORDINÆR_RETT
            Bestemmelse.BEGRENSET_RETT -> MaksdatobestemmelseDto.BEGRENSET_RETT
            Bestemmelse.SYTTI_ÅR -> MaksdatobestemmelseDto.SYTTI_ÅR
        },
        startdatoSykepengerettighet = startdatoSykepengerettighet,
        startdatoTreårsvindu = startdatoTreårsvindu,
        forbrukteDager = forbrukteDager.map { it.dto() },
        oppholdsdager = oppholdsdager.map { it.dto() },
        avslåtteDager = avslåtteDager.map { it.dto() },
        maksdato = maksdato,
        gjenståendeDager = gjenståendeDager
    )
}
