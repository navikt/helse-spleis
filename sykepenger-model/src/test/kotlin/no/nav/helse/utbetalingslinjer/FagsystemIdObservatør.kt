package no.nav.helse.utbetalingslinjer

import org.junit.jupiter.api.Assertions.fail

internal class FagsystemIdObservatør : FagsystemIdObserver {
    private val tilstander = mutableMapOf<FagsystemId, MutableList<String>>()

    fun tilstander(fagsystemId: FagsystemId) = tilstander[fagsystemId] ?: fail { "Finner ikke fagsystem" }

    override fun tilstandEndret(fagsystemId: FagsystemId, gammel: String, ny: String) {
        tilstander.getOrPut(fagsystemId) { mutableListOf(gammel) }.add(ny)
    }
}
