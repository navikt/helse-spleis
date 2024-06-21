package no.nav.helse.spleis.mediator.e2e

import java.time.LocalDate
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsperiodeDTO
import no.nav.helse.januar
import no.nav.helse.spleis.mediator.TestMessageFactory
import no.nav.helse.spleis.meldinger.model.SimuleringMessage
import no.nav.inntektsmeldingkontrakt.Periode
import org.junit.jupiter.api.Test

internal class MinimumSykdomsgradVurdertTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `Overstyring av avslag pga minimum sykdomsgrad`() {
        val a1 = "ag1"
        val a2 = "ag2"
        sendNySøknad(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100), orgnummer = a1)
        sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100)),
            orgnummer = a1
        )
        sendInntektsmelding(listOf(Periode(1.januar, 16.januar)), 1.januar, orgnummer = a1, beregnetInntekt = 19000.0)
        sendVilkårsgrunnlag(
            vedtaksperiodeIndeks = 0,
            skjæringstidspunkt = 1.januar,
            orgnummer = a1,
            arbeidsforhold = listOf(
                TestMessageFactory.Arbeidsforhold(a1, LocalDate.EPOCH, null, TestMessageFactory.Arbeidsforhold.Arbeidsforholdtype.ORDINÆRT),
                TestMessageFactory.Arbeidsforhold(a2, LocalDate.EPOCH, null, TestMessageFactory.Arbeidsforhold.Arbeidsforholdtype.ORDINÆRT)
            ),
            inntekterForSykepengegrunnlag = sykepengegrunnlag(
                1.januar, listOf(
                    TestMessageFactory.InntekterForSykepengegrunnlagFraLøsning.Inntekt(19000.0, a1),
                    TestMessageFactory.InntekterForSykepengegrunnlagFraLøsning.Inntekt(81000.0, a2),
                )
            )
        )
        sendYtelser(0, orgnummer = a1)

        assertTilstander(
            0,
            "AVVENTER_INFOTRYGDHISTORIKK",
            "AVVENTER_INNTEKTSMELDING",
            "AVVENTER_BLOKKERENDE_PERIODE",
            "AVVENTER_VILKÅRSPRØVING",
            "AVVENTER_HISTORIKK",
            "AVVENTER_GODKJENNING"
        )

        sendMinimumSykdomdsgradVurdert(listOf(1.januar to 31.januar), emptyList())
        sendYtelser(0, orgnummer = a1)
        sendSimulering(0, orgnummer = a1, status = SimuleringMessage.Simuleringstatus.OK)

        assertTilstander(
            0,
            "AVVENTER_INFOTRYGDHISTORIKK",
            "AVVENTER_INNTEKTSMELDING",
            "AVVENTER_BLOKKERENDE_PERIODE",
            "AVVENTER_VILKÅRSPRØVING",
            "AVVENTER_HISTORIKK",
            "AVVENTER_GODKJENNING",
            "AVVENTER_BLOKKERENDE_PERIODE",
            "AVVENTER_HISTORIKK",
            "AVVENTER_SIMULERING",
            "AVVENTER_GODKJENNING"
        )
    }
}