package no.nav.helse.sak

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.TestConstants.nySøknadHendelse
import no.nav.helse.readResource
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class SakSerializationTest {
    @Test
    fun `restoring av lagret sak gir samme objekt`() {
        val testObserver = TestObserver()

        val sak = Sak(aktørId = "id", fødselsnummer = "fnr")
        sak.addObserver(testObserver)

        // trigger endring på sak som gjør at vi kan få ut memento fra observer
        sak.håndter(nySøknadHendelse())

        val json = testObserver.lastSakEndretEvent!!.memento.state()

        assertDoesNotThrow {
            Sak.restore(Sak.Memento.fromString(json, ""))
        }
    }

    @Test
    fun `patcher manglende fødselsnummer`() {
        val fødselsnummer = "12345"

        val sak = Sak(aktørId = "id", fødselsnummer = fødselsnummer)
        val json = sak.memento().state()

        val jsonUtenFødselsnummer = ObjectMapper().let {
            it.writeValueAsString(it.readTree(json).also { jsonNode ->
                (jsonNode as ObjectNode).remove("fødselsnummer")
            })
        }

        assertEquals(json, Sak.restore(Sak.Memento.fromString(jsonUtenFødselsnummer, fødselsnummer)).memento().state())
    }

    @Test
    fun `deserialisering av en serialisert sak med gammelt skjema gir feil`() {
        val sakJson = "/serialisert_person_komplett_sak_med_gammel_versjon.json".readResource()
        assertThrows<SakskjemaForGammelt> { Sak.restore(Sak.Memento.fromString(sakJson, "")) }
    }

    @Test
    fun `deserialisering av en serialisert sak uten skjemaversjon gir feil`() {
        val sakJson = "/serialisert_person_komplett_sak_uten_versjon.json".readResource()
        assertThrows<SakskjemaForGammelt> { Sak.restore(Sak.Memento.fromString(sakJson, "")) }
    }

    private class TestObserver : SakObserver {
        var lastSakEndretEvent: SakObserver.SakEndretEvent? = null

        override fun sakEndret(sakEndretEvent: SakObserver.SakEndretEvent) {
            lastSakEndretEvent = sakEndretEvent
        }

        override fun vedtaksperiodeEndret(event: VedtaksperiodeObserver.StateChangeEvent) {

        }
    }
}
