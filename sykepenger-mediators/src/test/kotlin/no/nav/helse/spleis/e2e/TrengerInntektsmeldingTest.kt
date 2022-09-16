package no.nav.helse.spleis.e2e

import java.util.UUID
import no.nav.helse.februar
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsperiodeDTO
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.spleis.meldinger.model.SimuleringMessage
import no.nav.inntektsmeldingkontrakt.Periode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class TrengerInntektsmeldingTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `trenger ikke inntektsmelding for periode innenfor arbeidsgiverperioden`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 1.januar, tom = 14.januar, sykmeldingsgrad = 100))
        sendSøknad(listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 14.januar, sykmeldingsgrad = 100)))
        sendUtbetalingshistorikk(0)
        assertEquals(0, testRapid.inspektør.meldinger("trenger_inntektsmelding").size)
    }

    @Test
    fun `trenger inntektsmelding for periode utenfor arbeidsgiverperioden`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 1.januar, tom = 17.januar, sykmeldingsgrad = 100))
        val søknadId = sendSøknad(listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 17.januar, sykmeldingsgrad = 100)))
        sendUtbetalingshistorikk(0)
        val melding = testRapid.inspektør.siste("trenger_inntektsmelding")
        assertEquals(søknadId, UUID.fromString(melding["søknadIder"].toList().single().asText()))
    }

    @Test
    fun `sender ikke trenger_inntektsmelding hvor inntektsmelding har førsteFraværsdag i perioden, men arbeidsgiverperioden er før`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 17.januar, tom = 16.februar, sykmeldingsgrad = 100))
        sendSøknad(listOf(SoknadsperiodeDTO(fom = 17.januar, tom = 16.februar, sykmeldingsgrad = 100)))
        sendInntektsmelding(listOf(Periode(fom = 1.januar, tom = 16.januar)), førsteFraværsdag = 17.januar)
        sendYtelser(0)
        sendVilkårsgrunnlag(0)
        sendYtelserUtenSykepengehistorikk(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(0, true)
        sendUtbetaling()
        assertTilstander(
            0,
            "AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK",
            "AVVENTER_BLOKKERENDE_PERIODE",
            "AVVENTER_HISTORIKK",
            "AVVENTER_VILKÅRSPRØVING",
            "AVVENTER_HISTORIKK",
            "AVVENTER_SIMULERING",
            "AVVENTER_GODKJENNING",
            "TIL_UTBETALING",
            "AVSLUTTET"
        )

        sendNySøknad(SoknadsperiodeDTO(fom = 1.februar, tom = 28.februar, sykmeldingsgrad = 100))
        sendSøknad(listOf(SoknadsperiodeDTO(fom = 1.februar, tom = 28.februar, sykmeldingsgrad = 100)))

        sendNySøknad(SoknadsperiodeDTO(fom = 28.februar, tom = 16.mars, sykmeldingsgrad = 100))
        sendSøknad(listOf(SoknadsperiodeDTO(fom = 28.februar, tom = 16.mars, sykmeldingsgrad = 100)))
        assertTilstander(1, "TIL_INFOTRYGD")
        assertTilstander(2, "TIL_INFOTRYGD")

        assertEquals(1, testRapid.inspektør.meldinger("trenger_inntektsmelding").size)
        assertEquals(17.januar, testRapid.inspektør.siste("trenger_inntektsmelding")["fom"].asLocalDate())
    }

    @Test
    fun `trenger_inntektsmelding håndterer korrigerende sykmelding som forkorter perioden`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 1.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendSøknad(listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 26.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(listOf(Periode(fom = 1.januar, tom = 16.januar)), førsteFraværsdag = 1.januar)
        sendYtelser(0)
        sendVilkårsgrunnlag(0)
        sendYtelserUtenSykepengehistorikk(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(0)
        sendUtbetaling()

        sendNySøknad(SoknadsperiodeDTO(fom = 2.februar, tom = 12.februar, sykmeldingsgrad = 100))
        sendNySøknad(SoknadsperiodeDTO(fom = 2.februar, tom = 8.februar, sykmeldingsgrad = 100))
        sendSøknad(listOf(SoknadsperiodeDTO(fom = 2.februar, tom = 8.februar, sykmeldingsgrad = 100)))

        assertEquals(2, testRapid.inspektør.meldinger("trenger_inntektsmelding").size)
    }

    @Test
    fun `håndterer trenger_inntektsmelding isolert per arbeidsgiver`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 1.januar, tom = 20.januar, sykmeldingsgrad = 100))
        sendSøknad(listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 20.januar, sykmeldingsgrad = 100)))
        sendInntektsmelding(listOf(Periode(fom = 1.januar, tom = 16.januar)), førsteFraværsdag = 1.januar)

        val annenArbeidsgiver = "999999999"
        sendNySøknad(
            SoknadsperiodeDTO(fom = 1.januar, tom = 20.januar, sykmeldingsgrad = 100),
            orgnummer = annenArbeidsgiver
        )
        sendSøknad(
            listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 20.januar, sykmeldingsgrad = 100)),
            orgnummer = annenArbeidsgiver
        )

        assertEquals(1, testRapid
            .inspektør
            .meldinger("trenger_inntektsmelding")
            .filter { it["organisasjonsnummer"].asText() == ORGNUMMER }
            .size
        )
        assertEquals(1, testRapid
            .inspektør
            .meldinger("trenger_ikke_inntektsmelding")
            .filter { it["organisasjonsnummer"].asText() == ORGNUMMER }
            .size
        )
        assertEquals(1, testRapid
            .inspektør
            .meldinger("trenger_inntektsmelding")
            .filter { it["organisasjonsnummer"].asText() == annenArbeidsgiver }
            .size
        )
        assertEquals(0, testRapid
            .inspektør
            .meldinger("trenger_ikke_inntektsmelding")
            .filter { it["organisasjonsnummer"].asText() == annenArbeidsgiver }
            .size
        )
    }
}
