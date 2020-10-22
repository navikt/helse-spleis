package no.nav.helse.utbetalingslinjer

import no.nav.helse.hendelser.UtbetalingHendelse
import no.nav.helse.person.Aktivitetslogg

internal class FagsystemId private constructor(oppdragsliste: List<Oppdrag>) {

    private val oppdragsliste = Oppdrag.sorter(oppdragsliste).toMutableList()
    private val head get() = oppdragsliste.first()
    private val fagsystemId get() = head.fagsystemId()

    init {
        require(oppdragsliste.isNotEmpty())
    }

    internal fun utbetal() {
        head.utbetal(this)
    }

    internal fun håndter(utbetalingHendelse: UtbetalingHendelse): Boolean {
        if (!utbetalingHendelse.erRelevant(fagsystemId)) return false
        head.håndter(this, utbetalingHendelse)
        return true
    }

    private fun kobleTil(opprinnelig: Oppdrag, aktivitetslogg: Aktivitetslogg): Boolean {
        val oppdrag = opprinnelig.minus(head, aktivitetslogg)
        if (oppdrag.fagsystemId() != fagsystemId) return false
        if (!head.erUtbetalt()) throw IllegalStateException("Kan ikke utbetale en fagsystemId når tidligere oppdrag ikke er utbetalt!")
        oppdrag.nettoBeløp(head)
        oppdragsliste.add(0, oppdrag)
        return true
    }

    companion object {
        internal fun kobleTil(fagsystemIder: MutableList<FagsystemId>, oppdrag: Oppdrag, aktivitetslogg: Aktivitetslogg): FagsystemId =
            fagsystemIder.firstOrNull { it.kobleTil(oppdrag, aktivitetslogg) }
                ?: FagsystemId(listOf(oppdrag)).also { fagsystemIder.add(it) }
    }
}
