package no.nav.helse.spleis.jobs

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class BehandlingskoblingTest {
    private val vedtaksperiodeId = UUID.randomUUID()
    private val behandlingId = UUID.randomUUID()
    private val tidspunkt = LocalDateTime.of(2024, 1, 1, 12, 0)

    @Test
    fun `opprettet er tidsstempelet til første endring`() {
        val koblet = kobletBehandling(behandlingId, listOf(tidspunkt to null, tidspunkt.plusDays(1) to null))

        assertEquals(KobletBehandling(behandlingId, tidspunkt, emptyList()), koblet)
    }

    @Test
    fun `tar med alle utbetalinger uten duplikater`() {
        val utbetaling1 = UUID.randomUUID()
        val utbetaling2 = UUID.randomUUID()

        val koblet =
            kobletBehandling(
                behandlingId,
                listOf(
                    tidspunkt to null,
                    tidspunkt.plusHours(1) to utbetaling1,
                    tidspunkt.plusHours(2) to utbetaling1,
                    tidspunkt.plusHours(3) to utbetaling2,
                ),
            )

        assertEquals(listOf(utbetaling1, utbetaling2), koblet.utbetalinger)
    }

    @Test
    fun json() {
        val utbetalingId = UUID.randomUUID()
        val melding =
            Behandlingskobling(
                fødselsnummer = "12345678910",
                dryrun = true,
                vedtaksperioder =
                    listOf(
                        KobletVedtaksperiode(
                            vedtaksperiodeId,
                            listOf(KobletBehandling(behandlingId, tidspunkt, listOf(utbetalingId))),
                        ),
                    ),
            )

        val node = jacksonObjectMapper().readTree(melding.tilJson())

        assertEquals("spleis_behandlingskobling", node.path("@event_name").asText())
        assertEquals("12345678910", node.path("fødselsnummer").asText())
        assertEquals(true, node.path("dryrun").asBoolean())
        val vedtaksperiode = node.path("vedtaksperioder").single()
        assertEquals(vedtaksperiodeId.toString(), vedtaksperiode.path("vedtaksperiodeId").asText())
        val behandling = vedtaksperiode.path("behandlinger").single()
        assertEquals(behandlingId.toString(), behandling.path("behandlingId").asText())
        assertEquals("2024-01-01T12:00:00", behandling.path("opprettet").asText())
        assertEquals(utbetalingId.toString(), behandling.path("utbetalinger").single().asText())
    }
}
