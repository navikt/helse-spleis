package no.nav.helse.spleis.e2e

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.januar
import no.nav.syfo.kafka.felles.SoknadsperiodeDTO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ForkastetVedtaksperiodeTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `vedtaksperiode_forkastet`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        val søknadId = sendSøknad(listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)))
        val søknadId2 = sendSøknad(listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 27.januar, sykmeldingsgrad = 100)))

        val vedtaksperiodeForkastet = testRapid.inspektør.siste("vedtaksperiode_forkastet")

        assertEquals(
            "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK",
            vedtaksperiodeForkastet.path("tilstand").asText()
        )
        assertEquals(3.januar, LocalDate.parse(vedtaksperiodeForkastet.path("fom").asText()))
        assertEquals(26.januar, LocalDate.parse(vedtaksperiodeForkastet.path("tom").asText()))
        assertEquals(
            listOf(søknadId, søknadId2),
            vedtaksperiodeForkastet.path("hendelser").map { UUID.fromString(it.asText()) })
    }
}
