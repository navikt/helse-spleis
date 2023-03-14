package no.nav.helse.spleis.e2e

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsperiodeDTO
import no.nav.helse.januar
import no.nav.helse.mars
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class ForkastetVedtaksperiodeTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `vedtaksperiode_forkastet`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        val søknadId = sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        )
        val søknadId2 = sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 27.januar, sykmeldingsgrad = 100))
        )

        assertTilstander(0, "AVVENTER_INFOTRYGDHISTORIKK", "AVVENTER_INNTEKTSMELDING", "TIL_INFOTRYGD")
        assertTilstander(1, "TIL_INFOTRYGD")

        val vedtaksperiodeForkastet = testRapid.inspektør.meldinger("vedtaksperiode_forkastet")
        assertEquals(2, vedtaksperiodeForkastet.size)
        assertEquals(3.januar, LocalDate.parse(vedtaksperiodeForkastet.first().path("fom").asText()))
        assertEquals(26.januar, LocalDate.parse(vedtaksperiodeForkastet.first().path("tom").asText()))
        assertTrue(vedtaksperiodeForkastet.first().path("forlengerPeriode").isBoolean)
        assertTrue(vedtaksperiodeForkastet.first().path("harPeriodeInnenfor16Dager").isBoolean)
        assertEquals(3.januar, LocalDate.parse(vedtaksperiodeForkastet.last().path("fom").asText()))
        assertEquals(27.januar, LocalDate.parse(vedtaksperiodeForkastet.last().path("tom").asText()))
        assertTrue(vedtaksperiodeForkastet.last().path("forlengerPeriode").isBoolean)
        assertTrue(vedtaksperiodeForkastet.last().path("harPeriodeInnenfor16Dager").isBoolean)
        assertEquals(
            listOf(søknadId, søknadId2),
            vedtaksperiodeForkastet.flatMap { it.path("hendelser").map { UUID.fromString(it.asText()) } }.distinct()
        )
    }

    @Test
    fun `historiskeFolkeregisteridenter test`() {
        val historiskFnr = "123"
        val nyttFnr = "111"
        sendNySøknad(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100), fnr = historiskFnr)
        sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100)),
            historiskeFolkeregisteridenter = listOf(nyttFnr),
            fnr = historiskFnr
        )

        sendNySøknad(SoknadsperiodeDTO(fom = 1.mars, tom = 31.mars, sykmeldingsgrad = 100), fnr = nyttFnr)
        val søknadId2 = sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 1.mars, tom = 31.mars, sykmeldingsgrad = 100)),
            historiskeFolkeregisteridenter = listOf(historiskFnr),
            fnr = nyttFnr
        )

        val vedtaksperiodeForkastet = testRapid.inspektør.meldinger("vedtaksperiode_forkastet")
        assertEquals(1, vedtaksperiodeForkastet.size)
        assertEquals(1.mars, LocalDate.parse(vedtaksperiodeForkastet.first().path("fom").asText()))
        assertEquals(31.mars, LocalDate.parse(vedtaksperiodeForkastet.first().path("tom").asText()))
        assertEquals(
            listOf(søknadId2),
            vedtaksperiodeForkastet.flatMap { it.path("hendelser").map { UUID.fromString(it.asText()) } }.distinct()
        )
    }
}
