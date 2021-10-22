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

const val WARN_FORLENGER_OPPHØRT_OPPDRAG = "Utbetalingen forlenger et tidligere oppdrag som opphørte alle utbetalte dager. Sjekk simuleringen."

internal class Oppdrag private constructor(
    private val mottaker: String,
    private val fagområde: Fagområde,
    private val linjer: MutableList<Utbetalingslinje>,
    private var fagsystemId: String,
    private var endringskode: Endringskode,
    private val sisteArbeidsgiverdag: LocalDate?,
    private var nettoBeløp: Int = linjer.sumOf { it.totalbeløp() },
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
        visitor.preVisitOppdrag(this, totalbeløp(), nettoBeløp, tidsstempel, endringskode)
        linjer.forEach { it.accept(visitor) }
        visitor.postVisitOppdrag(this, totalbeløp(), nettoBeløp, tidsstempel)
    }

    internal fun mottaker() = mottaker
    internal fun fagområde() = fagområde
    internal fun fagsystemId() = fagsystemId

    internal operator fun contains(other: Oppdrag) = this.tilhører(other) || this.overlapperMed(other)

    private fun tilhører(other: Oppdrag) = this.fagsystemId == other.fagsystemId && this.fagområde == other.fagområde
    private fun overlapperMed(other: Oppdrag) = maxOf(this.førstedato, other.førstedato) <= minOf(this.sistedato, other.sistedato)

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

    internal fun totalbeløp() = linjerUtenOpphør().sumOf { it.totalbeløp() }
    internal fun stønadsdager() = sumOf { it.stønadsdager() }

    internal fun nettoBeløp() = nettoBeløp

    internal fun nettoBeløp(tidligere: Oppdrag) {
        nettoBeløp = this.totalbeløp() - tidligere.totalbeløp()
    }

    internal fun harUtbetalinger() = any(Utbetalingslinje::erForskjell)

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
        return somAnnullering().minus(this, aktivitetslogg)
    }

    private fun somAnnullering(): Oppdrag =
        Oppdrag(
            mottaker = mottaker,
            fagområde = fagområde,
            fagsystemId = fagsystemId,
            sisteArbeidsgiverdag = sisteArbeidsgiverdag
        )

    internal fun minus(eldre: Oppdrag, aktivitetslogg: IAktivitetslogg): Oppdrag {
        return when {
            harIngenKoblingTilTidligereOppdrag(eldre) -> this
            erAnnulleringsoppdrag() -> deleteAll(eldre)
            fomHarFlyttetSegFremover(eldre) -> {
                aktivitetslogg.warn("Utbetaling opphører tidligere utbetaling. Kontroller simuleringen")
                deleted(eldre)
            }
            fomHarFlyttetSegBakover(eldre) -> {
                aktivitetslogg.warn("Utbetaling fra og med dato er endret. Kontroller simuleringen")
                appended(eldre)
            }
            else -> erstatt(eldre, aktivitetslogg = aktivitetslogg)
        }
    }

    // Vi ønsker ikke å forlenge en annullering, eller et oppdrag vi ikke overlapper eller har samme fagsystemId som
    private fun harIngenKoblingTilTidligereOppdrag(eldre: Oppdrag) = eldre.isEmpty() || this !in eldre

    // Tomt oppdrag er synonymt med en annullering fordi måten vi genererer en annullering er å sende inn et oppdrag uten linjer
    private fun erAnnulleringsoppdrag() = this.isEmpty()

    // Vi har oppdaget utbetalingsdager tidligere i tidslinjen
    private fun fomHarFlyttetSegBakover(eldre: Oppdrag) = this.førstedato < eldre.førstedato

    // Vi har endret tidligere utbetalte dager til ikke-utbetalte dager i starten av tidslinjen
    private fun fomHarFlyttetSegFremover(eldre: Oppdrag) = this.førstedato > eldre.førstedato

    private fun deleteAll(tidligere: Oppdrag) = this.also { nåværende ->
        nåværende.kobleTil(tidligere)
        linjer.add(tidligere.last().opphørslinje(tidligere.first().fom))
    }

    private fun appended(tidligere: Oppdrag) = this.also { nåværende ->
        nåværende.kobleTil(tidligere)
        nåværende.first().kobleTil(tidligere.last())
        nåværende.zipWithNext { a, b -> b.kobleTil(a) }
    }

    private lateinit var tilstand: Tilstand
    private lateinit var sisteLinjeITidligereOppdrag: Utbetalingslinje
    private lateinit var linkTo: Utbetalingslinje

    private fun erstatt(avtroppendeOppdrag: Oppdrag, linkTo: Utbetalingslinje = avtroppendeOppdrag.last(), aktivitetslogg: IAktivitetslogg) =
        this.also { påtroppendeOppdrag ->
            this.linkTo = linkTo
            påtroppendeOppdrag.kobleTil(avtroppendeOppdrag)
            påtroppendeOppdrag.kopierLikeLinjer(avtroppendeOppdrag, aktivitetslogg)
            påtroppendeOppdrag.håndterLengreNåværende(avtroppendeOppdrag)
        }

    private fun erstatt(avtroppendeOppdrag: Oppdrag, aktivitetslogg: IAktivitetslogg): Oppdrag {
        return if (avtroppendeOppdrag.utenOpphørLinjer().isEmpty()) {
            aktivitetslogg.warn(WARN_FORLENGER_OPPHØRT_OPPDRAG)
            erstatt(avtroppendeOppdrag, avtroppendeOppdrag.last(), aktivitetslogg)
        } else {
            val avtroppendeUtenOpphør = avtroppendeOppdrag.utenOpphørLinjer()
            erstatt(avtroppendeUtenOpphør, avtroppendeUtenOpphør.last(), aktivitetslogg)
        }
    }

    private fun deleted(tidligere: Oppdrag) = this.also { nåværende ->
        val deletion = nåværende.deletionLinje(tidligere)
        nåværende.appended(tidligere)
        nåværende.add(0, deletion)
    }

    private fun opphørTidligereLinjeOgOpprettNy(nåværende: Utbetalingslinje, tidligere: Utbetalingslinje, aktivitetslogg: IAktivitetslogg) {
        linkTo = tidligere
        add(this.indexOf(nåværende), tidligere.opphørslinje(tidligere.fom))
        nåværende.kobleTil(linkTo)
        tilstand = Ny()
        aktivitetslogg.warn("Endrer tidligere oppdrag. Kontroller simuleringen.")
    }

    private fun deletionLinje(tidligere: Oppdrag) =
        tidligere.last().opphørslinje(tidligere.førstedato)

    private fun kopierMed(linjer: List<Utbetalingslinje>) = Oppdrag(
        mottaker,
        fagområde,
        linjer.toMutableList(),
        fagsystemId,
        endringskode,
        sisteArbeidsgiverdag,
        tidsstempel = tidsstempel
    )

    private fun kopierLikeLinjer(tidligere: Oppdrag, aktivitetslogg: IAktivitetslogg) {
        tilstand = if (tidligere.sistedato > this.sistedato) Slett() else Identisk()
        sisteLinjeITidligereOppdrag = tidligere.last()
        this.zip(tidligere).forEach { (a, b) -> tilstand.håndterForskjell(a, b, aktivitetslogg) }
    }

    private fun håndterLengreNåværende(tidligere: Oppdrag) {
        if (this.size <= tidligere.size) return
        this[tidligere.size].kobleTil(linkTo)
        this
            .subList(tidligere.size, this.size)
            .zipWithNext { a, b -> b.kobleTil(a) }
    }

    private fun kobleTil(tidligere: Oppdrag) {
        this.fagsystemId = tidligere.fagsystemId
        this.forEach { it.refFagsystemId = tidligere.fagsystemId }
        this.endringskode = Endringskode.ENDR
    }

    private fun håndterUlikhet(nåværende: Utbetalingslinje, tidligere: Utbetalingslinje, aktivitetslogg: IAktivitetslogg) {
        when {
            nåværende.kanEndreEksisterendeLinje(tidligere, sisteLinjeITidligereOppdrag) -> nåværende.endreEksisterendeLinje(tidligere)
            nåværende.skalOpphøreOgErstatte(tidligere, sisteLinjeITidligereOppdrag) -> opphørTidligereLinjeOgOpprettNy(nåværende, tidligere, aktivitetslogg)
            else -> opprettNyLinje(nåværende)
        }
    }

    private fun opprettNyLinje(nåværende: Utbetalingslinje) {
        nåværende.kobleTil(linkTo)
        linkTo = nåværende
        tilstand = Ny()
    }

    internal fun toBehovMap() = mutableMapOf(
        "mottaker" to mottaker,
        "fagområde" to fagområde.verdi,
        "linjer" to map { it.toMap() },
        "fagsystemId" to fagsystemId,
        "endringskode" to endringskode.toString()
    )

    internal fun toMap() = mutableMapOf(
        "mottaker" to mottaker,
        "fagområde" to fagområde.verdi,
        "linjer" to map { it.toMap() },
        "fagsystemId" to fagsystemId,
        "endringskode" to endringskode.toString(),
        "sisteArbeidsgiverdag" to sisteArbeidsgiverdag,
        "tidsstempel" to tidsstempel,
        "nettoBeløp" to nettoBeløp,
        "stønadsdager" to stønadsdager(),
        "fom" to førstedato,
        "tom" to sistedato
    )

    private interface Tilstand {
        fun håndterForskjell(nåværende: Utbetalingslinje, tidligere: Utbetalingslinje, aktivitetslogg: IAktivitetslogg)
    }

    private inner class Identisk : Tilstand {
        override fun håndterForskjell(nåværende: Utbetalingslinje, tidligere: Utbetalingslinje, aktivitetslogg: IAktivitetslogg) {
            when (nåværende == tidligere) {
                true -> nåværende.markerUendret(tidligere)
                false -> håndterUlikhet(nåværende, tidligere, aktivitetslogg)
            }
        }
    }

    private inner class Slett : Tilstand {
        override fun håndterForskjell(nåværende: Utbetalingslinje, tidligere: Utbetalingslinje, aktivitetslogg: IAktivitetslogg) {
            if (nåværende == tidligere) {
                if (nåværende == last()) return nåværende.kobleTil(linkTo)
                return nåværende.markerUendret(tidligere)
            }
            håndterUlikhet(nåværende, tidligere, aktivitetslogg)
        }
    }

    private inner class Ny : Tilstand {
        override fun håndterForskjell(nåværende: Utbetalingslinje, tidligere: Utbetalingslinje, aktivitetslogg: IAktivitetslogg) {
            nåværende.kobleTil(linkTo)
            linkTo = nåværende
        }
    }
}

