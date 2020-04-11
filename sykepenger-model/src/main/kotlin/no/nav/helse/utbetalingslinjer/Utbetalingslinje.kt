package no.nav.helse.utbetalingslinjer

import no.nav.helse.person.UtbetalingVisitor
import no.nav.helse.utbetalingslinjer.Klassekode.Arbeidsgiverlinje
import no.nav.helse.utbetalingslinjer.Linjetype.NY
import no.nav.helse.utbetalingstidslinje.genererUtbetalingsreferanse
import java.time.LocalDate
import java.util.*

internal class Utbetalingslinjer private constructor(
    private val mottaker: String,
    private val fagområde: Fagområde,
    private val linjer: List<Utbetalingslinje>,
    private val utbetalingsreferanse: String,
    private val linjertype: Linjetype,
    private val sjekksum: Int
): List<Utbetalingslinje> by linjer {

    internal constructor(
        mottaker: String,
        fagområde: Fagområde,
        linjer: List<Utbetalingslinje> = listOf()
    ): this(
        mottaker,
        fagområde,
        linjer,
        genererUtbetalingsreferanse(UUID.randomUUID()),
        NY,
        linjer.hashCode() * 67 + mottaker.hashCode()
    )

    internal fun accept(visitor: UtbetalingVisitor) {
        linjer.forEach { it.accept(visitor) }
    }

    internal fun referanse() = utbetalingsreferanse

    infix fun forskjell(other: Utbetalingslinjer): Utbetalingslinjer {
        return Utbetalingslinjer(mottaker, fagområde)
    }
}

internal class Utbetalingslinje internal constructor(
    internal var fom: LocalDate,
    internal var tom: LocalDate,
    internal var dagsats: Int,
    internal val grad: Double,
    private var delytelseId: Int = 1,
    private var refDelytelseId: Int? = null,
    private val linjetype: Linjetype = NY,
    private val klassekode: Klassekode = Arbeidsgiverlinje
) {

    internal fun accept(visitor: UtbetalingVisitor) {
        visitor.visitUtbetalingslinje(this, fom, tom, dagsats, grad, delytelseId, refDelytelseId)
    }

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

enum class Klassekode(val verdi: String) {
    Arbeidsgiverlinje(verdi = "SPREFAG-IOP");

    companion object {
        fun from(verdi: String) = when(verdi) {
            "SPREFAG-IOP" -> Arbeidsgiverlinje
            else -> throw UnsupportedOperationException("Vi støtter ikke klassekoden: $verdi")
        }
    }
}

internal enum class Fagområde(private val linjerStrategy: (Utbetaling) -> Utbetalingslinjer) {
    SPREF(Utbetaling::arbeidsgiverUtbetalingslinjer),
    SP(Utbetaling::personUtbetalingslinjer);

    internal fun utbetalingslinjer(utbetaling: Utbetaling): Utbetalingslinjer =
        linjerStrategy(utbetaling)

    internal fun referanse(utbetaling: Utbetaling): String =
        utbetalingslinjer(utbetaling).referanse()
}
