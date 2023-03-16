package no.nav.helse.spleis.e2e.ytelser

import no.nav.helse.april
import no.nav.helse.februar
import no.nav.helse.hendelser.Institusjonsopphold
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_AY_4
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_AY_5
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertActivities
import no.nav.helse.spleis.e2e.assertFunksjonellFeil
import no.nav.helse.spleis.e2e.assertIngenFunksjonelleFeil
import no.nav.helse.spleis.e2e.assertIngenVarsel
import no.nav.helse.spleis.e2e.assertIngenVarsler
import no.nav.helse.spleis.e2e.assertSisteTilstand
import no.nav.helse.spleis.e2e.assertTilstand
import no.nav.helse.spleis.e2e.assertVarsel
import no.nav.helse.spleis.e2e.forlengVedtak
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterInntektsmeldingMedValidering
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterSøknadMedValidering
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class YtelserE2ETest : AbstractEndToEndTest() {

    @Test
    fun `perioden får warnings dersom bruker har fått Dagpenger innenfor 4 uker før skjæringstidspunkt`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 19.januar))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 19.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(3.januar til 18.januar), 3.januar)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        assertFalse(hendelselogg.harFunksjonelleFeilEllerVerre())
        assertFalse(person.aktivitetslogg.logg(inspektør.vedtaksperioder(1.vedtaksperiode)).harVarslerEllerVerre())
        håndterYtelser(1.vedtaksperiode, dagpenger = listOf(3.januar.minusDays(14) til 5.januar.minusDays(15)))
        assertTrue(person.aktivitetslogg.logg(inspektør.vedtaksperioder(1.vedtaksperiode)).harVarslerEllerVerre())
        assertIngenFunksjonelleFeil()
        assertActivities(person)
    }


    @Test
    fun `perioden får warnings dersom bruker har fått AAP innenfor 6 måneder før skjæringstidspunkt`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 19.januar))
        håndterSøknadMedValidering(1.vedtaksperiode, Sykdom(3.januar, 19.januar, 100.prosent))
        håndterInntektsmeldingMedValidering(1.vedtaksperiode, listOf(Periode(3.januar, 18.januar)), 3.januar)
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        assertFalse(hendelselogg.harFunksjonelleFeilEllerVerre())
        assertFalse(person.aktivitetslogg.logg(inspektør.vedtaksperioder(1.vedtaksperiode)).harVarslerEllerVerre())
        håndterYtelser(1.vedtaksperiode, arbeidsavklaringspenger = listOf(3.januar.minusDays(60) til 5.januar.minusDays(60)))
        assertVarsel(Varselkode.RV_AY_3, 1.vedtaksperiode.filter())
        assertIngenFunksjonelleFeil()
        assertActivities(person)
    }

    @Test
    fun `AAP starter senere enn sykefraværstilfellet`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 19.januar))
        håndterSøknad(Sykdom(3.januar, 19.januar, 100.prosent))
        håndterInntektsmelding(listOf(3.januar til 18.januar))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode, arbeidsavklaringspenger = listOf(3.februar til 5.februar))
        assertIngenVarsel(Varselkode.RV_AY_3, 1.vedtaksperiode.filter())
    }

    @Test
    fun `Dagpenger starter senere enn sykefraværstilfellet`() {
        håndterSykmelding(Sykmeldingsperiode(3.januar, 19.januar))
        håndterSøknad(Sykdom(3.januar, 19.januar, 100.prosent))
        håndterInntektsmelding(listOf(3.januar til 18.januar))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode, dagpenger = listOf(3.februar til 5.februar))
        assertIngenVarsel(RV_AY_4, 1.vedtaksperiode.filter())
    }

    @Test
    fun `Foreldrepenger starter mindre enn 4 uker før sykefraværstilfellet`() {
        håndterSykmelding(Sykmeldingsperiode(3.mars, 19.mars))
        håndterSøknad(Sykdom(3.mars, 19.mars, 100.prosent))
        håndterInntektsmelding(listOf(3.mars til 18.mars))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode, foreldrepenger = listOf(3.februar til 20.februar))
        assertFunksjonellFeil(RV_AY_5)
    }

    @Test
    fun `Foreldrepenger starter mer enn 4 uker før sykefraværstilfellet`() {
        håndterSykmelding(Sykmeldingsperiode(3.mars, 19.mars))
        håndterSøknad(Sykdom(3.mars, 19.mars, 100.prosent))
        håndterInntektsmelding(listOf(3.mars til 18.mars))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode, foreldrepenger = listOf(3.januar til 20.januar))
        assertIngenFunksjonelleFeil()
    }

    @Test
    fun `Pleiepenger starter mindre enn 4 uker før sykefraværstilfellet`() {
        håndterSykmelding(Sykmeldingsperiode(3.mars, 19.mars))
        håndterSøknad(Sykdom(3.mars, 19.mars, 100.prosent))
        håndterInntektsmelding(listOf(3.mars til 18.mars))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode, pleiepenger = listOf(3.februar til 20.februar))
        assertFunksjonellFeil(Varselkode.RV_AY_6)
    }

    @Test
    fun `Pleiepenger starter mer enn 4 uker før sykefraværstilfellet`() {
        håndterSykmelding(Sykmeldingsperiode(3.mars, 19.mars))
        håndterSøknad(Sykdom(3.mars, 19.mars, 100.prosent))
        håndterInntektsmelding(listOf(3.mars til 18.mars))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode, pleiepenger = listOf(3.januar til 20.januar))
        assertIngenFunksjonelleFeil()
    }

    @Test
    fun `Omsorgspenger starter mindre enn 4 uker før sykefraværstilfellet`() {
        håndterSykmelding(Sykmeldingsperiode(3.mars, 19.mars))
        håndterSøknad(Sykdom(3.mars, 19.mars, 100.prosent))
        håndterInntektsmelding(listOf(3.mars til 18.mars))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode, omsorgspenger = listOf(3.februar til 20.februar))
        assertFunksjonellFeil(Varselkode.RV_AY_7)
    }

    @Test
    fun `Omsorgspenger starter mer enn 4 uker før sykefraværstilfellet`() {
        håndterSykmelding(Sykmeldingsperiode(3.mars, 19.mars))
        håndterSøknad(Sykdom(3.mars, 19.mars, 100.prosent))
        håndterInntektsmelding(listOf(3.mars til 18.mars))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode, omsorgspenger = listOf(3.januar til 20.januar))
        assertIngenFunksjonelleFeil()
    }

    @Test
    fun `Opplæringspenger starter mindre enn 4 uker før sykefraværstilfellet`() {
        håndterSykmelding(Sykmeldingsperiode(3.mars, 19.mars))
        håndterSøknad(Sykdom(3.mars, 19.mars, 100.prosent))
        håndterInntektsmelding(listOf(3.mars til 18.mars))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode, opplæringspenger = listOf(3.februar til 20.februar))
        assertFunksjonellFeil(Varselkode.RV_AY_8)
    }

    @Test
    fun `Opplæringspenger starter mer enn 4 uker før sykefraværstilfellet`() {
        håndterSykmelding(Sykmeldingsperiode(3.mars, 19.mars))
        håndterSøknad(Sykdom(3.mars, 19.mars, 100.prosent))
        håndterInntektsmelding(listOf(3.mars til 18.mars))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode, opplæringspenger = listOf(3.januar til 20.januar))
        assertIngenFunksjonelleFeil()
    }

    @Test
    fun `Foreldrepenger før og etter sykmelding`() {
        håndterSykmelding(Sykmeldingsperiode(1.april, 30.april))
        håndterSøknad(Sykdom(1.april, 30.april, 100.prosent))
        håndterInntektsmelding(listOf(1.april til 16.april))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode, foreldrepenger = listOf(1.februar til 28.februar, 1.mai til 31.mai ))
        assertIngenFunksjonelleFeil()
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING)
    }

    @Test
    fun `Svangerskapspenger før og etter sykmelding`() {
        håndterSykmelding(Sykmeldingsperiode(1.april, 30.april))
        håndterSøknad(Sykdom(1.april, 30.april, 100.prosent))
        håndterInntektsmelding(listOf(1.april til 16.april))
        håndterVilkårsgrunnlag(1.vedtaksperiode)
        håndterYtelser(1.vedtaksperiode, svangerskapspenger = listOf(1.februar til 28.februar, 1.mai til 31.mai ))
        assertIngenFunksjonelleFeil()
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_SIMULERING)
    }

    @Test
    fun `skal ikke ha varsler om andre ytelser ved sammenhengende sykdom etter nådd maksdato`() {
        createKorttidsPerson(UNG_PERSON_FNR_2018, 1.januar(1992), maksSykedager = 11)

        nyttVedtak(1.januar, 31.januar)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        håndterYtelser(
            2.vedtaksperiode,
            arbeidsavklaringspenger = listOf(1.februar til 28.februar),
            dagpenger = listOf(1.februar til 28.februar),
        )

        assertIngenVarsler()
    }

    @Test
    fun `skal ikke ha funksjonelle feil om andre ytelser ved sammenhengende sykdom etter nådd maksdato`() {
        createKorttidsPerson(UNG_PERSON_FNR_2018, 1.januar(1992), maksSykedager = 11)

        nyttVedtak(1.januar, 31.januar)

        håndterSykmelding(Sykmeldingsperiode(1.februar, 28.februar))
        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent))
        håndterYtelser(
            2.vedtaksperiode,
            foreldrepenger = listOf(1.februar til 28.februar),
            pleiepenger = listOf(1.februar til 28.februar),
            omsorgspenger = listOf(1.februar til 28.februar),
            opplæringspenger = listOf(1.februar til 28.februar),
            institusjonsoppholdsperioder = listOf(Institusjonsopphold.Institusjonsoppholdsperiode(1.februar, 28.februar)),
            svangerskapspenger = listOf(1.februar til 28.februar)
        )

        assertIngenFunksjonelleFeil()
        assertTilstand(2.vedtaksperiode, AVVENTER_GODKJENNING)
    }

    @Test
    fun `skal ikke ha varsler om andre ytelser for revurdering ved sammenhengende sykdom etter nådd maksdato`() {
        createKorttidsPerson(UNG_PERSON_FNR_2018, 1.januar(1992), maksSykedager = 11)

        nyttVedtak(1.januar, 31.januar)
        forlengVedtak(1.februar, 28.februar)

        håndterSøknad(Sykdom(1.februar, 28.februar, 95.prosent))
        håndterYtelser(
            2.vedtaksperiode,
            arbeidsavklaringspenger = listOf(1.februar til 28.februar),
            dagpenger = listOf(1.februar til 28.februar),
            foreldrepenger = listOf(1.februar til 28.februar),
            pleiepenger = listOf(1.februar til 28.februar),
            omsorgspenger = listOf(1.februar til 28.februar),
            opplæringspenger = listOf(1.februar til 28.februar),
            institusjonsoppholdsperioder = listOf(Institusjonsopphold.Institusjonsoppholdsperiode(1.februar, 28.februar)),
            svangerskapspenger = listOf(1.februar til 28.februar)
        )
        assertIngenVarsler()
    }
}
