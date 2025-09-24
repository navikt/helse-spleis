package no.nav.helse.spleis.e2e.søknad

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.a1
import no.nav.helse.februar
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Arbeid
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Permisjon
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INFOTRYGDHISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.tilstandsmaskin.TilstandType.START
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_INFOTRYGD
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.utbetalingslinjer.Utbetalingstatus
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SøknadArbeidsgiverE2ETest : AbstractDslTest() {

    @Test
    fun `korrigerer førstegangsbehandling med ferie i arbeidsgiverperioden - søknad mottatt i avventer vilkårsprøving`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(10.januar, 31.januar))
            håndterSøknad(Sykdom(10.januar, 31.januar, 100.prosent))
            håndterInntektsmelding(listOf(15.januar til 30.januar))
            nullstillTilstandsendringer()
            håndterSøknad(Sykdom(10.januar, 31.januar, 100.prosent), Ferie(10.januar, 15.januar))
            (10.januar til 12.januar).forEach { dato ->
                assertEquals(Dag.Arbeidsdag::class, inspektør.sykdomstidslinje[dato]::class) { "$dato er av annen dagtype" }
            }
            (13.januar til 15.januar).forEach { dato ->
                assertEquals(Dag.Feriedag::class, inspektør.sykdomstidslinje[dato]::class) { "$dato er av annen dagtype" }
            }
            assertTilstander(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)

        }
    }

    @Test
    fun `korrigerer førstegangsbehandling med ferie i arbeidsgiverperioden - søknad mottatt i avventer historikk`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(10.januar, 31.januar))
            håndterSøknad(Sykdom(10.januar, 31.januar, 100.prosent))
            håndterInntektsmelding(listOf(15.januar til 30.januar))
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            nullstillTilstandsendringer()
            håndterSøknad(Sykdom(10.januar, 31.januar, 100.prosent), Ferie(10.januar, 15.januar))
            (10.januar til 12.januar).forEach { dato ->
                assertEquals(Dag.Arbeidsdag::class, inspektør.sykdomstidslinje[dato]::class) { "$dato er av annen dagtype" }
            }
            (13.januar til 15.januar).forEach { dato ->
                assertEquals(Dag.Feriedag::class, inspektør.sykdomstidslinje[dato]::class) { "$dato er av annen dagtype" }
            }
            assertTilstander(1.vedtaksperiode, AVVENTER_HISTORIKK, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
        }
    }

    @Test
    fun `korrigerer førstegangsbehandling med ferie i arbeidsgiverperioden - søknad mottatt i avventer simulering`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(10.januar, 31.januar))
            håndterSøknad(Sykdom(10.januar, 31.januar, 100.prosent))
            håndterInntektsmelding(listOf(15.januar til 30.januar))
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            nullstillTilstandsendringer()
            håndterSøknad(Sykdom(10.januar, 31.januar, 100.prosent), Ferie(10.januar, 15.januar))
            (10.januar til 12.januar).forEach { dato ->
                assertEquals(Dag.Arbeidsdag::class, inspektør.sykdomstidslinje[dato]::class) { "$dato er av annen dagtype" }
            }
            (13.januar til 15.januar).forEach { dato ->
                assertEquals(Dag.Feriedag::class, inspektør.sykdomstidslinje[dato]::class) { "$dato er av annen dagtype" }
            }
            assertVarsel(Varselkode.RV_IV_7, 1.vedtaksperiode.filter())
            assertTilstander(1.vedtaksperiode, AVVENTER_SIMULERING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
        }
    }

    @Test
    fun `korrigerer førstegangsbehandling med ferie i arbeidsgiverperioden - søknad mottatt i avventer godkjenning`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(10.januar, 31.januar))
            håndterSøknad(Sykdom(10.januar, 31.januar, 100.prosent))
            håndterInntektsmelding(listOf(15.januar til 30.januar))
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            nullstillTilstandsendringer()
            håndterSøknad(Sykdom(10.januar, 31.januar, 100.prosent), Ferie(10.januar, 15.januar))
            (10.januar til 12.januar).forEach { dato ->
                assertEquals(Dag.Arbeidsdag::class, inspektør.sykdomstidslinje[dato]::class) { "$dato er av annen dagtype" }
            }
            (13.januar til 15.januar).forEach { dato ->
                assertEquals(Dag.Feriedag::class, inspektør.sykdomstidslinje[dato]::class) { "$dato er av annen dagtype" }
            }
            assertVarsel(Varselkode.RV_IV_7, 1.vedtaksperiode.filter())
            assertTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
        }
    }

    @Test
    fun `fatter vedtak for søknad utenfor arbeidsgiverperioden dersom det kun er ferie`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(4.januar, 21.januar))
            håndterSøknad(Sykdom(4.januar, 21.januar, 100.prosent), Ferie(19.januar, 21.januar))
            assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING)
        }
    }

    @Test
    fun `korrigerende søknad på periode i AUU - er fortsatt i AUU`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(4.januar, 21.januar))
            håndterSøknad(Sykdom(4.januar, 21.januar, 100.prosent))
            håndterSøknad(Sykdom(4.januar, 21.januar, 100.prosent), Ferie(19.januar, 21.januar))
            assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING)
        }
    }

    @Test
    fun `venter på IM ved forlengelse utenfor arbeidsgiverperioden dersom det kun er friskmelding - etter utbetaling`() {
        a1 {
            nyttVedtak(1.januar til 23.januar)
            håndterSykmelding(Sykmeldingsperiode(24.januar, 25.januar))
            håndterSøknad(Sykdom(24.januar, 25.januar, 100.prosent), Arbeid(24.januar, 25.januar))
            assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING)
        }
    }

    @Test
    fun `venter på IM ved forlengelse utenfor arbeidsgiverperioden dersom det kun er friskmelding og helg`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(4.januar, 19.januar))
            håndterSøknad(Sykdom(4.januar, 19.januar, 100.prosent))
            håndterSykmelding(Sykmeldingsperiode(20.januar, 25.januar))
            håndterSøknad(Sykdom(20.januar, 25.januar, 100.prosent), Arbeid(22.januar, 25.januar))
            assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING)
        }
    }

    @Test
    fun `venter på IM ved forlengelse utenfor arbeidsgiverperioden dersom det kun er friskmelding`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 16.januar))
            håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent))
            håndterSykmelding(Sykmeldingsperiode(17.januar, 25.januar))
            håndterSøknad(Sykdom(17.januar, 25.januar, 100.prosent), Arbeid(17.januar, 25.januar))
            assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING)
        }
    }

    @Test
    fun `venter på IM ved forlengelse utenfor arbeidsgiverperioden dersom det kun er ferie og friskmelding`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar))
            håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent), Ferie(16.januar, 20.januar))
            håndterSykmelding(Sykmeldingsperiode(21.januar, 25.januar))
            håndterSøknad(Sykdom(21.januar, 25.januar, 100.prosent), Arbeid(21.januar, 25.januar))
            assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING)
            assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING)
        }
    }

    @Test
    fun `fatter vedtak for søknad utenfor arbeidsgiverperioden dersom det kun er helg`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(4.januar, 21.januar))
            håndterSøknad(Sykdom(4.januar, 21.januar, 100.prosent))
            assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING)
        }
    }

    @Test
    fun `fatter vedtak for uferdig forlengelseperiode som bare strekkes inn i helg`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 5.januar))
            håndterSøknad(Sykdom(1.januar, 5.januar, 100.prosent))
            håndterSykmelding(Sykmeldingsperiode(6.januar, 19.januar))
            håndterSykmelding(Sykmeldingsperiode(20.januar, 21.januar))
            håndterSøknad(Sykdom(6.januar, 19.januar, 100.prosent))
            håndterSøknad(Sykdom(20.januar, 21.januar, 100.prosent))
            assertTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            assertTilstand(3.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            håndterArbeidsgiveropplysninger(
                listOf(
                    1.januar til 5.januar,
                    6.januar til 9.januar, // lager et tredagers opphold (10. januar - 12. januar) som forskyver agp
                    13.januar til 19.januar // til å slutte 19. januar. Periode nr 3. forlenger derfor kun helg, og skal også avsluttes uten utbetaling
                ),
                vedtaksperiodeId = 2.vedtaksperiode
            )
            assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
            assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
            assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
        }
    }

    @Test
    fun `avslutter uferdig forlengelseperiode som dekkes av arbeidsgiverperioden etter IM`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 5.januar))
            håndterSøknad(Sykdom(1.januar, 5.januar, 100.prosent))

            håndterSykmelding(Sykmeldingsperiode(6.januar, 19.januar))
            håndterSykmelding(Sykmeldingsperiode(20.januar, 23.januar))
            håndterSøknad(Sykdom(6.januar, 19.januar, 100.prosent))
            håndterSøknad(Sykdom(20.januar, 23.januar, 100.prosent))

            assertTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            assertTilstand(3.vedtaksperiode, AVVENTER_INNTEKTSMELDING)

            håndterArbeidsgiveropplysninger(
                listOf(
                    1.januar til 7.januar, // inntektsmeldingen oppgir nok opphold til at periode nr 3
                    15.januar til 23.januar  // haver innenfor arbeidsgiverperioden likevel
                ),
                vedtaksperiodeId = 2.vedtaksperiode
            )
            assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
            assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
            assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)

        }
    }

    @Test
    fun `fatter vedtak for ferdig forlengelseperiode som dekkes av arbeidsgiverperioden etter IM`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 16.januar))
            håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent))

            håndterSykmelding(Sykmeldingsperiode(17.januar, 21.januar))
            håndterSøknad(Sykdom(17.januar, 21.januar, 100.prosent))
            håndterArbeidsgiveropplysninger(
                listOf(
                    1.januar til 8.januar, // inntektsmeldingen oppgir nok opphold til at periode nr 2
                    12.januar til 19.januar  // haver innenfor arbeidsgiverperioden likevel
                ),
                vedtaksperiodeId = 2.vedtaksperiode
            )
            assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
            assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
        }
    }

    @Test
    fun `litt permisjon`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 7.januar))
            håndterSøknad(Sykdom(1.januar, 7.januar, 100.prosent), Permisjon(2.januar, 7.januar))
            assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
        }
    }

    @Test
    fun `bare ferie - kort periode`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 7.januar))
            håndterSøknad(Sykdom(1.januar, 7.januar, 100.prosent), Ferie(1.januar, 7.januar))
            assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
        }
    }

    @Test
    fun `bare permisjon - kort periode`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 7.januar))
            håndterSøknad(Sykdom(1.januar, 7.januar, 100.prosent), Permisjon(1.januar, 7.januar))
            assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
        }
    }

    @Test
    fun `bare ferie - lang periode`() {
        a1 {
            håndterSykmelding(januar)
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Ferie(1.januar, 31.januar))
            assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
        }
    }

    @Test
    fun `bare ferie - forlengelser`() {
        a1 {
            håndterSykmelding(januar)
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Ferie(1.januar, 31.januar))
            håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
            håndterSøknad(februar)
            håndterInntektsmelding(listOf(1.januar til 16.januar))

            assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
            assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE)
        }
    }

    @Test
    fun `bare ferie - etter periode med bare ferie`() {
        a1 {
            håndterSykmelding(januar)
            håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Ferie(1.januar, 31.januar))
            håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), Ferie(1.februar, 28.februar))
            assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
        }
    }

    @Test
    fun `bare ferie - etter tilbakevennende sykdom`() {
        a1 {
            nyttVedtak(januar)
            håndterSykmelding(Sykmeldingsperiode(5.februar, 28.februar))
            håndterInntektsmelding(
                listOf(1.januar til 16.januar),
                førsteFraværsdag = 5.februar
            )
            håndterSøknad(Sykdom(5.februar, 28.februar, 100.prosent), Ferie(5.februar, 28.februar))

            håndterYtelser(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)

            assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
        }
    }

    @Test
    fun `bare permisjon - lang periode`() {
        a1 {
            håndterSykmelding(januar)
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Permisjon(1.januar, 31.januar))
            assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
        }
    }

    @Test
    fun `starter med ferie`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar))
            håndterSøknad(
                Sykdom(1.januar, 20.januar, 100.prosent),
                Ferie(1.januar, 3.januar),
                Ferie(18.januar, 19.januar)
            )
            assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING)
        }
    }

    @Test
    fun `venter på IM ved søknad innenfor arbeidsgiverperioden fordi arbeid er gjenopptatt og perioden går ut over agp`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar))
            håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent), Arbeid(17.januar, 20.januar))
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
        }
    }

    @Test
    fun `fatter vedtak ved søknad utenfor arbeidsgiverperioden dersom ferie er utenfor`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar))
            håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent), Ferie(17.januar, 20.januar))
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
        }
    }

    @Test
    fun `hensyntar historikk fra infotrygd - får vite om det før IM`() {
        a1 {
            håndterUtbetalingshistorikkEtterInfotrygdendring(
                ArbeidsgiverUtbetalingsperiode(a1, 17.januar, 31.januar)
            )
            håndterSykmelding(Sykmeldingsperiode(3.februar, 18.februar))
            håndterSøknad(Sykdom(3.februar, 18.februar, 100.prosent))
            håndterInntektsmelding(
                listOf(Periode(1.januar, 16.januar)),
                førsteFraværsdag = 3.februar
            )
            assertForkastetPeriodeTilstander(1.vedtaksperiode, START, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `hensyntar historikk fra infotrygd - får vite om det etter IM`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(3.februar, 18.februar))
            håndterInntektsmelding(
                listOf(Periode(1.januar, 16.januar)),
                førsteFraværsdag = 3.februar
            )
            håndterSøknad(Sykdom(3.februar, 18.februar, 100.prosent))
            håndterUtbetalingshistorikkEtterInfotrygdendring(
                ArbeidsgiverUtbetalingsperiode(a1, 17.januar, 31.januar)
            )
            assertVarsler(listOf(Varselkode.RV_IM_3), 1.vedtaksperiode.filter())
            assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
        }
    }

    @Test
    fun `hensyntar historikk fra infotrygd - får vite om det etter IM - flere perioder`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(2.februar, 2.februar))
            håndterInntektsmelding(
                listOf(Periode(1.januar, 16.januar)),
                førsteFraværsdag = 2.februar
            )
            håndterUtbetalingshistorikkEtterInfotrygdendring(
                ArbeidsgiverUtbetalingsperiode(a1, 17.januar, 31.januar)
            )
            håndterSøknad(Sykdom(2.februar, 2.februar, 100.prosent))
            assertForkastetPeriodeTilstander(1.vedtaksperiode, START, TIL_INFOTRYGD)

            nullstillTilstandsendringer()
            håndterSykmelding(Sykmeldingsperiode(6.februar, 6.februar))
            håndterInntektsmelding(
                listOf(Periode(1.januar, 16.januar)),
                førsteFraværsdag = 6.februar
            )
            håndterSøknad(Sykdom(6.februar, 6.februar, 100.prosent))

            assertVarsler(emptyList(), 1.vedtaksperiode.filter())
            assertForkastetPeriodeTilstander(1.vedtaksperiode, TIL_INFOTRYGD)
            assertForkastetPeriodeTilstander(2.vedtaksperiode, START, TIL_INFOTRYGD)
        }
    }

    @Test
    fun `avslutter korte perioder med gap`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 5.januar))
            håndterSøknad(Sykdom(1.januar, 5.januar, 100.prosent))
            håndterSykmelding(Sykmeldingsperiode(9.januar, 12.januar))
            håndterSøknad(Sykdom(9.januar, 12.januar, 100.prosent))
            håndterSykmelding(Sykmeldingsperiode(16.januar, 31.januar))
            håndterSøknad(Sykdom(16.januar, 31.januar, 100.prosent))

            håndterArbeidsgiveropplysninger(
                listOf(
                    1.januar til 5.januar,
                    9.januar til 12.januar,
                    16.januar til 22.januar
                ),
                vedtaksperiodeId = 3.vedtaksperiode
            )
            assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
            assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
            assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
        }
    }

    @Test
    fun `avslutter korte perioder med gap med arbeidsgiversøknad før IM`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 5.januar))
            håndterSykmelding(Sykmeldingsperiode(9.januar, 12.januar))
            håndterSykmelding(Sykmeldingsperiode(16.januar, 31.januar))
            håndterSøknad(Sykdom(1.januar, 5.januar, 100.prosent))
            håndterSøknad(Sykdom(9.januar, 12.januar, 100.prosent))
            håndterSøknad(Sykdom(16.januar, 31.januar, 100.prosent))
            håndterArbeidsgiveropplysninger(
                listOf(
                    1.januar til 5.januar,
                    9.januar til 12.januar,
                    16.januar til 22.januar
                ),
                vedtaksperiodeId = 3.vedtaksperiode
            )
            assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
            assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
            assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
        }
    }

    @Test
    fun `avslutter korte perioder med forlengelse med arbeidsgiversøknad før IM`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 5.januar))
            håndterSykmelding(Sykmeldingsperiode(9.januar, 12.januar))
            håndterSykmelding(Sykmeldingsperiode(13.januar, 31.januar))
            håndterSøknad(Sykdom(1.januar, 5.januar, 100.prosent))
            håndterSøknad(Sykdom(9.januar, 12.januar, 100.prosent))
            håndterSøknad(Sykdom(13.januar, 31.januar, 100.prosent))
            håndterInntektsmelding(
                listOf(1.januar til 5.januar, 9.januar til 19.januar)
            )
            assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
            assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
            assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
        }
    }

    @Test
    fun `avslutter korte perioder med forlengelse med arbeidsgiversøknad etter IM`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 5.januar))
            håndterSykmelding(Sykmeldingsperiode(9.januar, 12.januar))
            håndterSykmelding(Sykmeldingsperiode(13.januar, 31.januar))
            håndterSøknad(Sykdom(1.januar, 5.januar, 100.prosent))
            håndterSøknad(Sykdom(9.januar, 12.januar, 100.prosent))
            håndterSøknad(Sykdom(13.januar, 31.januar, 100.prosent))
            håndterInntektsmelding(
                listOf(
                    1.januar til 5.januar,
                    9.januar til 19.januar
                )
            )
            assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
            assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
            assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
        }
    }

    @Test
    fun `avslutter korte perioder med gap med søknad før IM`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 5.januar))
            håndterSykmelding(Sykmeldingsperiode(9.januar, 12.januar))
            håndterSykmelding(Sykmeldingsperiode(16.januar, 31.januar))
            håndterSøknad(Sykdom(1.januar, 5.januar, 100.prosent))
            håndterSøknad(Sykdom(9.januar, 12.januar, 100.prosent))
            håndterSøknad(Sykdom(16.januar, 31.januar, 100.prosent))
            håndterArbeidsgiveropplysninger(
                listOf(
                    1.januar til 5.januar,
                    9.januar til 12.januar,
                    16.januar til 22.januar
                ),
                vedtaksperiodeId = 3.vedtaksperiode
            )
            assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
            assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
            assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
        }
    }

    @Test
    fun `avslutter korte perioder med gap med søknad etter IM`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 5.januar))
            håndterSykmelding(Sykmeldingsperiode(9.januar, 12.januar))
            håndterSykmelding(Sykmeldingsperiode(16.januar, 31.januar))
            håndterInntektsmelding(
                listOf(
                    1.januar til 5.januar,
                    9.januar til 12.januar,
                    16.januar til 22.januar
                ),
                førsteFraværsdag = 16.januar
            )
            håndterSøknad(Sykdom(1.januar, 5.januar, 100.prosent))
            håndterSøknad(Sykdom(9.januar, 12.januar, 100.prosent))
            håndterSøknad(Sykdom(16.januar, 31.januar, 100.prosent))
            assertEquals(1.januar til 5.januar, inspektør.periode(1.vedtaksperiode))
            assertEquals(6.januar til 12.januar, inspektør.periode(2.vedtaksperiode))
            assertEquals(13.januar til 31.januar, inspektør.periode(3.vedtaksperiode))
            assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
            assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
            assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
        }
    }

    @Test
    fun `avslutter korte perioder med forlengelse`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 5.januar))
            håndterSøknad(Sykdom(1.januar, 5.januar, 100.prosent))

            håndterSykmelding(Sykmeldingsperiode(9.januar, 12.januar))
            håndterSøknad(Sykdom(9.januar, 12.januar, 100.prosent))

            håndterSykmelding(Sykmeldingsperiode(13.januar, 31.januar))
            håndterSøknad(Sykdom(13.januar, 31.januar, 100.prosent))
            håndterInntektsmelding(
                listOf(
                    1.januar til 5.januar,
                    9.januar til 19.januar
                )
            )

            assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
            assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
            assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
        }
    }

    @Test
    fun `korrigerende søknad med gjenopptatt arbeid slik at hele perioden er innenfor arbeidsgiverperioden`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 18.januar))
            håndterSøknad(Sykdom(1.januar, 18.januar, 100.prosent))
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterSøknad(Sykdom(1.januar, 18.januar, 100.prosent), Arbeid(17.januar, 18.januar))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
            assertEquals(Utbetalingstatus.FORKASTET, inspektør.utbetalingtilstand(0))
            assertIngenFunksjonelleFeil(1.vedtaksperiode.filter())
        }
    }
}
