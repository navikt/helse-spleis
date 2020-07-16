package no.nav.helse.spleis.e2e

import com.fasterxml.jackson.databind.JsonNode
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.spleis.meldinger.model.SimuleringMessage
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.inntektsmeldingkontrakt.Periode
import no.nav.syfo.kafka.felles.SoknadsperiodeDTO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RollbackTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `rollback på en person uten tidligere state`() {
        påbegyntPeriode()
        val vedtaksperiodeId = testRapid.inspektør.vedtaksperiodeId(0).toString()
        sendRollbackDelete()

        val rollbackHendelse = finnHendelser("person_rullet_tilbake").last()
        assertEquals(vedtaksperiodeId, rollbackHendelse["vedtaksperioderSlettet"].first().asText())
    }

    @Test
    fun `rollback på en person med tidligere utbetalinger`() {
        utbetaltPeriode()
        sendNySøknad(SoknadsperiodeDTO(fom = 27.januar, tom = 10.februar, sykmeldingsgrad = 100))
        val uferdigVedtaksperiodeId = testRapid.inspektør.vedtaksperiodeId(1).toString()
        val versjoner = finnPersonversjoner()
        sendRollback(versjoner[versjoner.lastIndex - 1])

        val perioderTilInfotrygdEvents = finnHendelser("vedtaksperiode_endret")
            .filter { it["gjeldendeTilstand"].textValue() == "TIL_INFOTRYGD" }

        assertEquals(0, perioderTilInfotrygdEvents.size)

        val rollbackHendelse = finnHendelser("person_rullet_tilbake").last()
        assertEquals(1, rollbackHendelse["vedtaksperioderSlettet"].size())
        assertEquals(uferdigVedtaksperiodeId, rollbackHendelse["vedtaksperioderSlettet"].first().asText())
    }

    @Test
    fun `rollback på en person med påbegynt periode`() {
        påbegyntPeriode()

        sendRollback(finnPersonversjoner()[2])

        val tilstandEtterRollback = finnHendelser("vedtaksperiode_endret").last()
        assertEquals("TIL_INFOTRYGD", tilstandEtterRollback["gjeldendeTilstand"].textValue())

        val rollbackHendelse = finnHendelser("person_rullet_tilbake").last()
        assertEquals(0, rollbackHendelse["vedtaksperioderSlettet"].size())
    }

    @Test
    fun `rollback over utbetaling`() {
        utbetaltPeriode()
        val versjoner = finnPersonversjoner()
        sendRollback(versjoner[versjoner.lastIndex - 2])
        val versjonerEtterRollback = finnPersonversjoner()
        assertEquals(versjoner, versjonerEtterRollback, "Vedtaksperiode ble endret og persistert etter tilbakerulling som burde feilet.")

        val perioderTilInfotrygdEvents = finnHendelser("vedtaksperiode_endret")
            .filter { it["gjeldendeTilstand"].textValue() == "TIL_INFOTRYGD" }
        assertEquals(0, perioderTilInfotrygdEvents.size)

        val rollbackHendelse = finnHendelser("person_rullet_tilbake")
        assertEquals(0, rollbackHendelse.size)
    }

    @Test
    fun `rollback ved sletting over utbetaling`() {
        utbetaltPeriode()
        val versjoner = finnPersonversjoner()
        sendRollbackDelete()
        val versjonerEtterRollback = finnPersonversjoner()
        assertEquals(versjoner, versjonerEtterRollback, "Vedtaksperiode ble endret og persistert etter tilbakerulling som burde feilet.")

        val perioderTilInfotrygdEvents = finnHendelser("vedtaksperiode_endret")
            .filter { it["gjeldendeTilstand"].textValue() == "TIL_INFOTRYGD" }
        assertEquals(0, perioderTilInfotrygdEvents.size)

        val rollbackHendelse = finnHendelser("person_rullet_tilbake")
        assertEquals(0, rollbackHendelse.size)
    }

    private fun påbegyntPeriode() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(0, listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendVilkårsgrunnlag(0)
        sendYtelserUtenHistorikk(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)

        val tilstandFørRollback = finnHendelser("vedtaksperiode_endret").last()
        assertNotEquals("TIL_INFOTRYGD", tilstandFørRollback["gjeldendeTilstand"].textValue())
    }

    private fun utbetaltPeriode() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(0, listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(0, listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendVilkårsgrunnlag(0)
        sendYtelserUtenHistorikk(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(0)
        sendUtbetaling(0)

        val tilstandFørRollback = finnHendelser("vedtaksperiode_endret").last()
        assertNotEquals("TIL_INFOTRYGD", tilstandFørRollback["gjeldendeTilstand"].textValue())
    }

    private fun finnPersonversjoner(): List<Long> {
        return using(sessionOf(dataSource)) {
            it.run(
                queryOf("SELECT id FROM person WHERE fnr = ? ", UNG_PERSON_FNR_2018).map { row ->
                    row.long("id")
                }.asList
            )
        }
    }

    private fun finnHendelser(type: String): List<JsonNode> =
        (0 until testRapid.inspektør.antall())
            .map { testRapid.inspektør.melding(it) }
            .filter { it["@event_name"].textValue() == type }
}
