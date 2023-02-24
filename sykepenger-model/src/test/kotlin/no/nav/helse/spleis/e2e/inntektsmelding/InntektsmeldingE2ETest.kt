package no.nav.helse.spleis.e2e.inntektsmelding

import java.time.YearMonth
import java.util.UUID
import no.nav.helse.Toggle
import no.nav.helse.april
import no.nav.helse.assertForventetFeil
import no.nav.helse.august
import no.nav.helse.desember
import no.nav.helse.februar
import no.nav.helse.hendelser.ArbeidsgiverInntekt
import no.nav.helse.hendelser.Dagtype.Permisjonsdag
import no.nav.helse.hendelser.InntektForSykepengegrunnlag
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Inntektsmelding.Refusjon
import no.nav.helse.hendelser.Inntektsvurdering
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.inspectors.personLogg
import no.nav.helse.januar
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.november
import no.nav.helse.oktober
import no.nav.helse.person.Inntektskilde.EN_ARBEIDSGIVER
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_GJENNOMFØRT_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_INFOTRYGDHISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.TilstandType.TIL_UTBETALING
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_2
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_3
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_4
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_RE_1
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_VT_2
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.person.nullstillTilstandsendringer
import no.nav.helse.september
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.AktivitetsloggFilter
import no.nav.helse.spleis.e2e.assertActivities
import no.nav.helse.spleis.e2e.assertForkastetPeriodeTilstander
import no.nav.helse.spleis.e2e.assertFunksjonellFeil
import no.nav.helse.spleis.e2e.assertHarHendelseIder
import no.nav.helse.spleis.e2e.assertHarIkkeHendelseIder
import no.nav.helse.spleis.e2e.assertInfo
import no.nav.helse.spleis.e2e.assertIngenFunksjonelleFeil
import no.nav.helse.spleis.e2e.assertIngenInfo
import no.nav.helse.spleis.e2e.assertIngenVarsel
import no.nav.helse.spleis.e2e.assertIngenVarsler
import no.nav.helse.spleis.e2e.assertInntektForDato
import no.nav.helse.spleis.e2e.assertInntektshistorikkForDato
import no.nav.helse.spleis.e2e.assertSisteTilstand
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.assertVarsel
import no.nav.helse.spleis.e2e.forlengVedtak
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterInntektsmeldingMedValidering
import no.nav.helse.spleis.e2e.håndterOverstyrInntekt
import no.nav.helse.spleis.e2e.håndterOverstyrTidslinje
import no.nav.helse.spleis.e2e.håndterPåminnelse
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterSøknadMedValidering
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalingshistorikkEtterInfotrygdendring
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.lønnsinntekt
import no.nav.helse.spleis.e2e.nyPeriode
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.spleis.e2e.tilGodkjenning
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.testhelpers.assertNotNull
import no.nav.helse.testhelpers.inntektperioderForSammenligningsgrunnlag
import no.nav.helse.utbetalingslinjer.Oppdrag
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test

internal class InntektsmeldingE2ETest : AbstractEndToEndTest() {

