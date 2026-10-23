package no.nav.helse.spleis.mediator.e2e

import no.nav.helse.februar
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsperiodeDTO
import no.nav.helse.januar
import no.nav.helse.spleis.mediator.TestMessageFactory
import no.nav.helse.spleis.meldinger.model.SimuleringMessage
import no.nav.inntektsmeldingkontrakt.Periode
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class RevurderingseventyrkontraktTest : AbstractEndToEndMediatorTest() {
    @Test
    fun `revurdering starter uten å sende overstyring igangsatt`() {
        sendNySøknad(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100))
        sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 1.januar, tom = 31.januar, sykmeldingsgrad = 100)),
        )
        sendNavNoInntektsmelding(listOf(Periode(fom = 1.januar, tom = 16.januar)))
        sendVilkårsgrunnlag(0)
        sendYtelser(0)
        sendSimulering(0, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(0, true)
        sendUtbetaling(utbetalingOK = true)

        sendNySøknad(SoknadsperiodeDTO(fom = 1.februar, tom = 28.februar, sykmeldingsgrad = 100))
        sendSøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 1.februar, tom = 28.februar, sykmeldingsgrad = 100)),
        )
        sendYtelser(1)
        sendSimulering(1, SimuleringMessage.Simuleringstatus.OK)
        sendUtbetalingsgodkjenning(1, true)
        sendUtbetaling(utbetalingOK = true)

        sendOverstyrArbeidsgiveropplysninger(
            skjæringstidspunkt = 1.januar,
            arbeidsgiveropplysninger =
                listOf(
                    TestMessageFactory.Arbeidsgiveropplysning(
                        organisasjonsnummer = ORGNUMMER,
                        månedligInntekt = 20000.0,
                        forklaring = "forklaring",
                        subsumsjon = null,
                        refusjonsopplysninger =
                            listOf(
                                TestMessageFactory.Refusjonsopplysning(
                                    fom = 1.januar,
                                    tom = null,
                                    beløp = 20000.0,
                                ),
                            ),
                    ),
                ),
        )

        assertTrue(testRapid.inspektør.meldinger("overstyring_igangsatt").isEmpty())
        assertTilstander(
            0,
            "AVVENTER_INFOTRYGDHISTORIKK",
            "AVVENTER_INNTEKTSMELDING",
            "AVVENTER_BLOKKERENDE_PERIODE",
            "AVVENTER_VILKÅRSPRØVING",
            "AVVENTER_HISTORIKK",
            "AVVENTER_SIMULERING",
            "AVVENTER_GODKJENNING",
            "TIL_UTBETALING",
            "AVSLUTTET",
            "AVVENTER_REVURDERING",
            "AVVENTER_HISTORIKK_REVURDERING",
        )
        assertTilstander(
            1,
            "AVVENTER_INNTEKTSMELDING",
            "AVVENTER_BLOKKERENDE_PERIODE",
            "AVVENTER_HISTORIKK",
            "AVVENTER_SIMULERING",
            "AVVENTER_GODKJENNING",
            "TIL_UTBETALING",
            "AVSLUTTET",
            "AVVENTER_REVURDERING",
        )
    }
}
