package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.hendelser.Periode

data class BeregnetMaksdato(
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
}
