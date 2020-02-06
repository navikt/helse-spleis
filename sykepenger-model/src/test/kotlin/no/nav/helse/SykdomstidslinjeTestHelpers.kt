package no.nav.helse

import no.nav.helse.sykdomstidslinje.CompositeSykdomstidslinje
import no.nav.helse.sykdomstidslinje.ConcreteSykdomstidslinje
import no.nav.helse.sykdomstidslinje.dag.*
import no.nav.helse.tournament.historiskDagturnering
import java.time.LocalDate
import kotlin.streams.toList

internal val mandag = Uke(1).mandag
internal val tirsdag = mandag.plusDays(1)
internal val onsdag = tirsdag.plusDays(1)
internal val torsdag = onsdag.plusDays(1)
internal val fredag = torsdag.plusDays(1)
internal val lørdag = fredag.plusDays(1)
internal val søndag = lørdag.plusDays(1)

internal val Int.sykedager get() = lagTidslinje(this, ::Sykedag)
internal val Int.sykHelgdager get() = lagTidslinje(this, ::SykHelgedag)
internal val Int.egenmeldingsdager get() = lagTidslinje(this, ::Egenmeldingsdag)
internal val Int.arbeidsdager get() = lagTidslinje(this, ::Arbeidsdag)
internal val Int.implisittDager get() = lagTidslinje(this, ::ImplisittDag)
internal val Int.studieDager get() = lagTidslinje(this, ::Studiedag)
internal val Int.utenlandsdager get() = lagTidslinje(this, ::Utenlandsdag)
internal val Int.feriedager get() = lagTidslinje(this, ::Feriedag)
internal val Int.permisjonsdager get() = lagTidslinje(this, ::Permisjonsdag)

internal fun perioder(
    periode1: ConcreteSykdomstidslinje,
    periode2: ConcreteSykdomstidslinje,
    test: ConcreteSykdomstidslinje.(ConcreteSykdomstidslinje, ConcreteSykdomstidslinje) -> Unit
) {
    (periode1 + periode2).test(periode1, periode2)
}

internal fun perioder(
    periode1: ConcreteSykdomstidslinje,
    periode2: ConcreteSykdomstidslinje,
    periode3: ConcreteSykdomstidslinje,
    test: ConcreteSykdomstidslinje.(ConcreteSykdomstidslinje, ConcreteSykdomstidslinje, ConcreteSykdomstidslinje) -> Unit
) {
    (periode1 + periode2 + periode3).test(periode1, periode2, periode3)
}


internal fun perioder(
    periode1: ConcreteSykdomstidslinje,
    periode2: ConcreteSykdomstidslinje,
    periode3: ConcreteSykdomstidslinje,
    periode4: ConcreteSykdomstidslinje,
    test: ConcreteSykdomstidslinje.(ConcreteSykdomstidslinje, ConcreteSykdomstidslinje, ConcreteSykdomstidslinje, ConcreteSykdomstidslinje) -> Unit
) {
    (periode1 + periode2 + periode3 + periode4).test(periode1, periode2, periode3, periode4)
}

internal fun ConcreteSykdomstidslinje.fra(hendelseType: Dag.NøkkelHendelseType = Dag.NøkkelHendelseType.Søknad): ConcreteSykdomstidslinje {
    return this.fra(this.førsteDag())
}

internal fun ConcreteSykdomstidslinje.fra(fraOgMed: LocalDate, hendelseType: Dag.NøkkelHendelseType = Dag.NøkkelHendelseType.Søknad): ConcreteSykdomstidslinje {
    val builder = SykdomstidslinjeBuilder(fraOgMed)
        .antallDager(1)

    return CompositeSykdomstidslinje(this.flatten().map {
        when (it) {
            is Sykedag -> builder.sykedager
            is SykHelgedag -> builder.sykHelgedag
            is Egenmeldingsdag -> builder.egenmeldingsdager
            is ImplisittDag -> builder.implisittDager
            is Studiedag -> builder.studieDager
            is Utenlandsdag -> builder.utenlandsdager
            is Permisjonsdag -> builder.permisjonsdager
            is Feriedag -> builder.feriedager
            is Arbeidsdag -> builder.arbeidsdager
            is Ubestemtdag -> builder.ubestemtdag
            else -> throw IllegalArgumentException("ukjent dagtype: $it")
        }
    })
}

internal fun fra(): SykdomstidslinjeBuilder {
    return SykdomstidslinjeBuilder()
}

private fun lagTidslinje(
    antallDager: Int,
    generator: (LocalDate, Dag.NøkkelHendelseType) -> Dag
): ConcreteSykdomstidslinje =
    SykdomstidslinjeBuilder().antallDager(antallDager).lagTidslinje(generator)

internal class SykdomstidslinjeBuilder(startdato: LocalDate? = null) {

    private companion object {
        private var dato = LocalDate.of(2019, 1, 1)
    }

    init {
        startdato?.also {
            dato = it
        }
    }

    internal fun antallDager(antall: Int) =
        DagBuilder(antall.toLong())

    internal inner class DagBuilder(private val antallDager: Long) {

        private val dager
            get() = dato.datesUntil(dato.plusDays(antallDager)).also {
                dato = dato.plusDays(antallDager)
            }

        internal val sykedager get() = lagTidslinje(::Sykedag)
        internal val sykHelgedag get() = lagTidslinje(::SykHelgedag)
        internal val egenmeldingsdager get() = lagTidslinje(::Egenmeldingsdag)
        internal val arbeidsdager get() = lagTidslinje(::Arbeidsdag)
        internal val implisittDager get() = lagTidslinje(::ImplisittDag)
        internal val studieDager get() = lagTidslinje(::Studiedag)
        internal val utenlandsdager get() = lagTidslinje(::Utenlandsdag)
        internal val feriedager get() = lagTidslinje(::Feriedag)
        internal val permisjonsdager get() = lagTidslinje(::Permisjonsdag)
        internal val ubestemtdag get() = lagTidslinje(::Ubestemtdag)

        internal fun lagTidslinje(generator: (LocalDate, Dag.NøkkelHendelseType) -> Dag): ConcreteSykdomstidslinje {
            return CompositeSykdomstidslinje(dager.map { generator(it, Dag.NøkkelHendelseType.Søknad) }.toList())

        }
    }
}

private operator fun ConcreteSykdomstidslinje.plus(other: ConcreteSykdomstidslinje) =
    this.plus(other, ConcreteSykdomstidslinje.Companion::implisittDag, historiskDagturnering)
