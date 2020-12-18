package no.nav.helse.utbetalingslinjer

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Simulering
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.simulering
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.utbetaling
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.OppdragVisitor
import no.nav.helse.sykdomstidslinje.erHelg
import no.nav.helse.utbetalingstidslinje.genererUtbetalingsreferanse
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.streams.toList

internal class Oppdrag private constructor(
    private val mottaker: String,
    private val fagområde: Fagområde,
    private val linjer: MutableList<Utbetalingslinje>,
    private var fagsystemId: String,
    private var endringskode: Endringskode,
    private val sisteArbeidsgiverdag: LocalDate?,
    private var nettoBeløp: Int = linjer.sumBy { it.totalbeløp() },
    private val tidsstempel: LocalDateTime
) : MutableList<Utbetalingslinje> by linjer {
    internal companion object {
        internal fun periode(vararg oppdrag: Oppdrag): Periode {
            return oppdrag
                .filter(Oppdrag::isNotEmpty)
                .takeIf(List<*>::isNotEmpty)
                ?.let { liste -> Periode(liste.minOf { it.førstedato }, liste.maxOf { it.sistedato }) }
                ?: Periode(LocalDate.MIN, LocalDate.MAX)
        }
    }

    internal val førstedato get() = linjer.firstOrNull()?.let { it.datoStatusFom() ?: it.fom } ?: LocalDate.MIN
    internal val sistedato get() = linjer.lastOrNull()?.tom ?: LocalDate.MIN

    internal constructor(
        mottaker: String,
        fagområde: Fagområde,
        linjer: List<Utbetalingslinje> = listOf(),
        fagsystemId: String = genererUtbetalingsreferanse(UUID.randomUUID()),
        sisteArbeidsgiverdag: LocalDate?
    ) : this(
        mottaker,
        fagområde,
        linjer.toMutableList(),
        fagsystemId,
        Endringskode.NY,
        sisteArbeidsgiverdag,
        tidsstempel = LocalDateTime.now()
    )

    internal constructor(mottaker: String, fagområde: Fagområde) :
        this(mottaker, fagområde, sisteArbeidsgiverdag = null)

    internal fun accept(visitor: OppdragVisitor) {
        visitor.preVisitOppdrag(this, totalbeløp(), nettoBeløp, tidsstempel)
        linjer.forEach { it.accept(visitor) }
        visitor.postVisitOppdrag(this, totalbeløp(), nettoBeløp, tidsstempel)
    }

    internal fun mottaker() = mottaker
    internal fun fagområde() = fagområde
    internal fun fagsystemId() = fagsystemId

    internal operator fun contains(other: Oppdrag) =
        this.tilhører(other) || this.overlapperMed(other)
    private fun tilhører(other: Oppdrag) =
        this.fagsystemId == other.fagsystemId && this.fagområde == other.fagområde
    private fun overlapperMed(other: Oppdrag) =
        maxOf(this.førstedato, other.førstedato) <= minOf(this.sistedato, other.sistedato)

    internal fun overfør(
        aktivitetslogg: IAktivitetslogg,
        maksdato: LocalDate?,
        saksbehandler: String
    ) {
        utbetaling(
            aktivitetslogg = aktivitetslogg,
            oppdrag = utenUendretLinjer(),
            maksdato = maksdato,
            saksbehandler = saksbehandler
        )
    }

    internal fun simuler(aktivitetslogg: IAktivitetslogg, maksdato: LocalDate, saksbehandler: String) {
        simulering(
            aktivitetslogg = aktivitetslogg,
            oppdrag = utenUendretLinjer(),
            maksdato = maksdato,
            saksbehandler = saksbehandler
        )
    }

    internal fun totalbeløp() = linjerUtenOpphør().sumBy { it.totalbeløp() }
    internal fun stønadsdager() = sumBy { it.stønadsdager() }

    internal fun nettoBeløp() = nettoBeløp

    internal fun nettoBeløp(tidligere: Oppdrag) {
        nettoBeløp = this.totalbeløp() - tidligere.totalbeløp()
    }

    internal fun harUtbetalinger() = utenUendretLinjer().isNotEmpty()

    internal fun sammenlignMed(simulering: Simulering) =
        simulering.valider(utenUendretLinjer())

    private fun utenUendretLinjer() = kopierMed(filter(Utbetalingslinje::erForskjell))

    private fun utenOpphørLinjer() = kopierMed(linjerUtenOpphør())

    internal fun linjerUtenOpphør() = filter { !it.erOpphør() }

    internal fun erForskjelligFra(resultat: Simulering.SimuleringResultat): Boolean {
        return dagSatser().zip(dagSatser(resultat, førstedato, sistedato)).any { (oppdrag, simulering) ->
            oppdrag.first != simulering.first || oppdrag.second != simulering.second
        }
    }

    private fun dagSatser() = linjerUtenOpphør().flatMap { linje -> linje.dager().map { it to linje.beløp } }

    private fun dagSatser(resultat: Simulering.SimuleringResultat, fom: LocalDate, tom: LocalDate) =
        resultat.perioder.flatMap {
            it.utbetalinger.flatMap {
                it.detaljer.flatMap { detalj ->
                    detalj.periode.start.datesUntil(detalj.periode.endInclusive.plusDays(1))
                        .filter { it >= fom && it <= tom }
                        .filter { !it.erHelg() }
                        .map { it to detalj.sats.sats }
                        .toList()
                }
            }
        }

    internal fun annuller(aktivitetslogg: IAktivitetslogg): Oppdrag {
        return emptied().minus(this, aktivitetslogg)
    }

    private fun emptied(): Oppdrag =
        Oppdrag(
            mottaker = mottaker,
            fagområde = fagområde,
            fagsystemId = fagsystemId,
            sisteArbeidsgiverdag = sisteArbeidsgiverdag
        )

    internal fun minus(other: Oppdrag, aktivitetslogg: IAktivitetslogg): Oppdrag {
        val tidligere = other.utenOpphørLinjer()
        return when {
            tidligere.isEmpty() || this !in other ->
                this
            this.isEmpty() ->
                deleteAll(tidligere)
            this.førstedato > tidligere.førstedato -> {
                aktivitetslogg.warn("Utbetaling opphører tidligere utbetaling. Kontroller simuleringen")
                deleted(tidligere)
            }
            this.førstedato < tidligere.førstedato -> {
                aktivitetslogg.warn("Utbetaling fra og med dato er endret. Kontroller simuleringen")
                appended(tidligere)
            }
            this.førstedato == tidligere.førstedato ->
                ghosted(tidligere)
            else -> throw IllegalArgumentException("uventet utbetalingslinje forhold")
        }
    }

    private fun deleteAll(tidligere: Oppdrag) = this.also { nåværende ->
        nåværende.kobleTil(tidligere)
        linjer.add(tidligere.last().deletion(tidligere.first().fom))
    }

    private fun appended(tidligere: Oppdrag) = this.also { nåværende ->
        nåværende.kobleTil(tidligere)
        nåværende.first().linkTo(tidligere.last())
        nåværende.zipWithNext { a, b -> b.linkTo(a) }
    }

    private lateinit var tilstand: Tilstand
    private lateinit var sisteLinjeITidligereOppdrag: Utbetalingslinje
    private lateinit var linkTo: Utbetalingslinje

    private fun ghosted(tidligere: Oppdrag, linkTo: Utbetalingslinje = tidligere.last()) =
        this.also { nåværende ->
            this.linkTo = linkTo
            nåværende.kobleTil(tidligere)
            nåværende.kopierLikeLinjer(tidligere)
            nåværende.håndterLengreNåværende(tidligere)
        }

    private fun deleted(tidligere: Oppdrag) = this.also { nåværende ->
        val deletion = nåværende.deletionLinje(tidligere)
        nåværende.appended(tidligere)
        nåværende.add(0, deletion)
    }

    private fun deletionLinje(tidligere: Oppdrag) =
        tidligere.last().deletion(tidligere.førstedato)

    private fun kopierMed(linjer: List<Utbetalingslinje>) = Oppdrag(
        mottaker,
        fagområde,
        linjer.toMutableList(),
        fagsystemId,
        endringskode,
        sisteArbeidsgiverdag,
        tidsstempel = tidsstempel
    )

    private fun kopierLikeLinjer(tidligere: Oppdrag) {
        tilstand = if (tidligere.sistedato > this.sistedato) Slett() else Identisk()
        sisteLinjeITidligereOppdrag = tidligere.last()
        this.zip(tidligere).forEach { (a, b) -> tilstand.forskjell(a, b) }
    }

    private fun håndterLengreNåværende(tidligere: Oppdrag) {
        if (this.size <= tidligere.size) return
        this[tidligere.size].linkTo(linkTo)
        this
            .subList(tidligere.size, this.size)
            .zipWithNext { a, b -> b.linkTo(a) }
    }

    private fun kobleTil(tidligere: Oppdrag) {
        this.fagsystemId = tidligere.fagsystemId
        this.forEach { it.refFagsystemId = tidligere.fagsystemId }
        this.endringskode = Endringskode.ENDR
    }

    private interface Tilstand {
        fun forskjell(
            nåværende: Utbetalingslinje,
            tidligere: Utbetalingslinje
        )
    }

    private fun håndterUlikhet(nåværende: Utbetalingslinje, tidligere: Utbetalingslinje) {
        if (nåværende.kunTomForskjelligFra(tidligere) && tidligere == sisteLinjeITidligereOppdrag)
            return nåværende.utvidTom(tidligere)
        nåværende.linkTo(linkTo)
        linkTo = nåværende
        tilstand = Ny()
    }

    private inner class Identisk() : Tilstand {
        override fun forskjell(
            nåværende: Utbetalingslinje,
            tidligere: Utbetalingslinje
        ) {
            if (nåværende == tidligere) return nåværende.ghostFrom(tidligere)
            håndterUlikhet(nåværende, tidligere)
        }
    }

    private inner class Slett() : Tilstand {
        override fun forskjell(
            nåværende: Utbetalingslinje,
            tidligere: Utbetalingslinje
        ) {
            if (nåværende == tidligere) {
                if (nåværende == last()) return nåværende.linkTo(linkTo)
                return nåværende.ghostFrom(tidligere)
            }
            håndterUlikhet(nåværende, tidligere)
        }
    }

    private inner class Ny : Tilstand {
        override fun forskjell(
            nåværende: Utbetalingslinje,
            tidligere: Utbetalingslinje
        ) {
            nåværende.linkTo(linkTo)
            linkTo = nåværende
        }
    }
}

