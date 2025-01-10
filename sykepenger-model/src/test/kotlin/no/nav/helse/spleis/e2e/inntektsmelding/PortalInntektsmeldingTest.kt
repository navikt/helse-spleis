package no.nav.helse.spleis.e2e.inntektsmelding

import no.nav.helse.Toggle.Companion.PortalinntektsmeldingSomArbeidsgiveropplysninger
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.forlengVedtak
import no.nav.helse.februar
import no.nav.helse.hendelser.inntektsmelding.NAV_NO_SELVBESTEMT
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class PortalInntektsmeldingTest : AbstractDslTest() {

    @Test
    fun `uenige om arbeidsgiverperiode med NAV_NO som avsendersystem gir varsel`() = PortalinntektsmeldingSomArbeidsgiveropplysninger.enable {
        setupLiteGapA2SammeSkjæringstidspunkt()
        a2 {
            håndterInntektsmeldingPortal(listOf(2.januar til 17.januar), vedtaksperiodeId = 2.vedtaksperiode)
            assertInfo("Håndterer ikke arbeidsgiverperiode i AVSLUTTET", 1.vedtaksperiode.filter())
            assertVarsel(Varselkode.RV_IM_24, 2.vedtaksperiode.filter())
            val forespørselFebruar = observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last { it.vedtaksperiodeId == 2.vedtaksperiode }
            assertEquals(0, forespørselFebruar.forespurteOpplysninger.filterIsInstance<PersonObserver.Arbeidsgiverperiode>().size)
            assertEquals(0, forespørselFebruar.forespurteOpplysninger.filterIsInstance<PersonObserver.Inntekt>().size)
            assertEquals(1, forespørselFebruar.forespurteOpplysninger.filterIsInstance<PersonObserver.Refusjon>().size)
        }
    }

    @Test
    fun `tom arbeidsgiverperiode med NAV_NO som avsendersystem gir ikke varsel`() = PortalinntektsmeldingSomArbeidsgiveropplysninger.enable {
        setupLiteGapA2SammeSkjæringstidspunkt()
        a2 {
            håndterInntektsmeldingPortal(emptyList(), vedtaksperiodeId = 2.vedtaksperiode)
            assertVarsler(emptyList(), 2.vedtaksperiode.filter())
        }
    }

    @Test
    fun `tom arbeidsgiverperiode med NAV_NO_SELVBESTEMT som avsendersystem gir ikke varsel`() = PortalinntektsmeldingSomArbeidsgiveropplysninger.enable {
        setupLiteGapA2SammeSkjæringstidspunkt()
        a2 {
            håndterInntektsmeldingPortal(emptyList(), vedtaksperiodeId = 2.vedtaksperiode, avsendersystem = NAV_NO_SELVBESTEMT)
            assertVarsler(emptyList(), 2.vedtaksperiode.filter())
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
