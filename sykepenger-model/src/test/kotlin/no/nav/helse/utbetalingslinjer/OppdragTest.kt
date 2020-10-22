package no.nav.helse.utbetalingslinjer

import no.nav.helse.hendelser.UtbetalingHendelse
import no.nav.helse.person.Aktivitetslogg
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.util.*

internal class OppdragTest {

    private companion object {
        private const val ORGNUMMER = "123456789"
        private val FAGOMRÅDE = Fagområde.SykepengerRefusjon
    }

    private lateinit var fagsystemId: FagsystemId

    @Test
    fun `sorterer oppdrag basert på tidsstempel`() {
        val oppdrag1 = Oppdrag(ORGNUMMER, FAGOMRÅDE)
        val oppdrag2 = Oppdrag(ORGNUMMER, FAGOMRÅDE)
        val oppdrag3 = Oppdrag(ORGNUMMER, FAGOMRÅDE)
        val oppdrag = listOf(oppdrag2, oppdrag3, oppdrag1)
        val sorterteOppdrag = Oppdrag.sorter(oppdrag)

        assertEquals(oppdrag3, sorterteOppdrag[0])
        assertEquals(oppdrag2, sorterteOppdrag[1])
        assertEquals(oppdrag1, sorterteOppdrag[2])
    }

    @Test
    fun `håndtere utbetaling uten at oppdraget er overført`() {
        val oppdrag = oppdrag()
        assertThrows<IllegalStateException> { oppdrag.håndter(fagsystemId, utbetalinghendelse()) }
    }

    @Test
    fun `overføre et overført oppdrag`() {
        val oppdrag = oppdrag()
        oppdrag.utbetal(fagsystemId)
        assertThrows<IllegalStateException> { oppdrag.utbetal(fagsystemId) }
    }

    @Test
    fun `utbetale et utbetalt oppdrag`() {
        val oppdrag = oppdrag()
        oppdrag.utbetal(fagsystemId)
        oppdrag.håndter(fagsystemId, utbetalinghendelse())
        assertThrows<IllegalStateException> { oppdrag.utbetal(fagsystemId) }
        assertThrows<IllegalStateException> { oppdrag.håndter(fagsystemId, utbetalinghendelse()) }
    }

    private fun oppdrag() = Oppdrag(ORGNUMMER, FAGOMRÅDE).also {
        fagsystemId = FagsystemId.kobleTil(mutableListOf(), it, Aktivitetslogg())
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
