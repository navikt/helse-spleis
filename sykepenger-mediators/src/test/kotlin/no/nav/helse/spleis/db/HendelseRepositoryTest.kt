package no.nav.helse.spleis.db

import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.somFødselsnummer
import no.nav.helse.spleis.e2e.SpleisDataSource.migratedDb
import no.nav.helse.spleis.e2e.resetDatabase
import no.nav.helse.spleis.meldinger.model.HendelseMessage
import no.nav.helse.spleis.meldinger.model.NySøknadMessage
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

private val fnr = "01011012345".somFødselsnummer()
// primært for å slutte å ha teite sql-feil
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class HendelseRepositoryTest {
    private lateinit var dataSource: DataSource

    @BeforeAll
    internal fun setupAll() {
        dataSource = migratedDb
    }
    @BeforeEach
    internal fun setupEach() {
        resetDatabase()
    }

    @Test
    fun `skal klare å hente ny søknad-hendelse fra db`() {
        val hendelseId = UUID.randomUUID()
        val (id, navn) = lagre(TestMessages.nySøknad(hendelseId))
        assertEquals(hendelseId, id)
        assertEquals("NY_SØKNAD", navn)
    }

    private fun lagre(hendeleMessage: HendelseMessage): Pair<UUID, String> {
        val repo = HendelseRepository(dataSource)
        val ingenHendelser = repo.hentAlleHendelser(fnr)
        assertEquals(0, ingenHendelser.size)
        repo.lagreMelding(hendeleMessage)
        val hendelser = repo.hentAlleHendelser(fnr)
        assertEquals(1, hendelser.size)
        val (id, info) = hendelser.entries.first().toPair()
        val (navn, _) = info
        return id to navn
    }
}

private object TestMessages {
    private fun String.somPacket(validate: (packet: JsonMessage) -> Unit) =
        JsonMessage(this, MessageProblems(this)).also { packet ->
            validate(packet)
        }

    fun nySøknad(id: UUID): NySøknadMessage {
        val now = LocalDateTime.now()

        @Language("JSON")
        val json = """
        {
            "@id": "$id",
            "@event_name": "ny_soknad",
            "@opprettet": "$now",
            "fnr": "$fnr",
            "aktorId": "aktorId",
            "sykmeldingSkrevet": "$now",
            "fom": "2020-01-01",
            "tom": "2020-01-31",
            "arbeidsgiver": {
                "orgnummer": "orgnummer"
            },
            "soknadsperioder": []
        }
        """

        val packet = json.somPacket { packet ->
            packet.requireKey("@id")
            packet.requireKey("@event_name")
            packet.requireKey("@opprettet")
            packet.requireKey("sykmeldingSkrevet")
            packet.requireKey("fom")
            packet.requireKey("tom")
            packet.requireKey("fnr")
            packet.requireKey("aktorId")
            packet.requireKey("arbeidsgiver")
            packet.requireKey("arbeidsgiver.orgnummer")
            packet.requireKey("soknadsperioder")
        }

        return NySøknadMessage(packet)
    }
}