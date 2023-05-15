package no.nav.helse.spleis.mediator.e2e

import no.nav.helse.april
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsperiodeDTO
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.mars
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_4
import org.junit.jupiter.api.Test
import no.nav.inntektsmeldingkontrakt.Periode as IMPeriode

internal class InntektsmeldingReplayTest: AbstractEndToEndMediatorTest() {

    @Test
    fun `Replay av inntektsmelding skal ikke gi varsler på tvers av arbeidsgivere`() {
        nyPeriode(29.mars til 13.april, a1)
        nyPeriode(29.mars til 13.april, a2)

        nyPeriode(14.april til 23.april, a1)
        nyPeriode(14.april til 23.april, a2)

        sendInntektsmelding(listOf(IMPeriode(29.mars, 13.april)), førsteFraværsdag = 29.mars, orgnummer = a1)

        nyPeriode(24.april til 30.april, a1)

        assertTilstand(0, "AVSLUTTET_UTEN_UTBETALING")
        assertTilstand(1, "AVSLUTTET_UTEN_UTBETALING")
        assertTilstand(2, "AVVENTER_BLOKKERENDE_PERIODE")
        assertTilstand(3, "AVVENTER_INNTEKTSMELDING")
        assertTilstand(4, "AVVENTER_BLOKKERENDE_PERIODE")
        assertIngenVarsler()

        // At a2 nå spør om replay gjør at vi også replayer inntektsmeldingen a1 har sendt og a1 får
        // varsel om flere inntektsmeldinger selv om det kun finnes én
        nyPeriode(24.april til 30.april, a2)

        assertTilstand(0, "AVSLUTTET_UTEN_UTBETALING")
        assertIngenVarsler(0)
        assertTilstand(1, "AVSLUTTET_UTEN_UTBETALING")
        assertIngenVarsler(1)
        assertTilstand(2, "AVVENTER_BLOKKERENDE_PERIODE")
        assertIngenVarsler(2)
        assertTilstand(3, "AVVENTER_INNTEKTSMELDING")
        assertIngenVarsler(3)
        assertTilstand(4, "AVVENTER_BLOKKERENDE_PERIODE")
        assertVarsel(4, RV_IM_4)
        assertTilstand(5, "AVVENTER_INNTEKTSMELDING")
        assertIngenVarsler(5)
    }

    private fun nyPeriode(periode: Periode, orgnr: String) {
        sykmelding(periode, orgnr)
        søknad(periode, orgnr)
    }

    private fun sykmelding(periode: Periode, orgnr: String) = sendNySøknad(SoknadsperiodeDTO(fom = periode.start, tom = periode.endInclusive, sykmeldingsgrad = 100), orgnummer = orgnr)
    private fun søknad(periode: Periode, orgnr: String) = sendSøknad(perioder = listOf(SoknadsperiodeDTO(fom = periode.start, tom = periode.endInclusive, sykmeldingsgrad = 100)), orgnummer = orgnr)

    private companion object {
        val a1 = "a1"
        val a2 = "a2"
    }
}