package no.nav.helse.spleis.e2e.flere_arbeidsgivere

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.februar
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_VV_2
import no.nav.helse.person.tilstandsmaskin.TilstandType
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSOPPLYSNINGER_FOR_ANNEN_ARBEIDSGIVER
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

internal class FlereArbeidsgivereForlengelserTest : AbstractDslTest() {

    @Test
    fun `Tillater forlengelse av flere arbeidsgivere`() {
        val periode = 1.januar(2021) til 31.januar(2021)
        a1 {
            håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive))

        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(periode.start, periode.endInclusive))
        }
        a1 {
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent))
        }
        a2 {
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(periode.start, periode.endInclusive, 100.prosent))
        }
        a1 {
            håndterArbeidsgiveropplysninger(
                arbeidsgiverperioder = listOf(1.januar(2021) til 16.januar(2021)),
                vedtaksperiodeId = 1.vedtaksperiode,
            )

        }
        a2 {

            håndterArbeidsgiveropplysninger(
                arbeidsgiverperioder = listOf(1.januar(2021) til 16.januar(2021)),
                vedtaksperiodeId = 1.vedtaksperiode,
            )
        }
        a1 {
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
            håndterUtbetalt()

        }
        a2 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
            håndterUtbetalt()
        }

        //Forlengelsen starter her
        val forlengelseperiode = 1.februar(2021) til 28.februar(2021)
        a1 {
            håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive))
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(forlengelseperiode.start, forlengelseperiode.endInclusive))
        }
        a1 {
            håndterSøknad(
                Søknad.Søknadsperiode.Sykdom(
                    forlengelseperiode.start,
                    forlengelseperiode.endInclusive,
                    100.prosent
                )
            )
        }
        a2 {
            håndterSøknad(
                Søknad.Søknadsperiode.Sykdom(
                    forlengelseperiode.start,
                    forlengelseperiode.endInclusive,
                    100.prosent
                )
            )
        }

        a1 {
            assertSisteTilstand(2.vedtaksperiode, TilstandType.AVVENTER_HISTORIKK)

        }
        a2 {
            assertSisteTilstand(2.vedtaksperiode, TilstandType.AVVENTER_BLOKKERENDE_PERIODE)

        }
        a1 {
            håndterYtelser(2.vedtaksperiode)
            assertSisteTilstand(2.vedtaksperiode, TilstandType.AVVENTER_SIMULERING)
        }
        a2 {
            assertSisteTilstand(2.vedtaksperiode, TilstandType.AVVENTER_BLOKKERENDE_PERIODE)
        }

        a1 {
            håndterSimulering(2.vedtaksperiode)
            assertSisteTilstand(2.vedtaksperiode, TilstandType.AVVENTER_GODKJENNING)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
            assertSisteTilstand(2.vedtaksperiode, TilstandType.TIL_UTBETALING)
            håndterUtbetalt()
            assertSisteTilstand(2.vedtaksperiode, TilstandType.AVSLUTTET)
        }

        a2 {
            assertSisteTilstand(2.vedtaksperiode, TilstandType.AVVENTER_HISTORIKK)
            håndterYtelser(2.vedtaksperiode)
            assertSisteTilstand(2.vedtaksperiode, TilstandType.AVVENTER_SIMULERING)
            håndterSimulering(2.vedtaksperiode)
            assertSisteTilstand(2.vedtaksperiode, TilstandType.AVVENTER_GODKJENNING)
            håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
            assertSisteTilstand(2.vedtaksperiode, TilstandType.TIL_UTBETALING)
            håndterUtbetalt()
        }
        a1 {
            assertSisteTilstand(2.vedtaksperiode, TilstandType.AVSLUTTET)
        }
        a2 {
            assertSisteTilstand(2.vedtaksperiode, TilstandType.AVSLUTTET)
        }
    }

    @Test
    fun `Ghost forlenger annen arbeidsgiver - skal gå fint`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent))
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), vedtaksperiodeId = 1.vedtaksperiode)
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2)
            assertVarsel(RV_VV_2, 1.vedtaksperiode.filter())

            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }

        a2 {
            håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.februar, 28.februar, 100.prosent))
            håndterArbeidsgiveropplysninger(listOf(1.februar til 16.februar), vedtaksperiodeId = 1.vedtaksperiode)

            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }

        a1 {
            assertSisteTilstand(1.vedtaksperiode, TilstandType.AVSLUTTET)
        }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, TilstandType.AVSLUTTET)
        }
        a1 {
            assertEquals(1.januar, inspektør(a1).skjæringstidspunkt(1.vedtaksperiode))
        }
        a2 {
            assertEquals(1.januar, inspektør(a2).skjæringstidspunkt(1.vedtaksperiode))
        }

        assertSame(
            inspektør(a1).vilkårsgrunnlag(1.vedtaksperiode(a1)),
            inspektør(a2).vilkårsgrunnlag(1.vedtaksperiode(a2))
        )
    }

    @Test
    fun `forlengelse av AvsluttetUtenUtbetaling for flere arbeidsgivere skal ikke gå til AvventerHistorikk uten IM for begge arbeidsgivere`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 12.januar))
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 12.januar))
        }
        a1 {
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 12.januar, 100.prosent))
        }
        a2 {
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 12.januar, 100.prosent))
            håndterInntektsmelding(
                listOf(1.januar til 16.januar),
            )
        }
        a1 {
            håndterSykmelding(Sykmeldingsperiode(13.januar, 31.januar))
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(13.januar, 31.januar))

        }
        a1 {
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(13.januar, 31.januar, 100.prosent))
        }
        a2 {
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(13.januar, 31.januar, 100.prosent))
            assertTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSOPPLYSNINGER_FOR_ANNEN_ARBEIDSGIVER)
        }
    }
}
