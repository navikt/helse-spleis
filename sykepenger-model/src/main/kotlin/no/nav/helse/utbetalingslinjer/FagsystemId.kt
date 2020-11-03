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

    init {
        require(oppdragsliste.isNotEmpty())
    }

    internal fun fagsystemId() = fagsystemId
    internal fun nettoBeløp() = head.nettoBeløp()

    internal fun accept(visitor: FagsystemIdVisitor) {
        visitor.preVisitFagsystemId(this, fagsystemId, fagområde)
        visitor.preVisitOppdragsliste(oppdragsliste)
        oppdragsliste.onEach { it.accept(visitor) }
        visitor.postVisitOppdragsliste(oppdragsliste)
        visitor.postVisitFagsystemId(this, fagsystemId, fagområde)
    }

    internal fun håndter(utbetalingHendelse: UtbetalingHendelse): Boolean {
        if (!utbetalingHendelse.erRelevant(fagsystemId)) return false
        head.håndter(this, utbetalingHendelse)
        return true
    }

    internal fun håndter(annulleringHendelse: AnnullerUtbetaling, maksdato: LocalDate): Boolean {
        if (!annulleringHendelse.erRelevant(fagsystemId)) return false
        annuller(annulleringHendelse, maksdato, annulleringHendelse.saksbehandlerIdent, annulleringHendelse.saksbehandlerEpost, annulleringHendelse.opprettet)
        return true
    }

    internal fun håndter(utbetalingsgodkjenning: Utbetalingsgodkjenning, maksdato: LocalDate) {
        if (utbetalingsgodkjenning.valider().hasErrorsOrWorse()) return fjernUbetalte()
        utbetal(
            utbetalingsgodkjenning,
            maksdato,
            utbetalingsgodkjenning.saksbehandler(),
            utbetalingsgodkjenning.saksbehandlerEpost(),
            utbetalingsgodkjenning.godkjenttidspunkt()
        )
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

    private fun annuller(aktivitetslogg: IAktivitetslogg, maksdato: LocalDate, saksbehandler: String, saksbehandlerEpost: String, godkjenttidspunkt: LocalDateTime) {
        check(erUtbetalt()) { "kan ikke annullere en fagsystemId uten utbetalinger" }
        val oppdrag = sisteUtbetalte.annullere(this)
        oppdragsliste.add(0, oppdrag)
        utbetal(aktivitetslogg, maksdato, saksbehandler, saksbehandlerEpost, godkjenttidspunkt, true)
    }

    private fun utbetal(aktivitetslogg: IAktivitetslogg, maksdato: LocalDate, saksbehandler: String, saksbehandlerEpost: String, godkjenttidspunkt: LocalDateTime) {
        check(!erAnnullert()) { "kan ikke utbetale på en annullert fagsystemId" }
        utbetal(aktivitetslogg, maksdato, saksbehandler, saksbehandlerEpost, godkjenttidspunkt, false)
    }

    private fun utbetal(aktivitetslogg: IAktivitetslogg, maksdato: LocalDate, saksbehandler: String, saksbehandlerEpost: String, godkjenttidspunkt: LocalDateTime, erAnnullering: Boolean) {
        head.utbetal(this)
        Aktivitetslogg.Aktivitet.Behov.sendUtbetalingsbehov(
            aktivitetslogg = aktivitetslogg,
            oppdrag = head.removeUEND(),
            maksdato = maksdato,
            saksbehandler = saksbehandler,
            saksbehandlerEpost = saksbehandlerEpost,
            godkjenttidspunkt = godkjenttidspunkt,
            annullering = erAnnullering
        )
    }

    private fun fjernUbetalte() {
        oppdragsliste.removeIf { !it.erUtbetalt() }
    }

    private fun erUtbetalt() = oppdragsliste.any { it.erUtbetalt() }

    private fun kobleTil(opprinnelig: Oppdrag, aktivitetslogg: IAktivitetslogg): Boolean {
        if (opprinnelig.fagområde() != fagområde) return false
        val oppdrag = opprinnelig.minus(head, aktivitetslogg)
        if (oppdrag.fagsystemId() != fagsystemId) return false
        if (!head.erUtbetalt()) throw IllegalStateException("Kan ikke utbetale en fagsystemId når tidligere oppdrag ikke er utbetalt!")
        oppdrag.nettoBeløp(head)
        oppdragsliste.add(0, oppdrag)
        return true
    }

    internal companion object {
        internal fun kobleTil(fagsystemIder: MutableList<FagsystemId>, oppdrag: Oppdrag, aktivitetslogg: IAktivitetslogg): FagsystemId =
            fagsystemIder.firstOrNull { it.kobleTil(oppdrag, aktivitetslogg) }
                ?: FagsystemId(listOf(oppdrag)).also { fagsystemIder.add(it) }
    }
}
