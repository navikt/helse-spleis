package no.nav.helse.utbetalingslinjer

import no.nav.helse.person.OppdragVisitor
import no.nav.helse.utbetalingstidslinje.genererUtbetalingsreferanse
import java.time.LocalDate
import java.util.*

internal class Oppdrag private constructor(
    private val mottaker: String,
    private val fagområde: Fagområde,
    private val linjer: MutableList<Utbetalingslinje>,
    private var fagsystemId: String,
    private var endringskode: Endringskode,
    private val sisteArbeidsgiverdag: LocalDate?,
    private val sjekksum: Int
): MutableList<Utbetalingslinje> by linjer {

    internal constructor(
        mottaker: String,
        fagområde: Fagområde,
        linjer: List<Utbetalingslinje> = listOf(),
        fagsystemId: String = genererUtbetalingsreferanse(UUID.randomUUID()),
        sisteArbeidsgiverdag: LocalDate?
    ): this(
        mottaker,
        fagområde,
        linjer.toMutableList(),
        fagsystemId,
        Endringskode.NY,
        sisteArbeidsgiverdag,
        linjer.hashCode() * 67 + mottaker.hashCode()
    )

    internal fun accept(visitor: OppdragVisitor) {
        visitor.preVisitOppdrag(this)
        linjer.forEach { it.accept(visitor) }
        visitor.postVisitOppdrag(this)
    }

    internal fun fagsystemId() = fagsystemId

    internal val førstedato get() = linjer.firstOrNull()?.fom ?: LocalDate.MIN

    internal val sistedato get() = linjer.lastOrNull()?.tom ?: LocalDate.MIN

    internal fun removeUEND() = Oppdrag(
        mottaker,
        fagområde,
        linjer.filter { it.erForskjell() }.toMutableList(),
        fagsystemId,
        endringskode,
        sisteArbeidsgiverdag,
        linjer.hashCode() * 67 + mottaker.hashCode()
    )

    internal fun totalbeløp() = this.sumBy { it.totalbeløp() }
    internal fun dager() = this.flatMap { linje -> linje.dager().map { it to linje.dagsats } }

    infix fun forskjell(tidligere: Oppdrag): Oppdrag {
        return when {
            tidligere.isEmpty() ->
                this
            this.isEmpty() &&
                (this.sisteArbeidsgiverdag == null || this.sisteArbeidsgiverdag < tidligere.sistedato) ->
                deleteAll(tidligere)
            this.isEmpty() && this.sisteArbeidsgiverdag != null && this.sisteArbeidsgiverdag > tidligere.sistedato ->
                this
            this.førstedato > tidligere.sistedato ->
                this
            this.førstedato < tidligere.førstedato ->
                appended(tidligere)
            this.førstedato == tidligere.førstedato ->
                ghosted(tidligere)
            this.førstedato > tidligere.førstedato ->
                deleted(tidligere)
            else ->
                throw IllegalArgumentException("uventet utbetalingslinje forhold")
        }
    }

    private fun deleteAll(tidligere: Oppdrag) = this.also { nåværende ->
        nåværende.kobleTil(tidligere)
        linjer.add(
            tidligere.last().deletion(
                tidligere.fagsystemId,
                tidligere.first().fom,
                tidligere.last().tom
            )
        )
    }

    private fun appended(tidligere: Oppdrag) = this.also { nåværende ->
        nåværende.first().linkTo(tidligere.last())
        zipWithNext().map { (a, b) -> b.linkTo(a) }
        nåværende.kobleTil(tidligere)
        nåværende.håndterLengreTidligere(tidligere)
    }

    private var tilstand: Tilstand = Identisk()

    private lateinit var linkTo: Utbetalingslinje

    private fun ghosted(tidligere: Oppdrag, linkTo: Utbetalingslinje = tidligere.last()) =
        this.also { nåværende ->
            this.linkTo = linkTo
            nåværende.kobleTil(tidligere)
            nåværende.kopierLikeLinjer(tidligere)
            nåværende.håndterLengreNåværende(tidligere)
            nåværende.håndterLengreTidligere(tidligere)
        }

    private fun deleted(tidligere: Oppdrag): Oppdrag {
        return this.also { nåværende ->
            nåværende.kobleTil(tidligere)
            val deletion = nåværende.deletionLinje(tidligere)
            val revisedTidligere = tidligere.copyAfter(nåværende.førstedato.minusDays(1))                // Remove any periods from tidligere that overlap deletion
            nåværende.kopierLikeLinjer(revisedTidligere)
            nåværende.håndterLengreNåværende(revisedTidligere)
            nåværende.håndterLengreTidligere(tidligere)
            nåværende.add(0, deletion)
        }
    }

    private fun deletionLinje(tidligere: Oppdrag) =
        tidligere.last().deletion(
            tidligere.fagsystemId,
            tidligere.førstedato,
            this.førstedato.minusDays(1)
        ).also { linkTo = it }

    private fun copyAfter(dato: LocalDate) = Oppdrag(
        mottaker,
        fagområde,
        linjer.filter { it.fom > dato }.toMutableList(),
        fagsystemId,
        endringskode,
        sisteArbeidsgiverdag,
        sjekksum
    )

    private fun kopierLikeLinjer(tidligere: Oppdrag) =
        this.zip(tidligere).forEach { (a, b) -> tilstand.forskjell(a, b) }

    private fun håndterLengreNåværende(tidligere: Oppdrag) {
        if (this.size <= tidligere.size) return
        this[tidligere.size].linkTo(linkTo)
        this
            .subList(tidligere.size, this.size)
            .zipWithNext()
            .map { (a, b) -> b.linkTo(a) }
    }

    private fun håndterLengreTidligere(tidligere: Oppdrag) {
        if (this.sistedato >= tidligere.sistedato) return
        this.add(this.last().deletion(
            tidligere.fagsystemId,
            this.last().tom.plusDays(1),
            tidligere.last().tom
        ))
    }

    private fun kobleTil(tidligere: Oppdrag) {
        this.fagsystemId = tidligere.fagsystemId
        this.forEach { it.refFagsystemId = tidligere.fagsystemId }
        this.endringskode = Endringskode.UEND
    }

    internal fun emptied(): Oppdrag =
        Oppdrag(
            mottaker = mottaker,
            fagområde = fagområde,
            fagsystemId = fagsystemId,
            sisteArbeidsgiverdag = sisteArbeidsgiverdag
        )

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
