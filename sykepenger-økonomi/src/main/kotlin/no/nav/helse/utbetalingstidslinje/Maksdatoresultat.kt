package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.hendelser.Periode
import no.nav.helse.utbetalingstidslinje.Maksdatoberegning.Companion.TILSTREKKELIG_OPPHOLD_I_SYKEDAGER

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

    val sisteNødvendigeOppholdsdag = oppholdsdager.flatten().getOrNull(TILSTREKKELIG_OPPHOLD_I_SYKEDAGER - 1)
    val fremdelesSykEtterTilstrekkeligOpphold = sisteNødvendigeOppholdsdag != null && oppholdsdager.isNotEmpty() && avslåtteDager.any { it.endInclusive > sisteNødvendigeOppholdsdag }

    enum class Bestemmelse { IKKE_VURDERT, ORDINÆR_RETT, BEGRENSET_RETT, SYTTI_ÅR }
}
