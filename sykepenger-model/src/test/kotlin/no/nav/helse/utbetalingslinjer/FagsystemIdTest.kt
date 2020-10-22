package no.nav.helse.utbetalingslinjer

import no.nav.helse.hendelser.UtbetalingHendelse
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingstidslinje.MaksimumUtbetaling
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class FagsystemIdTest {

    companion object {
        private const val ORGNUMMER = "123456789"
    }

    private val fagsystemIder: MutableList<FagsystemId> = mutableListOf()
    private val oppdrag: MutableMap<Oppdrag, FagsystemId> = mutableMapOf()
    private lateinit var aktivitetslogg: Aktivitetslogg

    @BeforeEach
    fun beforeEach() {
        fagsystemIder.clear()
        oppdrag.clear()
        aktivitetslogg = Aktivitetslogg()
    }

    @Test
    fun `Ny fagsystemId`() {
        opprett(1.NAV)
        assertEquals(1, fagsystemIder.size)
    }

    @Test
    fun `Nytt element når fagsystemId'er er forskjellige`() {
        opprett(1.NAV)
        opprett(1.NAV, startdato = 17.januar)
        assertEquals(2, fagsystemIder.size)
    }

    @Test
    fun `legger nytt oppdrag til på eksisterende fagsystemId`() {
        opprettOgUtbetal(16.AP, 5.NAV)
        opprett(5.NAV(1300))
        assertEquals(1, fagsystemIder.size)
    }

    @Test
    fun `kan kun ha ett oppdrag som ikke er utbetalt`() {
        opprett(16.AP, 5.NAV)
        assertThrows<IllegalStateException> {
            opprett(5.NAV(1300))
        }
    }

    @Test
    fun `mapper riktig når det finnes flere fagsystemId'er`() {
        val oppdrag1 = opprettOgUtbetal(16.AP, 5.NAV)
        val oppdrag2 = opprettOgUtbetal(16.AP, 5.NAV, startdato = 1.mars)
        val oppdrag1Oppdatert = opprett(16.AP, 5.NAV(1300))
        assertEquals(2, fagsystemIder.size)
        assertEquals(oppdrag1.fagsystemId(), oppdrag1Oppdatert.fagsystemId())
        assertNotEquals(oppdrag2.fagsystemId(), oppdrag1Oppdatert.fagsystemId())
    }

    private fun opprettOgUtbetal(vararg dager: Utbetalingsdager, startdato: LocalDate = 1.januar, sisteDato: LocalDate? = null) =
        opprett(*dager, startdato = startdato, sisteDato = sisteDato).also {
            val fagsystemId = oppdrag.getValue(it)
            fagsystemId.utbetal()
            fagsystemId.håndter(utbetalingHendelse(it))
        }

    private fun opprett(vararg dager: Utbetalingsdager, startdato: LocalDate = 1.januar, sisteDato: LocalDate? = null): Oppdrag {
        val tidslinje = tidslinjeOf(*dager, startDato = startdato)
        MaksimumUtbetaling(
            listOf(tidslinje),
            Aktivitetslogg(),
            listOf(1.januar),
            1.januar
        ).betal().let {
            return OppdragBuilder(
                tidslinje,
                ORGNUMMER,
                Fagområde.SykepengerRefusjon,
                sisteDato ?: tidslinje.sisteDato()
            ).result().also {
                oppdrag[it] = FagsystemId.kobleTil(fagsystemIder, it, aktivitetslogg)
            }
        }
    }

    private fun utbetalingHendelse(oppdrag: Oppdrag) = UtbetalingHendelse(
        UUID.randomUUID(),
        UUID.randomUUID().toString(),
        "AKTØRID",
        "FØDSELSNUMMER",
        ORGNUMMER,
        oppdrag.fagsystemId(),
        UtbetalingHendelse.Oppdragstatus.AKSEPTERT,
        "",
        LocalDateTime.now(),
        "EN SAKSBEHANDLER",
        "saksbehandler@saksbehandlersen.no",
        false
    )
}
