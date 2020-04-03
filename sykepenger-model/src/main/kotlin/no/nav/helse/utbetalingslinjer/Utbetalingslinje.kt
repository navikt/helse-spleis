package no.nav.helse.utbetalingslinjer

import no.nav.helse.serde.UtbetalingslinjeData
import no.nav.helse.utbetalingslinjer.Linjetype.Ny
import no.nav.helse.utbetalingstidslinje.genererUtbetalingsreferanse
import java.time.LocalDate
import java.util.*

internal class Utbetalingslinjer(
    private val linjer: List<Utbetalingslinje> = listOf(),
    private val utbetalingsreferanse: String = genererUtbetalingsreferanse(UUID.randomUUID()),
    private val linjerType: Linjetype = Ny
): List<Utbetalingslinje> by linjer {

    internal fun accept(visitor: UtbetalingVisitor) {
        linjer.forEach { it.accept(visitor) }
    }
}

internal class Utbetalingslinje private constructor(
    internal var fom: LocalDate,
    internal var tom: LocalDate,
    internal var dagsats: Int,
    internal val grad: Double,
    private var delytelseId: Int = 1,
    private var refDelytelseId: Int? = null,
    private val linjetype: Linjetype = Ny
) {
    internal constructor(
        fom: LocalDate,
        tom: LocalDate,
        dagsats: Int,
        grad: Double
    ): this(fom, tom, dagsats, grad, 1, null, Ny)

    internal fun accept(visitor: UtbetalingVisitor) {
        visitor.visitUtbetalingslinje(this, fom, tom, dagsats, grad, delytelseId, refDelytelseId)
    }

    internal fun toData() : UtbetalingslinjeData =
        UtbetalingslinjeData(fom, tom, dagsats, grad)

    internal fun linkTo(other: Utbetalingslinje) {
        this.delytelseId = other.delytelseId + 1
        this.refDelytelseId = other.delytelseId
    }
}

internal enum class Linjetype(internal val melding: String) {
    Ny("NY"), Uendret("UEND"), Endret("ENDR")
}
