package no.nav.helse.spleis.e2e

import no.nav.helse.etterspurtBehov
import no.nav.helse.hendelser.*
import no.nav.helse.januar
import no.nav.helse.oktober
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.september
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class GodkjenningsbehovTest : AbstractEndToEndTest() {
    @Test
    fun `legger ved orgnummer for arbeidsgiver med kun sammenligningsgrunnlag i utbetalingsbehovet`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurdering = Inntektsvurdering(
                listOf(
                    sammenligningsgrunnlag(a1, 1.januar, INNTEKT.repeat(12)),
                    sammenligningsgrunnlag(a2, 1.oktober(2017), 1000.månedlig.repeat(9))
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                listOf(
                    grunnlag(a1, 1.januar, INNTEKT.repeat(3))
                ),
                arbeidsforhold = emptyList()
            ),
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1, 1.oktober(2017), null),
                Vilkårsgrunnlag.Arbeidsforhold(a2, LocalDate.EPOCH, 30.september(2017))
            ),
            orgnummer = a1
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)

        val orgnummereMedRelevanteArbeidsforhold = hendelselogg.etterspurtBehov<List<String>>(
            vedtaksperiodeId = 1.vedtaksperiode.id(a1),
            behov = Aktivitetslogg.Aktivitet.Behov.Behovtype.Godkjenning,
            felt = "orgnummereMedRelevanteArbeidsforhold"
        )
        assertEquals(listOf(a1, a2), orgnummereMedRelevanteArbeidsforhold)
    }
}
