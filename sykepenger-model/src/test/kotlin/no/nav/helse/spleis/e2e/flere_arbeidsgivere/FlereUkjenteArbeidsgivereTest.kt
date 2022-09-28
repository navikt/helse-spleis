package no.nav.helse.spleis.e2e.flere_arbeidsgivere

import no.nav.helse.februar
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertFunksjonellFeil
import no.nav.helse.spleis.e2e.assertSisteTilstand
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.nyPeriode
import no.nav.helse.spleis.e2e.nyttVedtak
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class FlereUkjenteArbeidsgivereTest : AbstractEndToEndTest() {

    @Test
    fun `én arbeidsgiver blir to - søknad for ag1 først`() {
        nyttVedtak(1.januar, 31.januar, orgnummer = a1)
        nyPeriode(1.februar til 20.februar, a1)
        nyPeriode(1.februar til 20.februar, a2)
        håndterInntektsmelding(listOf(1.februar til 16.februar), orgnummer = a2)
        assertFunksjonellFeil("Minst en arbeidsgiver inngår ikke i sykepengegrunnlaget", 2.vedtaksperiode.filter(a1))
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertTrue(inspektør(a1).periodeErForkastet(2.vedtaksperiode))
        assertTrue(inspektør(a2).periodeErForkastet(1.vedtaksperiode))
    }

    @Test
    fun `én arbeidsgiver blir to - forlenges kun av ny ag`() {
        nyttVedtak(1.januar, 31.januar, orgnummer = a1)
        nyPeriode(1.februar til 20.februar, a2)
        håndterInntektsmelding(listOf(1.februar til 16.februar), orgnummer = a2)
        assertFunksjonellFeil("Minst en arbeidsgiver inngår ikke i sykepengegrunnlaget", 1.vedtaksperiode.filter(a2))
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertTrue(inspektør(a2).periodeErForkastet(1.vedtaksperiode))
    }
}