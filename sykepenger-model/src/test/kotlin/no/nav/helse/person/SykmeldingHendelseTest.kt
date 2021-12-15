package no.nav.helse.person

import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class SykmeldingHendelseTest : AbstractPersonTest() {

    @Test
    fun `Sykmelding skaper Arbeidsgiver og Vedtaksperiode`() {
        person.håndter(sykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100.prosent)))
        assertFalse(inspektør.personLogg.hasErrorsOrWorse())
        assertTrue(inspektør.personLogg.hasActivities())
        assertFalse(inspektør.personLogg.hasErrorsOrWorse())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP, inspektør.sisteTilstand(1.vedtaksperiode))
    }

    @Test
    fun `To søknader uten overlapp`() {
        person.håndter(sykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100.prosent)))
        person.håndter(sykmelding(Sykmeldingsperiode(6.januar, 10.januar, 100.prosent)))
        assertFalse(inspektør.personLogg.hasErrorsOrWorse())
        assertTrue(inspektør.personLogg.hasActivities())
        assertFalse(inspektør.personLogg.hasErrorsOrWorse())
        assertEquals(2, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP, inspektør.sisteTilstand(1.vedtaksperiode))
        assertEquals(TilstandType.MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE, inspektør.sisteTilstand(2.vedtaksperiode))
    }

    @Test
    fun `Overlappende sykmelding, går til Infotrygd`() {
        person.håndter(sykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100.prosent)))
        person.håndter(sykmelding(Sykmeldingsperiode(4.januar, 6.januar, 100.prosent)))
        assertTrue(inspektør.personLogg.hasErrorsOrWorse())
        assertEquals(1, inspektør.vedtaksperiodeTeller)
        assertEquals(TilstandType.TIL_INFOTRYGD, inspektør.sisteTilstand(1.vedtaksperiode))
        assertTrue(inspektør.periodeErForkastet(1.vedtaksperiode))
    }

    private fun sykmelding(vararg sykeperioder: Sykmeldingsperiode, orgnummer: String = "987654321") =
        Sykmelding(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018.toString(),
            aktørId = "12345",
            orgnummer = orgnummer,
            sykeperioder = sykeperioder.toList(),
            sykmeldingSkrevet = Sykmeldingsperiode.periode(sykeperioder.toList())?.start?.atStartOfDay() ?: LocalDateTime.now(),
            mottatt = Sykmeldingsperiode.periode(sykeperioder.toList())!!.endInclusive.atStartOfDay()
        )
}
