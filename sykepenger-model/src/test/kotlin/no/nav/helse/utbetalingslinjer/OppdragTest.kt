package no.nav.helse.utbetalingslinjer

import no.nav.helse.hendelser.UtbetalingHendelse
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class OppdragTest {

    private companion object {
        private val FAGOMRÅDE = Fagområde.SykepengerRefusjon
        private const val FNR = "12345678910"
        private const val AKTØRID = "1234567891011"
        private const val ORGNUMMER = "123456789"
        private const val SAKSBEHANDLER = "EN SAKSBEHANDLER"
        private const val SAKSBEHANDLEREPOST = "saksbehandler@saksbehandlersen.no"
        private val MAKSDATO = LocalDate.now()
        private val GODKJENTTIDSPUNKT = LocalDateTime.now()
    }

    private lateinit var fagsystemId: FagsystemId
    private lateinit var aktivitetslogg: IAktivitetslogg

    @BeforeEach
    fun beforeEach() {
        aktivitetslogg = Aktivitetslogg()
    }

    @Test
    fun `sorterer oppdrag basert på tidsstempel`() {
        val oppdrag1 = Oppdrag(ORGNUMMER, FAGOMRÅDE)
        val oppdrag2 = Oppdrag(ORGNUMMER, FAGOMRÅDE)
        val oppdrag3 = Oppdrag(ORGNUMMER, FAGOMRÅDE)
        val oppdrag = listOf(oppdrag2 to Utbetalingstidslinje(), oppdrag3 to Utbetalingstidslinje(), oppdrag1 to Utbetalingstidslinje())
        val sorterteOppdrag = Oppdrag.sorter(oppdrag)

        assertEquals(oppdrag3, sorterteOppdrag[0].first)
        assertEquals(oppdrag2, sorterteOppdrag[1].first)
        assertEquals(oppdrag1, sorterteOppdrag[2].first)
    }

    @Test
    fun `håndtere utbetaling uten at oppdraget er overført`() {
        val oppdrag = oppdrag()
        assertThrows<IllegalStateException> { oppdrag.håndter(fagsystemId, utbetalinghendelse()) }
    }

    @Test
    fun `overføre et overført oppdrag`() {
        val oppdrag = oppdrag()
        oppdrag.utbetal(fagsystemId, aktivitetslogg, MAKSDATO, SAKSBEHANDLER, SAKSBEHANDLEREPOST, GODKJENTTIDSPUNKT)
        assertThrows<IllegalStateException> { oppdrag.utbetal(fagsystemId, aktivitetslogg, MAKSDATO, SAKSBEHANDLER, SAKSBEHANDLEREPOST, GODKJENTTIDSPUNKT) }
    }

    @Test
    fun `utbetale et utbetalt oppdrag`() {
        val oppdrag = oppdrag()
        oppdrag.utbetal(fagsystemId, aktivitetslogg, MAKSDATO, SAKSBEHANDLER, SAKSBEHANDLEREPOST, GODKJENTTIDSPUNKT)
        oppdrag.håndter(fagsystemId, utbetalinghendelse())
        assertThrows<IllegalStateException> { oppdrag.utbetal(fagsystemId, aktivitetslogg, MAKSDATO, SAKSBEHANDLER, SAKSBEHANDLEREPOST, GODKJENTTIDSPUNKT) }
        assertThrows<IllegalStateException> { oppdrag.håndter(fagsystemId, utbetalinghendelse()) }
    }

    private fun oppdrag() = Oppdrag(ORGNUMMER, FAGOMRÅDE).also {
        fagsystemId = FagsystemId.kobleTil(mutableListOf(), it, Utbetalingstidslinje(), Aktivitetslogg())
    }

    private fun utbetalinghendelse() = UtbetalingHendelse(
        UUID.randomUUID(),
        UUID.randomUUID().toString(),
        "AKTØRID",
        "FØDSELSNUMMER",
        "ORGNR",
        "fagsystem id",
        UtbetalingHendelse.Oppdragstatus.AKSEPTERT,
        "",
        LocalDateTime.now(),
        "EN SAKSBEHANDLER",
        "saksbehandler@saksbehandlersen.no",
        false
    )

}
