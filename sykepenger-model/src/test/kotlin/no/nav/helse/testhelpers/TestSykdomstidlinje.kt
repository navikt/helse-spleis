package no.nav.helse.testhelpers

import no.nav.helse.sykdomstidslinje.NySykdomstidslinje
import java.time.LocalDate

internal class TestSykdomstidslinje(
    private val førsteDato: LocalDate,
    private val sisteDato: LocalDate,
    private val daggenerator: (LocalDate, LocalDate, Number) -> NySykdomstidslinje
) {
    private var grad: Double = 100.0

    internal infix fun grad(grad: Number) = this.also { it.grad = grad.toDouble() }

    internal fun asNySykdomstidslinje() = daggenerator(førsteDato, sisteDato, grad)
    internal fun merge(annen: TestSykdomstidslinje) = this.asNySykdomstidslinje().merge(annen)
    internal fun merge(annen: NySykdomstidslinje) = this.asNySykdomstidslinje().merge(annen)

}
internal infix fun LocalDate.jobbTil(sisteDato: LocalDate) = TestSykdomstidslinje(this, sisteDato) { første: LocalDate, siste: LocalDate, _ -> NySykdomstidslinje.arbeidsdager(første, siste) }
internal infix fun LocalDate.ferieTil(sisteDato: LocalDate) = TestSykdomstidslinje(this, sisteDato) { første: LocalDate, siste: LocalDate, _ -> NySykdomstidslinje.feriedager(første, siste) }
internal infix fun LocalDate.sykTil(sisteDato: LocalDate) = TestSykdomstidslinje(this, sisteDato, NySykdomstidslinje.Companion::sykedager )
internal infix fun LocalDate.betalingTil(sisteDato: LocalDate) = TestSykdomstidslinje(this, sisteDato, NySykdomstidslinje.Companion::arbeidsgiverdager )

internal fun NySykdomstidslinje.merge(testTidslinje: TestSykdomstidslinje): NySykdomstidslinje = this.merge(testTidslinje.asNySykdomstidslinje())
