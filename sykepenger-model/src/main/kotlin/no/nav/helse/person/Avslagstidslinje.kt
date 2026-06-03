package no.nav.helse.person

import no.nav.helse.Tidslinje
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Avslagstidslinje.Avslagsdag
import no.nav.helse.utbetalingstidslinje.Begrunnelse

internal class Avslagstidslinje(
    vararg perioder: Pair<Periode, Avslagsdag>
) : Tidslinje<Avslagsdag, Avslagstidslinje>(*perioder) {

    data class Avslagsdag(
        val begrunnelser: List<Begrunnelse>,
        val kilde: String
    )

    //fun dto() = AvslåtteDagerDto(dager.map { it.dto() })

    override fun opprett(vararg perioder: Pair<Periode, Avslagsdag>): Avslagstidslinje {
        return Avslagstidslinje(*perioder)
    }

    //companion object {
    //    fun gjenopprett(dto: AvslåtteDagerDto) =
    //        Avslagstidslinje(
    //            dto.dager.map { Periode.gjenopprett(it) }
    //        )
    //}
}
