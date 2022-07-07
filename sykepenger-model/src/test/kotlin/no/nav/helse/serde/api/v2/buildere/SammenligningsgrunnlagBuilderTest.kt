package no.nav.helse.serde.api.v2.buildere

import no.nav.helse.desember
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.januar
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.serde.api.speil.builders.OppsamletSammenligningsgrunnlagBuilder
import no.nav.helse.spleis.e2e.*
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SammenligningsgrunnlagBuilderTest : AbstractEndToEndTest() {

    private companion object {
        private val AG1 = "987654321"
        private val AG2 = "123456789"
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
    fun `har sammenligningsgrunnlag for alle arbeidsgivere`() {
        nyeVedtak(1.januar, 31.januar, AG1, AG2) {
            lagInntektperioder(fom = 1.januar, inntekt = 20000.månedlig, orgnummer = AG1)
            lagInntektperioder(fom = 1.januar, inntekt = 20000.månedlig, orgnummer = AG2)
        }

        assertEquals(240000.0, grunnlag.sammenligningsgrunnlag(AG1, 1.januar))
        assertEquals(240000.0, grunnlag.sammenligningsgrunnlag(AG2, 1.januar))
    }
}
