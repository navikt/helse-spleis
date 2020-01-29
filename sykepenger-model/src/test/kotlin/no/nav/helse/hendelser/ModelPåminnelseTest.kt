package no.nav.helse.hendelser

import no.nav.helse.fixtures.januar
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.person.TilstandType
import no.nav.helse.person.Vedtaksperiode
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class ModelPåminnelseTest {

    companion object {
        private val vedtaksperiodeId = UUID.randomUUID()
        private val aktørId = "aktørId"
        private val fødselsnummer = "fødselsnummer"
        private val orgnummer = "orgnummer"
    }

    private lateinit var aktivitetslogger: Aktivitetslogger

    @BeforeEach
    fun setup() {
        aktivitetslogger = Aktivitetslogger()
    }

    @Test
    fun `warning ved påminnelse for nåværende tilstand`() {
        val tilstand = Vedtaksperiode.StartTilstand
        val vedtaksperiode = vedtaksperiode(tilstand)

        assertTrue(vedtaksperiode.håndter(påminnelse(tilstand.type)))
        assertTrue(aktivitetslogger.hasWarnings())
    }

    @Test
    fun `info ved påminnelse for annen tilstand`() {
        val tilstand = Vedtaksperiode.MottattNySøknad
        val vedtaksperiode = vedtaksperiode(Vedtaksperiode.StartTilstand)

        assertTrue(vedtaksperiode.håndter(påminnelse(tilstand.type)))
        assertFalse(aktivitetslogger.hasWarnings())
        assertTrue(aktivitetslogger.hasMessages())
    }

    @Test
    fun `påminnelse for en annen periode`() {
        val tilstand = Vedtaksperiode.StartTilstand
        val vedtaksperiode = vedtaksperiode(Vedtaksperiode.StartTilstand)
        assertFalse(vedtaksperiode.håndter(påminnelse(tilstand.type, UUID.randomUUID())))
        assertFalse(aktivitetslogger.hasMessages())
    }

    private fun vedtaksperiode(tilstand: Vedtaksperiode.Vedtaksperiodetilstand) =
        Vedtaksperiode(
            id = vedtaksperiodeId,
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = orgnummer,
            tilstand = tilstand
        ).apply { håndter(
            ModelNySøknad(
            hendelseId = UUID.randomUUID(),
                fnr = fødselsnummer,
                orgnummer = orgnummer,
                aktørId = aktørId,
                rapportertdato = LocalDateTime.now(),
                sykeperioder = listOf(Triple(1.januar, 10.januar, 100)),
                originalJson = "",
                aktivitetslogger = aktivitetslogger
        ))
        }


    private fun påminnelse(tilstandType: TilstandType, _vedtaksperiodeId:UUID = vedtaksperiodeId) =
        ModelPåminnelse(
            hendelseId = UUID.randomUUID(),
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = orgnummer,
            vedtaksperiodeId = _vedtaksperiodeId.toString(),
            antallGangerPåminnet = 0,
            tilstand = tilstandType,
            tilstandsendringstidspunkt = LocalDateTime.now(),
            påminnelsestidspunkt = LocalDateTime.now(),
            nestePåminnelsestidspunkt = LocalDateTime.now(),
            aktivitetslogger = aktivitetslogger
        )
}
