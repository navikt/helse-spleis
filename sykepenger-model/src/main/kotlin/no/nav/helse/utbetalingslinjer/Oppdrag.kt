package no.nav.helse.utbetalingslinjer

import no.nav.helse.person.UtbetalingVisitor
import no.nav.helse.utbetalingstidslinje.genererUtbetalingsreferanse
import java.time.LocalDate
import java.util.*

internal class Oppdrag private constructor(
    private val mottaker: String,
    private val fagområde: Fagområde,
    private val linjer: List<Utbetalingslinje>,
    private var fagsystemId: String,
    private var endringskode: Endringskode,
    private val sjekksum: Int
): List<Utbetalingslinje> by linjer {

    internal constructor(
        mottaker: String,
        fagområde: Fagområde,
        linjer: List<Utbetalingslinje> = listOf(),
        fagsystemId: String = genererUtbetalingsreferanse(UUID.randomUUID())
    ): this(
        mottaker,
        fagområde,
        linjer,
        fagsystemId,
        Endringskode.NY,
        linjer.hashCode() * 67 + mottaker.hashCode()
    )

    internal fun accept(visitor: UtbetalingVisitor) {
        visitor.preVisitOppdrag(this)
        linjer.forEach { it.accept(visitor) }
        visitor.postVisitOppdrag(this)
    }

    internal fun referanse() = fagsystemId

    private val førstedato get() = linjer.firstOrNull()?.fom ?: LocalDate.MIN

    private val sistedato get() = linjer.lastOrNull()?.tom ?: LocalDate.MIN

    internal fun removeUEND() = Oppdrag(
        mottaker,
        fagområde,
        linjer.filter { it.erForskjell() },
        fagsystemId,
        endringskode,
        linjer.hashCode() * 67 + mottaker.hashCode()
    )

    internal fun totalbeløp() = this.sumBy { it.totalbeløp() }

    infix fun forskjell(tidligere: Oppdrag): Oppdrag {
        return when {
            this.isEmpty() ->
                this
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

    private fun appended(tidligere: Oppdrag) = this.also { nåværende ->
        nåværende.first().linkTo(tidligere.last())
        zipWithNext().map { (a, b) -> b.linkTo(a) }
        nåværende.kobleTil(tidligere)
    }

    private var tilstand: Tilstand = Identisk()

    private lateinit var linkTo: Utbetalingslinje

    private fun ghosted(tidligere: Oppdrag) = this.also { nåværende ->
        linkTo = tidligere.last()
        nåværende.kobleTil(tidligere)
        nåværende.zip(tidligere).forEach { (a, b) -> tilstand.forskjell(a, b) }
        nåværende.håndterResten(tidligere)
    }

    private fun håndterResten(tidligere: Oppdrag) {
        if (this.size <= tidligere.size) return
        this[tidligere.size].linkTo(linkTo)
        this
            .subList(tidligere.size, this.size)
            .zipWithNext()
            .map { (a, b) -> b.linkTo(a) }
    }

    private fun kobleTil(tidligere: Oppdrag) {
        this.fagsystemId = tidligere.fagsystemId
        this.forEach { it.refFagsystemId = tidligere.fagsystemId }
        this.endringskode = Endringskode.UEND
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
            tilstand = Ny()
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
