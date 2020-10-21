package no.nav.helse.person

import no.nav.helse.utbetalingslinjer.Oppdrag

internal class FagsystemId private constructor(
    private val oppdragsliste: MutableList<Oppdrag>
) {
    private constructor(oppdrag: Oppdrag): this(mutableListOf(oppdrag))

    private val siste get() = oppdragsliste.last()

    init {
        require(oppdragsliste.isNotEmpty())
    }

    private fun håndter(opprinnelig: Oppdrag, aktivitetslogg: Aktivitetslogg): Boolean {
        val oppdrag = opprinnelig.minus(siste, aktivitetslogg)
        if (oppdrag.fagsystemId() != siste.fagsystemId()) return false
        oppdrag.nettoBeløp(siste)
        return oppdragsliste.add(oppdrag)
    }

    companion object {
        internal fun kobleTil(fagsystemIder: MutableList<FagsystemId>, oppdrag: Oppdrag, aktivitetslogg: Aktivitetslogg): Oppdrag =
            fagsystemIder.firstOrNull { it.håndter(oppdrag, aktivitetslogg) }?.siste
                ?: oppdrag.also { fagsystemIder.add(FagsystemId(it)) }
    }
}
