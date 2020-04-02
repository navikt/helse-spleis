package no.nav.helse.utbetalingslinjer

import no.nav.helse.utbetalingstidslinje.genererUtbetalingsreferanse
import java.time.LocalDate
import java.util.*

internal class Utbetalingslinjer(
    private val linjer: List<Utbetalingslinje> = listOf<Utbetalingslinje>(),
    private var utbetalingsreferanse: String = genererUtbetalingsreferanse(UUID.randomUUID())
) {
    fun accept(visitor: UtbetalingVisitor) {
        linjer.forEach { it.accept(visitor) }
    }
}
class Utbetalingslinje private constructor(
    internal var fom: LocalDate,
    internal var tom: LocalDate,
    internal var dagsats: Int,
    internal val grad: Double,
    internal var delytelseId: Int = 1,
    internal var refDelytelseId: Int? = null
) {
    internal constructor(
        fom: LocalDate,
        tom: LocalDate,
        dagsats: Int,
        grad: Double
    ): this(fom, tom, dagsats, grad, 1, null)

    fun accept(visitor: UtbetalingVisitor) {
        visitor.visitUtbetalingslinje(this, fom, tom, dagsats, grad, delytelseId, refDelytelseId)
    }

}
