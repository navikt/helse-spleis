package no.nav.helse.spleis.db

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.somFødselsnummer
import no.nav.helse.spleis.IMessageMediator
import no.nav.helse.spleis.db.TestMessages.NySøknad
import no.nav.helse.spleis.e2e.SpleisDataSource.migratedDb
import no.nav.helse.spleis.e2e.resetDatabase
import no.nav.helse.spleis.meldinger.SøknadRiver
import no.nav.helse.spleis.meldinger.TestMessageMediator
import no.nav.helse.spleis.meldinger.TestRapid
import no.nav.helse.spleis.meldinger.model.NySøknadMessage
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource

private val fnr = "01011012345".somFødselsnummer()
// primært for å slutte å ha teite sql-feil
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HendelseRepositoryTest {
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
        val repo = HendelseRepository(dataSource)
        val ingenEvents = repo.hentAlleHendelser(fnr)
        assertEquals(0, ingenEvents.size)
        repo.lagreMelding(NySøknad)
        val singleEvent = repo.hentAlleHendelser(fnr)
        assertEquals(1, singleEvent.size)
    }
}

private object TestMessages {
    val now = LocalDateTime.now().toString()
    val id = UUID.randomUUID()
    val deceitfulRiver: FalskeNyeSøknaderRiver = FalskeNyeSøknaderRiver(TestRapid(), TestMessageMediator())
    val NySøknad = deceitfulRiver.lagDenFordømteMeldinga(JsonMessage("""
        {
            "@id": "$id",
            "@event_name": "ny_soknad",
            "@opprettet": "$now",
            "fnr": "${fnr.toString()}",
            "aktorId": "aktorId",
            "sykmeldingSkrevet": "$now",
            "arbeidsgiver": {
                "orgnummer": "orgnummer"
            },
            "soknadsperioder": []
        }
    """.trimIndent(), MessageProblems("")))

}

internal class FalskeNyeSøknaderRiver(
    rapidsConnection: RapidsConnection,
    messageMediator: IMessageMediator
) : SøknadRiver(rapidsConnection, messageMediator) {
    override val eventName = "ny_søknad"
    override val riverName = "Ny søknad"

    override fun validate(message: JsonMessage) {
        message.requireKey("sykmeldingId")
        message.requireValue("status", "NY")
    }

    override fun createMessage(packet: JsonMessage) = NySøknadMessage(packet)
    fun lagDenFordømteMeldinga(packet: JsonMessage): NySøknadMessage {
        packet.requireKey("@id")
        packet.requireKey("@event_name")
        packet.requireKey("@opprettet")
        packet.requireKey("sykmeldingSkrevet")
        packet.requireKey("fnr")
        packet.requireKey("aktorId")
        packet.requireKey("arbeidsgiver")
        packet.requireKey("arbeidsgiver.orgnummer")
        packet.requireKey("soknadsperioder")
        return createMessage(packet)
    }
}
