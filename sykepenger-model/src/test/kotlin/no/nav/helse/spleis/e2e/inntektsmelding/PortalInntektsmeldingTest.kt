package no.nav.helse.spleis.e2e.inntektsmelding

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.forlengVedtak
import no.nav.helse.februar
import no.nav.helse.hendelser.inntektsmelding.NAV_NO_SELVBESTEMT
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_3
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import org.junit.jupiter.api.Test

internal class PortalInntektsmeldingTest : AbstractDslTest() {

    @Test
    fun `uenige om arbeidsgiverperiode med NAV_NO som avsendersystem gir varsel`() {
        setupLiteGapA2SammeSkjæringstidspunkt()
        a2 {
            håndterInntektsmeldingPortal(listOf(2.januar til 17.januar), vedtaksperiodeId = 2.vedtaksperiode)
            assertVarsel(RV_IM_3, 2.vedtaksperiode.filter())
        }
        a1 {
            assertVarsel(Varselkode.RV_IM_4, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `tom arbeidsgiverperiode med NAV_NO som avsendersystem gir ikke varsel`() {
        setupLiteGapA2SammeSkjæringstidspunkt()
        a2 {
            håndterInntektsmeldingPortal(emptyList(), vedtaksperiodeId = 2.vedtaksperiode)
            assertIngenVarsel(RV_IM_3, 2.vedtaksperiode.filter())
        }
        a1 {
            assertVarsel(Varselkode.RV_IM_4, 1.vedtaksperiode.filter())
        }
    }

    @Test
    fun `tom arbeidsgiverperiode med NAV_NO_SELVBESTEMT som avsendersystem gir ikke varsel`() {
        setupLiteGapA2SammeSkjæringstidspunkt()
        a2 {
            håndterInntektsmeldingPortal(emptyList(), vedtaksperiodeId = 2.vedtaksperiode, avsendersystem = NAV_NO_SELVBESTEMT)
            assertIngenVarsler(2.vedtaksperiode.filter())
        }
        a1 {
            assertVarsel(Varselkode.RV_IM_4, 1.vedtaksperiode.filter())
        }
    }

    private fun setupLiteGapA2SammeSkjæringstidspunkt() {
        listOf(a1, a2).nyeVedtak(januar)
        a1 { forlengVedtak(februar) }
        a2 {
            håndterSøknad(10.februar til 28.februar)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        }
    }
}
