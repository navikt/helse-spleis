package no.nav.helse.utbetalingslinjer

import no.nav.helse.hendelser.AnnullerUtbetaling
import no.nav.helse.hendelser.UtbetalingHendelse
import no.nav.helse.hendelser.Utbetalingsgodkjenning
import no.nav.helse.person.FagsystemIdVisitor
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.utbetalingslinjer.FagsystemId.Utbetaling.Companion.head
import no.nav.helse.utbetalingslinjer.FagsystemId.Utbetaling.Companion.sorterOppdrag
import no.nav.helse.utbetalingslinjer.FagsystemId.Utbetaling.Companion.utbetaltTidslinje
import no.nav.helse.utbetalingstidslinje.Historie
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import java.time.LocalDate
import java.time.LocalDateTime

internal class FagsystemId private constructor(
    private val fagsystemId: String,
    private val fagområde: Fagområde,
    private var tilstand: Tilstand,
    oppdragsliste: List<Utbetaling>,
    forkastet: List<Utbetaling>
) {

    private constructor(observatør: FagsystemIdObserver, oppdrag: Oppdrag, utbetalingstidslinje: Utbetalingstidslinje) : this(
        oppdrag.fagsystemId(),
        oppdrag.fagområde(),
        Initiell,
        emptyList(),
        emptyList()
    ) {
        register(observatør)
        tilstand.nyUtbetaling(this, Utbetaling(oppdrag, utbetalingstidslinje))
    }

    private val aktive = sorterOppdrag(oppdragsliste)
    private val forkastet = sorterOppdrag(forkastet)

    private val head get() = aktive.head()
    private val utbetalingstidslinje get() = head.utbetalingstidslinje()
    private val utbetaltTidslinje get() = aktive.utbetaltTidslinje()

    private var observer: FagsystemIdObserver = object : FagsystemIdObserver {}

    init {
        require((tilstand == Initiell && oppdragsliste.isEmpty()) || (tilstand != Initiell && oppdragsliste.isNotEmpty()))
    }

    internal fun nettoBeløp() = head.nettobeløp()

    internal fun append(orgnr: String, bøtte: Historie.Historikkbøtte) {
        if (utbetaltTidslinje.isEmpty()) return
        bøtte.add(orgnr, utbetaltTidslinje)
    }

    internal fun register(fagsystemIdObserver: FagsystemIdObserver) {
        observer = fagsystemIdObserver
    }

    internal fun accept(visitor: FagsystemIdVisitor) {
        visitor.preVisitFagsystemId(this, fagsystemId, fagområde, tilstand::class.simpleName!!, utbetalingstidslinje, utbetaltTidslinje)
        visitor.preVisitOppdragsliste()
        aktive.onEach { it.accept(visitor) }
        visitor.postVisitOppdragsliste()
        visitor.postVisitFagsystemId(this, fagsystemId, fagområde, tilstand::class.simpleName!!, utbetalingstidslinje, utbetaltTidslinje)
    }

    internal fun håndter(hendelse: Utbetalingsgodkjenning, maksdato: LocalDate) {
        tilstand.overfør(this, hendelse, hendelse.saksbehandlerEpost(), hendelse.saksbehandler(), hendelse.godkjenttidspunkt(), maksdato)
    }

    internal fun håndter(hendelse: AnnullerUtbetaling): Boolean {
        if (!hendelse.erRelevant(fagsystemId)) return false
        tilstand.annuller(this, hendelse, hendelse.saksbehandlerEpost, hendelse.saksbehandlerIdent, hendelse.opprettet)
        return true
    }

    internal fun håndter(hendelse: UtbetalingHendelse): Boolean {
        if (!hendelse.erRelevant(fagsystemId)) return false
        tilstand.kvittér(this, hendelse)
        return true
    }

    internal fun simuler(aktivitetslogg: IAktivitetslogg, maksdato: LocalDate, saksbehandler: String) {
        tilstand.simuler(this, aktivitetslogg, maksdato, saksbehandler)
    }

    internal fun erTom() = aktive.isEmpty()

    internal fun erAnnullert() = tilstand == Annullert

    private fun utvide(opprinnelig: Oppdrag, utbetalingstidslinje: Utbetalingstidslinje, aktivitetslogg: IAktivitetslogg): Boolean {
        val utbetaling = Utbetaling.lagUtvidelse(this, head, opprinnelig, utbetalingstidslinje, aktivitetslogg) ?: return false
        return tilstand.nyUtbetaling(this, utbetaling)
    }

    private fun nyUtbetaling(utbetaling: Utbetaling) {
        aktive.add(0, utbetaling)
    }

    private fun nyUtbetaling(nesteTilstand: Tilstand, utbetaling: Utbetaling) {
        nyUtbetaling(utbetaling)
        tilstand(nesteTilstand)
    }

    private fun overfør(nesteTilstand: Tilstand, aktivitetslogg: IAktivitetslogg, maksdato: LocalDate?, ident: String, epost: String, godkjenttidspunkt: LocalDateTime) {
        head.overfør(aktivitetslogg, maksdato, ident, epost, godkjenttidspunkt)
        tilstand(nesteTilstand)
    }

    private fun annuller(aktivitetslogg: IAktivitetslogg, ident: String, epost: String, godkjenttidspunkt: LocalDateTime) {
        nyUtbetaling(Utbetaling.lagAnnullering(head, aktivitetslogg))
        overfør(AnnulleringOverført, aktivitetslogg, null, ident, epost, godkjenttidspunkt)
    }

    private fun avsluttUtbetaling(nesteTilstand: Tilstand) {
        head.avslutt()
        tilstand(nesteTilstand)
    }

    private fun forkastUtbetaling() {
        val søppel = aktive.removeFirst()
        forkastet.add(0, søppel.avslutt())
    }

    internal companion object {
        internal fun utvide(fagsystemIder: MutableList<FagsystemId>, observatør: FagsystemIdObserver, oppdrag: Oppdrag, utbetalingstidslinje: Utbetalingstidslinje, aktivitetslogg: IAktivitetslogg): FagsystemId =
            fagsystemIder.firstOrNull { it.utvide(oppdrag, utbetalingstidslinje, aktivitetslogg) }
                ?: FagsystemId(observatør, oppdrag, utbetalingstidslinje).also {
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

        fun overfør(aktivitetslogg: IAktivitetslogg, maksdato: LocalDate?, ident: String, epost: String, godkjenttidspunkt: LocalDateTime) {
            oppdrag.overfør(aktivitetslogg, maksdato, ident, epost, godkjenttidspunkt)
        }

        fun avslutt(tidsstempel: LocalDateTime = LocalDateTime.now()) = apply {
            avsluttet = tidsstempel
        }

        companion object {
            fun lagUtvidelse(fagsystemId: FagsystemId, siste: Utbetaling, kandidat: Oppdrag, utbetalingstidslinje: Utbetalingstidslinje, aktivitetslogg: IAktivitetslogg): Utbetaling? {
                if (kandidat.fagområde() != fagsystemId.fagområde) return null
                val nytt = kandidat.minus(siste.oppdrag, aktivitetslogg)
                if (nytt.fagsystemId() != fagsystemId.fagsystemId) return null
                nytt.nettoBeløp(siste.oppdrag)
                return Utbetaling(nytt, utbetalingstidslinje)
            }

            fun lagAnnullering(siste: Utbetaling, aktivitetslogg: IAktivitetslogg): Utbetaling {
                val oppdrag = siste.oppdrag.emptied().minus(siste.oppdrag, aktivitetslogg)
                return Utbetaling(oppdrag, Utbetalingstidslinje())
            }

            fun sorterOppdrag(liste: List<Utbetaling>) = liste.sortedByDescending { it.opprettet }.toMutableList()

            fun List<Utbetaling>.head() = first()

            fun List<Utbetaling>.utbetaltTidslinje() = firstOrNull { it.avsluttet != null }?.utbetalingstidslinje ?: Utbetalingstidslinje()
        }
    }

    private interface Tilstand {
        fun entering(fagsystemId: FagsystemId) {}

        fun overfør(
            fagsystemId: FagsystemId,
            hendelse: Utbetalingsgodkjenning,
            epost: String,
            ident: String,
            godkjenttidspunkt: LocalDateTime,
            maksdato: LocalDate
        ) {
            throw IllegalStateException("Forventet ikke å utbetale på fagsystemId=${fagsystemId.fagsystemId} i tilstand=${this::class.simpleName}")
        }

        fun annuller(
            fagsystemId: FagsystemId,
            hendelse: AnnullerUtbetaling,
            epost: String,
            ident: String,
            godkjenttidspunkt: LocalDateTime
        ) {
            throw IllegalStateException("Forventet ikke å annullere på fagsystemId=${fagsystemId.fagsystemId} i tilstand=${this::class.simpleName}")
        }

        fun kvittér(fagsystemId: FagsystemId, hendelse: UtbetalingHendelse) {
            throw IllegalStateException("Forventet ikke kvittering på fagsystemId=${fagsystemId.fagsystemId} i tilstand=${this::class.simpleName}")
        }

        fun simuler(
            fagsystemId: FagsystemId,
            aktivitetslogg: IAktivitetslogg,
            maksdato: LocalDate,
            saksbehandler: String
        ) {
            throw IllegalStateException("Forventet ikke simulering på fagsystemId=${fagsystemId.fagsystemId} i tilstand=${this::class.simpleName}")
        }

        fun nyUtbetaling(fagsystemId: FagsystemId, utbetaling: Utbetaling): Boolean {
            throw IllegalStateException("Kan ikke legge til ny utbetaling fagsystemId=${fagsystemId.fagsystemId} i tilstand=${this::class.simpleName}")
        }
    }


    private object Initiell : Tilstand {
        override fun nyUtbetaling(fagsystemId: FagsystemId, utbetaling: Utbetaling): Boolean {
            fagsystemId.nyUtbetaling(Ny, utbetaling)
            return true
        }
    }

    private object Ny : Tilstand {
        override fun overfør(
            fagsystemId: FagsystemId,
            hendelse: Utbetalingsgodkjenning,
            epost: String,
            ident: String,
            godkjenttidspunkt: LocalDateTime,
            maksdato: LocalDate
        ) {
            if (hendelse.valider().hasErrorsOrWorse()) return fagsystemId.tilstand(Avvist)
            fagsystemId.overfør(UtbetalingOverført, hendelse, maksdato, ident, epost, godkjenttidspunkt)
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

    private object Aktiv : Tilstand {
        override fun nyUtbetaling(fagsystemId: FagsystemId, utbetaling: Utbetaling): Boolean {
            fagsystemId.nyUtbetaling(Ubetalt, utbetaling)
            return true
        }

        override fun annuller(
            fagsystemId: FagsystemId,
            hendelse: AnnullerUtbetaling,
            epost: String,
            ident: String,
            godkjenttidspunkt: LocalDateTime
        ) {
            fagsystemId.annuller(hendelse, ident, epost, godkjenttidspunkt)
        }
    }

    private object Ubetalt : Tilstand {
        override fun overfør(
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
            fagsystemId.overfør(UtbetalingOverført, hendelse, maksdato, ident, epost, godkjenttidspunkt)
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
            fagsystemId.annuller(hendelse, ident, epost, godkjenttidspunkt)
        }
    }

    private object UtbetalingOverført : Tilstand {
        override fun kvittér(fagsystemId: FagsystemId, hendelse: UtbetalingHendelse) {
            if (hendelse.valider().hasErrorsOrWorse()) return fagsystemId.tilstand(Avvist)
            fagsystemId.avsluttUtbetaling(Aktiv)
        }
    }

    private object AnnulleringOverført : Tilstand {
        override fun kvittér(fagsystemId: FagsystemId, hendelse: UtbetalingHendelse) {
            if (hendelse.valider().hasErrorsOrWorse()) return fagsystemId.tilstand(Avvist)
            fagsystemId.avsluttUtbetaling(Annullert)
        }
    }

    private object Avvist : Tilstand {
        override fun nyUtbetaling(fagsystemId: FagsystemId, utbetaling: Utbetaling) = false
    }

    private object Annullert : Tilstand {
        override fun nyUtbetaling(fagsystemId: FagsystemId, utbetaling: Utbetaling) = false
    }
}
