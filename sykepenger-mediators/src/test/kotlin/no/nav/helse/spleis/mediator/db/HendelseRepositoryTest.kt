package no.nav.helse.spleis.mediator.db

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.test_support.TestDataSource
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.Personidentifikator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.db.HendelseRepository
import no.nav.helse.spleis.mediator.databaseContainer
import no.nav.helse.spleis.meldinger.OverstyrArbeidsgiveropplysningerRiver.Companion.requireArbeidsgiveropplysninger
import no.nav.helse.spleis.meldinger.model.HendelseMessage
import no.nav.helse.spleis.meldinger.model.OverstyrArbeidsgiveropplysningerMessage
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private val fnr = Personidentifikator("01011012345")

internal class HendelseRepositoryTest {
    private lateinit var dataSource: TestDataSource

    @BeforeEach
    internal fun setup() {
        dataSource = databaseContainer.nyTilkobling()
    }

    @AfterEach
    internal fun tearDown() {
        databaseContainer.droppTilkobling(dataSource)
    }

    @Test
    fun `skal klare å hente ny overstyr arbeidsgiveropplysninger-hendelse fra db`() {
        val hendelseId = UUID.randomUUID()
        val (id, navn) = lagre(TestMessages.overstyrArbeidsgiveropplysninger(hendelseId))
        assertEquals(hendelseId, id)
        assertEquals("OVERSTYRARBEIDSGIVEROPPLYSNINGER", navn)
    }

    private fun lagre(hendeleMessage: HendelseMessage): Pair<UUID, String> {
        val repo = HendelseRepository(dataSource.ds)
        val ingenHendelser = repo.hentAlleHendelser(fnr)
        assertEquals(0, ingenHendelser.size)
        repo.lagreMelding(hendeleMessage)
        val hendelser = repo.hentAlleHendelser(fnr)
        assertEquals(1, hendelser.size)
        val (id, navn) = hendelser.entries.single().value
        return id to navn
    }
}

private object TestMessages {
    private fun String.somPacket(validate: (packet: JsonMessage) -> Unit) =
        JsonMessage(this, MessageProblems(this), SimpleMeterRegistry()).also { packet ->
            validate(packet)
        }

    fun overstyrArbeidsgiveropplysninger(id: UUID): OverstyrArbeidsgiveropplysningerMessage {
        val now = LocalDateTime.now()

        @Language("JSON")
        val json = """
        {
            "@id": "${UUID.randomUUID()}",
            "@event_name": "overstyr_inntekt_og_refusjon",
            "@opprettet": "$now",
            "fødselsnummer": "bar",
            "skjæringstidspunkt": "2018-01-01",
            "arbeidsgivere": []
        }
        """

        val packet = json.somPacket { packet ->
            packet.requireKey(
                "@id",
                "@event_name",
                "@opprettet",
                "fødselsnummer",
                "skjæringstidspunkt"
            )
            packet.requireArbeidsgiveropplysninger()
        }

        return OverstyrArbeidsgiveropplysningerMessage(packet, Meldingsporing(id, fnr.toString()))
    }
}
