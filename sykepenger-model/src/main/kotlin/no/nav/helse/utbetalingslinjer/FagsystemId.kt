package no.nav.helse.utbetalingslinjer

import no.nav.helse.hendelser.AnnullerUtbetaling
import no.nav.helse.hendelser.UtbetalingHendelse
import no.nav.helse.hendelser.Utbetalingsgodkjenning
import no.nav.helse.person.FagsystemIdVisitor
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.utbetalingslinjer.FagsystemId.Utbetaling.Companion.head
import no.nav.helse.utbetalingslinjer.FagsystemId.Utbetaling.Companion.sorterOppdrag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import java.time.LocalDate
import java.time.LocalDateTime

internal class FagsystemId private constructor(
    oppdragsliste: List<Utbetaling>,
    forkastet: List<Utbetaling> = emptyList()
) {

    private constructor(oppdrag: Oppdrag, utbetalingstidslinje: Utbetalingstidslinje) : this(listOf(Utbetaling(oppdrag, utbetalingstidslinje)))

    private val aktive = sorterOppdrag(oppdragsliste)
    private val forkastet = sorterOppdrag(forkastet)

    private val head get() = aktive.head()
    private val utbetalingstidslinje get() = head.utbetalingstidslinje()
    private var tilstand: Tilstand = Ny

    private var observer: FagsystemIdObserver = object : FagsystemIdObserver {}

    init {
        require(oppdragsliste.isNotEmpty())
    }

    internal fun fagsystemId() = head.fagsystemId()
    internal fun fagområde() = head.fagområde()
    internal fun nettoBeløp() = head.nettobeløp()

    internal fun register(fagsystemIdObserver: FagsystemIdObserver) {
        observer = fagsystemIdObserver
    }

    internal fun accept(visitor: FagsystemIdVisitor) {
        visitor.preVisitFagsystemId(this, fagsystemId(), fagområde(), utbetalingstidslinje)
        visitor.preVisitOppdragsliste()
        aktive.onEach { it.accept(visitor) }
        visitor.postVisitOppdragsliste()
        visitor.postVisitFagsystemId(this, fagsystemId(), fagområde(), utbetalingstidslinje)
    }

    internal fun håndter(hendelse: Utbetalingsgodkjenning, maksdato: LocalDate) {
        tilstand.utbetal(this, hendelse, hendelse.saksbehandlerEpost(), hendelse.saksbehandler(), hendelse.godkjenttidspunkt(), maksdato)
    }

    internal fun håndter(hendelse: AnnullerUtbetaling): Boolean {
        if (!hendelse.erRelevant(fagsystemId())) return false
        tilstand.annuller(this, hendelse, hendelse.saksbehandlerEpost, hendelse.saksbehandlerIdent, hendelse.opprettet)
        return true
    }

    internal fun håndter(hendelse: UtbetalingHendelse): Boolean {
        if (!hendelse.erRelevant(fagsystemId())) return false
        tilstand.kvittér(this, hendelse)
        return true
    }

    internal fun simuler(aktivitetslogg: IAktivitetslogg, maksdato: LocalDate, saksbehandler: String) {
        tilstand.simuler(this, aktivitetslogg, maksdato, saksbehandler)
    }

    internal fun erTom() = aktive.isEmpty()

    internal fun erAnnullert() = tilstand == Annullert

    private fun utvide(opprinnelig: Oppdrag, utbetalingstidslinje: Utbetalingstidslinje, aktivitetslogg: IAktivitetslogg): Boolean {
        val utbetaling = head.lagUtvidelse(opprinnelig, utbetalingstidslinje, aktivitetslogg) ?: return false
        return tilstand.nyUtbetaling(this, utbetaling)
    }

    private fun nyUtbetaling(utbetaling: Utbetaling) {
        aktive.add(0, utbetaling)
    }

    private fun forkastUtbetaling() {
        val søppel = aktive.removeFirst()
        forkastet.add(0, søppel.avslutt())
    }

    internal companion object {
        internal fun utvide(fagsystemIder: MutableList<FagsystemId>, oppdrag: Oppdrag, utbetalingstidslinje: Utbetalingstidslinje, aktivitetslogg: IAktivitetslogg): FagsystemId =
            fagsystemIder.firstOrNull { it.utvide(oppdrag, utbetalingstidslinje, aktivitetslogg) }
                ?: FagsystemId(oppdrag, utbetalingstidslinje).also {
                    if (oppdrag.isEmpty()) return@also
                    fagsystemIder.add(it)
                }
    }

    private fun tilstand(nyTilstand: Tilstand) {
        val gammelTilstand = tilstand
        tilstand = nyTilstand
        observer.tilstandEndret(this, gammelTilstand::class.simpleName!!, nyTilstand::class.simpleName!!)
        tilstand.entering(this)
    }

    private class Utbetaling(
        private val oppdrag: Oppdrag,
        private val utbetalingstidslinje: Utbetalingstidslinje,
        private val opprettet: LocalDateTime = LocalDateTime.now(),
        private var avsluttet: LocalDateTime? = null
    ) {
        fun fagsystemId() = oppdrag.fagsystemId()
        fun fagområde() = oppdrag.fagområde()
        fun nettobeløp() = oppdrag.nettoBeløp()
        fun utbetalingstidslinje() = utbetalingstidslinje

        fun accept(visitor: FagsystemIdVisitor) {
            visitor.preVisitUtbetaling(oppdrag, utbetalingstidslinje, opprettet, avsluttet)
            oppdrag.accept(visitor)
            utbetalingstidslinje.accept(visitor)
            visitor.postVisitUtbetaling(oppdrag, utbetalingstidslinje, opprettet, avsluttet)
        }

        fun simuler(aktivitetslogg: IAktivitetslogg, maksdato: LocalDate, ident: String) {
            oppdrag.simuler(aktivitetslogg, maksdato, ident)
        }

        fun annuller(
            fagsystemId: FagsystemId,
            aktivitetslogg: IAktivitetslogg,
            ident: String,
            epost: String,
            godkjenttidspunkt: LocalDateTime
        ) {
            val oppdrag = oppdrag.annullere(fagsystemId, aktivitetslogg, ident, epost, godkjenttidspunkt)
            fagsystemId.nyUtbetaling(Utbetaling(oppdrag, Utbetalingstidslinje()))
        }

        fun utbetal(
            fagsystemId: FagsystemId,
            aktivitetslogg: IAktivitetslogg,
            maksdato: LocalDate,
            ident: String,
            epost: String,
            godkjenttidspunkt: LocalDateTime
        ) {
            oppdrag.utbetal(fagsystemId, aktivitetslogg, maksdato, ident, epost, godkjenttidspunkt)
        }

        fun kvittér(fagsystemId: FagsystemId, hendelse: UtbetalingHendelse) {
            oppdrag.håndter(fagsystemId, hendelse)
        }

        fun lagUtvidelse(opprinnelig: Oppdrag, utbetalingstidslinje: Utbetalingstidslinje, aktivitetslogg: IAktivitetslogg): Utbetaling? {
            if (opprinnelig.fagområde() != fagområde()) return null
            val nytt = opprinnelig.minus(oppdrag, aktivitetslogg)
            if (nytt.fagsystemId() != fagsystemId()) return null
            nytt.nettoBeløp(oppdrag)
            return Utbetaling(nytt, utbetalingstidslinje)
        }

        fun avslutt(tidsstempel: LocalDateTime = LocalDateTime.now()) = apply {
            avsluttet = tidsstempel
        }

        companion object {
            fun sorterOppdrag(liste: List<Utbetaling>) = liste.sortedByDescending { it.opprettet }.toMutableList()

            fun List<Utbetaling>.head() = first()
            fun List<Utbetaling>.sisteAvsluttede() = firstOrNull { it.avsluttet != null }
        }
    }

    private interface Tilstand {
        fun entering(fagsystemId: FagsystemId) {}

        fun utbetal(
            fagsystemId: FagsystemId,
            hendelse: Utbetalingsgodkjenning,
            epost: String,
            ident: String,
            godkjenttidspunkt: LocalDateTime,
            maksdato: LocalDate
        ) {
            throw IllegalStateException("Forventet ikke å utbetale på fagsystemId=${fagsystemId.fagsystemId()} i tilstand=${this::class.simpleName}")
        }

        fun annuller(
            fagsystemId: FagsystemId,
            hendelse: AnnullerUtbetaling,
            epost: String,
            ident: String,
            godkjenttidspunkt: LocalDateTime
        ) {
            throw IllegalStateException("Forventet ikke å annullere på fagsystemId=${fagsystemId.fagsystemId()} i tilstand=${this::class.simpleName}")
        }

        fun kvittér(fagsystemId: FagsystemId, hendelse: UtbetalingHendelse) {
            throw IllegalStateException("Forventet ikke kvittering på fagsystemId=${fagsystemId.fagsystemId()} i tilstand=${this::class.simpleName}")
        }

        fun simuler(
            fagsystemId: FagsystemId,
            aktivitetslogg: IAktivitetslogg,
            maksdato: LocalDate,
            saksbehandler: String
        ) {
            throw IllegalStateException("Forventet ikke simulering på fagsystemId=${fagsystemId.fagsystemId()} i tilstand=${this::class.simpleName}")
        }

        fun nyUtbetaling(fagsystemId: FagsystemId, utbetaling: Utbetaling): Boolean {
            throw IllegalStateException("Kan ikke legge til ny utbetaling fagsystemId=${fagsystemId.fagsystemId()} i tilstand=${this::class.simpleName}")
        }
    }

    private object Ny: Tilstand {
        override fun utbetal(
            fagsystemId: FagsystemId,
            hendelse: Utbetalingsgodkjenning,
            epost: String,
            ident: String,
            godkjenttidspunkt: LocalDateTime,
            maksdato: LocalDate
        ) {
            if (hendelse.valider().hasErrorsOrWorse()) return fagsystemId.tilstand(Avvist)
            fagsystemId.head.utbetal(fagsystemId, hendelse, maksdato, ident, epost, godkjenttidspunkt)
            fagsystemId.tilstand(UtbetalingOverført)
        }

        override fun simuler(
            fagsystemId: FagsystemId,
            aktivitetslogg: IAktivitetslogg,
            maksdato: LocalDate,
            saksbehandler: String
        ) {
            fagsystemId.head.simuler(aktivitetslogg, maksdato, saksbehandler)
        }
    }

    private object Aktiv: Tilstand {

        override fun nyUtbetaling(fagsystemId: FagsystemId, utbetaling: Utbetaling): Boolean {
            fagsystemId.nyUtbetaling(utbetaling)
            fagsystemId.tilstand(Ubetalt)
            return true
        }

        override fun annuller(
            fagsystemId: FagsystemId,
            hendelse: AnnullerUtbetaling,
            epost: String,
            ident: String,
            godkjenttidspunkt: LocalDateTime
        ) {
            fagsystemId.head.annuller(fagsystemId, hendelse, ident, epost, godkjenttidspunkt)
            fagsystemId.tilstand(AnnulleringOverført)
        }
    }

    private object Ubetalt: Tilstand {
        override fun utbetal(
            fagsystemId: FagsystemId,
            hendelse: Utbetalingsgodkjenning,
            epost: String,
            ident: String,
            godkjenttidspunkt: LocalDateTime,
            maksdato: LocalDate
        ) {
            if (hendelse.valider().hasErrorsOrWorse()) {
                fagsystemId.forkastUtbetaling()
                return fagsystemId.tilstand(Aktiv)
            }
            fagsystemId.head.utbetal(fagsystemId, hendelse, maksdato, ident, epost, godkjenttidspunkt)
            fagsystemId.tilstand(UtbetalingOverført)
        }

        override fun simuler(
            fagsystemId: FagsystemId,
            aktivitetslogg: IAktivitetslogg,
            maksdato: LocalDate,
            saksbehandler: String
        ) {
            fagsystemId.head.simuler(aktivitetslogg, maksdato, saksbehandler)
        }

        override fun annuller(
            fagsystemId: FagsystemId,
            hendelse: AnnullerUtbetaling,
            epost: String,
            ident: String,
            godkjenttidspunkt: LocalDateTime
        ) {
            fagsystemId.forkastUtbetaling()
            fagsystemId.head.annuller(fagsystemId, hendelse, ident, epost, godkjenttidspunkt)
            fagsystemId.tilstand(AnnulleringOverført)
        }
    }

    private object UtbetalingOverført: Tilstand {
        override fun kvittér(fagsystemId: FagsystemId, hendelse: UtbetalingHendelse) {
            fagsystemId.head.kvittér(fagsystemId, hendelse)
            if (hendelse.valider().hasErrorsOrWorse()) return fagsystemId.tilstand(Avvist)
            fagsystemId.head.avslutt()
            fagsystemId.tilstand(Aktiv)
        }
    }

    private object AnnulleringOverført: Tilstand {
        override fun kvittér(fagsystemId: FagsystemId, hendelse: UtbetalingHendelse) {
            fagsystemId.head.kvittér(fagsystemId, hendelse)
            if (hendelse.valider().hasErrorsOrWorse()) return fagsystemId.tilstand(Avvist)
            fagsystemId.tilstand(Annullert)
        }
    }

    private object Avvist: Tilstand {
        override fun nyUtbetaling(fagsystemId: FagsystemId, utbetaling: Utbetaling) = false
    }

    private object Annullert: Tilstand {
        override fun entering(fagsystemId: FagsystemId) {
            fagsystemId.head.avslutt()
        }

        override fun nyUtbetaling(fagsystemId: FagsystemId, utbetaling: Utbetaling) = false
    }
}
