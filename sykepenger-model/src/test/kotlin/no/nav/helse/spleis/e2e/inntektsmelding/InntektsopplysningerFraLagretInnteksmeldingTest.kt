package no.nav.helse.spleis.e2e.inntektsmelding

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.assertForventetFeil
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.februar
import no.nav.helse.hendelser.Avsender.ARBEIDSGIVER
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_27
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.Kilde
import no.nav.helse.person.tilstandsmaskin.TilstandType
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

    @Test
    fun `annen periode enn den som er til behandling mangler inntektsmelding`() {
        a1 {
            nyttVedtak(januar, ghosts = listOf(a2))
            håndterOverstyrTidslinje((1.januar til 31.januar).map { ManuellOverskrivingDag (it, Dagtype.Svangerskapspengerdag)})
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
            håndterUtbetalt()
            håndterSøknad(februar)


            assertVarsler(1.vedtaksperiode, Varselkode.RV_VV_2, Varselkode.RV_UT_23)

        }

        a2 {
            håndterSøknad(1.januar til 9.februar)
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            assertVarsler(1.vedtaksperiode, Varselkode.RV_IM_4) //Hvorfor får vi varsel når dette er første inntektsmelding på a2????
        }

        a1 {
            håndterInntektsopplysningerFraLagretInnteksmelding(
                vedtaksperiodeId = 2.vedtaksperiode,
                inntekt = INNTEKT,
                refusjon = INNTEKT
            )
            assertVarsler(2.vedtaksperiode, RV_IM_27)
            assertForventetFeil(
                forklaring = "Ettersom det er en annen periode som skal videre først må man påminne den som er neste til behandling",
                nå = {
                    assertSisteTilstand(1.vedtaksperiode, TilstandType.AVVENTER_REVURDERING)
                },
                ønsket = {
                    assertSisteTilstand(1.vedtaksperiode, TilstandType.AVVENTER_HISTORIKK_REVURDERING)
                }
            )
            håndterPåminnelse(1.vedtaksperiode, TilstandType.AVVENTER_REVURDERING, flagg = setOf("ønskerReberegning"))
            assertSisteTilstand(1.vedtaksperiode, TilstandType.AVVENTER_HISTORIKK_REVURDERING)
        }
    }
}
