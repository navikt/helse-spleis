package no.nav.helse.person

import no.nav.helse.hendelser.ModelNySøknad
import no.nav.helse.oktober
import no.nav.helse.readResource
import no.nav.helse.september
import no.nav.helse.toJsonNode
import no.nav.syfo.kafka.sykepengesoknad.dto.*
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.util.*

internal class PersonSerializationTest {
    @Test
    fun `restoring av lagret person gir samme objekt`() {
        val testObserver = TestObserver()

        val person = Person(aktørId = "id", fødselsnummer = "fnr")
        person.addObserver(testObserver)

        // trigger endring på person som gjør at vi kan få ut memento fra observer
        person.håndter(nySøknad("id"))

        val json = testObserver.lastPersonEndretEvent!!.memento.state()

        assertDoesNotThrow {
            Person.restore(Person.Memento.fromString(json))
        }
    }

    @Test
    fun `deserialisering av en serialisert person med gammelt skjema gir feil`() {
        val json = "/serialisert_person_komplett_sak_med_gammel_versjon.json".readResource()
        assertThrows<PersonskjemaForGammelt> { Person.restore(Person.Memento.fromString(json)) }
    }

    @Test
    fun `deserialisering av en serialisert person uten skjemaversjon gir feil`() {
        val json = "/serialisert_person_komplett_sak_uten_versjon.json".readResource()
        assertThrows<PersonskjemaForGammelt> { Person.restore(Person.Memento.fromString(json)) }
    }

    private class TestObserver : PersonObserver {
        var lastPersonEndretEvent: PersonObserver.PersonEndretEvent? = null

        override fun personEndret(personEndretEvent: PersonObserver.PersonEndretEvent) {
            lastPersonEndretEvent = personEndretEvent
        }

        override fun vedtaksperiodeEndret(event: VedtaksperiodeObserver.StateChangeEvent) {

        }
    }

    private fun nySøknad(aktørId: String) = ModelNySøknad(
        hendelseId = UUID.randomUUID(),
        fnr = "fnr",
        aktørId = aktørId,
        orgnummer = "123456789",
        rapportertdato = LocalDateTime.now(),
        sykeperioder = listOf(Triple(16.september, 5.oktober, 100)),
        originalJson = SykepengesoknadDTO(
            id = "123",
            type = SoknadstypeDTO.ARBEIDSTAKERE,
            status = SoknadsstatusDTO.NY,
            aktorId = aktørId,
            fnr = "fnr",
            sykmeldingId = UUID.randomUUID().toString(),
            arbeidsgiver = ArbeidsgiverDTO(
                "Hello world",
                "123456789"
            ),
            fom = 16.september,
            tom = 5.oktober,
            opprettet = LocalDateTime.now(),
            egenmeldinger = emptyList(),
            soknadsperioder = listOf(
                SoknadsperiodeDTO(16.september, 5.oktober,100)
            ),
            fravar = emptyList()
        ).toJsonNode().toString(),
        aktivitetslogger = Aktivitetslogger()
    )

}
