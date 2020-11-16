package no.nav.helse.utbetalingslinjer

internal interface FagsystemIdObserver {

    fun tilstandEndret(fagsystemId: FagsystemId, gammel: String, ny: String) {}
    fun utbetalt() {}
    fun annullert() {}
    fun kvittert() {}
    fun overført() {}
}
