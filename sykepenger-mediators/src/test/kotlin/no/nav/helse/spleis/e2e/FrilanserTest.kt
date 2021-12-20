package no.nav.helse.spleis.e2e

import no.nav.helse.spleis.TestMessageFactory.InntekterForSykepengegrunnlagFraLøsning
import no.nav.helse.spleis.TestMessageFactory.InntekterForSykepengegrunnlagFraLøsning.Arbeidsforhold
import no.nav.helse.testhelpers.januar
import no.nav.inntektsmeldingkontrakt.Periode
import no.nav.syfo.kafka.felles.FravarDTO
import no.nav.syfo.kafka.felles.FravarstypeDTO
import no.nav.syfo.kafka.felles.SoknadsperiodeDTO
import org.junit.jupiter.api.Test
import java.time.YearMonth


internal class FrilanserTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `Person med frilanserinntekt i løpet av de siste 3 månedene sendes til infotrygd`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(
            0,
            listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100)),
            fravær = listOf(FravarDTO(19.januar, 26.januar, FravarstypeDTO.PERMISJON))
        )
        sendInntektsmelding(0, listOf(Periode(fom = 3.januar, tom = 18.januar)), førsteFraværsdag = 3.januar)
        sendYtelser(0)
        sendVilkårsgrunnlag(
            vedtaksperiodeIndeks = 0,
            inntekterForSykepengegrunnlag = (10..12).map {
                InntekterForSykepengegrunnlagFraLøsning(
                    måned = YearMonth.of(2017, it),
                    inntekter = listOf(
                        InntekterForSykepengegrunnlagFraLøsning.Inntekt(INNTEKT, ORGNUMMER)
                    ),
                    arbeidsforhold = listOf(Arbeidsforhold(orgnummer = ORGNUMMER, type = "frilanserOppdragstakerHonorarPersonerMm"))
                )
            }
        )
        assertTilstander(
            0,
            "MOTTATT_SYKMELDING_FERDIG_GAP",
            "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP",
            "AVVENTER_HISTORIKK",
            "AVVENTER_VILKÅRSPRØVING",
            "TIL_INFOTRYGD"
        )
    }
}
