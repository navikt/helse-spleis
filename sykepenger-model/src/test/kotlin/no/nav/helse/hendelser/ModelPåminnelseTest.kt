package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.person.TilstandType
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.VedtaksperiodeMediator
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
        val vedtaksperiode = vedtaksperiode()

        assertTrue(vedtaksperiode.håndter(påminnelse(tilstand.type)))
        assertTrue(aktivitetslogger.hasWarnings())
    }

    @Test
    fun `info ved påminnelse for annen tilstand`() {
        val tilstand = Vedtaksperiode.MottattNySøknad
        val vedtaksperiode = vedtaksperiode()

        assertTrue(vedtaksperiode.håndter(påminnelse(tilstand.type)))
        assertFalse(aktivitetslogger.hasWarnings())
        assertTrue(aktivitetslogger.hasMessages())
    }

    @Test
    fun `påminnelse for en annen periode`() {
        val tilstand = Vedtaksperiode.StartTilstand
        val vedtaksperiode = vedtaksperiode()
        assertFalse(vedtaksperiode.håndter(påminnelse(tilstand.type, UUID.randomUUID())))
        assertFalse(aktivitetslogger.hasMessages())
    }

    private fun vedtaksperiode() =
        Vedtaksperiode(
            director = object : VedtaksperiodeMediator {},
            id = vedtaksperiodeId,
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = orgnummer
        )


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
