package no.nav.helse.testhelpers

import no.nav.helse.hendelser.Periode
import no.nav.helse.sykdomstidslinje.BesteStrategy
import no.nav.helse.sykdomstidslinje.NySykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse.Hendelseskilde
import no.nav.helse.sykdomstidslinje.merge
import java.time.LocalDate

internal class TestSykdomstidslinje(
    private val førsteDato: LocalDate,
    private val sisteDato: LocalDate,
    private val daggenerator: (LocalDate, LocalDate, Number, Hendelseskilde) -> NySykdomstidslinje
) {
    private var grad: Double = 100.0

    internal infix fun grad(grad: Number) = this.also { it.grad = grad.toDouble() }

    internal fun asNySykdomstidslinje() = daggenerator(førsteDato, sisteDato, grad, TestEvent.testkilde)
    internal fun merge(annen: TestSykdomstidslinje) = this.asNySykdomstidslinje().merge(annen)
    internal fun merge(annen: NySykdomstidslinje) = this.asNySykdomstidslinje().merge(annen)
    internal fun lås(periode: Periode) = this.asNySykdomstidslinje().also { it.lås(periode) }
    internal fun låsOpp(periode: Periode) = this.asNySykdomstidslinje().also { it.låsOpp(periode) }
}

internal infix fun LocalDate.jobbTil(sisteDato: LocalDate) =
    TestSykdomstidslinje(this, sisteDato) { første: LocalDate, siste: LocalDate, _, kilde: Hendelseskilde ->
        NySykdomstidslinje.arbeidsdager(første, siste, kilde)
    }

internal infix fun LocalDate.ferieTil(sisteDato: LocalDate) =
    TestSykdomstidslinje(this, sisteDato) { første: LocalDate, siste: LocalDate, _, kilde: Hendelseskilde ->
        NySykdomstidslinje.feriedager(første, siste, kilde)
    }

internal infix fun LocalDate.sykTil(sisteDato: LocalDate) = TestSykdomstidslinje(this, sisteDato, NySykdomstidslinje.Companion::sykedager )
internal infix fun LocalDate.betalingTil(sisteDato: LocalDate) = TestSykdomstidslinje(this, sisteDato, NySykdomstidslinje.Companion::arbeidsgiverdager )

internal fun NySykdomstidslinje.merge(testTidslinje: TestSykdomstidslinje): NySykdomstidslinje = this.merge(testTidslinje.asNySykdomstidslinje())

internal fun List<TestSykdomstidslinje>.merge(beste: BesteStrategy) = this.map { it.asNySykdomstidslinje() }.merge(beste)
