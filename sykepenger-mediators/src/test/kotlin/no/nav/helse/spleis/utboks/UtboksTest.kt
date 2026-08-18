package no.nav.helse.spleis.utboks

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.Personidentifikator
import no.nav.helse.hendelser.MeldingsreferanseId
import org.junit.jupiter.api.Test
import java.lang.reflect.Proxy
import java.sql.Connection
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

internal class UtboksTest {

    @Test
    fun `melding_om_melding_håndtert bør ikke komme sammen med melding_om_melding_ikke_håndtert_fordi_person_ikke_funnet`() {
        val dao = InMemoryUtboksDao()
        val utboks = utboks(innkommendeMelding(), dao)
        utboks.nyMelding { personidentifikator -> Utboksmelding.BeholdEtterSending(
            UtgåendeMelding.nyRapidmelding(
                personidentifikator = personidentifikator,
                eventName = "melding_om_melding_ikke_håndtert_fordi_person_ikke_funnet",
                innhold = emptyMap()
            )
        )}
        utboks.lagre()
        assertEquals(listOf("melding_om_melding_ikke_håndtert_fordi_person_ikke_funnet"), dao.usendteEvents())
    }

    @Test
    fun `melding_om_melding_håndtert skal alltid komme for meldinger som ikke er melding_om_melding_ikke_håndtert_fordi_person_ikke_funnet`() {
        val dao = InMemoryUtboksDao()
        val utboks = utboks(innkommendeMelding(), dao)
        utboks.nyMelding { personidentifikator -> Utboksmelding.BeholdEtterSending(
            UtgåendeMelding.nyRapidmelding(
                personidentifikator = personidentifikator,
                eventName = "utgående_test_event",
                innhold = emptyMap()
            )
        )}
        utboks.lagre()
        assertEquals(listOf("utgående_test_event", "melding_om_melding_håndtert"), dao.usendteEvents())
    }

    @Test
    fun `forårsaket av for ikke-behov`() {
        val dao = InMemoryUtboksDao()
        val innkommendeMeldingId: UUID = UUID.randomUUID()
        val utboks = utboks(innkommendeMelding(meldingsreferanseId = innkommendeMeldingId), dao)
        utboks.nyMelding { personidentifikator -> Utboksmelding.BeholdEtterSending(
            UtgåendeMelding.nyRapidmelding(
                personidentifikator = personidentifikator,
                eventName = "utgående_test_event",
                innhold = emptyMap()
            )
        )}
        utboks.lagre()
        assertEquals(listOf("utgående_test_event", "melding_om_melding_håndtert"), dao.usendteEvents())
        dao.usendte().forEach { utgåendeMelding ->
            utgåendeMelding.json.path("@forårsaket_av").apply {
                assertEquals("innkommende_test_event", path("event_name").asText())
                assertEquals("$innkommendeMeldingId", path("id").asText())
                assertTrue(path("behov").isMissingNode)
                assertDoesNotThrow { LocalDateTime.parse(path("opprettet").asText()) }
            }
        }
        // Inneholder akkurat det samme som @forårsaket_av, så vet ikke hvorfor dette eventet finnes..
        dao.usendte().single { it.eventName == "melding_om_melding_håndtert" }.json.apply {
            assertEquals("innkommende_test_event", path("originalt_event_name").asText())
            assertEquals("$innkommendeMeldingId", path("original_id").asText())
        }
    }

    @Test
    fun `forårsaket av for behov`() {
        val dao = InMemoryUtboksDao()
        val innkommendeMeldingId: UUID = UUID.randomUUID()
        val utboks = utboks(innkommendeMelding(meldingsreferanseId = innkommendeMeldingId, navn = "behov", behov = listOf("Behov1", "Behov2")), dao)
        utboks.nyMelding { personidentifikator -> Utboksmelding.ForkastEtterSending(
            UtgåendeMelding.nyRapidmelding(
                personidentifikator = personidentifikator,
                eventName = "utgående_test_event",
                innhold = emptyMap()
            )
        )}
        utboks.lagre()
        assertEquals(listOf("utgående_test_event", "melding_om_melding_håndtert"), dao.usendteEvents())
        dao.usendte().forEach { utgåendeMelding ->
            utgåendeMelding.json.path("@forårsaket_av").apply {
                assertEquals("behov", path("event_name").asText())
                assertEquals("$innkommendeMeldingId", path("id").asText())
                assertEquals(listOf("Behov1", "Behov2"), path("behov").map { it.asText() })
                assertDoesNotThrow { LocalDateTime.parse(path("opprettet").asText()) }
            }
        }
        // Inneholder akkurat det samme som @forårsaket_av, så vet ikke hvorfor dette eventet finnes..
        dao.usendte().single { it.eventName == "melding_om_melding_håndtert" }.json.apply {
            assertEquals("behov", path("originalt_event_name").asText())
            assertEquals("$innkommendeMeldingId", path("original_id").asText())
        }
    }

    @Test
    fun `kan ikke legge til meldinger etter lagring`() {
        val dao = InMemoryUtboksDao()
        val utboks = utboks(innkommendeMelding(), dao)
        utboks.lagre()
        val error = assertThrows<IllegalStateException> {
            utboks.nyMelding { personidentifikator -> Utboksmelding.BeholdEtterSending(
                UtgåendeMelding.nyRapidmelding(
                    personidentifikator = personidentifikator,
                    eventName = "utgående_test_event",
                    innhold = emptyMap()
                )
            )}
        }
        assertEquals("Utboksen er lukket, kan ikke legge til melding", error.message)
    }

    @Test
    fun `kan ikke legge til melding for annen person`() {
        val dao = InMemoryUtboksDao()
        val utboks = utboks(innkommendeMelding(), dao)
        val error = assertThrows<IllegalArgumentException> {
            utboks.nyMelding { _ -> Utboksmelding.BeholdEtterSending(
                UtgåendeMelding.nyRapidmelding(
                    personidentifikator = Personidentifikator("22222222222"),
                    eventName = "utgående_test_event",
                    innhold = emptyMap()
                )
            )}
        }
        assertEquals("Kan ikke sende ut meldinger for andre i denne utboksen!", error.message)
    }

    private companion object {
        val fakeConnection = Proxy.newProxyInstance(
            Connection::class.java.classLoader,
            arrayOf(Connection::class.java)
        ) { _, method, _ -> error("Connection should not be used: ${method.name}") } as Connection

        fun utboks(innkommendeMelding: InnkommendeMelding, dao: InMemoryUtboksDao) = Utboks(
            utsender = TestUtsender(),
            innkommendeMelding = innkommendeMelding,
            utboksDao = dao
        )
        fun Utboks.lagre() = lagre(fakeConnection)
        fun InMemoryUtboksDao.usendteEvents() = usendte().map { it.eventName }

        fun innkommendeMelding(
            navn: String = "innkommende_test_event",
            meldingsreferanseId: UUID = UUID.randomUUID(),
            personidentifikator: Personidentifikator = Personidentifikator("11111111111"),
            opprettet: LocalDateTime = LocalDateTime.now(),
            behov: List<String>? = null
        ) = InnkommendeMelding(
            navn = navn,
            meldingsreferanseId = MeldingsreferanseId(meldingsreferanseId),
            personidentifikator = personidentifikator,
            opprettet = opprettet,
            behov = behov
        )
    }
}