    @Test
    fun `To inntektsmeldinger samarbeider om å strekke en vedtaksperiode`() = Toggle.AuuHåndtererIkkeInntekt.enable {
        val im1Inntekt = INNTEKT
        val im2Inntekt = INNTEKT + 2000.månedlig
        nyPeriode(18.januar til 2.februar)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        nullstillTilstandsendringer()
        // IM1: Denne treffer ikke 18/1 - 2/2 nå
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = im1Inntekt)
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        nullstillTilstandsendringer()
        // IM2: Denne strekker perioden tilbake til 17/1
        håndterInntektsmelding(listOf(17.januar til 30.januar, 31.januar til 2.februar), førsteFraværsdag = 3.februar, beregnetInntekt = im2Inntekt)
        // Nå replayes IM1:
        //      -> Nå overlapper IM1 allikevel og strekker perioden tilbake til 1/1
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
        assertEquals(1.januar til 2.februar, inspektør.periode(1.vedtaksperiode))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        // Legger inntekt fra IM1 til grunn
        assertInntektForDato(im1Inntekt, 1.januar, inspektør)
    }

    @Test
    fun `To inntektsmeldinger krangler om arbeidsgiverperioden`() = Toggle.AuuHåndtererIkkeInntekt.enable {
        val inntektsmelding1 = UUID.randomUUID()
        val inntektsmelding2 = UUID.randomUUID()

        nyPeriode(20.mars.somPeriode())
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVSLUTTET_UTEN_UTBETALING)

        // Inntektsmelding treffer ikke (litt kødden siden den bare er 12 dager..)
        håndterInntektsmelding(listOf(5.mars til 16.mars), id = inntektsmelding1)
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVSLUTTET_UTEN_UTBETALING)
        assertHarIkkeHendelseIder(1.vedtaksperiode, inntektsmelding1)

        // Arbeidsgiver bare kødda, dette er riktig inntektsmelding
        håndterInntektsmelding(listOf(1.mars til 6.mars, 10.mars til 20.mars), førsteFraværsdag = 21.mars, id = inntektsmelding2) {
            // Før replay har vi nå gått til avventer inntektsmelding på bakgrunn av denne inntektsmeldingen
            // MEN, ettersom første fraværsdag er satt til 21.mars "treffer" ikke inntekt & refusjon
            // Så vi trenger en annen inntektsmelding som kan gi inntekt og refusjon for 20.mars som nå skal utbetales
            assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING, AVVENTER_INNTEKTSMELDING)
            assertHarHendelseIder(1.vedtaksperiode, inntektsmelding2)
            assertHarIkkeHendelseIder(1.vedtaksperiode, inntektsmelding1)
            assertEquals(1.mars til 20.mars, inspektør.periode(1.vedtaksperiode))
            assertEquals(listOf(1.mars til 6.mars, 10.mars til 19.mars), inspektør.arbeidsgiverperioder(1.vedtaksperiode))
            val arbeidsgiverperioden = inspektør.arbeidsgiverperioden(1.vedtaksperiode)!!
            assertFalse(arbeidsgiverperioden.erFørsteUtbetalingsdagFørEllerLik(19.mars.somPeriode()))
            assertTrue(arbeidsgiverperioden.erFørsteUtbetalingsdagFørEllerLik(20.mars.somPeriode()))
        }

        // Når vi nå replayer inntektsmeldinger så treffer plutselig inntektsmelding1 ettersom inntektsmelding2 strakk perioden tilbake til 1.mars
        // Men inntektsmelding1 sier nå at arbeidsgiverperioden er noe annet og ingenting skal utbetales alikevel 🤷‍
        assertEquals(5.mars til 20.mars, inspektør.arbeidsgiverperiode(1.vedtaksperiode))
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING, AVVENTER_INNTEKTSMELDING, AVSLUTTET_UTEN_UTBETALING)
    }

    @Test
    fun `Lang og useriøs arbeidsgiverperiode`() {
        nyPeriode(1.januar til 31.januar)
        håndterInntektsmelding(listOf(1.januar til 31.januar))
        assertEquals(1.januar til 16.januar, inspektør.arbeidsgiverperiode(1.vedtaksperiode))
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
    }

    @Test
    fun `Kort og useriøs arbeidsgiverperiode`() {
        nyPeriode(1.januar til 31.januar)
        håndterInntektsmelding(listOf(1.januar til 5.januar))
        assertEquals(1.januar til 16.januar, inspektør.arbeidsgiverperiode(1.vedtaksperiode))
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
    }

    @Test
    fun `Skal ikke bruke inntekt fra gammel inntektsmelding`() {
        håndterInntektsmelding(arbeidsgiverperioder = listOf(1.januar til 16.januar))
        nyPeriode(1.april til 30.april)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
    }

    @Test
    fun `Arbeidsgiver opplyser om feilaktig ny arbeidsgiverperiode som dekker hele perioden som skal utbetales`() {
        nyttVedtak(1.januar, 20.januar, arbeidsgiverperiode = listOf(1.januar til 16.januar))
        assertEquals(1.januar til 16.januar, inspektør.arbeidsgiverperiode(1.vedtaksperiode))
        nyttVedtak(25.januar, 25.januar, arbeidsgiverperiode = listOf(25.januar til 9.februar))
        assertEquals(1.januar til 16.januar, inspektør.arbeidsgiverperiode(2.vedtaksperiode))
    }

    @Test
    fun `to inntektsmeldinger på rappen`() {
        nyPeriode(1.januar til 10.januar)
        nyPeriode(11.januar til 31.januar)
        nullstillTilstandsendringer()
        håndterInntektsmelding(listOf(1.januar til 16.januar), harFlereInntektsmeldinger = true)
        håndterInntektsmelding(listOf(1.januar til 16.januar), harFlereInntektsmeldinger = true)
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        assertVarsel(RV_IM_4, 2.vedtaksperiode.filter())
        assertFunksjonellFeil(RV_VT_2, 2.vedtaksperiode.filter())
        assertTilstander(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK)
    }

    @Test
    fun `bestridelse av sykdom`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 10.januar))
        håndterSøknad(Sykdom(1.januar, 10.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(11.januar, 25.januar))
        håndterSøknad(Sykdom(11.januar, 25.januar, 100.prosent))
        håndterInntektsmelding(emptyList(), 17.januar, begrunnelseForReduksjonEllerIkkeUtbetalt = "BetvilerArbeidsufoerhet")
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(2.vedtaksperiode, TIL_INFOTRYGD)
    }

    @Test
    fun `inntektsmelding med harFlereInntektsmeldinger flagg satt`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), harFlereInntektsmeldinger = true)
        assertVarsel(RV_IM_4)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
    }


    @Test
    fun `to korte perioder og en lang rett etter foreldrepenger - ag sier med rette at det ikke er noen AGP`() {
        håndterSykmelding(Sykmeldingsperiode(29.august(2022), 5.september(2022)))
        håndterSykmelding(Sykmeldingsperiode(6.september(2022), 9.september(2022)))
        håndterSøknad(Sykdom(29.august(2022), 5.september(2022), 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(10.september(2022), 24.september(2022)))
        håndterSøknad(Sykdom(6.september(2022), 9.september(2022), 100.prosent))

        håndterInntektsmelding(emptyList(), 29.august(2022), begrunnelseForReduksjonEllerIkkeUtbetalt = "ManglerOpptjening")

        assertForventetFeil("""
            Sykmeldt etter f.eks foreldrepenger eller i ny stilling betyr at arbeidsgiver ikke er pliktiget til å betale AGP,
             men at NAV dekker sykepenger allerede fra første sykedag
             
             I dag risikerer vi at bruker ikke får utbetalt penger, siden vi sender de korte periodene til AUU,
             og det er ikke sikkert at en saksbehandler ser sakene i det hele tatt""".trimIndent(), nå = {
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        }, ønsket = {
            assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
            assertSisteTilstand(2.vedtaksperiode, TIL_INFOTRYGD)
        })
    }

    @Test
    fun `mange korte perioder som ikke er sykdom`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 1.januar))
        håndterSøknad(Sykdom(1.januar, 1.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(10.januar, 10.januar))
        håndterSøknad(Sykdom(10.januar, 10.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(20.januar, 20.januar))
        håndterSøknad(Sykdom(20.januar, 20.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(30.januar, 30.januar))
        håndterSøknad(Sykdom(30.januar, 30.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(1.februar, 19.februar))
        håndterSøknad(Sykdom(1.februar, 19.februar, 100.prosent))
        håndterInntektsmelding(listOf(1.februar til 16.februar))
        håndterVilkårsgrunnlag(5.vedtaksperiode)
        håndterYtelser(5.vedtaksperiode)

        assertForventetFeil(
            forklaring = "Inntektsmelding forteller _implisitt_ at 1.jan-30.jan er arbeidsdager. Dagene henger igjen som sykedager i modellen." +
                    "De spredte sykedagene er innenfor 16 dager fra hverandre, slik at de teller med i samme arbeidsgiverperiodetelling." +
                    "Dette medfører at vi starter utbetaling tidligere enn det arbeidsgiver har ment å fortelle oss er riktig.",
            nå = {
                assertEquals(Dag.Sykedag::class, inspektør.sykdomstidslinje[1.januar]::class)
                assertEquals(Dag.Sykedag::class, inspektør.sykdomstidslinje[10.januar]::class)
                assertEquals(Dag.SykHelgedag::class, inspektør.sykdomstidslinje[20.januar]::class)
                assertEquals(Dag.Sykedag::class, inspektør.sykdomstidslinje[30.januar]::class)
                assertEquals(13.februar, inspektør.utbetaling(0).inspektør.arbeidsgiverOppdrag.first().inspektør.fom)
            },
            ønsket = {
                assertEquals(1.februar, inspektør.utbetaling(0).inspektør.arbeidsgiverOppdrag.first().inspektør.fom)
                fail("""\_(ツ)_/¯""")
            }
        )
    }

    @Test
    fun `ulik arbeidsgiverperiode - flere arbeidsgivere`() {
        håndterSykmelding(Sykmeldingsperiode(22.januar, 15.februar), orgnummer = a1)
        håndterSøknad(Sykdom(22.januar, 15.februar, 100.prosent), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(22.januar, 15.februar), orgnummer = a2)
        håndterSøknad(Sykdom(22.januar, 15.februar, 100.prosent), orgnummer = a2)

        håndterInntektsmelding(listOf(11.januar til 13.januar, 20.januar til 2.februar), orgnummer = a1)

        assertEquals("UUGR AAAAAGG SSSSSHH SSSSSHH SSSSSHH SSSS", inspektør(a1).sykdomshistorikk.sykdomstidslinje().toShortString())
        håndterInntektsmelding(listOf(16.februar til 3.mars), orgnummer = a2)

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING, orgnummer = a2)

        håndterSykmelding(Sykmeldingsperiode(16.februar, 10.mars), orgnummer = a2)
        håndterSøknad(Sykdom(16.februar, 10.mars, 100.prosent), orgnummer = a2)
        assertEquals("AAAAARR AAAAARR AAAAARR AAAASHH SSSSSHH SSSSSHH SSSSSH", inspektør(a2).sykdomshistorikk.sykdomstidslinje().toShortString())

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, orgnummer = a2)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)

        håndterVilkårsgrunnlag(1.vedtaksperiode,
            orgnummer = a1,
            inntektsvurdering = Inntektsvurdering(
                inntekter = inntektperioderForSammenligningsgrunnlag {
                    1.januar(2017) til 1.desember(2017) inntekter {
                        a1 inntekt INNTEKT
                        a2 inntekt INNTEKT
                    }
                }
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                inntekter = listOf(
                    ArbeidsgiverInntekt(a1, listOf(
                        desember(2017).lønnsinntekt(),
                        november(2017).lønnsinntekt(),
                        oktober(2017).lønnsinntekt()
                    )),
                    ArbeidsgiverInntekt(a2, listOf(
                        desember(2017).lønnsinntekt(),
                        november(2017).lønnsinntekt(),
                        oktober(2017).lønnsinntekt(),
                    )),
                ), arbeidsforhold = emptyList()
            ),
            arbeidsforhold = listOf(
                Vilkårsgrunnlag.Arbeidsforhold(a1, 1.januar(2017), null),
                Vilkårsgrunnlag.Arbeidsforhold(a2, 1.januar(2017), null)
            )
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt()
        håndterPåminnelse(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING, orgnummer = a2)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, orgnummer = a2)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a2)
    }

    @Test
    fun `Periode uten inntekt går ikke videre ved mottatt inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 6.januar))
        håndterSøknad(Sykdom(1.januar, 6.januar, 100.prosent))


        håndterSykmelding(Sykmeldingsperiode(9.januar, 19.januar))
        håndterSøknad(Sykdom(9.januar, 19.januar, 100.prosent))
        håndterInntektsmelding(listOf(9.januar til 19.januar, 23.januar til 27.januar), førsteFraværsdag = 23.januar)

        assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING)

        assertEquals(listOf(1.januar til 6.januar, 9.januar til 18.januar), inspektør.arbeidsgiverperioder(1.vedtaksperiode))
        assertEquals(listOf(1.januar til 6.januar, 9.januar til 18.januar), inspektør.arbeidsgiverperioder(2.vedtaksperiode))
        val arbeidsgiverperioden = inspektør.arbeidsgiverperioden(2.vedtaksperiode)!!
        assertFalse(arbeidsgiverperioden.erFørsteUtbetalingsdagFørEllerLik(18.januar.somPeriode()))
        assertTrue(arbeidsgiverperioden.erFørsteUtbetalingsdagFørEllerLik(19.januar.somPeriode()))

        assertForventetFeil(
            forklaring = "Inntektsmelding forteller _implisitt_ at 1.jan-6.jan er arbeidsdager. Dagene henger igjen som sykedager i modellen",
            nå = {
                assertEquals(Dag.Sykedag::class, inspektør.sykdomstidslinje[1.januar]::class)
                assertEquals(Dag.SykHelgedag::class, inspektør.sykdomstidslinje[6.januar]::class)
            },
            ønsket = {
                fail("""\_(ツ)_/¯""")
            }
        )
    }

    @Test
    fun `Feilutbetaling på grunn av feilberegnet arbeidsgiverperiode`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 6.januar))
        håndterSøknad(Sykdom(1.januar, 6.januar, 100.prosent))


        håndterSykmelding(Sykmeldingsperiode(9.januar, 19.januar))
        håndterSøknad(Sykdom(9.januar, 19.januar, 100.prosent))
        håndterInntektsmelding(listOf(9.januar til 24.januar))
        assertVarsel(RV_IM_3, 2.vedtaksperiode.filter())
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)

        assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVSLUTTET_UTEN_UTBETALING)
        assertForventetFeil(
            forklaring = "Inntektsmelding forteller _implisitt_ at 1.jan-6.jan er arbeidsdager. Dagene henger igjen som sykedager i modellen." +
                    "De spredte sykedagene er innenfor 16 dager fra hverandre, slik at de teller med i samme arbeidsgiverperiodetelling." +
                    "Dette medfører at vi starter utbetaling tidligere enn det arbeidsgiver har ment å fortelle oss er riktig.",
            nå = {
                assertEquals(19.januar, inspektør.utbetaling(0).inspektør.arbeidsgiverOppdrag.first().inspektør.fom)
                assertTilstander(
                    2.vedtaksperiode,
                    START,
                    AVVENTER_INFOTRYGDHISTORIKK,
                    AVVENTER_INNTEKTSMELDING,
                    AVVENTER_BLOKKERENDE_PERIODE,
                    AVVENTER_VILKÅRSPRØVING,
                    AVVENTER_HISTORIKK,
                    AVVENTER_SIMULERING
                )
            },
            ønsket = {
                assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
                fail("""¯\_(ツ)_/¯""")
            }
        )
    }

    @Test
    fun `strekker ikke periode tilbake før første fraværsdag`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 8.januar))
        håndterSøknad(Sykdom(1.januar, 8.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(1.februar, 20.februar))
        håndterSøknad(Sykdom(1.februar, 20.februar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 8.januar, 10.januar til 17.januar), 1.februar)
        assertEquals(1.februar til 20.februar, inspektør.periode(2.vedtaksperiode))
    }

    @Test
    fun `lagrer inntekt én gang`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 8.januar))
        håndterSøknad(Sykdom(1.januar, 8.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(9.januar, 20.januar))
        håndterSøknad(Sykdom(9.januar, 20.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        assertEquals(1, inspektør.inntektInspektør.size)
    }

    @Test
    fun `arbeidsgiverperiode fra inntektsmelding trumfer ferieopplysninger`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), Ferie(1.januar, 5.januar))
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        val utbetaling = inspektør.utbetaling(0).inspektør
        assertTrue((1.januar til 16.januar).all { utbetaling.utbetalingstidslinje[it] is Utbetalingsdag.ArbeidsgiverperiodeDag })
    }

    @Test
    fun `dersom spleis regner arbeidsgiverperioden ulik fra arbeidsgiver lages warning - im først`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(1.januar, 15.januar)))
        assertVarsel(RV_IM_3, 1.vedtaksperiode.filter())
    }

    @Test
    fun `dersom spleis regner arbeidsgiverperioden ulik fra arbeidsgiver lages warning - søknad først`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(2.januar, 15.januar)))
        assertVarsel(RV_IM_3, 1.vedtaksperiode.filter())
    }

    @Test
    fun `vi sammenligner ikke arbeidsgiverperiodeinformasjon dersom inntektsmelding har oppgitt første fraværsdag`() {
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(1.februar, 28.februar, 100.prosent))
        håndterInntektsmeldingMedValidering(
            1.vedtaksperiode,
            listOf(Periode(1.januar, 16.januar)),
            førsteFraværsdag = 1.februar
        )
        assertIngenVarsler(1.vedtaksperiode.filter())
    }

    @Test
    fun `Opphør i refusjon som overlapper med senere periode`() {
        håndterSykmelding(Sykmeldingsperiode(1.november(2020), 20.november(2020)))
        håndterSøknad(Sykdom(1.november(2020), 20.november(2020), 100.prosent))
        håndterInntektsmelding(
            listOf(Periode(1.november(2020), 16.november(2020))),
            førsteFraværsdag = 1.november(2020)
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.november(2019) til 1.oktober(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt()
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
            TIL_UTBETALING,
            AVSLUTTET
        )

        håndterInntektsmelding(
            listOf(Periode(1.november(2020), 16.november(2020))),
            førsteFraværsdag = 1.november(2020), refusjon = Refusjon(INNTEKT, 6.desember(2020), emptyList())
        )

        håndterSykmelding(Sykmeldingsperiode(21.november(2020), 10.desember(2020)))
        håndterSøknad(Sykdom(21.november(2020), 10.desember(2020), 100.prosent))

        håndterYtelser(2.vedtaksperiode)
        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
    }

    @Test
    fun `Opphør i refusjon som ikke overlapper med senere periode fører ikke til at perioden forkastes`() {
        håndterSykmelding(Sykmeldingsperiode(1.november(2020), 20.november(2020)))
        håndterSøknad(Sykdom(1.november(2020), 20.november(2020), 100.prosent))
        håndterInntektsmelding(
            listOf(Periode(1.november(2020), 16.november(2020))),
            førsteFraværsdag = 1.november(2020)
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.november(2019) til 1.oktober(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt()

        håndterSykmelding(Sykmeldingsperiode(25.november(2020), 10.desember(2020)))
        håndterSøknad(Sykdom(25.november(2020), 10.desember(2020), 100.prosent))
        håndterInntektsmelding(
            listOf(Periode(1.november(2020), 16.november(2020))),
            førsteFraværsdag = 1.november(2020), refusjon = Refusjon(INNTEKT, 6.desember(2020), emptyList())
        )

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
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING
        )
    }

    @Test
    fun `Opphør i refusjon som kommer mens førstegangssak er i play kaster perioden`() {
        håndterSykmelding(Sykmeldingsperiode(1.november(2020), 20.november(2020)))
        håndterInntektsmelding(
            listOf(Periode(1.november(2020), 16.november(2020))),
            førsteFraværsdag = 1.november(2020)
        )
        håndterSøknad(Sykdom(1.november(2020), 20.november(2020), 100.prosent))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.november(2019) til 1.oktober(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(1.vedtaksperiode)
        håndterInntektsmelding(
            listOf(Periode(1.november(2020), 16.november(2020))),
            førsteFraværsdag = 1.november(2020), refusjon = Refusjon(INNTEKT, 6.november(2020), emptyList())
        )
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING
        )
        assertVarsel(
            RV_IM_4,
            AktivitetsloggFilter.person()
        )
    }

    @Test
    fun `En periode som opprinnelig var en forlengelse oppdager at den er en gap periode uten utbetaling ved inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(25.november(2020), 30.november(2020)))
        håndterSøknad(Sykdom(25.november(2020), 30.november(2020), 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(1.desember(2020), 7.desember(2020)))
        håndterSøknad(Sykdom(1.desember(2020), 7.desember(2020), 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(8.desember(2020), 14.desember(2020)))
        håndterSøknad(Sykdom(8.desember(2020), 14.desember(2020), 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(15.desember(2020), 3.januar(2021)))
        håndterSøknad(Sykdom(15.desember(2020), 3.januar(2021), 100.prosent))

        håndterInntektsmelding(
            listOf(
                Periode(25.november(2020), 27.november(2020)),
                Periode(1.desember(2020), 7.desember(2020)),
                Periode(10.desember(2020), 10.desember(2020)),
                Periode(15.desember(2020), 19.desember(2020))
            ),
            førsteFraværsdag = 15.desember(2020),
            beregnetInntekt = 30000.månedlig,
            refusjon = Refusjon(30000.månedlig, null, emptyList())
        )
        assertTilstander(
            3.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVSLUTTET_UTEN_UTBETALING
        )
        håndterVilkårsgrunnlag(4.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.desember(2019) til 1.november(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(4.vedtaksperiode)
        assertTilstander(
            4.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
    }

    @Test
    fun `En periode som opprinnelig var en forlengelse oppdager at den er fortsatt en en forlengelse uten utbetaling ved inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(25.november(2020), 30.november(2020)))
        håndterSøknad(Sykdom(25.november(2020), 30.november(2020), 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(1.desember(2020), 7.desember(2020)))
        håndterSøknad(Sykdom(1.desember(2020), 7.desember(2020), 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(8.desember(2020), 14.desember(2020)))
        håndterSøknad(Sykdom(8.desember(2020), 14.desember(2020), 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(15.desember(2020), 3.januar(2021)))
        håndterSøknad(Sykdom(15.desember(2020), 3.januar(2021), 100.prosent))

        håndterInntektsmelding(
            listOf(
                Periode(25.november(2020), 27.november(2020)),
                Periode(1.desember(2020), 7.desember(2020)),
                Periode(8.desember(2020), 10.desember(2020)),
                Periode(15.desember(2020), 17.desember(2020))
            ),
            førsteFraværsdag = 15.desember(2020),
            beregnetInntekt = 30000.månedlig,
            refusjon = Refusjon(30000.månedlig, null, emptyList())
        )
        assertTilstander(
            3.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVSLUTTET_UTEN_UTBETALING
        )
        håndterVilkårsgrunnlag(4.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.desember(2019) til 1.november(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(4.vedtaksperiode)
        assertTilstander(
            4.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
    }

    @Test
    fun `En periode som opprinnelig var en forlengelse oppdager at den er en gap periode med utbetaling ved inntektsmelding`() {
        håndterSykmelding(Sykmeldingsperiode(25.november(2020), 30.november(2020)))
        håndterSøknad(Sykdom(25.november(2020), 30.november(2020), 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(1.desember(2020), 7.desember(2020)))
        håndterSøknad(Sykdom(1.desember(2020), 7.desember(2020), 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(8.desember(2020), 3.januar(2021)))
        håndterSøknad(Sykdom(8.desember(2020), 3.januar(2021), 100.prosent))

        håndterInntektsmelding(
            listOf(
                Periode(25.november(2020), 27.november(2020)),
                Periode(1.desember(2020), 7.desember(2020)),
                Periode(10.desember(2020), 10.desember(2020)),
                Periode(15.desember(2020), 19.desember(2020))
            ),
            førsteFraværsdag = 15.desember(2020),
            beregnetInntekt = 30000.månedlig,
            refusjon = Refusjon(30000.månedlig, null, emptyList())
        )
        håndterVilkårsgrunnlag(3.vedtaksperiode, inntektsvurdering = Inntektsvurdering(
            inntekter = inntektperioderForSammenligningsgrunnlag {
                1.desember(2019) til 1.november(2020) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }
        ))
        håndterYtelser(3.vedtaksperiode)
        assertTilstander(
            3.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING
        )
    }

    @Test
    fun `Replayede inntektsmeldinger påvirker ikke tidligere vedtaksperioder enn den som trigget replay`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))

        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), 1.januar)

        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

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
            TIL_UTBETALING,
            AVSLUTTET
        )

        håndterInntektsmelding(listOf(Periode(1.mars, 16.mars)), 1.mars)
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING
        )
        assertFalse(person.aktivitetslogg.logg(inspektør.vedtaksperioder(1.vedtaksperiode)).harVarslerEllerVerre())
    }

    @Test
    fun `Replay av inntektsmelding skal håndteres av periode som trigget replay og etterfølgende perioder 1`() {
        håndterInntektsmelding(
            listOf(Periode(1.januar, 10.januar), Periode(21.januar, 26.januar)),
            førsteFraværsdag = 21.januar
        )
        håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar))
        håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(21.januar, 31.januar))
        håndterSøknad(Sykdom(21.januar, 31.januar, 100.prosent))
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVSLUTTET_UTEN_UTBETALING
        )

        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING
        )
    }

    @Test
    fun `Replay av inntektsmelding skal håndteres av periode som trigget replay og etterfølgende perioder 2`() {
        håndterInntektsmelding(
            listOf(Periode(1.januar, 10.januar), Periode(21.januar, 26.januar)),
            førsteFraværsdag = 21.januar
        )
        håndterSykmelding(Sykmeldingsperiode(1.januar, 20.januar))
        håndterSykmelding(Sykmeldingsperiode(21.januar, 31.januar))
        håndterSøknad(Sykdom(1.januar, 20.januar, 100.prosent))
        håndterSøknad(Sykdom(21.januar, 31.januar, 100.prosent))

        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVSLUTTET_UTEN_UTBETALING
        )

        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING
        )
    }

    @Test
    fun `Inntektsmelding utvider ikke vedtaksperiode bakover over tidligere utbetalt periode i IT - IM kommer før sykmelding`() {
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), førsteFraværsdag = 3.februar)
        val utbetalinger = ArbeidsgiverUtbetalingsperiode(ORGNUMMER, 17.januar, 21.januar, 100.prosent, 1000.daglig)
        val inntektshistorikk = listOf(Inntektsopplysning(ORGNUMMER, 17.januar, INNTEKT, true))
        håndterUtbetalingshistorikkEtterInfotrygdendring(utbetalinger, inntektshistorikk = inntektshistorikk)
        håndterSykmelding(Sykmeldingsperiode(3.februar, 25.februar))
        håndterSøknad(Sykdom(3.februar, 25.februar, 100.prosent))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        assertEquals(3.februar til 25.februar, inspektør.periode(1.vedtaksperiode))
    }

    @Test
    fun `replay strekker periode tilbake og lager overlapp`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 19.januar))
        håndterSøknad(Sykdom(3.januar, 19.januar, 100.prosent, null))
        håndterInntektsmelding(listOf(3.januar til 18.januar), 3.januar)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterSykmelding(Sykmeldingsperiode(20.januar, 3.februar))
        håndterSøknad(Sykdom(20.januar, 3.februar, 100.prosent, null))
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        håndterSykmelding(Sykmeldingsperiode(7.februar, 7.februar))
        håndterSøknad(Sykdom(7.februar, 7.februar, 100.prosent, null))
        håndterInntektsmelding(listOf(3.januar til 18.januar), 7.februar)
        håndterVilkårsgrunnlag(3.vedtaksperiode)
        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode)
        håndterUtbetalt()

        håndterInntektsmelding(listOf(3.januar til 18.januar), 23.februar)
        håndterSykmelding(Sykmeldingsperiode(23.februar, 25.februar))
        håndterSøknad(Sykdom(23.februar, 25.februar, 100.prosent, null))
        håndterVilkårsgrunnlag(4.vedtaksperiode)
        håndterYtelser(4.vedtaksperiode)
        håndterSimulering(4.vedtaksperiode)

        assertEquals(3.januar til 19.januar, inspektør.periode(1.vedtaksperiode))
        assertEquals(20.januar til 3.februar, inspektør.periode(2.vedtaksperiode))
        assertEquals(7.februar til 7.februar, inspektør.periode(3.vedtaksperiode))
        assertEquals(23.februar til 25.februar, inspektør.periode(4.vedtaksperiode))
    }

    @Test
    fun `Inntektsmelding med error som treffer flere perioder`() {
        håndterSykmelding(Sykmeldingsperiode(29.mars(2021), 31.mars(2021)))
        håndterSykmelding(Sykmeldingsperiode(6.april(2021), 17.april(2021)))
        håndterSøknad(Sykdom(29.mars(2021), 31.mars(2021), 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(18.april(2021), 2.mai(2021)))
        håndterSøknad(Sykdom(6.april(2021), 17.april(2021), 100.prosent))
        håndterSøknad(Sykdom(18.april(2021), 2.mai(2021), 100.prosent))

        håndterInntektsmeldingMedValidering(
            1.vedtaksperiode,
            listOf(Periode(29.mars(2021), 31.mars(2021)), Periode(6.april(2021), 18.april(2021))),
            beregnetInntekt = INGEN,
            refusjon = Refusjon(INNTEKT, null, emptyList())
        )

        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVSLUTTET_UTEN_UTBETALING,
            AVSLUTTET_UTEN_UTBETALING
        )

        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVSLUTTET_UTEN_UTBETALING,
            AVSLUTTET_UTEN_UTBETALING
        )

        assertForkastetPeriodeTilstander(
            3.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            TIL_INFOTRYGD
        )
    }

    @Test
    fun `Inntektsmelding med error som treffer flere perioder uten gap`() {
        håndterSykmelding(Sykmeldingsperiode(29.mars(2021), 31.mars(2021)))
        håndterSykmelding(Sykmeldingsperiode(1.april(2021), 17.april(2021)))
        håndterSøknad(Sykdom(29.mars(2021), 31.mars(2021), 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(18.april(2021), 2.mai(2021)))
        håndterSøknad(Sykdom(1.april(2021), 17.april(2021), 100.prosent))
        håndterSøknad(Sykdom(18.april(2021), 2.mai(2021), 100.prosent))

        håndterInntektsmeldingMedValidering(
            1.vedtaksperiode,
            arbeidsgiverperioder = listOf(29.mars(2021) til 31.mars(2021), 1.april(2021) til 12.april(2021)),
            beregnetInntekt = INGEN,
            refusjon = Refusjon(INNTEKT, null, emptyList())
        )

        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVSLUTTET_UTEN_UTBETALING,
            AVSLUTTET_UTEN_UTBETALING
        )

        assertForkastetPeriodeTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            TIL_INFOTRYGD
        )

        assertForkastetPeriodeTilstander(
            3.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            TIL_INFOTRYGD
        )
    }

    @Test
    fun `Unngår aggresiv håndtering av arbeidsdager før opplyst AGP ved tidligere revurdering uten endring`() {
        nyPeriode(1.januar til 16.januar)
        håndterSykmelding(Sykmeldingsperiode(17.januar, 31.januar))
        håndterSøknad(Sykdom(17.januar, 31.januar, 100.prosent), Ferie(22.januar, 23.januar))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        håndterOverstyrTidslinje((22.januar til 23.januar).map { ManuellOverskrivingDag(it, Permisjonsdag) })
        håndterYtelser(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        nyPeriode(1.februar til 28.februar)
        håndterInntektsmelding(listOf(1.februar til 16.februar))

        assertEquals("SSSSSHH SSSSSHH SSSSSHH PPSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
    }

    @Test
    fun `Unngår aggresiv håndtering av arbeidsdager før opplyst AGP ved pågående revurdering`() {
        nyPeriode(1.januar til 16.januar)
        nyttVedtak(17.januar, 31.januar, arbeidsgiverperiode = listOf(1.januar til 16.januar))

        forlengVedtak(1.februar, 28.februar)
        forlengVedtak(1.mars, 31.mars)
        håndterOverstyrInntekt(INNTEKT + 500.månedlig, skjæringstidspunkt = 1.januar)
        håndterYtelser(4.vedtaksperiode)

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_GJENNOMFØRT_REVURDERING)
        assertSisteTilstand(3.vedtaksperiode, AVVENTER_GJENNOMFØRT_REVURDERING)
        assertSisteTilstand(4.vedtaksperiode, AVVENTER_SIMULERING_REVURDERING)

        håndterInntektsmelding(listOf(1.mars til 16.mars), førsteFraværsdag = 1.mars)

        assertEquals("SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSH", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
    }

    @Test
    fun `Unngår aggresiv håndtering av arbeidsdager før opplyst AGP ved senere utbetalt periode på annen arbeidsgiver`() {
        nyPeriode(1.februar til 16.februar, a1)
        nyttVedtak(1.april, 30.april, orgnummer = a2)
        nullstillTilstandsendringer()

        håndterInntektsmelding(listOf(16.januar til 31.januar), orgnummer = a1)

        assertEquals(1.februar til 16.februar, inspektør(a1).periode(1.vedtaksperiode))
        assertEquals(1.februar, inspektør(a1).skjæringstidspunkt(1.vedtaksperiode))
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
    }

    @Test
    fun `inntektsmelding oppgir arbeidsgiverperiode senere`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 7.januar))
        håndterSøknad(Sykdom(1.januar, 7.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(8.januar, 20.januar))
        håndterSøknad(Sykdom(8.januar, 20.januar, 100.prosent))
        håndterInntektsmelding(listOf(8.januar til 23.januar))
        assertIngenFunksjonelleFeil(2.vedtaksperiode.filter())
        val tidslinje = inspektør.sykdomstidslinje
        assertTrue((1.januar til 7.januar).all { tidslinje[it] is Dag.Arbeidsdag || tidslinje[it] is Dag.FriskHelgedag })
        assertTrue((8.januar til 23.januar).all { tidslinje[it] is Dag.Sykedag || tidslinje[it] is Dag.SykHelgedag || tidslinje[it] is Dag.Arbeidsgiverdag || tidslinje[it] is Dag.ArbeidsgiverHelgedag })
        assertIngenVarsel(
            RV_IM_4,
            1.vedtaksperiode.filter()
        )
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertEquals(8.januar, inspektør.skjæringstidspunkt(2.vedtaksperiode))
    }

    @Test
    fun `Håndterer ikke inntektsmelding to ganger ved replay - hvor vi har en tidligere periode og gap`() {
        // Ved en tidligere periode resettes trimming av inntektsmelding og vi ender med å håndtere samme inntektsmelding flere ganger i en vedtaksperiode
        nyttVedtak(1.januar(2017), 31.januar(2017))

        håndterInntektsmelding(listOf(10.januar til 25.januar), førsteFraværsdag = 10.januar)
        håndterSykmelding(Sykmeldingsperiode(10.januar, 31.januar))
        assertIngenVarsler(2.vedtaksperiode.filter())
        håndterSøknad(Sykdom(10.januar, 31.januar, 100.prosent))
        assertIngenVarsler(2.vedtaksperiode.filter())
        assertIngenVarsler(2.vedtaksperiode.filter())
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        assertIngenVarsler(2.vedtaksperiode.filter())
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        assertIngenVarsler(2.vedtaksperiode.filter())
    }

    @Test
    fun `Håndterer ikke inntektsmelding to ganger ved replay`() {
        // Happy case av testen med navn: Håndterer ikke inntektsmelding to ganger ved replay - hvor vi har en tidligere periode og gap
        håndterInntektsmelding(listOf(10.januar til 25.januar), førsteFraværsdag = 10.januar)
        håndterSykmelding(Sykmeldingsperiode(10.januar, 31.januar))
        håndterSøknad(Sykdom(10.januar, 31.januar, 100.prosent))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertIngenVarsler(1.vedtaksperiode.filter())
    }

    @Test
    fun `Inntekstmelding kommer i feil rekkefølge - riktig inntektsmelding skal bli valgt i vilkårgrunnlaget`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(5.februar, 28.februar))
        håndterSøknad(Sykdom(5.februar, 28.februar, 100.prosent))

        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            førsteFraværsdag = 5.februar,
            beregnetInntekt = 42000.månedlig
        )
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 1.januar, beregnetInntekt = INNTEKT)

        håndterVilkårsgrunnlag(1.vedtaksperiode)

        val vilkårsgrunnlag = inspektør(a1).vilkårsgrunnlag(1.vedtaksperiode)?.inspektør ?: fail { "finner ikke vilkårsgrunnlag" }
        val sykepengegrunnlagInspektør = vilkårsgrunnlag.sykepengegrunnlag.inspektør
        val sammenligningsgrunnlagInspektør = vilkårsgrunnlag.sammenligningsgrunnlag.inspektør

        assertEquals(EN_ARBEIDSGIVER, sykepengegrunnlagInspektør.inntektskilde)
        assertEquals(EN_ARBEIDSGIVER, inspektør(a1).inntektskilde(1.vedtaksperiode))
        assertEquals(0, vilkårsgrunnlag.avviksprosent?.roundToInt())
        assertEquals(1, sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
        sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
            assertEquals(INNTEKT, it.inntektsopplysning.inspektør.beløp)
            assertEquals(no.nav.helse.person.inntekt.Inntektsmelding::class, it.inntektsopplysning::class)
        }
        assertEquals(1, sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)
        sammenligningsgrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
            assertEquals(31000.månedlig, it.rapportertInntekt)
        }
    }

    @Test
    fun `Ber ikke om ny IM hvis det bare er helg mellom to perioder`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 26.januar))
        håndterSøknad(Sykdom(1.januar, 26.januar, 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), førsteFraværsdag = 1.januar)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterSykmelding(Sykmeldingsperiode(29.januar, 31.januar))
        håndterSøknad(Sykdom(29.januar, 31.januar, 100.prosent))

        assertFalse(29.januar til 31.januar in observatør.manglendeInntektsmeldingVedtaksperioder.map { it.fom til it.tom })
    }

    @Test
    fun `legger ved inntektsmeldingId på vedtaksperiode_endret-event for forlengende vedtaksperioder`() {
        val inntektsmeldingId = UUID.randomUUID()
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), id = inntektsmeldingId)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))

        observatør.hendelseider(1.vedtaksperiode.id(ORGNUMMER)).contains(inntektsmeldingId)
        observatør.hendelseider(2.vedtaksperiode.id(ORGNUMMER)).contains(inntektsmeldingId)
    }

    @Test
    fun `legger ved inntektsmeldingId på vedtaksperiode_endret-event for første etterfølgende av en kort periode`() {
        val inntektsmeldingId = UUID.randomUUID()
        håndterSykmelding(Sykmeldingsperiode(1.januar, 10.januar))
        håndterSykmelding(Sykmeldingsperiode(11.januar, 31.januar))
        håndterSøknad(Sykdom(1.januar, 10.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), id = inntektsmeldingId)
        håndterSøknad(Sykdom(11.januar, 31.januar, 100.prosent))

        observatør.hendelseider(1.vedtaksperiode.id(ORGNUMMER)).contains(inntektsmeldingId)
        observatør.hendelseider(2.vedtaksperiode.id(ORGNUMMER)).contains(inntektsmeldingId)
    }

    @Test
    fun `Avventer inntektsmelding venter faktisk på inntektsmelding, går ikke videre selv om senere periode avsluttes`() {
        håndterSykmelding(Sykmeldingsperiode(28.oktober, 8.november))
        håndterSøknad(Sykdom(28.oktober, 8.november, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(9.november, 22.november))
        håndterSøknad(Sykdom(9.november, 22.november, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(10.desember, 14.desember))
        håndterSøknad(Sykdom(10.desember, 14.desember, 100.prosent))

        assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
        )
        assertTilstander(3.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVSLUTTET_UTEN_UTBETALING)
    }

    @Test
    fun `Inntektsmelding treffer periode som dekker hele arbeidsgiverperioden`() {
        håndterSykmelding(Sykmeldingsperiode(28.oktober, 8.november))
        håndterSøknad(Sykdom(28.oktober, 8.november, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(9.november, 22.november))
        håndterSøknad(Sykdom(9.november, 22.november, 100.prosent))
        håndterInntektsmelding(listOf(Periode(27.oktober, 8.november)), 27.oktober)

        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVSLUTTET_UTEN_UTBETALING,
            AVSLUTTET_UTEN_UTBETALING
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING
        )
        assertEquals(27.oktober til 8.november, inspektør.periode(1.vedtaksperiode))
        assertTrue(inspektør.sykdomstidslinje[27.oktober] is Dag.ArbeidsgiverHelgedag)
    }

    @Test
    fun `vilkårsvurdering med flere arbeidsgivere skal ikke medføre at vi går til avventer historikk fra mottatt sykmelding ferdig forlengelse uten IM`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar), orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 16.januar), orgnummer = a2)
        håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterInntektsmelding(listOf(1.januar til 16.januar), orgnummer = a1)
        val skjæringstidspunkt = inspektør(a1).skjæringstidspunkt(1.vedtaksperiode)
        val inntektsvurdering = Inntektsvurdering(inntektperioderForSammenligningsgrunnlag {
            skjæringstidspunkt.minusMonths(12L).withDayOfMonth(1) til skjæringstidspunkt.minusMonths(1L)
                .withDayOfMonth(1) inntekter {
                a1 inntekt INNTEKT
                a2 inntekt INNTEKT
            }
        })
        val inntektsvurderingForSykepengegrunnlag =
            InntektForSykepengegrunnlag(inntekter = listOf(a1, a2).map { arbeidsgiver ->
                ArbeidsgiverInntekt(arbeidsgiver, (0..2).map {
                    val yearMonth = YearMonth.from(skjæringstidspunkt).minusMonths(3L - it)
                    ArbeidsgiverInntekt.MånedligInntekt(
                        yearMonth = yearMonth,
                        type = ArbeidsgiverInntekt.MånedligInntekt.Inntekttype.LØNNSINNTEKT,
                        inntekt = INNTEKT,
                        fordel = "fordel",
                        beskrivelse = "beskrivelse"
                    )
                })
            }, arbeidsforhold = emptyList())
        håndterVilkårsgrunnlag(
            1.vedtaksperiode,
            inntektsvurdering = inntektsvurdering,
            inntektsvurderingForSykepengegrunnlag = inntektsvurderingForSykepengegrunnlag,
            orgnummer = a1
        )
        håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        nullstillTilstandsendringer()
        håndterSykmelding(Sykmeldingsperiode(17.januar, 31.januar), orgnummer = a2)
        håndterSøknad(Sykdom(17.januar, 31.januar, 100.prosent), orgnummer = a2)

        assertTilstander(1.vedtaksperiode, AVVENTER_SIMULERING, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, orgnummer = a2)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, orgnummer = a2)
        assertForventetFeil(
            forklaring = "Fordi vi allerede har vilkårsprøvd skjæringstidspunktet mener vi at vi har " +
                    "'nødvendig inntekt for vilkårsprøving' for alle arbeidsgiverne, slik at periode 1 hos ag1 går derfor videre til utbetaling." +
                    "Ideelt sett skulle vi her ha forkastet vilkårsgrunnlaget siden det 1) ikke er benyttet enda, og 2) vi har fått inntekt for arbeidsgiveren vi trodde var ghost.",
            nå = {
                assertNotNull(inspektør(a1).vilkårsgrunnlag(1.vedtaksperiode))
            },
            ønsket = {
                assertNull(inspektør(a1).vilkårsgrunnlag(1.vedtaksperiode))
            }
        )
    }

    @Test
    fun `inntektsmelding uten relevant inntekt (fordi perioden er i agp) flytter perioden til ferdig-tilstand`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 5.januar))
        håndterSøknad(Sykdom(1.januar, 5.januar, 100.prosent), orgnummer = ORGNUMMER)

        håndterSykmelding(Sykmeldingsperiode(9.januar, 10.januar))
        håndterSøknad(Sykdom(9.januar, 10.januar, 100.prosent), orgnummer = ORGNUMMER)

        håndterSykmelding(Sykmeldingsperiode(12.januar, 24.januar))
        håndterSøknad(Sykdom(12.januar, 24.januar, 100.prosent))

        håndterInntektsmelding(
            listOf(
                Periode(1.januar, 5.januar),
                Periode(9.januar, 10.januar),
                Periode(12.januar, 20.januar)
            ), 12.januar
        )
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(3.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING)
    }

    @Test
    fun `Ny inntektsmelding som treffer AvventerGodkjenning - hensyntar nye refusjonsopplysninger`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), refusjon = Refusjon(INNTEKT, null, emptyList()))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        assertFalse(inspektør.utbetaling(0).inspektør.personOppdrag.harUtbetalinger())
        assertEquals(17.januar til 31.januar, Oppdrag.periode(inspektør.utbetaling(0).inspektør.arbeidsgiverOppdrag))

        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), refusjon = Refusjon(INGEN, null, emptyList()))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

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
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
        )
        assertVarsel(
            RV_IM_4,
            AktivitetsloggFilter.person()
        )
        assertTrue(inspektør.utbetaling(1).inspektør.personOppdrag.harUtbetalinger())
        assertEquals(17.januar til 31.januar, Oppdrag.periode(inspektør.utbetaling(1).inspektør.personOppdrag))
    }

    @Test
    fun `Ny inntektsmelding som treffer AvventerGodkjenning - hensyntar ny inntekt`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), beregnetInntekt = INNTEKT)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), beregnetInntekt = INNTEKT + 1000.månedlig)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

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
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
        )
        assertVarsel(
            RV_IM_4,
            AktivitetsloggFilter.person()
        )

        assertInntektForDato(INNTEKT + 1000.månedlig, 1.januar, inspektør = inspektør)

    }

    @Test
    fun `Opphør av naturalytelser kaster periode til infotrygd`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), 1.januar, harOpphørAvNaturalytelser = true)

        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            TIL_INFOTRYGD
        )
    }

    @Test
    fun `inntektsmelding med oppgitt første fraværsdag treffer midt i en periode`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 16.januar))
        håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(30.januar, 12.februar))
        håndterSøknad(Sykdom(30.januar, 12.februar, 100.prosent))

        håndterInntektsmelding(listOf(Periode(1.januar, 16.januar)), 1.februar)
        assertFalse(inspektør.sykdomstidslinje[30.januar] is Dag.Arbeidsdag)
        assertFalse(inspektør.sykdomstidslinje[31.januar] is Dag.Arbeidsdag)
        assertInntektshistorikkForDato(INNTEKT, 30.januar, inspektør = inspektør)
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()
        assertVarsel(RV_RE_1)
        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }

    @Test
    fun `inntektsmelding oppgir første fraværsdag i en periode med ferie etter sykdom`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), Ferie(1.februar, 11.februar))
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 12.februar)
        val tidslinje = inspektør.sykdomstidslinje
        assertTrue((17.januar til 31.januar).all { tidslinje[it] is Dag.Sykedag || tidslinje[it] is Dag.SykHelgedag })
        assertFalse((1.februar til 11.februar).all { tidslinje[it] is Dag.Arbeidsdag || tidslinje[it] is Dag.FriskHelgedag })
        assertTrue((12.februar til 28.februar).all { tidslinje[it] is Dag.Sykedag || tidslinje[it] is Dag.SykHelgedag })
        assertIngenVarsel(
            RV_IM_4,
            1.vedtaksperiode.filter()
        )
        assertIngenVarsel(RV_IM_2, 1.vedtaksperiode.filter())
        assertVarsel(
            RV_IM_4,
            2.vedtaksperiode.filter()
        )
        assertVarsel(RV_IM_2, 2.vedtaksperiode.filter())
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
    }

    @Test
    fun `inntektsmelding oppgir første fraværsdag i en periode med ferie etter sykdom med kort periode først`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 16.januar))
        håndterSykmelding(Sykmeldingsperiode(17.januar, 31.januar))
        håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent))
        håndterSøknad(Sykdom(17.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), Ferie(1.februar, 11.februar))
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 12.februar)

        val tidslinje = inspektør.sykdomstidslinje
        assertTrue((17.januar til 31.januar).all { tidslinje[it] is Dag.Sykedag || tidslinje[it] is Dag.SykHelgedag })
        assertFalse((1.februar til 11.februar).all { tidslinje[it] is Dag.Arbeidsdag || tidslinje[it] is Dag.FriskHelgedag })
        assertTrue((12.februar til 28.februar).all { tidslinje[it] is Dag.Sykedag || tidslinje[it] is Dag.SykHelgedag })
        assertIngenVarsel(
            RV_IM_4,
            1.vedtaksperiode.filter()
        )
        assertIngenVarsel(
            RV_IM_4,
            2.vedtaksperiode.filter()
        )
        assertIngenVarsel(
            RV_IM_2,
            2.vedtaksperiode.filter()
        )
        assertVarsel(
            RV_IM_4,
            3.vedtaksperiode.filter()
        )
        assertVarsel(
            RV_IM_2,
            3.vedtaksperiode.filter()
        )
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
        assertSisteTilstand(3.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
    }

    @Test
    fun `Går ikke videre fra AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK hvis forrige periode ikke er ferdig behandlet`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(20.februar, 28.februar))
        håndterSøknad(Sykdom(20.februar, 28.februar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(2.januar, 1.februar))
        håndterSøknad(Sykdom(2.januar, 1.februar, 100.prosent))

        assertForkastetPeriodeTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            TIL_INFOTRYGD
        )
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVSLUTTET_UTEN_UTBETALING)
        assertForkastetPeriodeTilstander(3.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, TIL_INFOTRYGD)
    }

    @Test
    fun `Går videre fra AVVENTER_UFERDIG hvis en gammel periode er i AVSLUTTET_UTEN_UTBETALING`() {
        håndterSykmelding(Sykmeldingsperiode(20.november(2017), 12.desember(2017)))
        håndterSøknad(Sykdom(20.november(2017), 12.desember(2017), 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(1.januar, 12.januar))
        håndterSøknad(Sykdom(1.januar, 12.januar, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(20.februar, 28.februar))
        håndterSøknad(Sykdom(20.februar, 28.februar, 100.prosent))
        håndterInntektsmelding(listOf(Periode(20.februar, 8.mars)), 20.februar)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))

        håndterSykmelding(Sykmeldingsperiode(20.november(2017), 13.desember(2017)))
        håndterSøknad(Sykdom(20.november(2017), 13.desember(2017), 100.prosent))

        assertForkastetPeriodeTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, TIL_INFOTRYGD)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVSLUTTET_UTEN_UTBETALING)
        assertTilstander(3.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVSLUTTET_UTEN_UTBETALING, AVSLUTTET_UTEN_UTBETALING)
        assertForkastetPeriodeTilstander(4.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, TIL_INFOTRYGD)
    }

    @Test
    fun `første fraværsdato fra inntektsmelding er ulik utregnet første fraværsdato`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar))
        håndterSøknad(Sykdom(3.januar, 26.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)), 4.januar)
        assertVarsel(
            RV_IM_2,
            1.vedtaksperiode.filter()
        )
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING
        )
    }

    @Test
    fun `første fraværsdato fra inntektsmelding er ulik utregnet første fraværsdato for påfølgende perioder`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar))
        håndterSykmelding(Sykmeldingsperiode(27.januar, 7.februar))
        håndterSøknad(Sykdom(3.januar, 26.januar, 100.prosent))
        håndterSøknad(Sykdom(27.januar, 7.februar, 100.prosent))
        håndterInntektsmeldingMedValidering(
            1.vedtaksperiode,
            listOf(Periode(3.januar, 18.januar)),
            3.januar
        )
        assertFalse(person.personLogg.harVarslerEllerVerre())
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE
        )
    }

    @Test
    fun `første fraværsdato i inntektsmelding er utenfor perioden`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 26.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)), 27.januar)
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING
        )
    }

    @Test
    fun `første fraværsdato i inntektsmelding, før søknad, er utenfor perioden`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)), 27.januar)
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 26.januar, 100.prosent))
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING
        )
    }

    @Test
    fun `Inntektsmelding opplyser om endret arbeidsgiverperiode`() {
        nyttVedtak(2.januar, 31.januar)
        nyPeriode(12.februar til 28.februar)

        assertEquals(2.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
        Assertions.assertNotNull(inspektør.vilkårsgrunnlag(1.vedtaksperiode))

        håndterInntektsmelding(arbeidsgiverperioder = listOf(1.januar til 16.januar), førsteFraværsdag = 12.februar)

        assertEquals(2.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
        Assertions.assertNotNull(inspektør.vilkårsgrunnlag(1.vedtaksperiode))
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        Assertions.assertNotNull(inspektør.vilkårsgrunnlag(2.vedtaksperiode))
        håndterYtelser(2.vedtaksperiode)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_SIMULERING)
    }

    @Test
    fun `To tilstøtende perioder inntektsmelding først`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 7.januar))
        håndterSykmelding(Sykmeldingsperiode(8.januar, 23.februar))
        håndterInntektsmelding(listOf(Periode(3.januar, 18.januar)))
        håndterSøknad(Sykdom(3.januar, 7.januar, 100.prosent))
        håndterSøknad(Sykdom(8.januar, 23.februar, 100.prosent))
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)

        assertIngenFunksjonelleFeil(1.vedtaksperiode.filter())
        assertIngenFunksjonelleFeil(2.vedtaksperiode.filter())
        assertActivities(person)
        assertNotNull(inspektør.sisteMaksdato(2.vedtaksperiode))
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVSLUTTET_UTEN_UTBETALING
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }

    @Test
    fun `To tilstøtende perioder, inntektsmelding 2 med arbeidsdager i starten`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 7.januar))
        håndterSøknad(Sykdom(3.januar, 7.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(8.januar, 23.februar))
        håndterSøknad(Sykdom(8.januar, 23.februar, 100.prosent))
        håndterInntektsmelding(
            listOf(Periode(3.januar, 7.januar), Periode(15.januar, 20.januar), Periode(23.januar, 28.januar))
        )

        assertIngenFunksjonelleFeil()
        assertActivities(person)
        assertTilstander(
            1.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVSLUTTET_UTEN_UTBETALING,
            AVSLUTTET_UTEN_UTBETALING
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING
        )
    }

    @Test
    fun `ignorer inntektsmeldinger på påfølgende perioder`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 26.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)

        håndterSykmelding(Sykmeldingsperiode(29.januar, 23.februar))
        håndterSøknadMedValidering(2.vedtaksperiode, Sykdom(29.januar, 23.februar, 100.prosent))
        håndterInntektsmeldingMedValidering(2.vedtaksperiode, listOf(Periode(3.januar, 18.januar)))
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)

        assertIngenFunksjonelleFeil()
        assertActivities(person)
        assertNotNull(inspektør.sisteMaksdato(1.vedtaksperiode))
        assertNotNull(inspektør.sisteMaksdato(2.vedtaksperiode))
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
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }

    @Test
    fun `Inntektsmelding vil ikke utvide vedtaksperiode til tidligere vedtaksperiode`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 26.januar))
        håndterSøknad(Sykdom(3.januar, 26.januar, 100.prosent))
        håndterInntektsmelding(listOf(3.januar til 18.januar), 3.januar)
        assertIngenFunksjonelleFeil()
        assertIngenVarsler()
        håndterVilkårsgrunnlag(1.vedtaksperiode, INNTEKT)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 23.februar))
        håndterSøknad(Sykdom(1.februar, 23.februar, 100.prosent))
        håndterInntektsmelding(listOf(3.januar til 18.januar), førsteFraværsdag = 1.februar) // Touches prior periode
        assertIngenFunksjonelleFeil()

        håndterVilkårsgrunnlag(2.vedtaksperiode, INNTEKT)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, true)
        håndterUtbetalt(Oppdragstatus.AKSEPTERT)
        assertIngenFunksjonelleFeil()

        assertNotNull(inspektør.sisteMaksdato(1.vedtaksperiode))
        assertNotNull(inspektør.sisteMaksdato(2.vedtaksperiode))
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
            TIL_UTBETALING,
            AVSLUTTET
        )
        assertTilstander(
            2.vedtaksperiode,
            START,
            AVVENTER_INFOTRYGDHISTORIKK,
            AVVENTER_INNTEKTSMELDING,
            AVVENTER_BLOKKERENDE_PERIODE,
            AVVENTER_VILKÅRSPRØVING,
            AVVENTER_HISTORIKK,
            AVVENTER_SIMULERING,
            AVVENTER_GODKJENNING,
            TIL_UTBETALING,
            AVSLUTTET
        )
    }

    @Test
    fun `inntektsmelding oppgir ny arbeidsgiverperiode i en sammenhengende periode`() {
        nyttVedtak(1.januar, 31.januar)
        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), Ferie(1.februar, 28.februar))
        håndterYtelser(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)

        håndterSykmelding(Sykmeldingsperiode(1.mars, 31.mars))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
        håndterInntektsmelding(listOf(27.februar til 14.mars))
        // Siden vi tidligere fylte ut 2. vedtaksperiode med arbeidsdager ville vi regne ut et ekstra skjæringstidspunkt i den sammenhengende perioden
        assertEquals(listOf(1.januar), person.skjæringstidspunkter())
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING)
        håndterYtelser(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)

        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode)

        assertIngenVarsel(
            RV_IM_4,
            1.vedtaksperiode.filter()
        )
        assertIngenVarsel(
            RV_IM_2,
            2.vedtaksperiode.filter()
        )
        assertVarsel(
            RV_IM_4,
            3.vedtaksperiode.filter()
        )
        assertEquals(1.januar til 31.mars, inspektør.utbetalinger.last().inspektør.periode)
    }

    @Test
    fun `vedtaksperiode i AVSLUTTET_UTEN_UTBETALING burde utvides ved replay av inntektsmelding`() {
        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 1.januar)
        håndterSykmelding(Sykmeldingsperiode(4.januar, 10.januar))
        håndterSøknad(Sykdom(4.januar, 10.januar, 100.prosent))
        håndterSykmelding(Sykmeldingsperiode(11.januar, 31.januar))
        håndterSøknad(Sykdom(11.januar, 31.januar, 100.prosent))

        assertEquals(1.januar til 10.januar, inspektør.vedtaksperioder(1.vedtaksperiode).periode())

        håndterVilkårsgrunnlag(2.vedtaksperiode)
        assertIngenFunksjonelleFeil(2.vedtaksperiode.filter(ORGNUMMER))
    }

    @Test
    fun `kaste ut vedtaksperiode hvis arbeidsgiver ikke utbetaler arbeidsgiverperiode`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar), begrunnelseForReduksjonEllerIkkeUtbetalt = "begrunnelse")
        assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
        assertInfo("Arbeidsgiver har redusert utbetaling av arbeidsgiverperioden på grunn av: begrunnelse", 1.vedtaksperiode.filter())
        assertFunksjonellFeil("Arbeidsgiver har redusert utbetaling av arbeidsgiverperioden", 1.vedtaksperiode.filter())
    }

    @Test
    fun `Replay av inntektsmelding, men inntektsmeldingen er allerde hensyntatt av perioden`() {
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        assertIngenVarsel(RV_IM_4)
    }

    @Test
    fun `arbeidsgiverperioden starter tidligere`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 10.januar))
        håndterSøknad(Sykdom(3.januar, 10.januar, 100.prosent))
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)

        håndterSykmelding(Sykmeldingsperiode(12.januar, 16.januar))
        håndterSøknad(Sykdom(12.januar, 16.januar, 100.prosent))
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)

        håndterSykmelding(Sykmeldingsperiode(19.januar, 21.januar))
        håndterSøknad(Sykdom(19.januar, 21.januar, 100.prosent))
        assertSisteTilstand(3.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)

        håndterInntektsmelding(listOf(1.januar til 16.januar), førsteFraværsdag = 19.januar)

        assertTrue(inspektør.sykdomstidslinje[1.januar] is Dag.Arbeidsgiverdag)
        assertTrue(inspektør.sykdomstidslinje[2.januar] is Dag.Arbeidsgiverdag)
        assertTrue(inspektør.sykdomstidslinje[11.januar] is Dag.Arbeidsgiverdag)

        assertEquals(1.januar til 10.januar, inspektør.periode(1.vedtaksperiode))
        assertEquals(11.januar til 16.januar, inspektør.periode(2.vedtaksperiode))

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(3.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
    }

    @Test
    fun `Korrigerende inntektsmelding før søknad`() {
        nyPeriode(1.januar til 16.januar)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        håndterInntektsmelding(listOf(2.januar til 17.januar))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        // På dette tidspunktet har AUU'en lagret dagene i historikken og innsett at skjæringstidspunktet er 1.januar
        nyPeriode(17.januar til 31.januar)
        // IM 1 replayes først og blir lagret på 2.januar av forlengelsen -> kan ikke beregne sykepengegrunnlag
        // IM 2 replayes deretter og blir lagret på 1.januar av forlengelsen -> kan beregne sykepengegrunnlag og går videre
        assertInntektshistorikkForDato(INNTEKT, dato = 1.januar, førsteFraværsdag = 1.januar, inspektør = inspektør)
        assertInntektshistorikkForDato(INNTEKT, dato = 2.januar, førsteFraværsdag = 2.januar, inspektør = inspektør)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
    }

    @Test
    fun `padding med arbeidsdager før arbeidsgiverperioden`() {
        håndterSykmelding(Sykmeldingsperiode(28.januar, 16.februar))
        håndterSøknad(Sykdom(28.januar, 16.februar, 100.prosent))
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)

        håndterSykmelding(Sykmeldingsperiode(17.februar, 8.mars))
        håndterSøknad(Sykdom(17.februar, 8.mars, 100.prosent))
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)

        håndterSykmelding(Sykmeldingsperiode(9.mars, 31.mars))
        håndterSøknad(Sykdom(9.mars, 31.mars, 100.prosent))
        assertSisteTilstand(3.vedtaksperiode, AVVENTER_INNTEKTSMELDING)

        val førsteDagIArbeidsgiverperioden = 28.februar
        håndterInntektsmelding(arbeidsgiverperioder = listOf(førsteDagIArbeidsgiverperioden til 15.mars), førsteFraværsdag = 28.februar)
        assertEquals("R AAAAARR AAAAARR AAAAARR AAAAARR AASSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSH", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
        assertNull(inspektør.arbeidsgiverperiode(1.vedtaksperiode))
        assertEquals(28.februar til 15.mars, inspektør.arbeidsgiverperiode(2.vedtaksperiode))
        assertEquals(28.februar til 15.mars, inspektør.arbeidsgiverperiode(2.vedtaksperiode))
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(3.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)

        håndterVilkårsgrunnlag(3.vedtaksperiode)
        håndterYtelser(3.vedtaksperiode)
        håndterSimulering(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode)
        håndterUtbetalt()
        assertSisteTilstand(3.vedtaksperiode, AVSLUTTET)

        assertEquals(28.januar, inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.skjæringstidspunkt)
        assertEquals(28.februar, inspektør.vedtaksperioder(2.vedtaksperiode).inspektør.skjæringstidspunkt)
        assertEquals(28.februar, inspektør.vedtaksperioder(3.vedtaksperiode).inspektør.skjæringstidspunkt)

        val beregnetSykdomstidslinje = inspektør.sykdomshistorikk.sykdomstidslinje()
        val beregnetSykdomstidslinjeDager = beregnetSykdomstidslinje.inspektør.dager
        assertTrue(beregnetSykdomstidslinjeDager.filterKeys { it in 28.januar til førsteDagIArbeidsgiverperioden.minusDays(1) }.values.all {
            (it is Dag.Arbeidsdag || it is Dag.FriskHelgedag) && it.kommerFra(Inntektsmelding::class)
        }) { beregnetSykdomstidslinje.toShortString() }
        assertTrue(beregnetSykdomstidslinjeDager.filterKeys { it in førsteDagIArbeidsgiverperioden til 31.mars }.values.all {
            (it is Dag.Sykedag || it is Dag.SykHelgedag) && it.kommerFra(Søknad::class)
        }) { beregnetSykdomstidslinje.toShortString() }
    }

    @Test
    fun `Inntektsmelding strekker periode tilbake når agp er kant-i-kant`() {
        nyPeriode(1.februar til 16.februar)
        assertEquals(1.februar til 16.februar, inspektør.periode(1.vedtaksperiode))
        håndterInntektsmelding(listOf(16.januar til 31.januar), førsteFraværsdag = 1.februar)
        assertEquals(16.januar til 16.februar, inspektør.periode(1.vedtaksperiode))
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
    }

    @Test
    fun `Inntektsmelding strekker periode tilbake når det er en helgedag mellom agp og periode`() {
        nyPeriode(22.januar til 16.februar)
        assertEquals(22.januar til 16.februar, inspektør.periode(1.vedtaksperiode))
        håndterInntektsmelding(listOf(5.januar til 20.januar), førsteFraværsdag = 22.januar)
        assertEquals(5.januar til 16.februar, inspektør.periode(1.vedtaksperiode))
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
    }

    @Test
    fun `Arbeidsgiverperiode treffer ingen vedtaksperioder og oppgitt begrunnelseForReduksjonEllerIkkeUtbetalt`() {
        nyPeriode(22.januar til 16.februar)
        assertEquals("SSSSSHH SSSSSHH SSSSSHH SSSSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
        håndterInntektsmelding(listOf(5.januar til 20.januar), førsteFraværsdag = 22.januar, begrunnelseForReduksjonEllerIkkeUtbetalt = "Mjau")
        assertEquals(0, inspektør.sykdomshistorikk.sykdomstidslinje().count())
        assertSisteTilstand(1.vedtaksperiode, TIL_INFOTRYGD)
    }

    @Test
    fun `Arbeidsgiverperiode skal ikke valideres før historikken er oppdatert`() {
        nyPeriode(1.januar til 15.januar)
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        nyPeriode(16.januar til 31.januar)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
        assertIngenVarsler(1.vedtaksperiode.filter())
        assertIngenVarsler(2.vedtaksperiode.filter())
    }


    @Test
    fun `arbeidsgiveperiode i forkant av vedtaksperiode med en dags gap`() {
        håndterSykmelding(Sykmeldingsperiode(6.februar, 28.februar))
        håndterSøknad(Sykdom(6.februar, 28.februar, 100.prosent))
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(20.januar til 4.februar),
            førsteFraværsdag = 6.februar
        )
        assertEquals(6.februar til 28.februar, inspektør.periode(1.vedtaksperiode))
        assertEquals("GG UUUUUGG UUUUUGG ?SSSSHH SSSSSHH SSSSSHH SSS", inspektør.sykdomshistorikk.sykdomstidslinje().toShortString())
        assertEquals(1, inspektør.inntektInspektør.size)
        assertIngenInfo("Inntektsmelding ikke håndtert")
    }

    @Test
    fun `Hensyntar korrigert inntekt før vilkårsprøving`() {
        nyPeriode(1.januar til 31.januar)
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 25000.månedlig)
        håndterInntektsmelding(listOf(1.januar til 16.januar), beregnetInntekt = 30000.månedlig)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
        håndterVilkårsgrunnlag(1.vedtaksperiode)

        val vilkårsgrunnlag = inspektør.vilkårsgrunnlag(1.vedtaksperiode)
        assertNotNull(vilkårsgrunnlag)
        val sykepengegrunnlagInspektør = vilkårsgrunnlag.inspektør.sykepengegrunnlag.inspektør
        assertEquals(1, sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)

        sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
            assertEquals(30000.månedlig, it.inntektsopplysning.omregnetÅrsinntekt())
            assertEquals(no.nav.helse.person.inntekt.Inntektsmelding::class, it.inntektsopplysning::class)
        }

    }

    @Test
    fun `Hensyntar korrigert inntekt i avventer blokkerende`() {
        tilGodkjenning(1.januar, 31.januar, ORGNUMMER)
        nyPeriode(1.mars til 31.mars)
        håndterInntektsmelding(listOf(1.mars til 16.mars), beregnetInntekt = 25000.månedlig)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)

        håndterInntektsmelding(listOf(1.mars til 16.mars), beregnetInntekt = 30000.månedlig)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        håndterVilkårsgrunnlag(2.vedtaksperiode)

        val vilkårsgrunnlag = inspektør.vilkårsgrunnlag(2.vedtaksperiode)
        assertNotNull(vilkårsgrunnlag)
        val sykepengegrunnlagInspektør = vilkårsgrunnlag.inspektør.sykepengegrunnlag.inspektør
        assertEquals(1, sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysninger.size)

        sykepengegrunnlagInspektør.arbeidsgiverInntektsopplysningerPerArbeidsgiver.getValue(a1).inspektør.also {
            assertEquals(30000.månedlig, it.inntektsopplysning.omregnetÅrsinntekt())
            assertEquals(no.nav.helse.person.inntekt.Inntektsmelding::class, it.inntektsopplysning::class)
        }

    }


}
