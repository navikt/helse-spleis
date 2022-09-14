package no.nav.helse.spleis.e2e

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsperiodeDTO
import no.nav.helse.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ForkastetVedtaksperiodeTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `vedtaksperiode_forkastet`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        val søknadId = sendSøknad(listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)))
        val søknadId2 = sendSøknad(listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 27.januar, sykmeldingsgrad = 100)))

        assertTilstander(0, "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK", "TIL_INFOTRYGD")
        assertTilstander(1, "TIL_INFOTRYGD")

        val vedtaksperiodeForkastet = testRapid.inspektør.meldinger("vedtaksperiode_forkastet")
        assertEquals(2, vedtaksperiodeForkastet.size)
        assertEquals(3.januar, LocalDate.parse(vedtaksperiodeForkastet.first().path("fom").asText()))
        assertEquals(26.januar, LocalDate.parse(vedtaksperiodeForkastet.first().path("tom").asText()))
        assertEquals(3.januar, LocalDate.parse(vedtaksperiodeForkastet.last().path("fom").asText()))
        assertEquals(27.januar, LocalDate.parse(vedtaksperiodeForkastet.last().path("tom").asText()))
        assertEquals(
            listOf(søknadId, søknadId2),
            vedtaksperiodeForkastet.flatMap { it.path("hendelser").map { UUID.fromString(it.asText()) } }.distinct()
        )
    }
}
