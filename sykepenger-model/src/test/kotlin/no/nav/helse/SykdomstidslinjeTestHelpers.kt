package no.nav.helse

import no.nav.helse.hendelser.Søknad
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

internal val Int.sykedager get() = lagTidslinje(this, DagFactory::sykedag)
internal val Int.sykHelgdager get() = lagTidslinje(this, DagFactory::sykHelgedag)
internal val Int.egenmeldingsdager get() = lagTidslinje(this, DagFactory::egenmeldingsdag)
internal val Int.arbeidsdager get() = lagTidslinje(this, DagFactory::arbeidsdag)
internal val Int.implisittDager get() = lagTidslinje(this, DagFactory::implisittDag)
internal val Int.studieDager get() = lagTidslinje(this, DagFactory::studiedag)
internal val Int.utenlandsdager get() = lagTidslinje(this, DagFactory::utenlandsdag)
internal val Int.feriedager get() = lagTidslinje(this, DagFactory::feriedag)
internal val Int.permisjonsdager get() = lagTidslinje(this, DagFactory::permisjonsdag)

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

internal fun ConcreteSykdomstidslinje.fra(): ConcreteSykdomstidslinje {
    return this.fra(this.førsteDag())
}

internal fun ConcreteSykdomstidslinje.fra(fraOgMed: LocalDate, factory: DagFactory = Søknad.SøknadDagFactory): ConcreteSykdomstidslinje {
    val builder = SykdomstidslinjeBuilder(fraOgMed, factory)
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
    generator: DagFactory.(LocalDate) -> Dag
): ConcreteSykdomstidslinje =
    SykdomstidslinjeBuilder().antallDager(antallDager).lagTidslinje(generator)

internal class SykdomstidslinjeBuilder(startdato: LocalDate? = null, private val factory: DagFactory = Søknad.SøknadDagFactory) {

    private companion object {
        private var dato = LocalDate.of(2019, 1, 1)
    }

    init {
        startdato?.also {
            dato = it
        }
    }

    internal fun antallDager(antall: Int) =
        DagBuilder(antall.toLong(), factory)

    internal inner class DagBuilder(private val antallDager: Long, private val factory: DagFactory) {

        private val dager
            get() = dato.datesUntil(dato.plusDays(antallDager)).also {
                dato = dato.plusDays(antallDager)
            }

        internal val sykedager get() = lagTidslinje(DagFactory::sykedag)
        internal val sykHelgedag get() = lagTidslinje(DagFactory::sykHelgedag)
        internal val egenmeldingsdager get() = lagTidslinje(DagFactory::egenmeldingsdag)
        internal val arbeidsdager get() = lagTidslinje(DagFactory::arbeidsdag)
        internal val implisittDager get() = lagTidslinje(DagFactory::implisittDag)
        internal val studieDager get() = lagTidslinje(DagFactory::studiedag)
        internal val utenlandsdager get() = lagTidslinje(DagFactory::utenlandsdag)
        internal val feriedager get() = lagTidslinje(DagFactory::feriedag)
        internal val permisjonsdager get() = lagTidslinje(DagFactory::permisjonsdag)
        internal val ubestemtdag get() = lagTidslinje(DagFactory::ubestemtdag)

        internal fun lagTidslinje(generator: DagFactory.(LocalDate) -> Dag): ConcreteSykdomstidslinje {
            return CompositeSykdomstidslinje(dager.map { factory.generator(it) }.toList())

        }
    }
}

private operator fun ConcreteSykdomstidslinje.plus(other: ConcreteSykdomstidslinje) = this.plus(other, ::ImplisittDag, historiskDagturnering)
