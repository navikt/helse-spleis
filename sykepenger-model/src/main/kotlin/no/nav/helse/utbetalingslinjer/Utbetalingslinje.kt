package no.nav.helse.utbetalingslinjer

import no.nav.helse.person.UtbetalingVisitor
import no.nav.helse.utbetalingslinjer.Klassekode.Arbeidsgiverlinje
import no.nav.helse.utbetalingslinjer.Linjetype.*
import no.nav.helse.utbetalingstidslinje.genererUtbetalingsreferanse
import java.time.LocalDate
import java.util.*

internal class Utbetalingslinjer private constructor(
    private val mottaker: String,
    private val fagområde: Fagområde,
    private val linjer: List<Utbetalingslinje>,
    private var utbetalingsreferanse: String,
    private var linjertype: Linjetype,
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

    private val førstedato get() = linjer.first().fom

    private val sistedato get() = linjer.last().tom

    internal fun removeUEND() = Utbetalingslinjer(
        mottaker,
        fagområde,
        linjer.filter { it.erForskjell() },
        utbetalingsreferanse,
        linjertype,
        linjer.hashCode() * 67 + mottaker.hashCode()
    )

    infix fun forskjell(tidligere: Utbetalingslinjer): Utbetalingslinjer {
        return when {
            this.førstedato > tidligere.sistedato ->
                this
            this.førstedato < tidligere.førstedato && this.sistedato >= tidligere.sistedato ->
                appended(tidligere)
            this.førstedato == tidligere.førstedato && this.sistedato >= tidligere.sistedato ->
                ghosted(tidligere)
            else ->
                throw IllegalArgumentException("uventet utbetalingslinje forhold")
        }
    }

    private fun appended(tidligere: Utbetalingslinjer) = this.also { nåværende ->
        nåværende.first().linkTo(tidligere.last())
        zipWithNext().map { (a, b) -> b.linkTo(a) }
        nåværende.utbetalingsreferanse = tidligere.utbetalingsreferanse
        nåværende.linjertype = UEND
    }

    private var tilstand: Tilstand = Identisk()

    private lateinit var linkTo: Utbetalingslinje

    private fun ghosted(tidligere: Utbetalingslinjer) = this.also { nåværende ->
        nåværende.utbetalingsreferanse = tidligere.utbetalingsreferanse
        nåværende.linjertype = UEND
        linkTo = tidligere.last()
        nåværende.zip(tidligere).forEach { (a, b) -> tilstand.forskjell(a, b) }
        if (nåværende.size <= tidligere.size) return@also
        nåværende[tidligere.size - 1].linkTo(tidligere.last())
        nåværende
            .subList(tidligere.size - 1, nåværende.size)
            .zipWithNext()
            .map { (a, b) -> b.linkTo(a) }
    }

    private interface Tilstand {
        fun forskjell(
            nåværende: Utbetalingslinje,
            tidligere: Utbetalingslinje
        )
    }

    private inner class Identisk : Tilstand {
        override fun forskjell(
            nåværende: Utbetalingslinje,
            tidligere: Utbetalingslinje
        ) {
            if (nåværende.equals(tidligere)) return nåværende.ghostFrom(tidligere)
            if (nåværende.kunTomForskjelligFra(tidligere)) {
                nåværende.utvidTom(tidligere)
                tilstand = Ny()
                return
            }
            nåværende.linkTo(linkTo)
            linkTo = nåværende
        }
    }

    private inner class Ny: Tilstand {
        override fun forskjell(
            nåværende: Utbetalingslinje,
            tidligere: Utbetalingslinje
        ) {
            nåværende.linkTo(linkTo)
            linkTo = nåværende
        }
    }
}

internal class Utbetalingslinje internal constructor(
    internal var fom: LocalDate,
    internal var tom: LocalDate,
    internal var dagsats: Int,
    internal val grad: Double,
    private var delytelseId: Int = 1,
    private var refDelytelseId: Int? = null,
    private var linjetype: Linjetype = NY,
    private var klassekode: Klassekode = Arbeidsgiverlinje
) {

    internal fun accept(visitor: UtbetalingVisitor) {
        visitor.visitUtbetalingslinje(this, fom, tom, dagsats, grad, delytelseId, refDelytelseId)
    }

    internal fun linkTo(other: Utbetalingslinje) {
        this.delytelseId = other.delytelseId + 1
        this.refDelytelseId = other.delytelseId
    }

    override fun equals(other: Any?) = other is Utbetalingslinje && this.equals(other)

    private fun equals(other: Utbetalingslinje) =
        this.fom == other.fom &&
            this.tom == other.tom &&
            this.dagsats == other.dagsats &&
            this.grad == other.grad

    internal fun kunTomForskjelligFra(other: Utbetalingslinje) =
        this.fom == other.fom &&
            this.dagsats == other.dagsats &&
            this.grad == other.grad

    override fun hashCode(): Int {
        return fom.hashCode() * 37 +
            tom.hashCode() * 17 +
            dagsats.hashCode() * 41 +
            grad.hashCode()
    }

    internal fun ghostFrom(tidligere: Utbetalingslinje) = copyWith(UEND, tidligere)

    internal fun utvidTom(tidligere: Utbetalingslinje) = copyWith(ENDR, tidligere)

    private fun copyWith(linjetype: Linjetype, tidligere: Utbetalingslinje) {
        this.delytelseId = tidligere.delytelseId
        this.refDelytelseId = tidligere.refDelytelseId
        this.klassekode = tidligere.klassekode
        this.linjetype = linjetype
    }

    internal fun erForskjell() = linjetype != UEND
}

internal enum class Linjetype {
    NY, UEND, ENDR
}

internal enum class Klassekode(internal val verdi: String) {
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
