package no.nav.helse.spleis.e2e.tilkommen_inntekt

import no.nav.helse.Toggle
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.UgyldigeSituasjonerObservatør.Companion.assertUgyldigSituasjon
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.nyttVedtak
import no.nav.helse.februar
import no.nav.helse.hendelser.Søknad.InntektFraNyttArbeidsforhold
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_VV_1
import no.nav.helse.person.beløp.BeløpstidslinjeTest.Companion.assertBeløpstidslinje
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class TilkommenInntektTredjeRakettTest : AbstractDslTest() {

    @Test
    fun `Oppretter vedtaksperiode for tilkommen inntekt og legger til inntekt som inntektsendring på behandlingsendring`() = Toggle.TilkommenInntektV3.enable {
        a1 {
            nyttVedtak(januar)
            assertUgyldigSituasjon("peker på søknaden") { // TODO: tenk litt på dokumentsporing, vi har lovet flex at på sis-topicet tilhører ALDRI én søknad flere vedtaksperioder
                håndterSøknad(
                    februar,
                    inntekterFraNyeArbeidsforhold = listOf(InntektFraNyttArbeidsforhold(1.februar, 28.februar, a2, 1000)),
                )
            }
            assertEquals(2, inspektør.vedtaksperiodeTeller)
            assertTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK)
        }
        a2 {
            assertEquals(1, inspektør.vedtaksperiodeTeller)
            assertTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
            assertBeløpstidslinje(
                inspektør(1.vedtaksperiode).inntektsendringer,
                februar,
                50.daglig
            )
        }
        a1 {
            assertUgyldigSituasjon("peker på søknaden") {
                håndterYtelser(2.vedtaksperiode)
            }
            assertVarsler(2.vedtaksperiode, Varselkode.`Tilkommen inntekt som støttes`)
            assertUtbetalingsbeløp(2.vedtaksperiode, 1382, 1431)
        }
    }

    @Test
    fun `Inntekt skal avkortes av tilkommen inntekt før det 6G justeres`() = Toggle.TilkommenInntektV3.enable {
        a1 {
            nyttVedtak(januar, beregnetInntekt = INNTEKT * 3)
            assertUtbetalingsbeløp(1.vedtaksperiode, forventetArbeidsgiverbeløp = 2161, forventetArbeidsgiverRefusjonsbeløp = 4292, subset = 17.januar til 31.januar)
            assertUgyldigSituasjon("peker på søknaden") {
                håndterSøknad(
                    februar,
                    inntekterFraNyeArbeidsforhold = listOf(InntektFraNyttArbeidsforhold(1.februar, 28.februar, a2, 1000)),
                )
            }
            assertEquals(2, inspektør.vedtaksperiodeTeller)
            assertTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK)
        }
        a2 {
            assertEquals(1, inspektør.vedtaksperiodeTeller)
            assertTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
            assertBeløpstidslinje(
                inspektør(1.vedtaksperiode).inntektsendringer,
                februar,
                50.daglig
            )
        }
        a1 {
            assertUgyldigSituasjon("peker på søknaden") {
                håndterYtelser(2.vedtaksperiode)
            }
            assertVarsler(2.vedtaksperiode, Varselkode.`Tilkommen inntekt som støttes`)
            assertUtbetalingsbeløp(2.vedtaksperiode, forventetArbeidsgiverbeløp = 2136, forventetArbeidsgiverRefusjonsbeløp = 4292)
        }
    }

    @Test
    fun `Tilkommen inntekt på førstegangsbehandling`() = Toggle.TilkommenInntektV3.enable {
        a1 {
            håndterSøknad(januar, inntekterFraNyeArbeidsforhold = listOf(InntektFraNyttArbeidsforhold(fom = 18.januar, tom = 31.januar, orgnummer = a2, 10_000)))
            /** TODO:  Lolzi, den "fangSisteVedtaksperiode"-tingen om skal håndtere dette for oss i testene har jo nå plukket opp a2 sin vedtaksperiode
            Det kan man sikkert fikse der, men det gadd jeg ikke nå */
            assertUgyldigSituasjon("peker på søknaden") { håndterUtbetalingshistorikk(1.vedtaksperiode) }
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            assertUgyldigSituasjon("peker på søknaden") { håndterVilkårsgrunnlag(1.vedtaksperiode) }
            /** TODO: RV_VV_1 skal jo ikke skje da, #noe må gjøres. Nå ligger a2 i inntektsgrunnlaget, og om den deaktiveres hensyntas den ikke ved beregning av utbetalingsitdslinjer, og det er vel egentlig rett, så må vel fikse at hen ikke legger seg i inntektsgrunnlaget
            Men nå ligger hen der med 0,- så det blir jo beregningsmessig rett da (?) **/
            assertVarsler(1.vedtaksperiode, RV_VV_1, Varselkode.`Tilkommen inntekt som støttes`)
            assertUgyldigSituasjon("peker på søknaden") { håndterYtelser(1.vedtaksperiode) }
            assertUgyldigSituasjon("peker på søknaden") { håndterSimulering(1.vedtaksperiode) }
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)


            assertUtbetalingsbeløp(1.vedtaksperiode, 1431, 1431, subset = 17.januar.somPeriode()) // Syk og ingen tilkommen her
            assertUtbetalingsbeløp(1.vedtaksperiode, 842, 1431, subset = 18.januar til 31.januar)

            assertUgyldigSituasjon("peker på søknaden") { håndterUtbetalingsgodkjenning(1.vedtaksperiode) }
            håndterUtbetalt()
        }

        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertBeløpstidslinje(inspektør(1.vedtaksperiode).inntektsendringer, 18.januar til 31.januar, 1000.daglig)
        }
    }
}
