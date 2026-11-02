package no.nav.helse.spleis.jobs

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.jacksonObjectMapper
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

        assertEquals("spleis_behandlingskobling", node.path("@event_name").asString())
        assertEquals("12345678910", node.path("fødselsnummer").asString())
        assertEquals(true, node.path("dryrun").asBoolean())
        val vedtaksperiode = node.path("vedtaksperioder").values().single()
        assertEquals(vedtaksperiodeId.toString(), vedtaksperiode.path("vedtaksperiodeId").asString())
        val behandling = vedtaksperiode.path("behandlinger").values().single()
        assertEquals(behandlingId.toString(), behandling.path("behandlingId").asString())
        assertEquals("2024-01-01T12:00:00", behandling.path("opprettet").asString())
        assertEquals(
            utbetalingId.toString(),
            behandling
                .path("utbetalinger")
                .values()
                .single()
                .asString(),
        )
    }
}
