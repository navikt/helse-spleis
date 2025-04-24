package no.nav.helse.spleis.e2e.ytelser

import no.nav.helse.april
import no.nav.helse.desember
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.UNG_PERSON_FNR_2018
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.februar
import no.nav.helse.fredag
import no.nav.helse.hendelser.Dagtype
import no.nav.helse.hendelser.GradertPeriode
import no.nav.helse.hendelser.Institusjonsopphold
import no.nav.helse.hendelser.ManuellOverskrivingDag
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Permisjon
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.juli
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.oktober
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.TilstandType.TIL_UTBETALING
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_AY_11
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_AY_12
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_AY_4
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_AY_5
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_AY_6
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_AY_7
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_AY_8
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_21
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_23
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.september
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.OverstyrtArbeidsgiveropplysning
import no.nav.helse.spleis.e2e.assertActivities
import no.nav.helse.spleis.e2e.assertIngenFunksjonelleFeil
import no.nav.helse.spleis.e2e.assertSisteTilstand
import no.nav.helse.spleis.e2e.assertTilstand
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.assertVarsel
import no.nav.helse.spleis.e2e.assertVarsler
import no.nav.helse.spleis.e2e.forlengVedtak
import no.nav.helse.spleis.e2e.håndterArbeidsgiveropplysninger
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterOverstyrArbeidsgiveropplysninger
import no.nav.helse.spleis.e2e.håndterOverstyrTidslinje
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalingshistorikkEtterInfotrygdendring
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlagFlereArbeidsgivere
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.manuellForeldrepengedag
import no.nav.helse.spleis.e2e.nullstillTilstandsendringer
import no.nav.helse.spleis.e2e.nyPeriode
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.søndag
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class YtelserE2ETest : AbstractEndToEndTest() {

    @Test
    fun `Utbetaler vedtak også overstyrer saksbehandler hele perioden til andre ytelser kan medføre at vi feilaktig annullerer tidligere utebetalte perioder`() {
        håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(a1, 1.januar, 31.januar))
        nyttVedtak(mars)
        nyttVedtak(mai, vedtaksperiodeIdInnhenter = 2.vedtaksperiode)
        val korrelasjonsIdMars = inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.behandlinger.last().endringer.last().utbetaling!!.inspektør.korrelasjonsId
        val korrelasjonsIdMai = inspektør.vedtaksperioder(2.vedtaksperiode).inspektør.behandlinger.last().endringer.last().utbetaling!!.inspektør.korrelasjonsId

        håndterSøknad(juli)
        håndterArbeidsgiveropplysninger(listOf(1.juli til 16.juli), vedtaksperiodeIdInnhenter = 3.vedtaksperiode)
        håndterVilkårsgrunnlag(3.vedtaksperiode)

        assertEquals(listOf(1.juli til 16.juli), inspektør.vedtaksperioder(3.vedtaksperiode).inspektør.arbeidsgiverperiode)
        håndterYtelser(3.vedtaksperiode)
        assertEquals(listOf(1.juli til 16.juli), inspektør.vedtaksperioder(3.vedtaksperiode).inspektør.arbeidsgiverperiode)

        håndterSimulering(3.vedtaksperiode)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode)
        håndterUtbetalt()

        håndterOverstyrTidslinje(juli.map { ManuellOverskrivingDag(it, Dagtype.Foreldrepengerdag) })
        håndterYtelser(3.vedtaksperiode)

        assertEquals(emptyList<Periode>(), inspektør.vedtaksperioder(3.vedtaksperiode).inspektør.arbeidsgiverperiode)
        val juliutbetaling = inspektør.vedtaksperioder(3.vedtaksperiode).inspektør.behandlinger.last().endringer.last().utbetaling!!.inspektør
        val korrelasjonsIdJuli = juliutbetaling.korrelasjonsId

        assertEquals(4, inspektør.utbetalinger.size)
        assertNotEquals(korrelasjonsIdMars, korrelasjonsIdJuli)
        assertTrue(juliutbetaling.annulleringer.isEmpty())
        assertVarsler(listOf(RV_UT_23), 3.vedtaksperiode.filter())
    }

    @Test
    fun `masse perioder med med andre ytelser`() {
        nyttVedtak(januar)
        håndterSøknad(februar)
        håndterSøknad(mars)
        håndterSøknad(april)
        håndterOverstyrTidslinje((1.februar til 15.april).map { ManuellOverskrivingDag(it, Dagtype.Foreldrepengerdag) })
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET)
        assertEquals(1.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK)
        assertEquals(1.januar, inspektør.skjæringstidspunkt(2.vedtaksperiode))
        assertSisteTilstand(3.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        assertEquals(1.januar, inspektør.skjæringstidspunkt(3.vedtaksperiode))
        assertSisteTilstand(4.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        assertEquals(16.april, inspektør.skjæringstidspunkt(4.vedtaksperiode))
    }

    @Test
    fun `Det er ikke mulig å bli en AUU som vil omgjøres om man kombinerer snæx som begrunnelseForReduksjonEllerIkkeUtbetalt og andre ytelser`() {
        håndterSøknad(1.januar til 15.januar) // Denne må være kortere enn 16 dager
        nullstillTilstandsendringer()
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            begrunnelseForReduksjonEllerIkkeUtbetalt = "noe"
        ) // Denn må jo være satt da
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        assertVarsel(Varselkode.RV_IM_8, 1.vedtaksperiode.filter())
        assertTilstander(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_VILKÅRSPRØVING, AVVENTER_HISTORIKK, AVVENTER_SIMULERING, AVVENTER_GODKJENNING, TIL_UTBETALING, AVSLUTTET)

        val forlengelse = 16.januar til 31.januar
        håndterSøknad(forlengelse)
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_GODKJENNING)
        håndterOverstyrTidslinje(forlengelse.map { ManuellOverskrivingDag(it, Dagtype.Foreldrepengerdag) })
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)

        håndterSøknad(februar)
        assertSisteTilstand(3.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
    }

    @Test
    fun `Foreldrepenger påvirker skjæringstidspunkt for senere perioder`() {
        nyttVedtak(januar)
        håndterOverstyrArbeidsgiveropplysninger(1.januar, listOf(OverstyrtArbeidsgiveropplysning(a1, INNTEKT * 1.1)))
        håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)

        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), Permisjon(1.februar, 28.februar))
        håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent), Permisjon(1.mars, 1.mars))

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_GODKJENNING_REVURDERING)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        assertSisteTilstand(3.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)

        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK)
        assertSisteTilstand(3.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)

        nullstillTilstandsendringer()
        assertEquals(1.januar, inspektør.skjæringstidspunkt(3.vedtaksperiode))
        assertEquals("PPPP PPPPPPP PPPPPPP PPPPPPP PPP", inspektør.vedtaksperioder(2.vedtaksperiode).sykdomstidslinje.toShortString())

        håndterYtelser(2.vedtaksperiode, foreldrepenger = listOf(GradertPeriode(1.februar til 1.mars, 100)))

        assertEquals("PPPP PPPPPPP PPPPPPP PPPPPPP PPP", inspektør.vedtaksperioder(2.vedtaksperiode).sykdomstidslinje.toShortString())
        assertEquals(1.januar, inspektør.skjæringstidspunkt(3.vedtaksperiode))

        håndterUtbetalingsgodkjenning(2.vedtaksperiode)

        assertVarsel(RV_AY_5, 2.vedtaksperiode.filter())
        assertTilstander(2.vedtaksperiode, AVVENTER_HISTORIKK, AVVENTER_GODKJENNING, AVSLUTTET)
        assertTilstander(3.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)
    }

    @Test
    fun `Foreldrepenger påvirker skjæringstidspunkt annen arbeidsgiver på periode etter`() {
        håndterSøknad(januar, a1)
        håndterSøknad(februar, a2)
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        håndterArbeidsgiveropplysninger(
            listOf(1.februar til 16.februar),
            orgnummer = a2,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)
        assertEquals(1.januar, inspektør(a2).skjæringstidspunkt(1.vedtaksperiode))
        håndterYtelser(1.vedtaksperiode, foreldrepenger = listOf(GradertPeriode(januar, 100)), orgnummer = a1)

        assertVarsel(RV_AY_5, 1.vedtaksperiode.filter())
        assertEquals(1.januar, inspektør(a2).skjæringstidspunkt(1.vedtaksperiode))
        assertEquals("SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør(a1).vedtaksperioder(1.vedtaksperiode).sykdomstidslinje.toShortString())
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt()
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a2)
    }

    @Test
    fun `Foreldrepenger påvirker skjæringstidspunkt annen arbeidsgiver ved delvis overlapp`()  {
        håndterSøknad(januar, a1)
        håndterSøknad(28.januar til 28.februar, a2)
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        håndterArbeidsgiveropplysninger(
            listOf(28.januar til 13.februar),
            orgnummer = a2,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode, orgnummer = a1)

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)
        assertEquals(1.januar, inspektør(a2).skjæringstidspunkt(1.vedtaksperiode))
        assertEquals("SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør(a1).vedtaksperioder(1.vedtaksperiode).sykdomstidslinje.toShortString())

        håndterYtelser(1.vedtaksperiode, foreldrepenger = listOf(GradertPeriode(januar, 100)), orgnummer = a1)

        assertVarsel(RV_AY_5, 1.vedtaksperiode.filter(orgnummer = a1))
        assertEquals("SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør(a1).vedtaksperioder(1.vedtaksperiode).sykdomstidslinje.toShortString())
        assertEquals(1.januar, inspektør(a2).skjæringstidspunkt(1.vedtaksperiode))

        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt()

        assertSisteTilstand(1.vedtaksperiode, AVVENTER_HISTORIKK, orgnummer = a2)
    }

    @Test
    fun `periode med bare andre ytelser etter langt gap kobler seg på feil utbetaling`() {
        nyttVedtak(januar)

        håndterSøknad(mars)
        håndterArbeidsgiveropplysninger(
            listOf(1.mars til 16.mars),
            vedtaksperiodeIdInnhenter = 2.vedtaksperiode
        )
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        håndterYtelser(2.vedtaksperiode, foreldrepenger = listOf(GradertPeriode(mars, 100)))

        assertVarsel(RV_AY_5, 2.vedtaksperiode.filter())

        val korrelasjonsIdJanuar = inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.behandlinger.last().endringer.last().utbetaling!!.inspektør.korrelasjonsId
        val korrelasjonsIdMars = inspektør.vedtaksperioder(2.vedtaksperiode).inspektør.behandlinger.last().endringer.last().utbetaling!!.inspektør.korrelasjonsId

        assertNotEquals(korrelasjonsIdJanuar, korrelasjonsIdMars)
    }

    @Test
    fun `Foreldrepenger i halen klemrer ikke vekk skjæringstidspunkt`() {
        nyttVedtak(1.januar til søndag(28.januar))
        // Saksbehandler overstyrer i snuten
        håndterOverstyrTidslinje((1.januar til fredag(26.januar)).map { ManuellOverskrivingDag(it, Dagtype.Foreldrepengerdag) })
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        assertEquals(27.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))

        håndterYtelser(1.vedtaksperiode, foreldrepenger = listOf(GradertPeriode(1.januar til søndag(28.januar), 100)))

        assertVarsler(listOf(RV_AY_5, Varselkode.RV_IV_7, RV_UT_23), 1.vedtaksperiode.filter())
        assertTrue(inspektør.sykdomstidslinje[27.januar] is Dag.SykHelgedag)
        assertTrue(inspektør.sykdomstidslinje[28.januar] is Dag.SykHelgedag)
        assertEquals(27.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
    }

    @Test
    fun `perioden får warnings dersom bruker har fått Dagpenger innenfor 4 uker før skjæringstidspunkt`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 19.januar))
        håndterSøknad(Sykdom(3.januar, 19.januar, 100.prosent))
        håndterArbeidsgiveropplysninger(listOf(3.januar til 18.januar), vedtaksperiodeIdInnhenter = 1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode, dagpenger = listOf(3.januar.minusDays(14) til 5.januar.minusDays(15)))

        assertVarsler(listOf(RV_AY_4), 1.vedtaksperiode.filter())
        assertIngenFunksjonelleFeil()
        assertActivities()
    }

    @Test
    fun `perioden får warnings dersom bruker har fått AAP innenfor 6 måneder før skjæringstidspunkt`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 19.januar))
        håndterSøknad(Sykdom(3.januar, 19.januar, 100.prosent))
        håndterArbeidsgiveropplysninger(listOf(Periode(3.januar, 18.januar)), vedtaksperiodeIdInnhenter = 1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode, arbeidsavklaringspenger = listOf(3.januar.minusDays(60) til 5.januar.minusDays(60)))

        assertVarsler(listOf(Varselkode.RV_AY_3), 1.vedtaksperiode.filter())
        assertIngenFunksjonelleFeil()
        assertActivities()
    }

    @Test
    fun `AAP starter senere enn sykefraværstilfellet`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 19.januar))
        håndterSøknad(Sykdom(3.januar, 19.januar, 100.prosent))
        håndterArbeidsgiveropplysninger(listOf(3.januar til 18.januar), vedtaksperiodeIdInnhenter = 1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode, arbeidsavklaringspenger = listOf(3.februar til 5.februar))

        assertVarsler(emptyList(), 1.vedtaksperiode.filter())
    }

    @Test
    fun `Dagpenger starter senere enn sykefraværstilfellet`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 19.januar))
        håndterSøknad(Sykdom(3.januar, 19.januar, 100.prosent))
        håndterArbeidsgiveropplysninger(listOf(3.januar til 18.januar), vedtaksperiodeIdInnhenter = 1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode, dagpenger = listOf(3.februar til 5.februar))
        assertVarsler(emptyList(), 1.vedtaksperiode.filter())
    }

    @Test
    fun `Foreldrepenger starter mindre enn 4 uker før sykefraværstilfellet`() {
        håndterSykmelding(Sykmeldingsperiode(3.mars, 19.mars))
        håndterSøknad(Sykdom(3.mars, 19.mars, 100.prosent))
        håndterArbeidsgiveropplysninger(listOf(3.mars til 18.mars), vedtaksperiodeIdInnhenter = 1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode, foreldrepenger = listOf(GradertPeriode(3.februar til 20.februar, 100)))
        assertVarsel(RV_AY_5, 1.vedtaksperiode.filter())
        assertTilstand(1.vedtaksperiode, AVVENTER_SIMULERING)
    }

    @Test
    fun `Foreldrepenger minst 14 dager før perioden`() {
        håndterSøknad(Sykdom(1.oktober, 31.oktober, 100.prosent))
        håndterArbeidsgiveropplysninger(listOf(1.oktober til 16.oktober), vedtaksperiodeIdInnhenter = 1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode, foreldrepenger = listOf(GradertPeriode(16.september til 30.september, 100)))

        assertVarsler(listOf(RV_AY_5, RV_AY_12), 1.vedtaksperiode.filter())
        assertTilstand(1.vedtaksperiode, AVVENTER_SIMULERING)
    }

    @Test
    fun `Overlappende svangerskapspenger`() {
        håndterSøknad(Sykdom(1.januar, 19.januar, 100.prosent))
        håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), vedtaksperiodeIdInnhenter = 1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode, svangerskapspenger = listOf(GradertPeriode(3.januar til 20.januar, 100)))
        assertVarsel(RV_AY_11, 1.vedtaksperiode.filter())
    }

    @Test
    fun `Foreldrepenger starter mer enn 4 uker før sykefraværstilfellet`() {
        håndterSykmelding(Sykmeldingsperiode(3.mars, 19.mars))
        håndterSøknad(Sykdom(3.mars, 19.mars, 100.prosent))
        håndterArbeidsgiveropplysninger(listOf(3.mars til 18.mars), vedtaksperiodeIdInnhenter = 1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode, foreldrepenger = listOf(GradertPeriode(3.januar til 20.januar, 100)))
        assertIngenFunksjonelleFeil()
    }

    @Test
    fun `forlengelse trenger ikke sjekke mot 4-ukers vindu`() {
        nyttVedtak(januar)
        forlengVedtak(februar)

        håndterOverstyrTidslinje(listOf(ManuellOverskrivingDag(31.januar, Dagtype.Feriedag)))
        håndterYtelser(
            1.vedtaksperiode,
            foreldrepenger = listOf(GradertPeriode(20.januar til 31.januar, 100)),
            svangerskapspenger = listOf(GradertPeriode(20.januar til 31.januar, 100)),
            omsorgspenger = listOf(GradertPeriode(20.januar til 31.januar, 100)),
            opplæringspenger = listOf(GradertPeriode(20.januar til 31.januar, 100)),
            pleiepenger = listOf(GradertPeriode(20.januar til 31.januar, 100))
        )
        assertVarsler(listOf(RV_AY_5, RV_AY_6, RV_AY_7, RV_AY_8, RV_AY_11, RV_UT_23), 1.vedtaksperiode.filter())
        håndterSimulering(1.vedtaksperiode)
        håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
        håndterYtelser(
            2.vedtaksperiode,
            foreldrepenger = listOf(GradertPeriode(20.januar til 31.januar, 100)),
            svangerskapspenger = listOf(GradertPeriode(20.januar til 31.januar, 100)),
            omsorgspenger = listOf(GradertPeriode(20.januar til 31.januar, 100)),
            opplæringspenger = listOf(GradertPeriode(20.januar til 31.januar, 100)),
            pleiepenger = listOf(GradertPeriode(20.januar til 31.januar, 100))
        )
        assertVarsler(emptyList(), 2.vedtaksperiode.filter())
    }

    @Test
    fun `Omsorgspenger starter mer enn 4 uker før sykefraværstilfellet`() {
        håndterSykmelding(Sykmeldingsperiode(3.mars, 19.mars))
        håndterSøknad(Sykdom(3.mars, 19.mars, 100.prosent))
        håndterArbeidsgiveropplysninger(listOf(3.mars til 18.mars), vedtaksperiodeIdInnhenter = 1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode, omsorgspenger = listOf(GradertPeriode(3.januar til 20.januar, 100)))
        assertIngenFunksjonelleFeil()
    }

    @Test
    fun `Foreldrepenger før og etter sykmelding`() {
        håndterSykmelding(april)
        håndterSøknad(april)
        håndterArbeidsgiveropplysninger(listOf(1.april til 16.april), vedtaksperiodeIdInnhenter = 1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode, foreldrepenger = listOf(GradertPeriode(februar, 100), GradertPeriode(mai, 100)))
        assertIngenFunksjonelleFeil()
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING)
    }

    @Test
    fun `Svangerskapspenger før og etter sykmelding`() {
        håndterSykmelding(april)
        håndterSøknad(april)
        håndterArbeidsgiveropplysninger(listOf(1.april til 16.april), vedtaksperiodeIdInnhenter = 1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(
            1.vedtaksperiode,
            svangerskapspenger = listOf(
                GradertPeriode(20.februar til 28.februar, 100),
                GradertPeriode(mai, 100)
            )
        )
        assertIngenFunksjonelleFeil()
        assertVarsler(emptyList(), 1.vedtaksperiode.filter())
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING)
    }

    @Test
    fun `skal ikke ha varsler om andre ytelser ved sammenhengende sykdom etter nådd maksdato`() {
        createKorttidsPerson(UNG_PERSON_FNR_2018, 1.januar(1992), maksSykedager = 11)

        nyttVedtak(januar)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
        håndterSøknad(februar)
        håndterYtelser(
            2.vedtaksperiode,
            arbeidsavklaringspenger = listOf(februar),
            dagpenger = listOf(februar),
        )

        assertVarsler(emptyList(), 1.vedtaksperiode.filter())
        assertVarsler(emptyList(), 2.vedtaksperiode.filter())
    }

    @Test
    fun `skal ikke ha funksjonelle feil om andre ytelser ved sammenhengende sykdom etter nådd maksdato`() {
        createKorttidsPerson(UNG_PERSON_FNR_2018, 1.januar(1992), maksSykedager = 11)

        nyttVedtak(januar)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
        håndterSøknad(februar)
        håndterYtelser(
            2.vedtaksperiode,
            foreldrepenger = listOf(GradertPeriode(februar, 100)),
            svangerskapspenger = listOf(GradertPeriode(februar, 100)),
            pleiepenger = listOf(GradertPeriode(februar, 100)),
            omsorgspenger = listOf(GradertPeriode(februar, 100)),
            opplæringspenger = listOf(GradertPeriode(februar, 100)),
            institusjonsoppholdsperioder = listOf(Institusjonsopphold.Institusjonsoppholdsperiode(1.februar, 28.februar))
        )

        assertIngenFunksjonelleFeil()
        assertTilstand(2.vedtaksperiode, AVVENTER_GODKJENNING)
    }

    @Test
    fun `skal ikke ha varsler om andre ytelser for revurdering ved sammenhengende sykdom etter nådd maksdato`() {
        createKorttidsPerson(UNG_PERSON_FNR_2018, 1.januar(1992), maksSykedager = 11)

        nyttVedtak(januar)
        forlengVedtak(februar)

        håndterSøknad(Sykdom(1.februar, 28.februar, 95.prosent))
        håndterYtelser(
            2.vedtaksperiode,
            foreldrepenger = listOf(GradertPeriode(februar, 100)),
            svangerskapspenger = listOf(GradertPeriode(februar, 100)),
            pleiepenger = listOf(GradertPeriode(februar, 100)),
            omsorgspenger = listOf(GradertPeriode(februar, 100)),
            opplæringspenger = listOf(GradertPeriode(februar, 100)),
            institusjonsoppholdsperioder = listOf(Institusjonsopphold.Institusjonsoppholdsperiode(1.februar, 28.februar)),
            arbeidsavklaringspenger = listOf(februar),
            dagpenger = listOf(februar)
        )
        assertVarsler(emptyList(), 1.vedtaksperiode.filter())
        assertVarsler(emptyList(), 2.vedtaksperiode.filter())
    }

    @Test
    fun `Var ikke permisjon i forlengelsen likevel`() {
        nyttVedtak(januar)
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), Permisjon(1.februar, 28.februar))
        håndterYtelser(2.vedtaksperiode, orgnummer = a1)
        håndterSøknad(februar)
        håndterYtelser(2.vedtaksperiode, orgnummer = a1)
        håndterSimulering(2.vedtaksperiode, orgnummer = a1)
        assertEquals("SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.sykdomstidslinje.toShortString())
    }

    @Test
    fun `Annen ytelse i periode som tidligere var sykdom`() {
        nyPeriode(1.januar til 16.januar, orgnummer = a1)
        nyPeriode(1.januar til 16.januar, orgnummer = a2)
        nyPeriode(17.januar til 25.januar, orgnummer = a1)
        nyPeriode(17.januar til 31.januar, orgnummer = a2)
        nyPeriode(26.januar til 31.januar, orgnummer = a1)
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            orgnummer = a1
        )
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            orgnummer = a2
        )

        håndterVilkårsgrunnlagFlereArbeidsgivere(2.vedtaksperiode, a1, a2, orgnummer = a1)
        håndterYtelser(2.vedtaksperiode, orgnummer = a1)
        håndterSimulering(2.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        håndterYtelser(2.vedtaksperiode, orgnummer = a2)
        håndterSimulering(2.vedtaksperiode, orgnummer = a2)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)

        håndterYtelser(3.vedtaksperiode, orgnummer = a1)
        håndterSimulering(3.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(3.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, orgnummer = a1)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertSisteTilstand(3.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING, orgnummer = a2)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET, orgnummer = a2)

        håndterOverstyrTidslinje((20..31).map { ManuellOverskrivingDag(it.januar, Dagtype.Pleiepengerdag) }, orgnummer = a1)
        håndterOverstyrTidslinje((20..31).map { ManuellOverskrivingDag(it.januar, Dagtype.Pleiepengerdag) }, orgnummer = a2)
        håndterYtelser(2.vedtaksperiode, orgnummer = a1)
        assertVarsel(RV_UT_23, 2.vedtaksperiode.filter(orgnummer = a1))
        assertVarsel(RV_UT_23, 2.vedtaksperiode.filter(orgnummer = a2))
        håndterSimulering(2.vedtaksperiode, orgnummer = a1)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        assertEquals(1.januar, inspektør(a1).skjæringstidspunkt(3.vedtaksperiode))
        assertEquals(1.januar, inspektør(a2).skjæringstidspunkt(2.vedtaksperiode))
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_HISTORIKK_REVURDERING, orgnummer = a2)
        assertSisteTilstand(3.vedtaksperiode, AVVENTER_REVURDERING, orgnummer = a1)
    }

    @Test
    fun `graderte foreldrepenger i halen`() {
        håndterSøknad(januar)
        håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), vedtaksperiodeIdInnhenter = 1.vedtaksperiode)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode, foreldrepenger = listOf(GradertPeriode(30.januar til 31.januar, 50)))
        assertVarsel(RV_AY_5, 1.vedtaksperiode.filter())
        assertEquals("SSSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.sykdomstidslinje.toShortString())
    }

    /*
    ønsker å gjenbruke inntektsopplysninger, når det er en sammenhengende periode med andre ytelser
    mellom denne perioden og en periode der vi har inntektsopplysninger

    eller, altså, en periode med bare andre ytelser burde ikke trenge inntekter, men så lenge vi ikke får lov til
    å gå videre med ingenting, så må vi heller late som om inntektsmeldingen vi fikk en gang i hine hårde dager
    gjelder for oss også.

    det vil altså si, i tilfelle andre ytelser så skal det potensielt _enorme_ gapet mellom siste sykedag og denne
    perioden, om det gapet bare er fyllt opp av andre ytelser, _ikke_ regnes som et gap mtp inntektsmelding. men det er
    et gap mtp nytt skjæringstidspunkt, om det skulle komme en ny sykedag
     */
    @Test
    fun `Overstyr til andre ytelser i andre pølse, og så kommer det en tredje -- Da vil vi gjenbruke inntektsmelding`() {
        nyttVedtak(januar)
        forlengVedtak(februar)

        håndterOverstyrTidslinje(februar.map { manuellForeldrepengedag(it) })
        håndterYtelser(2.vedtaksperiode)
        håndterSimulering(2.vedtaksperiode)
        håndterUtbetalingsgodkjenning(2.vedtaksperiode)
        håndterUtbetalt()

        assertVarsel(RV_UT_23, 2.vedtaksperiode.filter())

        nyPeriode(mars)
        assertEquals(1.mars, inspektør.skjæringstidspunkt(3.vedtaksperiode))
        håndterOverstyrTidslinje(mars.map { manuellForeldrepengedag(it) })

        assertEquals(1.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode))
        assertEquals(1.januar, inspektør.skjæringstidspunkt(2.vedtaksperiode))
        assertEquals(1.januar, inspektør.skjæringstidspunkt(3.vedtaksperiode))

        assertTilstander(3.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVVENTER_HISTORIKK)
    }

    @Test
    fun `periode med arbeid i snuten som får omslukende ytelser`() {
        nyPeriode(januar)
        håndterArbeidsgiveropplysninger(listOf(2.januar til 17.januar), vedtaksperiodeIdInnhenter = 1.vedtaksperiode)

        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode, foreldrepenger = listOf(GradertPeriode(31.desember(2017) til 1.februar, 100)))

        assertVarsel(RV_AY_5, 1.vedtaksperiode.filter())
        assertEquals("ASSSSHH SSSSSHH SSSSSHH SSSSSHH SSS", inspektør.sykdomstidslinje.toShortString())
        assertTilstand(1.vedtaksperiode, AVVENTER_SIMULERING)
    }
}
