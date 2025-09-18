package no.nav.helse.person

import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.januar
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

internal class SykmeldingHendelseTest : AbstractEndToEndTest() {

    @Test
    fun `Sykmelding skaper Arbeidsgiver og Sykmeldingsperiode`() {
        val aktivitetslogg = Aktivitetslogg()
        person.håndterSykmelding(sykmelding(Sykmeldingsperiode(1.januar, 5.januar)), aktivitetslogg)
        assertFalse(aktivitetslogg.harFunksjonelleFeilEllerVerre())
        assertFalse(aktivitetslogg.harFunksjonelleFeilEllerVerre())
        assertEquals(0, inspektør.vedtaksperiodeTeller)
        assertEquals(1, inspektør.sykmeldingsperioder().size)
    }

    @Test
    fun `To søknader uten overlapp`() {
        val aktivitetslogg = Aktivitetslogg()
        person.håndterSykmelding(sykmelding(Sykmeldingsperiode(1.januar, 5.januar)), aktivitetslogg)
        person.håndterSykmelding(sykmelding(Sykmeldingsperiode(6.januar, 10.januar)), aktivitetslogg)
        assertFalse(aktivitetslogg.harFunksjonelleFeilEllerVerre())
        assertFalse(aktivitetslogg.harFunksjonelleFeilEllerVerre())
        assertEquals(0, inspektør.vedtaksperiodeTeller)
        assertEquals(2, inspektør.sykmeldingsperioder().size)
    }

    private fun sykmelding(vararg sykeperioder: Sykmeldingsperiode) =
        a1Hendelsefabrikk.lagSykmelding(
            sykeperioder = sykeperioder
        )
}
