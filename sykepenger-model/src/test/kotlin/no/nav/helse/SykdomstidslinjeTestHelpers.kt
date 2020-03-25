package no.nav.helse

import no.nav.helse.hendelser.Søknad
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.dag.*
import no.nav.helse.sykdomstidslinje.merge
import no.nav.helse.testhelpers.mandag
import no.nav.helse.tournament.historiskDagturnering
import java.time.LocalDate
import kotlin.streams.toList

internal val mandag = 1.mandag
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
    periode1: Sykdomstidslinje,
    periode2: Sykdomstidslinje,
    test: Sykdomstidslinje.(Sykdomstidslinje, Sykdomstidslinje) -> Unit
) {
    listOf(periode1, periode2).merge(historiskDagturnering).test(periode1, periode2)
}

internal fun perioder(
    periode1: Sykdomstidslinje,
    periode2: Sykdomstidslinje,
    periode3: Sykdomstidslinje,
    test: Sykdomstidslinje.(Sykdomstidslinje, Sykdomstidslinje, Sykdomstidslinje) -> Unit
) {
    listOf(periode1, periode2, periode3).merge(historiskDagturnering).test(periode1, periode2, periode3)
}


internal fun perioder(
    periode1: Sykdomstidslinje,
    periode2: Sykdomstidslinje,
    periode3: Sykdomstidslinje,
    periode4: Sykdomstidslinje,
    test: Sykdomstidslinje.(Sykdomstidslinje, Sykdomstidslinje, Sykdomstidslinje, Sykdomstidslinje) -> Unit
) {
    listOf(periode1, periode2, periode3, periode4).merge(historiskDagturnering).test(periode1, periode2, periode3, periode4)
}

internal fun Sykdomstidslinje.fra(): Sykdomstidslinje {
    return this.fra(this.førsteDag())
}

internal fun Sykdomstidslinje.fra(fraOgMed: LocalDate, factory: DagFactory = Søknad.SøknadDagFactory): Sykdomstidslinje {
    val builder = SykdomstidslinjeBuilder(fraOgMed, factory)
        .antallDager(1)

    return Sykdomstidslinje(this.flatten().flatMap {
        when (it) {
            is Sykedag -> builder.sykedager.flatten()
            is SykHelgedag -> builder.sykHelgedag.flatten()
            is Egenmeldingsdag -> builder.egenmeldingsdager.flatten()
            is ImplisittDag -> builder.implisittDager.flatten()
            is Studiedag -> builder.studieDager.flatten()
            is Utenlandsdag -> builder.utenlandsdager.flatten()
            is Permisjonsdag -> builder.permisjonsdager.flatten()
            is Feriedag -> builder.feriedager.flatten()
            is Arbeidsdag -> builder.arbeidsdager.flatten()
            is Ubestemtdag -> builder.ubestemtdag.flatten()
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
): Sykdomstidslinje =
    SykdomstidslinjeBuilder().antallDager(antallDager).lagTidslinje(generator)

private fun lagTidslinje(
    antallDager: Int,
    generator: DagFactory.(LocalDate, Double) -> Dag,
    grad: Double = 100.0
): Sykdomstidslinje =
    SykdomstidslinjeBuilder().antallDager(antallDager).lagTidslinje(generator, grad)

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

        internal fun lagTidslinje(generator: DagFactory.(LocalDate) -> Dag): Sykdomstidslinje {
            return Sykdomstidslinje(dager.map { factory.generator(it) }.toList())
        }

        internal fun lagTidslinje(generator: DagFactory.(LocalDate,Double) -> Dag, grad: Double = 100.0): Sykdomstidslinje {
            return Sykdomstidslinje(dager.map { factory.generator(it, grad) }.toList())
        }
    }
}
