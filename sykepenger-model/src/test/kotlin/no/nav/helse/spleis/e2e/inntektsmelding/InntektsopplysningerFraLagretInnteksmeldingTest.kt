package no.nav.helse.spleis.e2e.inntektsmelding

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.hendelser.Avsender.ARBEIDSGIVER
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.januar
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_27
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.Kilde
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_VILKÅRSPRØVING
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull

internal class InntektsopplysningerFraLagretInnteksmeldingTest: AbstractDslTest() {

    @Test
    fun `periode i AvventerInntektsmelding`() {
        a1 {
            håndterSøknad(januar)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            assertNull(inspektør.faktaavklartInntekt(1.vedtaksperiode))
            assertEquals(Beløpstidslinje(), inspektør.refusjon(1.vedtaksperiode))

            val inntektssmeldingMeldingsreferanseId = MeldingsreferanseId(UUID.randomUUID())
            val inntektsmeldingMottatt = LocalDateTime.now().minusYears(3)

            håndterInntektsopplysningerFraLagretInnteksmelding(
                vedtaksperiodeId = 1.vedtaksperiode,
                inntekt = INNTEKT,
                refusjon = INNTEKT * 0.8,
                inntektssmeldingMeldingsreferanseId = inntektssmeldingMeldingsreferanseId,
                inntektsmeldingMottatt = inntektsmeldingMottatt
            )
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
            assertVarsler(1.vedtaksperiode, RV_IM_27)

            inspektør.faktaavklartInntekt(1.vedtaksperiode).apply {
                assertEquals(this?.beløp, INNTEKT)
                assertEquals(this?.hendelseId, inntektssmeldingMeldingsreferanseId.id)
            }

            val forventetRefusjonstidslinje = Beløpstidslinje.fra(
                periode = januar,
                beløp = INNTEKT * 0.8,
                kilde = Kilde(inntektssmeldingMeldingsreferanseId, ARBEIDSGIVER, inntektsmeldingMottatt)
            )

            assertEquals(forventetRefusjonstidslinje, inspektør.refusjon(1.vedtaksperiode))

            assertEquals(inntektssmeldingMeldingsreferanseId.id to 1.vedtaksperiode, observatør.inntektsmeldingHåndtert.single())
        }
    }

    @Test
    fun `periode i noe annet enn AvventerInntektsmelding`() {
        a1 {
            nyttVedtak(januar)
            nullstillTilstandsendringer()

            håndterInntektsopplysningerFraLagretInnteksmelding(
                vedtaksperiodeId = 1.vedtaksperiode,
                inntekt = INNTEKT,
                refusjon = INNTEKT
            )

            assertTilstander(1.vedtaksperiode, AVSLUTTET)
            assertInfo("Håndterer ikke inntektsopplysninger fra lagret inntektsmelding i tilstand Avsluttet")
        }
    }
}
