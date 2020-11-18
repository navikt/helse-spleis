package no.nav.helse.utbetalingslinjer

import no.nav.helse.person.FagsystemIdVisitor
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import org.junit.jupiter.api.Assertions.fail

internal class FagsystemIdInspektør(fagsystemIder: List<FagsystemId>) : FagsystemIdVisitor {
    private val fagsystemIder = mutableListOf<String>()
    private val utbetalingstidslinjer = mutableMapOf<Int, Utbetalingstidslinje>()
    private val utbetalteTidslinjer = mutableMapOf<Int, Utbetalingstidslinje>()
    private val tilstander = mutableMapOf<Int, String>()
    private var fagsystemIdTeller = 0

    init {
        fagsystemIder.onEach { it.accept(this) }
    }

    fun fagsystemId(fagsystemIndeks: Int) = fagsystemIder[fagsystemIndeks]
    fun tilstand(fagsystemIndeks: Int) = tilstander[fagsystemIndeks] ?: fail { "Finner ikke tilstand for fagsystem med indeks $fagsystemIndeks" }
    fun utbetalingstidslinje(fagsystemIndeks: Int) = utbetalingstidslinjer[fagsystemIndeks] ?: fail { "Finner ikke utbetalingstidslinje for fagsystem med indeks $fagsystemIndeks" }
    fun utbetaltTidslinje(fagsystemIndeks: Int) = utbetalteTidslinjer[fagsystemIndeks] ?: fail { "Finner ikke utbetalingstidslinje for fagsystem med indeks $fagsystemIndeks" }

    override fun preVisitFagsystemId(fagsystemId: FagsystemId, id: String, fagområde: Fagområde, tilstand: String, utbetalingstidslinje: Utbetalingstidslinje, utbetaltTidslinje: Utbetalingstidslinje) {
        fagsystemIder.add(fagsystemIdTeller, id)
        utbetalingstidslinjer[fagsystemIdTeller] = utbetalingstidslinje
        utbetalteTidslinjer[fagsystemIdTeller] = utbetaltTidslinje
        tilstander[fagsystemIdTeller] = tilstand
    }

    override fun postVisitFagsystemId(fagsystemId: FagsystemId, id: String, fagområde: Fagområde, tilstand: String, utbetalingstidslinje: Utbetalingstidslinje, utbetaltTidslinje: Utbetalingstidslinje) {
        fagsystemIdTeller += 1
    }
}
