package no.nav.helse.person

import java.time.LocalDateTime
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
        assertFalse(person.personLogg.harFunksjonelleFeilEllerVerre())
        assertTrue(person.personLogg.harAktiviteter())
        assertFalse(person.personLogg.harFunksjonelleFeilEllerVerre())
        assertEquals(0, inspektør.vedtaksperiodeTeller)
        assertEquals(1, inspektør.sykmeldingsperioder().size)
    }

    @Test
    fun `To søknader uten overlapp`() {
        person.håndter(sykmelding(Sykmeldingsperiode(1.januar, 5.januar, 100.prosent)))
        person.håndter(sykmelding(Sykmeldingsperiode(6.januar, 10.januar, 100.prosent)))
        assertFalse(person.personLogg.harFunksjonelleFeilEllerVerre())
        assertTrue(person.personLogg.harAktiviteter())
        assertFalse(person.personLogg.harFunksjonelleFeilEllerVerre())
        assertEquals(0, inspektør.vedtaksperiodeTeller)
        assertEquals(2, inspektør.sykmeldingsperioder().size)
    }

    private fun sykmelding(vararg sykeperioder: Sykmeldingsperiode) =
        a1Hendelsefabrikk.lagSykmelding(
            sykeperioder = sykeperioder,
            sykmeldingSkrevet = Sykmeldingsperiode.periode(sykeperioder.toList())?.start?.atStartOfDay() ?: LocalDateTime.now(),
            mottatt = Sykmeldingsperiode.periode(sykeperioder.toList())!!.endInclusive.atStartOfDay()
        )
}
