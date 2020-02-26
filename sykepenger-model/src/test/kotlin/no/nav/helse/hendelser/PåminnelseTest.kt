package no.nav.helse.hendelser

import no.nav.helse.person.*
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class PåminnelseTest {

    companion object {
        private val vedtaksperiodeId = UUID.randomUUID()
        private val aktørId = "aktørId"
        private val fødselsnummer = "fødselsnummer"
        private val orgnummer = "orgnummer"
    }

    private lateinit var aktivitetslogger: Aktivitetslogger
    private lateinit var aktivitetslogg: Aktivitetslogg

    @BeforeEach
    fun setup() {
        aktivitetslogger = Aktivitetslogger()
        aktivitetslogg = Aktivitetslogg()
    }

    @Test
    fun `info ved påminnelse for nåværende tilstand`() {
        val tilstand = Vedtaksperiode.MottattSykmelding
        val vedtaksperiode = vedtaksperiode()

        assertTrue(vedtaksperiode.håndter(påminnelse(tilstand.type)))
        assertTrue(aktivitetslogger.hasMessagesOld(), aktivitetslogger.toString())
        assertFalse(aktivitetslogger.hasWarningsOld(), aktivitetslogger.toString())
    }

    @Test
    fun `info ved påminnelse for annen tilstand`() {
        val tilstand = Vedtaksperiode.StartTilstand
        val vedtaksperiode = vedtaksperiode()
        assertTrue(vedtaksperiode.håndter(påminnelse(tilstand.type)))
        assertFalse(aktivitetslogger.hasWarningsOld())
        assertTrue(aktivitetslogger.hasMessagesOld())
    }

    @Test
    fun `påminnelse for en annen periode`() {
        val tilstand = Vedtaksperiode.StartTilstand
        val vedtaksperiode = vedtaksperiode()
        assertFalse(vedtaksperiode.håndter(påminnelse(tilstand.type, UUID.randomUUID())))
        assertFalse(aktivitetslogger.hasMessagesOld())
    }

    private fun vedtaksperiode(): Vedtaksperiode {
        val person = Person(aktørId, fødselsnummer)
        return Vedtaksperiode(
            person = person,
            arbeidsgiver = Arbeidsgiver(person, orgnummer),
            id = vedtaksperiodeId,
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = orgnummer
        )
            .also { it.håndter(sykmelding()) }
    }

    private fun sykmelding(): Sykmelding {
        return Sykmelding(
            UUID.randomUUID(),
            fødselsnummer,
            aktørId,
            orgnummer,
            listOf(Triple(1.januar, 6.januar, 100)),
            aktivitetslogger,
            aktivitetslogg
        )
    }

    private fun påminnelse(tilstandType: TilstandType, _vedtaksperiodeId: UUID = vedtaksperiodeId) =
        Påminnelse(
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = orgnummer,
            vedtaksperiodeId = _vedtaksperiodeId.toString(),
            antallGangerPåminnet = 0,
            tilstand = tilstandType,
            tilstandsendringstidspunkt = LocalDateTime.now(),
            påminnelsestidspunkt = LocalDateTime.now(),
            nestePåminnelsestidspunkt = LocalDateTime.now(),
            aktivitetslogger = aktivitetslogger,
            aktivitetslogg = aktivitetslogg
        )
}
