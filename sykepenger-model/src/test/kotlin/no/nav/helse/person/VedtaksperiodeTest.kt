package no.nav.helse.person

import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.testhelpers.oktober
import no.nav.helse.testhelpers.september
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class VedtaksperiodeTest {

    private val aktør = "1234"
    private val fødselsnummer = "5678"
    private val organisasjonsnummer = "123456789"
    private val person = Person(aktør, fødselsnummer)
    private val arbeidsgiver = Arbeidsgiver(person, organisasjonsnummer)

    @Test
    fun `påminnelse returnerer true basert på om påminnelsen ble håndtert eller ikke`() {
        val id = UUID.randomUUID()
        val vedtaksperiode = periodeFor(sykmelding = sykmelding(), id = id)

        assertFalse(vedtaksperiode.håndter(påminnelse(UUID.randomUUID(), TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP)))
        assertTrue(vedtaksperiode.håndter(påminnelse(id, TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP)))
    }

    private fun sykmelding(
        fnr: String = fødselsnummer,
        aktørId: String = aktør,
        orgnummer: String = organisasjonsnummer,
        perioder: List<Sykmeldingsperiode> = listOf(Sykmeldingsperiode(16.september, 5.oktober, 100.prosent))
    ) = Sykmelding(
        meldingsreferanseId = UUID.randomUUID(),
        fnr = fnr,
        aktørId = aktørId,
        orgnummer = orgnummer,
        sykeperioder = perioder,
        mottatt = Sykmeldingsperiode.periode(perioder)?.start?.atStartOfDay() ?: LocalDateTime.now()
    )

    private fun påminnelse(vedtaksperiodeId: UUID, tilstandType: TilstandType) = Påminnelse(
        meldingsreferanseId = UUID.randomUUID(),
        aktørId = "",
        fødselsnummer = "",
        organisasjonsnummer = "",
        vedtaksperiodeId = vedtaksperiodeId.toString(),
        tilstand = tilstandType,
        antallGangerPåminnet = 1,
        tilstandsendringstidspunkt = LocalDateTime.now(),
        påminnelsestidspunkt = LocalDateTime.now(),
        nestePåminnelsestidspunkt = LocalDateTime.now()
    )

    private fun periodeFor(sykmelding: Sykmelding, id: UUID = UUID.randomUUID()) = Vedtaksperiode(
        person = person,
        arbeidsgiver = arbeidsgiver,
        id = id,
        aktørId = sykmelding.aktørId(),
        fødselsnummer = sykmelding.fødselsnummer(),
        organisasjonsnummer = sykmelding.organisasjonsnummer()
    ).also {
        person.håndter(sykmelding)
    }
}
