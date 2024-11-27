package no.nav.helse.spleis.mediator.e2e

import no.nav.helse.februar
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsperiodeDTO
import no.nav.helse.januar
import org.junit.jupiter.api.Test


internal class ArbeidsledigMediatorTest : AbstractEndToEndMediatorTest() {

    @Test
    fun `arbeidsledigsøknad`() {
        sendNySøknadArbeidsledig(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        sendArbeidsledigsøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 3.januar, tom = 26.januar, sykmeldingsgrad = 100))
        )
        assertTilstander(0, "TIL_INFOTRYGD")
    }

    @Test
    fun `overgang til arbeidsledig`() {
        nyttVedtak(1.januar, 31.januar)
        sendNySøknadArbeidsledig(SoknadsperiodeDTO(fom = 1.februar, tom = 26.februar, sykmeldingsgrad = 100), tidligereArbeidsgiverOrgnummer = ORGNUMMER)
        sendArbeidsledigsøknad(
            perioder = listOf(SoknadsperiodeDTO(fom = 1.februar, tom = 26.februar, sykmeldingsgrad = 100)),
            tidligereArbeidsgiverOrgnummer = ORGNUMMER
        )
        assertTilstander(1, "AVVENTER_BLOKKERENDE_PERIODE", "AVVENTER_HISTORIKK")
    }
}
