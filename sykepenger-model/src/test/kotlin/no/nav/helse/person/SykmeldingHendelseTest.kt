package no.nav.helse.person

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.inspectors.personLogg
import no.nav.helse.januar
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class SykmeldingHendelseTest : AbstractEndToEndTest() {

    @Test
    fun `Sykmelding skaper Arbeidsgiver og Sykmeldingsperiode`() {
        person.håndter(sykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100.prosent)))
        assertFalse(person.personLogg.hasErrorsOrWorse())
        assertTrue(person.personLogg.hasActivities())
        assertFalse(person.personLogg.hasErrorsOrWorse())
        assertEquals(0, inspektør.vedtaksperiodeTeller)
        assertEquals(1, inspektør.sykmeldingsperioder().size)
    }

    @Test
    fun `To søknader uten overlapp`() {
        person.håndter(sykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100.prosent)))
        person.håndter(sykmelding(Sykmeldingsperiode(6.januar, 10.januar, 100.prosent)))
        assertFalse(person.personLogg.hasErrorsOrWorse())
        assertTrue(person.personLogg.hasActivities())
        assertFalse(person.personLogg.hasErrorsOrWorse())
        assertEquals(0, inspektør.vedtaksperiodeTeller)
        assertEquals(2, inspektør.sykmeldingsperioder().size)
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
