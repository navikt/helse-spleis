package no.nav.helse.utbetalingslinjer

import no.nav.helse.person.FagsystemIdVisitor
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import org.junit.jupiter.api.Assertions.fail
import java.time.LocalDateTime

internal class FagsystemIdInspektør(fagsystemIder: List<FagsystemId>) : FagsystemIdVisitor {
    private val fagsystemIder = mutableListOf<String>()
    private val oppdragstilstander = mutableMapOf<Int, MutableList<Oppdrag.Utbetalingtilstand>>()
    private val utbetalingstidslinjer = mutableMapOf<Int, Utbetalingstidslinje>()
    private var fagsystemIdTeller = 0
    private var oppdragteller = 0

    init {
        fagsystemIder.onEach { it.accept(this) }
    }

    fun oppdragtilstander(fagsystemIndeks: Int) = oppdragstilstander[fagsystemIndeks] ?: fail {
        "Finner ikke fagsystem med indeks $fagsystemIndeks"
    }

    fun oppdragtilstand(fagsystemIndeks: Int, oppdragIndeks: Int) =
        oppdragstilstander[fagsystemIndeks]?.get(oppdragIndeks) ?: fail { "Finner ikke fagsystem med indeks $fagsystemIndeks eller oppdrag med indeks $oppdragIndeks" }

    fun utbetalingstidslinje(fagsystemIndeks: Int) = utbetalingstidslinjer[fagsystemIndeks] ?: fail { "Finner ikke utbetalingstidslinje for fagsystem med indeks $fagsystemIndeks" }

    override fun preVisitFagsystemId(fagsystemId: FagsystemId, id: String, fagområde: Fagområde, utbetalingstidslinje: Utbetalingstidslinje) {
        fagsystemIder.add(fagsystemIdTeller, id)
        utbetalingstidslinjer[fagsystemIdTeller] = utbetalingstidslinje
    }

    override fun preVisitOppdragsliste() {
        oppdragteller = 0
    }

    override fun preVisitOppdrag(
        oppdrag: Oppdrag,
        totalBeløp: Int,
        nettoBeløp: Int,
        tidsstempel: LocalDateTime,
        utbetalingtilstand: Oppdrag.Utbetalingtilstand
    ) {
        oppdragstilstander.getOrPut(fagsystemIdTeller) { mutableListOf() }
            .add(0, utbetalingtilstand)
    }

    override fun postVisitOppdrag(
        oppdrag: Oppdrag,
        totalBeløp: Int,
        nettoBeløp: Int,
        tidsstempel: LocalDateTime,
        utbetalingtilstand: Oppdrag.Utbetalingtilstand
    ) {
        oppdragteller += 1
    }

    override fun postVisitFagsystemId(fagsystemId: FagsystemId, id: String, fagområde: Fagområde, utbetalingstidslinje: Utbetalingstidslinje) {
        fagsystemIdTeller += 1
    }
}
