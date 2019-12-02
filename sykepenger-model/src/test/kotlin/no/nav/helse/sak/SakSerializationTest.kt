package no.nav.helse.sak

import no.nav.helse.TestConstants.nySøknadHendelse
import no.nav.helse.readResource
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class SakSerializationTest {
    @Test
    fun `restoring av lagret sak gir samme objekt`() {
        val testObserver = TestObserver()

        val sak = Sak(aktørId = "id")
        sak.addObserver(testObserver)

        // trigger endring på sak som gjør at vi kan få ut memento fra observer
        sak.håndter(nySøknadHendelse())

        val json = testObserver.lastSakEndretEvent!!.memento.toString()

        assertDoesNotThrow {
            Sak.fromJson(json)
        }
    }

    @Test
    fun `deserialisering av en serialisert sak med gammelt skjema gir feil`() {
        val sakJson = "/serialisert_person_komplett_sak_med_gammel_versjon.json".readResource()
        assertThrows<SakskjemaForGammelt> { Sak.fromJson(sakJson) }
    }

    @Test
    fun `deserialisering av en serialisert sak uten skjemaversjon gir feil`() {
        val sakJson = "/serialisert_person_komplett_sak_uten_versjon.json".readResource()
        assertThrows<SakskjemaForGammelt> { Sak.fromJson(sakJson) }
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
