package no.nav.helse.spleis.e2e

import no.nav.helse.spleis.meldinger.model.SimuleringMessage
import no.nav.helse.testhelpers.april
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.inntektsmeldingkontrakt.Periode
import no.nav.syfo.kafka.felles.SoknadsperiodeDTO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

internal class AvstemmingMediatorTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `avstemming`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(0, listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendUtbetalingsgrunnlag(0)
        sendYtelser(0)
        sendVilkårsgrunnlag(0)
        sendYtelserUtenSykepengehistorikk(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(0)
        sendUtbetaling()
        sendNySøknad(SoknadsperiodeDTO(fom = 1.februar, tom = 28.februar, sykmeldingsgrad = 100))
        sendNySøknad(SoknadsperiodeDTO(fom = 1.februar, tom = 28.februar, sykmeldingsgrad = 100))
        sendNySøknad(SoknadsperiodeDTO(fom = 1.april, tom = 30.april, sykmeldingsgrad = 100))
        sendSøknad(2, listOf(SoknadsperiodeDTO(fom = 1.april, tom = 30.april, sykmeldingsgrad = 100)))
        sendInntektsmelding(2, listOf(Periode(fom = 1.april, tom = 18.april)), førsteFraværsdag = 1.april)
        sendUtbetalingsgrunnlag(2)
        sendYtelserUtenSykepengehistorikk(2)
        sendVilkårsgrunnlag(2,
            inntekter = (4.rangeTo(12).map { YearMonth.of(2017, it) to 31000.00 } + 1.rangeTo(3).map { YearMonth.of(2018, it) to 31000.00 })
        )
        sendYtelserUtenSykepengehistorikk(2)

        sendAvstemming()
        val avstemt = testRapid.inspektør.siste("person_avstemt")
        val vedtaksperioder = avstemt.path("arbeidsgivere").path(0).path("vedtaksperioder")
        val forkastedeVedtaksperioder = avstemt.path("arbeidsgivere").path(0).path("forkastedeVedtaksperioder")
        val utbetalinger = avstemt.path("arbeidsgivere").path(0).path("utbetalinger")

        assertEquals(UNG_PERSON_FNR_2018, avstemt.path("fødselsnummer").asText())
        assertEquals(AKTØRID, avstemt.path("aktørId").asText())
        assertEquals(1, avstemt.path("arbeidsgivere").size())
        assertEquals(ORGNUMMER, avstemt.path("arbeidsgivere").path(0).path("organisasjonsnummer").asText())
        assertEquals(1, vedtaksperioder.size())
        assertDoesNotThrow { UUID.fromString(vedtaksperioder.path(0).path("id").asText()) }
        assertEquals("AVVENTER_SIMULERING", vedtaksperioder.path(0).path("tilstand").asText())
        assertDoesNotThrow { LocalDateTime.parse(vedtaksperioder.path(0).path("tidsstempel").asText()) }

        assertEquals(2, forkastedeVedtaksperioder.size())
        assertDoesNotThrow { UUID.fromString(forkastedeVedtaksperioder.path(0).path("id").asText()) }
        assertEquals("AVSLUTTET", forkastedeVedtaksperioder.path(0).path("tilstand").asText())
        assertDoesNotThrow { LocalDateTime.parse(forkastedeVedtaksperioder.path(0).path("tidsstempel").asText()) }
        assertDoesNotThrow { UUID.fromString(forkastedeVedtaksperioder.path(1).path("id").asText()) }
        assertEquals("TIL_INFOTRYGD", forkastedeVedtaksperioder.path(1).path("tilstand").asText())
        assertDoesNotThrow { LocalDateTime.parse(forkastedeVedtaksperioder.path(1).path("tidsstempel").asText()) }

        assertEquals(2, utbetalinger.size())
        assertDoesNotThrow { UUID.fromString(utbetalinger.path(0).path("id").asText()) }
        assertEquals("UTBETALT", utbetalinger.path(0).path("status").asText())
        assertEquals("UTBETALING", utbetalinger.path(0).path("type").asText())
        assertDoesNotThrow { LocalDateTime.parse(utbetalinger.path(0).path("tidsstempel").asText()) }
        assertDoesNotThrow { UUID.fromString(utbetalinger.path(1).path("id").asText()) }
        assertEquals("IKKE_UTBETALT", utbetalinger.path(1).path("status").asText())
        assertEquals("UTBETALING", utbetalinger.path(1).path("type").asText())
        assertDoesNotThrow { LocalDateTime.parse(utbetalinger.path(1).path("tidsstempel").asText()) }
    }
}


