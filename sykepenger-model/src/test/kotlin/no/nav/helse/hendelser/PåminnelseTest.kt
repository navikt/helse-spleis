package no.nav.helse.hendelser

import no.nav.helse.person.*
import no.nav.helse.testhelpers.april
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class PåminnelseTest {

    private companion object {
        private val vedtaksperiodeId = UUID.randomUUID()
        private const val aktørId = "aktørId"
        private const val fødselsnummer = "fødselsnummer"
        private const val orgnummer = "orgnummer"
    }

    private lateinit var aktivitetslogg: Aktivitetslogg

    @Test
    fun `info ved påminnelse for annen tilstand`() {
        val tilstand = Vedtaksperiode.Start
        val vedtaksperiode = vedtaksperiode()
        assertTrue(vedtaksperiode.håndter(påminnelse(tilstand.type)))
        assertFalse(aktivitetslogg.hasWarnings())
        assertTrue(aktivitetslogg.hasMessages())
    }

    @Test
    fun `påminnelse for en annen periode`() {
        val tilstand = Vedtaksperiode.Start
        val vedtaksperiode = vedtaksperiode()
        påminnelse(tilstand.type, UUID.randomUUID()).also {
            assertFalse(vedtaksperiode.håndter(it))
            assertFalse(it.aktivitetslogg.hasMessages(), it.aktivitetslogg.toString())
        }
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
            listOf(Sykmeldingsperiode(1.januar, 6.januar, 100)),
            mottatt = 1.april.atStartOfDay()
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
            nestePåminnelsestidspunkt = LocalDateTime.now()
        ).also { aktivitetslogg = it.aktivitetslogg }
}
