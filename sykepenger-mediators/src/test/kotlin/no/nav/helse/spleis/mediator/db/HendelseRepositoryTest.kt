package no.nav.helse.spleis.mediator.db

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.Personidentifikator
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.nyttFødselsnummer
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.db.HendelseRepository
import no.nav.helse.spleis.mediator.databaseContainer
import no.nav.helse.spleis.meldinger.OverstyrArbeidsgiveropplysningerRiver.Companion.requireArbeidsgiveropplysninger
import no.nav.helse.spleis.meldinger.model.HendelseMessage
import no.nav.helse.spleis.meldinger.model.OverstyrArbeidsgiveropplysningerMessage
import no.nav.helse.testdatabase.TestDataSource
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class HendelseRepositoryTest {
    // unikt per testinstans - trygt å dele database med andre tester uten kollisjon
    private val fnr = Personidentifikator(nyttFødselsnummer())
    private lateinit var dataSource: TestDataSource

    @BeforeEach
    internal fun setup() {
        dataSource = databaseContainer.nyTilkobling()
    }

    @Test
    fun `skal klare å hente ny overstyr arbeidsgiveropplysninger-hendelse fra db`() {
        val hendelseId = MeldingsreferanseId(UUID.randomUUID())
        val (id, navn) = lagre(TestMessages.overstyrArbeidsgiveropplysninger(hendelseId, fnr))
        assertEquals(hendelseId.id, id)
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
        JsonMessage(this, MessageProblems(this)).also { packet ->
            validate(packet)
        }

    fun overstyrArbeidsgiveropplysninger(id: MeldingsreferanseId, fnr: Personidentifikator): OverstyrArbeidsgiveropplysningerMessage {
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
            packet.requireKey("@id", "@event_name", "@opprettet", "fødselsnummer", "skjæringstidspunkt")
            packet.requireArbeidsgiveropplysninger()
        }

        return OverstyrArbeidsgiveropplysningerMessage(packet, Meldingsporing(id, fnr.toString()))
    }
}
