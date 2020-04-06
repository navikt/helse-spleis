package no.nav.helse.utbetalingslinjer

import no.nav.helse.serde.UtbetalingslinjeData
import no.nav.helse.utbetalingslinjer.Linjetype.NY
import no.nav.helse.utbetalingstidslinje.genererUtbetalingsreferanse
import java.time.LocalDate
import java.util.*

internal class Utbetalingslinjer private constructor(
    private val mottaker: String,
    private val mottakertype: Mottakertype,
    private val linjer: List<Utbetalingslinje>,
    private val utbetalingsreferanse: String,
    private val linjertype: Linjetype,
    private val sjekksum: Int
): List<Utbetalingslinje> by linjer {

    internal constructor(
        mottaker: String,
        mottakertype: Mottakertype,
        linjer: List<Utbetalingslinje> = listOf()
    ): this(
        mottaker,
        mottakertype,
        linjer,
        genererUtbetalingsreferanse(UUID.randomUUID()),
        NY,
        linjer.hashCode() * 67 + mottaker.hashCode()
    )

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
    private val linjetype: Linjetype = NY
) {
    internal constructor(
        fom: LocalDate,
        tom: LocalDate,
        dagsats: Int,
        grad: Double
    ): this(fom, tom, dagsats, grad, 1, null, NY)

    internal fun accept(visitor: UtbetalingVisitor) {
        visitor.visitUtbetalingslinje(this, fom, tom, dagsats, grad, delytelseId, refDelytelseId)
    }

    internal fun toData() : UtbetalingslinjeData =
        UtbetalingslinjeData(fom, tom, dagsats, grad, delytelseId, refDelytelseId, linjetype.toString())

    internal fun linkTo(other: Utbetalingslinje) {
        this.delytelseId = other.delytelseId + 1
        this.refDelytelseId = other.delytelseId
    }

    override fun hashCode(): Int {
        return fom.hashCode() * 37 +
            tom.hashCode() * 17 +
            dagsats.hashCode() * 41 +
            grad.hashCode()
    }
}

enum class Linjetype {
    NY, UEND, ENDR
}

internal enum class Mottakertype(internal val melding: String) {
    Arbeidsgiver("ARBEIDSGIVER"), Person("PERSON");
}
