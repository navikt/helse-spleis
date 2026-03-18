package no.nav.helse.spleis.e2e.flere_arbeidsgivere

import java.util.UUID
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.TestPerson
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.assertInntektsgrunnlag
import no.nav.helse.dsl.forlengVedtak
import no.nav.helse.februar
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.person.EventSubscription
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SØ_10
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INFOTRYGDHISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSOPPLYSNINGER_FOR_ANNEN_ARBEIDSGIVER
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_REFUSJONSOPPLYSNINGER_ANNEN_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_SØKNAD_FOR_OVERLAPPENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.tilstandsmaskin.TilstandType.START
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.tilstandsmaskin.TilstandType.TIL_UTBETALING
import no.nav.helse.spleis.e2e.AktivitetsloggFilter
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.spleis.e2e.arbeidsgiveropplysninger.TrengerArbeidsgiveropplysningerTest.Companion.assertEtterspurt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.inspectors.inspektør
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

internal class FlereArbeidsgivereFlytTest : AbstractDslTest() {

    @Test
    fun `ag2 strekkes tilbake før ag1 - ag2 er i utgangspunktet innenfor agp`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 4.januar, 100.prosent))
            håndterSøknad(Sykdom(5.januar, 12.januar, 100.prosent))
            håndterSøknad(Sykdom(13.januar, 26.januar, 100.prosent))
        }
        a2 {
            håndterSøknad(Sykdom(13.januar, 26.januar, 100.prosent))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_AVSLUTTET_UTEN_UTBETALING)
        }
        a1 { håndterInntektsmelding(listOf(1.januar til 16.januar)) }
        a2 { håndterInntektsmelding(listOf(1.januar til 16.januar)) }
        a1 {
            assertEquals(13.januar til 26.januar, inspektør.periode(3.vedtaksperiode))
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(3.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }
        a2 {
            assertEquals(1.januar til 26.januar, inspektør.periode(1.vedtaksperiode))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
        }
    }

    @Test
    fun `En egenmelding på bare en arbeidsgiver`() {
        a1 { håndterSøknad(Sykdom(2.januar, 17.januar, 100.prosent)) }
        a2 { håndterSøknad(Sykdom(2.januar, 17.januar, 100.prosent), egenmeldinger = listOf(1.januar til 1.januar)) }
        a1 { assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING) }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING) }
        a2 { håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar)) }
        a1 {
            håndterInntektsmelding(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING)
        }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE) }
        assertDoesNotThrow { a2 { håndterPåminnelse(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE) } }
    }

    @Test
    fun `ag2 strekkes tilbake før ag1 - ag2 er utenfor agp`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 4.januar, 100.prosent))
            håndterSøknad(Sykdom(5.januar, 12.januar, 100.prosent))
            håndterSøknad(Sykdom(13.januar, 31.januar, 100.prosent))
        }
        a2 {
            håndterSøknad(Sykdom(13.januar, 31.januar, 100.prosent))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        }
        a1 { håndterInntektsmelding(listOf(1.januar til 16.januar)) }
        a2 { håndterInntektsmelding(listOf(1.januar til 16.januar)) }
        a1 {
            assertEquals(13.januar til 31.januar, inspektør.periode(3.vedtaksperiode))
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(3.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }
        a2 {
            assertEquals(januar, inspektør.periode(1.vedtaksperiode))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
        }
    }

    @Test
    fun `En periode i AvventerTidligerEllerOverlappendePerioder for hver arbeidsgiver - kun en periode skal gå videre`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
            håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars))
            håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
            håndterArbeidsgiveropplysninger(listOf(1.mars til 16.mars), vedtaksperiodeId = 2.vedtaksperiode)
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars))
            håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
            håndterArbeidsgiveropplysninger(listOf(1.mars til 16.mars))
        }
        a1 {
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), vedtaksperiodeId = 1.vedtaksperiode)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
        }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE) }
    }

    @Test
    fun `To overlappende vedtaksperioder for forskjellige arbeidsgivere - skal ikke gå videre uten at begge har IM og søknad`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
        }
        a2 { håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar)) }
        a1 {
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
        }
        a1 {
            assertTilstander(
                1.vedtaksperiode,
                START,
                AVVENTER_INFOTRYGDHISTORIKK,
                AVVENTER_INNTEKTSMELDING,
                AVVENTER_SØKNAD_FOR_OVERLAPPENDE_PERIODE,
            )
        }
    }

    @Test
    fun `utbetaling på ag1 reduseres selv om det ikke utbetales noe til ag2`() {
        (a1 og a2).nyeVedtak(januar, inntekt = 40000.månedlig)
        a1 { forlengVedtak(februar) }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(1.mars, 10.mars))
            håndterSøknad(Sykdom(1.mars, 10.mars, 100.prosent))
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        }
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.mars, 10.mars))
            håndterSøknad(Sykdom(1.mars, 10.mars, 100.prosent))
            håndterYtelser(3.vedtaksperiode)
            val utbetalingstidslinje = inspektør.utbetalingstidslinjer(3.vedtaksperiode)
            assertEquals(1080.daglig, utbetalingstidslinje[1.mars].økonomi.inspektør.arbeidsgiverbeløp)
            assertEquals(INGEN, utbetalingstidslinje[1.mars].økonomi.inspektør.personbeløp)
            assertEquals(100, utbetalingstidslinje[1.mars].økonomi.inspektør.totalGrad)
        }
    }

    @Test
    fun `foreldet dag på ag1 påvirker ikke total sykdomsgrad`() {
        a1 { håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), sendtTilNAVEllerArbeidsgiver = 1.mai) }
        a2 { håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent)) }
        a1 { håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar)) }
        a2 { håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar)) }
        a1 {
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            assertVarsel(Varselkode.RV_SØ_2, 1.vedtaksperiode.filter())
            val utbetalingstidslinje = inspektør.utbetalingstidslinjer(1.vedtaksperiode)
            val økonomiInspektør = utbetalingstidslinje[17.januar].økonomi.inspektør
            assertEquals(INGEN, økonomiInspektør.arbeidsgiverbeløp)
            assertEquals(INGEN, økonomiInspektør.personbeløp)
            assertEquals(100, økonomiInspektør.totalGrad)
        }
    }

    @Test
    fun `flere AG - kort periode har gap på arbeidsgivernivå men er sammenhengende på personnivå - kort periode`() {
        (a1 og a2).nyeVedtak(januar, inntekt = INNTEKT)
        a1 { forlengVedtak(februar) }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(1.mars, 10.mars))
            håndterSøknad(Sykdom(1.mars, 10.mars, 100.prosent))
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        }
    }

    @Test
    fun `flere AG - periode har gap på arbeidsgivernivå men er sammenhengende på personnivå - sender feilaktig flere perioder til behandling`() {
        (a1 og a2).nyeVedtak(januar, inntekt = 20000.månedlig)
        a1 { forlengVedtak(februar) }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars))
            håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
            håndterArbeidsgiveropplysninger(listOf(1.mars til 16.mars), beregnetInntekt = 20000.månedlig, vedtaksperiodeId = 2.vedtaksperiode)
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_GODKJENNING)
        }
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars))
            håndterSøknad(Sykdom(1.mars, 31.mars, 38.prosent))
            håndterYtelser(3.vedtaksperiode)
            assertSisteTilstand(3.vedtaksperiode, AVVENTER_SIMULERING)
            assertEquals(0, inspektør.utbetalingstidslinjer(3.vedtaksperiode).inspektør.avvistDagTeller)
        }
        a2 { assertSisteTilstand(2.vedtaksperiode, AVVENTER_REFUSJONSOPPLYSNINGER_ANNEN_PERIODE) }
    }

    @Test
    fun `To overlappende vedtaksperioder med en forlengelse - vedtaksperiode for ag2 dytter vedtaksperiode for ag1 videre`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
        }
        a1 {
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
        }
        a2 {
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
        }
        a1 {
            assertTilstander(
                1.vedtaksperiode,
                START,
                AVVENTER_INFOTRYGDHISTORIKK,
                AVVENTER_INNTEKTSMELDING,
                AVVENTER_SØKNAD_FOR_OVERLAPPENDE_PERIODE,
                AVVENTER_INNTEKTSOPPLYSNINGER_FOR_ANNEN_ARBEIDSGIVER,
                AVVENTER_BLOKKERENDE_PERIODE,
                AVVENTER_VILKÅRSPRØVING,
            )
        }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE) }
    }

    @Test
    fun `drawio -- MANGLER SØKNAD`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars))
        }
        a2 { håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars)) }
        a1 {
            håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
            assertEquals(emptyList<Periode>(), inspektør.sykmeldingsperioder())
        }
        a2 { håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent)) }
        a1 {
            håndterArbeidsgiveropplysninger(listOf(1.mars til 16.mars), vedtaksperiodeId = 1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSOPPLYSNINGER_FOR_ANNEN_ARBEIDSGIVER)
        }
        a2 { håndterArbeidsgiveropplysninger(listOf(1.mars til 16.mars)) }
        a1 { utbetalPeriode(1.vedtaksperiode) }
        a2 { utbetalPeriodeEtterVilkårsprøving(1.vedtaksperiode) }
        a1 { assertSisteTilstand(1.vedtaksperiode, AVSLUTTET) }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVSLUTTET) }
    }

    @Test
    fun `drawio -- ULIK LENGDE PÅ SYKEFRAVÆR`() {
        a1 { håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar)) }
        a2 { håndterSykmelding(Sykmeldingsperiode(5.januar, 5.februar)) }
        a1 { assertEquals(listOf(januar), inspektør.sykmeldingsperioder()) }
        a2 { assertEquals(listOf(5.januar til 5.februar), inspektør.sykmeldingsperioder()) }

        a1 {
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
            assertEquals(emptyList<Periode>(), inspektør.sykmeldingsperioder())
        }
        a2 { assertEquals(listOf(5.januar til 5.februar), inspektør.sykmeldingsperioder()) }

        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
            assertEquals(listOf(februar), inspektør.sykmeldingsperioder())
        }
        a1 {
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_SØKNAD_FOR_OVERLAPPENDE_PERIODE)
        }
        a2 {
            håndterSøknad(Sykdom(5.januar, 5.februar, 100.prosent))
            assertEquals(emptyList<Periode>(), inspektør.sykmeldingsperioder())
        }
        a1 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSOPPLYSNINGER_FOR_ANNEN_ARBEIDSGIVER) }
        a2 {
            håndterArbeidsgiveropplysninger(listOf(5.januar til 20.januar))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_SØKNAD_FOR_OVERLAPPENDE_PERIODE)
        }
        a1 { utbetalPeriode(1.vedtaksperiode) }
        a1 { assertSisteTilstand(1.vedtaksperiode, AVSLUTTET) }

        a2 { håndterSykmelding(Sykmeldingsperiode(6.februar, 26.februar)) }
        a2 { håndterSøknad(Sykdom(6.februar, 26.februar, 100.prosent)) }

        a1 { assertEquals(listOf(februar), inspektør.sykmeldingsperioder()) }
        a2 { assertEquals(emptyList<Periode>(), inspektør.sykmeldingsperioder()) }

        a2 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_SØKNAD_FOR_OVERLAPPENDE_PERIODE) }

        nullstillTilstandsendringer()
        a1 { håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent)) }
        a2 { utbetalPeriodeEtterVilkårsprøving(1.vedtaksperiode) }

        a1 { assertIngenFunksjonelleFeil() }
        a1 { assertTilstander(1.vedtaksperiode, AVSLUTTET) }
        a2 { assertTilstander(1.vedtaksperiode, AVVENTER_SØKNAD_FOR_OVERLAPPENDE_PERIODE, AVVENTER_REFUSJONSOPPLYSNINGER_ANNEN_PERIODE, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET) }
        a1 { assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK) }
        a2 { assertTilstander(2.vedtaksperiode, AVVENTER_SØKNAD_FOR_OVERLAPPENDE_PERIODE, AVVENTER_REFUSJONSOPPLYSNINGER_ANNEN_PERIODE) }
    }

    @Test
    fun `drawio -- BURDE BLOKKERE PGA MANGLENDE IM`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        }
        a1 {
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSOPPLYSNINGER_FOR_ANNEN_ARBEIDSGIVER)
        }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING) }
        a2 {
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }
        a1 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING) }
        a1 { utbetalPeriode(1.vedtaksperiode) }
        a2 { utbetalPeriodeEtterVilkårsprøving(1.vedtaksperiode) }
        a1 { assertSisteTilstand(1.vedtaksperiode, AVSLUTTET) }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVSLUTTET) }
    }

    @Test
    fun `drawio -- PERIODE HOS AG1 STREKKER SEG OVER TO PERIODER HOS AG2 - Må vente på alle IM`() {
        a1 { håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar)) }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(2.januar, 18.januar))
            håndterSykmelding(Sykmeldingsperiode(20.januar, 30.januar))
            håndterSøknad(Sykdom(2.januar, 18.januar, 100.prosent))
            håndterSøknad(Sykdom(20.januar, 30.januar, 100.prosent))
        }
        a1 { håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent)) }
        a1 { håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar)) }
        a2 { håndterArbeidsgiveropplysninger(listOf(2.januar til 17.januar)) }
        a2 { observatør.assertEtterspurt(2.vedtaksperiode, EventSubscription.Refusjon::class) }

        a1 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_REFUSJONSOPPLYSNINGER_ANNEN_PERIODE) }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_REFUSJONSOPPLYSNINGER_ANNEN_PERIODE)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        }
        a2 {
            håndterInntektsmelding(
                listOf(2.januar til 17.januar),
                førsteFraværsdag = 20.januar
            )
        }
        a1 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING) }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }
        a1 { utbetalPeriode(1.vedtaksperiode) }
        a1 { assertSisteTilstand(1.vedtaksperiode, AVSLUTTET) }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }
        a2 { utbetalPeriodeEtterVilkårsprøving(1.vedtaksperiode) }
        a1 { assertSisteTilstand(1.vedtaksperiode, AVSLUTTET) }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK)
        }
        a2 { utbetalPeriodeEtterVilkårsprøving(2.vedtaksperiode) }
        a1 { assertSisteTilstand(1.vedtaksperiode, AVSLUTTET) }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
        }
    }

    @Test
    fun `Skal vente på inntektsmelding på gap-perioder selv om skjæringstidspunktet er det samme`() {
        a1 { håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar)) }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 18.januar))
            håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
            håndterSøknad(Sykdom(1.januar, 18.januar, 100.prosent))
        }
        a1 { håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent)) }
        a2 { håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent)) }
        a1 { håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar)) }
        a2 { håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar)) }
        a2 { observatør.assertEtterspurt(2.vedtaksperiode, EventSubscription.Refusjon::class) }

        a1 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE) }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        }
        a2 {
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a1 {
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }
        a2 { assertSisteTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING) }
    }

    @Test
    fun `drawio -- Må vente på alle IM (forlengelse)`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 17.januar))
            håndterSykmelding(Sykmeldingsperiode(18.januar, 17.februar))
        }
        a2 { håndterSykmelding(Sykmeldingsperiode(18.januar, 17.februar)) }
        a1 {
            håndterSøknad(Sykdom(1.januar, 17.januar, 100.prosent))
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2)
            assertVarsel(Varselkode.RV_VV_2, 1.vedtaksperiode.filter())
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterSøknad(Sykdom(18.januar, 17.februar, 100.prosent))
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_SØKNAD_FOR_OVERLAPPENDE_PERIODE)
        }
        a2 {
            håndterSøknad(Sykdom(18.januar, 17.februar, 100.prosent))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        }
        nullstillTilstandsendringer()
        a2 { håndterArbeidsgiveropplysninger(listOf(18.januar til 2.februar)) }
        a1 {
            assertTilstander(1.vedtaksperiode, AVVENTER_GODKJENNING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE) }
        a1 {
            assertInntektsgrunnlag(1.januar, forventetAntallArbeidsgivere = 2) {
                assertInntektsgrunnlag(a1, INNTEKT)
                assertInntektsgrunnlag(a2, INNTEKT)
            }
        }
    }

    @Test
    fun `drawio -- Må vente på alle IM (gap)`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 22.januar))
            håndterSykmelding(Sykmeldingsperiode(25.januar, 17.februar))
        }
        a2 { håndterSykmelding(Sykmeldingsperiode(25.januar, 17.februar)) }
        a1 {
            håndterSøknad(Sykdom(1.januar, 22.januar, 100.prosent))
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
            håndterSøknad(Sykdom(25.januar, 17.februar, 100.prosent))
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        }
        a2 {
            håndterSøknad(Sykdom(25.januar, 17.februar, 100.prosent))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        }
        a1 { håndterArbeidsgiveropplysninger(arbeidsgiverperioder = emptyList(), vedtaksperiodeId = 2.vedtaksperiode) }
        a2 { håndterArbeidsgiveropplysninger(listOf(25.januar til 9.februar)) }
        a1 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE) }
        a1 {
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            utbetalPeriodeEtterVilkårsprøving(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
            utbetalPeriode(2.vedtaksperiode)
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET)
        }
        a2 {
            utbetalPeriodeEtterVilkårsprøving(1.vedtaksperiode)
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        }
    }

    @Test
    fun `Kort periode skal ikke blokkeres av mangelende søknad`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar))
            håndterSykmelding(Sykmeldingsperiode(21.januar, 31.januar))
            håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent))
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar))
            håndterSykmelding(Sykmeldingsperiode(21.januar, 31.januar))
            håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent))
        }
        a1 { håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar)) }
        a2 { håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar)) }
        a1 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING) }
        a2 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE) }
    }

    @Test
    fun `En arbeidsgiver uten sykdom, blir syk i forlengelsen - skal vente på inntektsmelding`() {
        val periode1 = 1.januar(2021) til 31.januar(2021)
        val periode2 = 1.februar(2021) til 28.februar(2021)
        a1 {
            håndterSykmelding(Sykmeldingsperiode(periode1.start, periode1.endInclusive))
            håndterSøknad(Sykdom(periode1.start, periode1.endInclusive, 100.prosent))
            håndterArbeidsgiveropplysninger(listOf(1.januar(2021) til 16.januar(2021)))
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2)
            assertVarsel(Varselkode.RV_VV_2, 1.vedtaksperiode.filter())
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
            håndterUtbetalt()
            håndterSykmelding(Sykmeldingsperiode(periode2.start, periode2.endInclusive))
            håndterSøknad(Sykdom(periode2.start, periode2.endInclusive, 100.prosent))
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(periode2.start, periode2.endInclusive))
            håndterSøknad(Sykdom(periode2.start, periode2.endInclusive, 100.prosent))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            håndterArbeidsgiveropplysninger(listOf(1.februar(2021) til 16.februar(2021)))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }
    }

    @Test
    fun `En arbeidsgiver uten sykdom, blir syk i forlengelsen - skal vente på inntektsmelding selv om saksbehandler har overstyrt ghostinntekten`() {
        val periode1 = januar
        val periode2 = februar
        a1 {
            håndterSykmelding(Sykmeldingsperiode(periode1.start, periode1.endInclusive))
            håndterSøknad(Sykdom(periode1.start, periode1.endInclusive, 100.prosent))
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2)
            assertVarsel(Varselkode.RV_VV_2, 1.vedtaksperiode.filter())
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterOverstyrInntekt(skjæringstidspunkt = 1.januar, inntekt = 1200.månedlig, organisasjonsnummer = a2)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
            håndterUtbetalt()
            håndterSykmelding(Sykmeldingsperiode(periode2.start, periode2.endInclusive))
            håndterSøknad(Sykdom(periode2.start, periode2.endInclusive, 100.prosent))
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(periode2.start, periode2.endInclusive))
            håndterSøknad(Sykdom(periode2.start, periode2.endInclusive, 100.prosent))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            håndterArbeidsgiveropplysninger(listOf(1.februar til 16.februar))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        }
    }

    @Test
    fun `To arbeidsgivere, skjæringstidspunkt i måneden før ag2, ag2 sin forlengelse skal ikke vente på inntektsmelding etter inntektsmelding er mottatt`() {
        val periode1 = 31.januar til 20.februar
        val periode2 = 1.februar til 17.februar
        val periode3 = 18.februar til 25.februar
        a1 {
            håndterSykmelding(Sykmeldingsperiode(periode1.start, periode1.endInclusive))
            håndterSøknad(Sykdom(periode1.start, periode1.endInclusive, 100.prosent))
            håndterInntektsmelding(listOf(31.januar til 15.februar))
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(periode2.start, periode2.endInclusive))
            håndterSøknad(Sykdom(periode2.start, periode2.endInclusive, 100.prosent))
            håndterInntektsmelding(listOf(1.februar til 16.februar))
            håndterSykmelding(Sykmeldingsperiode(periode3.start, periode3.endInclusive))
            håndterSøknad(Sykdom(periode3.start, periode3.endInclusive, 100.prosent))
        }
        a1 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING) }
        a2 {
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
            assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE)
        }
    }

    @Test
    fun `kort periode hos annen arbeidsgiver skal ikke blokkere videre behandling pga manglende IM`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 16.januar))
            håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent))
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
        }
    }

    @Test
    fun `kort periode hos annen arbeidsgiver vi tidligere har utbetalt til skal ikke blokkere videre behandling pga manglende IM`() {
        a2 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
            utbetalPeriode(1.vedtaksperiode)
            assertVarsel(Varselkode.RV_VV_2, 1.vedtaksperiode.filter())
        }
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars))
            håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(1.mars, 16.mars))
            håndterSøknad(Sykdom(1.mars, 16.mars, 100.prosent))
        }
        a1 {
            håndterArbeidsgiveropplysninger(listOf(1.mars til 16.mars))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
        }
    }

    @Test
    fun `inntektsmelding for arbeidsgiver 2 har ikke full refusjon - kan gå videre til utbetaling`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        }
        a1 {
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                beregnetInntekt = INNTEKT,
                refusjon = Inntektsmelding.Refusjon(INNTEKT, null),
            )
        }
        a2 {
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                beregnetInntekt = INNTEKT,
                refusjon = Inntektsmelding.Refusjon(INGEN, null),
            )
        }
        a1 { assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING) }
    }

    @Test
    fun `gjenopptaBehandling poker ikke neste arbeidsgiver til AvventerHistorikk før den blir kastet ut`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
        }
        a1 {
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        }

        a2 {
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        }
        a1 {
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
        }
        a2 {
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar))
        }
        a1 {
            håndterAnmodningOmForkasting(1.vedtaksperiode, force = true)
            assertForkastetPeriodeTilstander(
                1.vedtaksperiode,
                START,
                AVVENTER_INFOTRYGDHISTORIKK,
                AVVENTER_INNTEKTSMELDING,
                AVVENTER_INNTEKTSOPPLYSNINGER_FOR_ANNEN_ARBEIDSGIVER,
                AVVENTER_BLOKKERENDE_PERIODE,
                AVVENTER_VILKÅRSPRØVING,
                TIL_INFOTRYGD,
            )
        }
        a2 {
            assertForkastetPeriodeTilstander(
                1.vedtaksperiode,
                START,
                AVVENTER_INNTEKTSMELDING,
                AVVENTER_BLOKKERENDE_PERIODE,
                TIL_INFOTRYGD,
            )
        }
    }

    @Test
    fun `bruker har fyllt inn andre inntektskilder i søknad hvor vi har sykmeldingsperioder for begge arbeidsgivere`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), andreInntektskilder = true)
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), andreInntektskilder = true)
        }
        a1 { assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD) }
        a2 { assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD) }
        a1 { assertVarsler(emptyList(), 1.vedtaksperiode.filter()) }
        a2 { assertVarsler(emptyList(), 1.vedtaksperiode.filter()) }
    }

    @Test
    fun `bruker har satt andre inntektskilder men vi kjenner ikke til sykdom for mer enn en arbeidsgiver`() {
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), andreInntektskilder = true)
            assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
            assertFunksjonellFeil(RV_SØ_10, AktivitetsloggFilter.Alle)
        }
    }

    @Test
    fun `Gammel sykemeldingsperiode skal ikke blokkere videre behandling av en senere søknad`() {
        a1 { håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar)) }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(5.februar, 28.februar))
            håndterSøknad(Sykdom(5.februar, 28.februar, 100.prosent))
            håndterArbeidsgiveropplysninger(listOf(5.februar til 20.februar))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
        }
    }

    @Test
    fun `får tidligere sykmelding og søknad for en annen arbeidsgiver`() {
        a1 {
            nyPeriode(februar, a1)
            håndterInntektsmelding(listOf(1.februar til 16.februar), INNTEKT, 1.februar)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
        }
        a2 {
            nyPeriode(januar, a2)
            håndterInntektsmelding(listOf(1.januar til 16.januar), INNTEKT, 1.januar)
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)

            assertVarsel(Varselkode.RV_VV_2, 1.vedtaksperiode.filter())
        }
        a1 {
            assertTilstander(
                1.vedtaksperiode,
                START,
                AVVENTER_INFOTRYGDHISTORIKK,
                AVVENTER_INNTEKTSMELDING,
                AVVENTER_BLOKKERENDE_PERIODE,
                AVVENTER_VILKÅRSPRØVING,
                AVVENTER_HISTORIKK,
                AVVENTER_SIMULERING,
                AVVENTER_GODKJENNING,
                AVVENTER_INNTEKTSOPPLYSNINGER_FOR_ANNEN_ARBEIDSGIVER,
                AVVENTER_BLOKKERENDE_PERIODE,
            )
        }
        a2 {
            assertTilstander(
                1.vedtaksperiode,
                START,
                AVVENTER_INNTEKTSMELDING,
                AVVENTER_BLOKKERENDE_PERIODE,
                AVVENTER_VILKÅRSPRØVING,
                AVVENTER_HISTORIKK,
                AVVENTER_SIMULERING,
                AVVENTER_GODKJENNING,
            )
        }
    }

    @Test
    fun `sykmelding og søknad i forlengelsen til a1 kommer før sykmelding til a2 - skal ikke ha flere perioder i AvventerGodkjenning`() {
        (a2 og a1).nyeVedtak(januar, inntekt = INNTEKT)
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
            håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
            håndterYtelser(2.vedtaksperiode)
            håndterSimulering(2.vedtaksperiode)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_GODKJENNING)
        }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
            håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
            // arbeidsgiveren spleis hører om først er den som blir valgt først til å gå videre i gjenopptaBehandling dersom periodenes tom er lik
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK)
        }
        a1 { assertSisteTilstand(2.vedtaksperiode, AVVENTER_REFUSJONSOPPLYSNINGER_ANNEN_PERIODE) }
    }

    private fun TestPerson.TestArbeidsgiver.utbetalPeriodeEtterVilkårsprøving(vedtaksperiodeId: UUID) {
        håndterYtelser(vedtaksperiodeId)
        håndterSimulering(vedtaksperiodeId)
        håndterUtbetalingsgodkjenning(vedtaksperiodeId)
        håndterUtbetalt()
    }

    private fun TestPerson.TestArbeidsgiver.utbetalPeriode(vedtaksperiodeId: UUID) {
        håndterVilkårsgrunnlagFlereArbeidsgivere(vedtaksperiodeId, a1, a2)
        utbetalPeriodeEtterVilkårsprøving(vedtaksperiodeId)
    }
}
