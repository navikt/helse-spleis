package no.nav.helse.utbetalingslinjer

import no.nav.helse.hendelser.AnnullerUtbetaling
import no.nav.helse.hendelser.UtbetalingHendelse
import no.nav.helse.hendelser.Utbetalingsgodkjenning
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.FagsystemIdVisitor
import no.nav.helse.person.IAktivitetslogg
import java.time.LocalDate
import java.time.LocalDateTime

internal class FagsystemId private constructor(oppdragsliste: List<Oppdrag>) {

    private val oppdragsliste = Oppdrag.sorter(oppdragsliste).toMutableList()
    private val head get() = oppdragsliste.first()
    private val fagsystemId get() = head.fagsystemId()
    private val fagområde get() = head.fagområde()
    private val sisteUtbetalte get() = oppdragsliste.first { it.erUtbetalt() }
    private var tilstand: Tilstand = Ny

    private var observer: FagsystemIdObserver = object : FagsystemIdObserver {}

    init {
        require(oppdragsliste.isNotEmpty())
    }

    internal fun fagsystemId() = fagsystemId
    internal fun nettoBeløp() = head.nettoBeløp()

    internal fun register(fagsystemIdObserver: FagsystemIdObserver) {
        observer = fagsystemIdObserver
    }

    internal fun accept(visitor: FagsystemIdVisitor) {
        visitor.preVisitFagsystemId(this, fagsystemId, fagområde)
        visitor.preVisitOppdragsliste(oppdragsliste)
        oppdragsliste.onEach { it.accept(visitor) }
        visitor.postVisitOppdragsliste(oppdragsliste)
        visitor.postVisitFagsystemId(this, fagsystemId, fagområde)
    }

    internal fun håndter(hendelse: Utbetalingsgodkjenning, maksdato: LocalDate) {
        tilstand.utbetal(this, hendelse, hendelse.saksbehandlerEpost(), hendelse.saksbehandler(), hendelse.godkjenttidspunkt(), maksdato)
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
        Aktivitetslogg.Aktivitet.Behov.simulering(
            aktivitetslogg = aktivitetslogg,
            oppdrag = head.removeUEND(),
            maksdato = maksdato,
            saksbehandler = saksbehandler
        )
    }

    internal fun erTom() = oppdragsliste.isEmpty()

    internal fun erAnnullert() = head.erUtbetalt() && head.linjerUtenOpphør().isEmpty()

    private fun annuller(
        aktivitetslogg: IAktivitetslogg,
        saksbehandler: String,
        saksbehandlerEpost: String,
        godkjenttidspunkt: LocalDateTime
    ) {
        fjernUbetalte()
        val oppdrag = head.annullere(this, aktivitetslogg, saksbehandler, saksbehandlerEpost, godkjenttidspunkt)
        oppdragsliste.add(0, oppdrag)
    }

    private fun utbetal(
        aktivitetslogg: IAktivitetslogg,
        maksdato: LocalDate,
        saksbehandler: String,
        saksbehandlerEpost: String,
        godkjenttidspunkt: LocalDateTime
    ) {
        head.utbetal(this, aktivitetslogg, maksdato, saksbehandler, saksbehandlerEpost, godkjenttidspunkt)
    }

    private fun kvittér(hendelse: UtbetalingHendelse) {
        head.håndter(this, hendelse)
    }

    private fun fjernUbetalte() {
        oppdragsliste.removeIf { it.erUbetalt() }
    }

    private fun erUtbetalt() = oppdragsliste.any { it.erUtbetalt() }

    private fun kobleTil(opprinnelig: Oppdrag, aktivitetslogg: IAktivitetslogg): Boolean {
        if (opprinnelig.fagområde() != fagområde) return false
        val oppdrag = opprinnelig.minus(head, aktivitetslogg)
        if (oppdrag.fagsystemId() != fagsystemId) return false
        tilstand.kobleTil(this, oppdrag)
        return true
    }

    private fun kobleTil(nytt: Oppdrag) {
        nytt.nettoBeløp(head)
        oppdragsliste.add(0, nytt)
    }

    internal companion object {
        internal fun kobleTil(fagsystemIder: MutableList<FagsystemId>, oppdrag: Oppdrag, aktivitetslogg: IAktivitetslogg): FagsystemId =
            fagsystemIder.firstOrNull { it.kobleTil(oppdrag, aktivitetslogg) }
                ?: FagsystemId(listOf(oppdrag)).also {
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

    private interface Tilstand {
        fun utbetal(
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

        fun kobleTil(fagsystemId: FagsystemId, oppdrag: Oppdrag) {
            throw IllegalStateException("Kan ikke legge til nytt oppdrag fagsystemId=${fagsystemId.fagsystemId} i tilstand=${this::class.simpleName}")
        }

        fun entering(fagsystemId: FagsystemId) {}
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
            fagsystemId.utbetal(hendelse, maksdato, ident, epost, godkjenttidspunkt)
            fagsystemId.tilstand(UtbetalingOverført)
        }
    }

    private object Aktiv: Tilstand {
        override fun entering(fagsystemId: FagsystemId) {
            fagsystemId.fjernUbetalte()
        }

        override fun kobleTil(fagsystemId: FagsystemId, oppdrag: Oppdrag) {
            fagsystemId.kobleTil(oppdrag)
            fagsystemId.tilstand(Ubetalt)
        }

        override fun annuller(
            fagsystemId: FagsystemId,
            hendelse: AnnullerUtbetaling,
            epost: String,
            ident: String,
            godkjenttidspunkt: LocalDateTime
        ) {
            fagsystemId.annuller(hendelse, ident, epost, godkjenttidspunkt)
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
            if (hendelse.valider().hasErrorsOrWorse()) return fagsystemId.tilstand(Aktiv)
            fagsystemId.utbetal(hendelse, maksdato, ident, epost, godkjenttidspunkt)
            fagsystemId.tilstand(UtbetalingOverført)
        }

        override fun annuller(
            fagsystemId: FagsystemId,
            hendelse: AnnullerUtbetaling,
            epost: String,
            ident: String,
            godkjenttidspunkt: LocalDateTime
        ) {
            fagsystemId.annuller(hendelse, ident, epost, godkjenttidspunkt)
            fagsystemId.tilstand(AnnulleringOverført)
        }
    }

    private object UtbetalingOverført: Tilstand {
        override fun kvittér(fagsystemId: FagsystemId, hendelse: UtbetalingHendelse) {
            fagsystemId.kvittér(hendelse)
            if (hendelse.valider().hasErrorsOrWorse()) return fagsystemId.tilstand(Avvist)
            fagsystemId.tilstand(Aktiv)
        }
    }

    private object AnnulleringOverført: Tilstand {
        override fun kvittér(fagsystemId: FagsystemId, hendelse: UtbetalingHendelse) {
            fagsystemId.kvittér(hendelse)
            if (hendelse.valider().hasErrorsOrWorse()) return fagsystemId.tilstand(Avvist)
            fagsystemId.tilstand(Annullert)
        }
    }

    private object Avvist: Tilstand {
        override fun entering(fagsystemId: FagsystemId) {
            fagsystemId.fjernUbetalte()
        }
    }

    private object Annullert: Tilstand
}
