package no.nav.helse.person

import no.nav.helse.Tidslinje
import no.nav.helse.dto.AvslagstidslinjeDto
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

    fun dto() = AvslagstidslinjeDto(gruppér().map { (periode, avslagsdag) ->
        AvslagstidslinjeDto.AvslagstidslinjedagDto(
            begrunnelser = avslagsdag.begrunnelser.map { it.dto() },
            kilde = avslagsdag.kilde,
            periode = periode.dto()
        )
    })
    override fun opprett(vararg perioder: Pair<Periode, Avslagsdag>): Avslagstidslinje {
        return Avslagstidslinje(*perioder)
    }

    companion object {
        fun gjenopprett(dto: AvslagstidslinjeDto) =
            Avslagstidslinje(
                *dto.perioder.map { dag ->
                    Periode(dag.periode.fom, dag.periode.tom) to Avslagsdag(
                        begrunnelser = dag.begrunnelser.map { Begrunnelse.gjenopprett(it) },
                        kilde = dag.kilde
                    )
                }.toTypedArray()
            )
    }
}
