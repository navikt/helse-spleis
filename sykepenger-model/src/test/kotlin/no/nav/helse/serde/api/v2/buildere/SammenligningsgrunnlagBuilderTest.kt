package no.nav.helse.serde.api.v2.buildere

import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.testhelpers.desember
import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class SammenligningsgrunnlagBuilderTest : AbstractEndToEndTest() {

    private companion object {
        private const val AG1 = "987654321"
        private const val AG2 = "123456789"
    }

    private val grunnlag get() = OppsamletSammenligningsgrunnlagBuilder(person)

    private val primitivInntekt = 40000.0
    private val inntekt = primitivInntekt.månedlig

    @Test
    fun `har sammenligningsgrunnlag for enkel periode`() {
        nyttVedtak(1.januar, 31.januar) {
            lagInntektperioder(fom = 1.januar, inntekt = inntekt)
        }
        assertEquals(480000.0, grunnlag.sammenligningsgrunnlag(ORGNUMMER, 1.januar))
    }

    @Test
    fun `har ikke sammenligningsgrunnlag etter overgang fra Infotrygd`() {
        val skjæringstidspunkt = 1.desember(2017)
        val infotrygdperioder = arrayOf(ArbeidsgiverUtbetalingsperiode(ORGNUMMER, skjæringstidspunkt, 31.desember(2017), 100.prosent, inntekt))
        val inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, skjæringstidspunkt, inntekt, true))

        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
        håndterUtbetalingshistorikk(1.vedtaksperiode, *infotrygdperioder, inntektshistorikk = inntektshistorikk)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertEquals(null, grunnlag.sammenligningsgrunnlag(ORGNUMMER, skjæringstidspunkt))
    }

    @Test
    fun `har sammenligningsgrunnlag for alle arbeidsgivere`() {
        nyeVedtak(1.januar, 31.januar, AG1, AG2) {
            lagInntektperioder(fom = 1.januar, inntekt = 20000.månedlig, orgnummer = AG1)
            lagInntektperioder(fom = 1.januar, inntekt = 20000.månedlig, orgnummer = AG2)
        }

        assertEquals(240000.0, grunnlag.sammenligningsgrunnlag(AG1, 1.januar))
        assertEquals(240000.0, grunnlag.sammenligningsgrunnlag(AG2, 1.januar))
    }
}
