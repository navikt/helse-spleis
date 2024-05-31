package no.nav.helse.spleis.mediator.e2e

import java.time.LocalDate
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsperiodeDTO
import no.nav.helse.januar
import no.nav.helse.spleis.mediator.TestMessageFactory
import no.nav.helse.spleis.mediator.TestMessageFactory.Arbeidsforhold.Arbeidsforholdtype
import no.nav.helse.spleis.meldinger.model.SimuleringMessage
import no.nav.inntektsmeldingkontrakt.Periode
import org.junit.jupiter.api.Test

internal class FlereArbeidsgivereMediatorTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `overstyring av arbeidsforhold fører til tilstandsendring`() {
        val a1 = "ag1"
        val a2 = "ag2"
        sendNySøknad(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100), orgnummer = a1)
        sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100)),
            orgnummer = a1
        )
        sendInntektsmelding(listOf(Periode(1.januar, 16.januar)), 1.januar, orgnummer = a1)
        sendVilkårsgrunnlag(
            vedtaksperiodeIndeks = 0,
            skjæringstidspunkt = 1.januar,
            orgnummer = a1,
            arbeidsforhold = listOf(
                TestMessageFactory.Arbeidsforhold(a1, LocalDate.EPOCH, null, Arbeidsforholdtype.ORDINÆRT),
                TestMessageFactory.Arbeidsforhold(a2, LocalDate.EPOCH, null, Arbeidsforholdtype.ORDINÆRT)
            ),
            inntekterForSykepengegrunnlag = sykepengegrunnlag(
                1.januar, listOf(
                    TestMessageFactory.InntekterForSykepengegrunnlagFraLøsning.Inntekt(INNTEKT, a1),
                    TestMessageFactory.InntekterForSykepengegrunnlagFraLøsning.Inntekt(1000.0, a2),
                )
            )
        )
        sendYtelser(0, orgnummer = a1)
        sendSimulering(0, orgnummer = a1, status = SimuleringMessage.Simuleringstatus.OK)
        sendOverstyringArbeidsforhold(1.januar, listOf(
            TestMessageFactory.ArbeidsforholdOverstyrt(
            a2,
            true,
            "forklaring"
        )))
        sendYtelser(0, orgnummer = a1)
        sendSimulering(0, orgnummer = a1, status = SimuleringMessage.Simuleringstatus.OK)
        assertTilstander(
            0,
            "AVVENTER_INFOTRYGDHISTORIKK",
            "AVVENTER_INNTEKTSMELDING",
            "AVVENTER_BLOKKERENDE_PERIODE",
            "AVVENTER_VILKÅRSPRØVING",
            "AVVENTER_HISTORIKK",
            "AVVENTER_SIMULERING",
            "AVVENTER_GODKJENNING",
            "AVVENTER_BLOKKERENDE_PERIODE",
            "AVVENTER_HISTORIKK",
            "AVVENTER_SIMULERING",
            "AVVENTER_GODKJENNING"
        )
    }
}
