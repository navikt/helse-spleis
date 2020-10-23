package no.nav.helse.utbetalingslinjer

import no.nav.helse.hendelser.UtbetalingHendelse
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.FagsystemIdVisitor

internal class FagsystemId private constructor(oppdragsliste: List<Oppdrag>) {

    private val oppdragsliste = Oppdrag.sorter(oppdragsliste).toMutableList()
    private val head get() = oppdragsliste.first()
    private val fagsystemId get() = head.fagsystemId()
    private val sisteUtbetalte get() = oppdragsliste.first { it.erUtbetalt() }

    init {
        require(oppdragsliste.isNotEmpty())
    }

    internal fun accept(visitor: FagsystemIdVisitor) {
        visitor.preVisitFagsystemId(this, fagsystemId)
        visitor.preVisitOppdragsliste(oppdragsliste)
        oppdragsliste.onEach { it.accept(visitor) }
        visitor.postVisitOppdragsliste(oppdragsliste)
        visitor.postVisitFagsystemId(this, fagsystemId)
    }

    internal fun annullere() {
        check(erUtbetalt()) { "kan ikke annullere en fagsystemId uten utbetalinger" }
        val oppdrag = sisteUtbetalte.annullere(this)
        oppdragsliste.add(0, oppdrag)
        utbetal()
    }

    internal fun utbetal() {
        check(!erAnnullert()) { "kan ikke utbetale på en annullert fagsystemId" }
        head.utbetal(this)
    }

    internal fun håndter(utbetalingHendelse: UtbetalingHendelse): Boolean {
        if (!utbetalingHendelse.erRelevant(fagsystemId)) return false
        head.håndter(this, utbetalingHendelse)
        return true
    }

    internal fun erAnnullert() = head.erUtbetalt() && head.linjerUtenOpphør().isEmpty()

    private fun erUtbetalt() = oppdragsliste.any { it.erUtbetalt() }

    private fun kobleTil(opprinnelig: Oppdrag, aktivitetslogg: Aktivitetslogg): Boolean {
        val oppdrag = opprinnelig.minus(head, aktivitetslogg)
        if (oppdrag.fagsystemId() != fagsystemId) return false
        if (!head.erUtbetalt()) throw IllegalStateException("Kan ikke utbetale en fagsystemId når tidligere oppdrag ikke er utbetalt!")
        oppdrag.nettoBeløp(head)
        oppdragsliste.add(0, oppdrag)
        return true
    }

    internal companion object {
        internal fun kobleTil(fagsystemIder: MutableList<FagsystemId>, oppdrag: Oppdrag, aktivitetslogg: Aktivitetslogg): FagsystemId =
            fagsystemIder.firstOrNull { it.kobleTil(oppdrag, aktivitetslogg) }
                ?: FagsystemId(listOf(oppdrag)).also { fagsystemIder.add(it) }
    }
}
