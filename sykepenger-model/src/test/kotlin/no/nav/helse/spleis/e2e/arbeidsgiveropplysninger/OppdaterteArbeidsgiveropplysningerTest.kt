package no.nav.helse.spleis.e2e.arbeidsgiveropplysninger

import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.UNG_PERSON_FNR_2018
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.februar
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.tilstandsmaskin.TilstandType
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.OverstyrtArbeidsgiveropplysning
import no.nav.helse.spleis.e2e.assertTilstand
import no.nav.helse.spleis.e2e.assertVarsel
import no.nav.helse.spleis.e2e.håndterArbeidsgiveropplysninger
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterOverstyrArbeidsgiveropplysninger
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSkjønnsmessigFastsettelse
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlagFlereArbeidsgivere
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.nyPeriode
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class OppdaterteArbeidsgiveropplysningerTest : AbstractEndToEndTest() {

    @Test
    fun `Søknad fra annen arbeidsgiver flytter skjæringstidspunktet i AVVENTER_INNTEKTSMELDING`() {
        nyPeriode(2.januar til 31.januar, orgnummer = a1)
        nyPeriode(januar, orgnummer = a2)

        assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.filter { it.vedtaksperiodeId == 1.vedtaksperiode.id(a1) }.size)
        assertEquals(1, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.filter { it.vedtaksperiodeId == 1.vedtaksperiode.id(a2) }.size)

        val expectedForespørsel = PersonObserver.TrengerArbeidsgiveropplysningerEvent(
            personidentifikator = UNG_PERSON_FNR_2018,
            yrkesaktivitetssporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a1),
            vedtaksperiodeId = 1.vedtaksperiode.id(a1),
            skjæringstidspunkt = 1.januar,
            sykmeldingsperioder = listOf(2.januar til 31.januar),
            egenmeldingsperioder = emptyList(),
            førsteFraværsdager = listOf(
                PersonObserver.FørsteFraværsdag(Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a2), 1.januar),
                PersonObserver.FørsteFraværsdag(Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a1), 2.januar)
            ),
            forespurteOpplysninger = setOf(
                PersonObserver.Inntekt,
                PersonObserver.Refusjon,
                PersonObserver.Arbeidsgiverperiode
            )
        )
        val actualForespørsel = observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last {
            it.vedtaksperiodeId == 1.vedtaksperiode.id(a1)
        }
        assertEquals(expectedForespørsel, actualForespørsel)
    }

    @Test
    fun `Søknad fra annen arbeidsgiver flytter skjæringstidspunktet, skal ikke be om nye opplysninger i annen tilstand enn AVVENTER_INNTEKTSMELDING`() {
        nyttVedtak(2.januar til 31.januar, orgnummer = a1)
        nyPeriode(januar, orgnummer = a2)

        assertEquals(1, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.filter { it.vedtaksperiodeId == 1.vedtaksperiode.id(a1) }.size)
        assertEquals(1, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.filter { it.vedtaksperiodeId == 1.vedtaksperiode.id(a2) }.size)
    }

    @Test
    fun `oppdaterte opplysninger for mars når ag2 tetter gapet`() {
        nyPeriode(januar, a1)
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2, orgnummer = a1)
        assertVarsel(Varselkode.RV_VV_2, 1.vedtaksperiode.filter(orgnummer = a1))
        this@OppdaterteArbeidsgiveropplysningerTest.håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        this@OppdaterteArbeidsgiveropplysningerTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        nyPeriode(mars, orgnummer = a1)
        nyPeriode(februar, orgnummer = a2)
        assertEquals(4, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
        assertEquals(1, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.filter { it.vedtaksperiodeId == 1.vedtaksperiode.id(a1) }.size)
        assertEquals(1, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.filter { it.vedtaksperiodeId == 1.vedtaksperiode.id(a2) }.size)
        val arbeidsgiveropplysningerEventer = observatør.trengerArbeidsgiveropplysningerVedtaksperioder.filter {
            it.vedtaksperiodeId == 2.vedtaksperiode.id(a1)
        }
        assertEquals(2, arbeidsgiveropplysningerEventer.size)
        arbeidsgiveropplysningerEventer.last().also { trengerArbeidsgiveropplysningerEvent ->
            val expectedForespørsel = PersonObserver.TrengerArbeidsgiveropplysningerEvent(
                personidentifikator = UNG_PERSON_FNR_2018,
                yrkesaktivitetssporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a1),
                vedtaksperiodeId = 2.vedtaksperiode.id(a1),
                skjæringstidspunkt = 1.januar,
                sykmeldingsperioder = listOf(mars),
                egenmeldingsperioder = emptyList(),
                førsteFraværsdager = listOf(
                    PersonObserver.FørsteFraværsdag(Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a2), 1.februar),
                    PersonObserver.FørsteFraværsdag(Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a1), 1.mars)
                ),
                forespurteOpplysninger = setOf(
                    PersonObserver.Refusjon,
                    PersonObserver.Arbeidsgiverperiode
                )
            )
            assertEquals(expectedForespørsel, trengerArbeidsgiveropplysningerEvent)
        }
    }

    @Test
    fun `Overlappende søknad fører til oppdatert forespørsel`() {
        nyPeriode(20.januar til 20.februar, orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent), Søknad.Søknadsperiode.Arbeid(18.januar, 31.januar), orgnummer = a2)

        assertEquals(20.januar, inspektør(a1).skjæringstidspunkt(1.vedtaksperiode))
        assertEquals(1.januar, inspektør(a2).skjæringstidspunkt(1.vedtaksperiode))
        assertEquals(1, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.filter { it.vedtaksperiodeId == 1.vedtaksperiode.id(a2) }.size)
        assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.filter { it.vedtaksperiodeId == 1.vedtaksperiode.id(a1) }.size)
    }

    @Test
    fun `Sender oppdatert forespørsel når vi vi får inn ny inntekt på en forrige periode`() {
        nyPeriode(januar)
        nyPeriode(10.februar til 10.mars)

        assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
        håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), vedtaksperiodeIdInnhenter = 1.vedtaksperiode)

        assertEquals(3, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
        val expectedForespørsel = PersonObserver.TrengerArbeidsgiveropplysningerEvent(
            personidentifikator = UNG_PERSON_FNR_2018,
            yrkesaktivitetssporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a1),
            vedtaksperiodeId = 2.vedtaksperiode.id(a1),
            skjæringstidspunkt = 10.februar,
            sykmeldingsperioder = listOf(10.februar til 10.mars),
            egenmeldingsperioder = emptyList(),
            førsteFraværsdager = listOf(PersonObserver.FørsteFraværsdag(Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a1), 10.februar)),
            forespurteOpplysninger = setOf(
                PersonObserver.Inntekt,
                PersonObserver.Refusjon
            )
        )

        assertEquals(expectedForespørsel, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last())
    }


    @Test
    fun `Sender ikke med skjønnsmessig inntekt ved oppdatert forespørsel`() {
        nyttVedtak(januar)
        nyPeriode(mars)
        assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)

        this@OppdaterteArbeidsgiveropplysningerTest.håndterSkjønnsmessigFastsettelse(1.januar, listOf(OverstyrtArbeidsgiveropplysning(a1, INNTEKT / 2)))
        assertEquals(3, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
        val forespørsel = observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last()
        assertEquals(
            PersonObserver.Inntekt,
            forespørsel.forespurteOpplysninger.first { it is PersonObserver.Inntekt }
        )
    }

    @Test
    fun `Sender oppdatert forespørsel ved nytt vilkårsgrunnlag pga saksbehandleroverstyrt inntekt`() {
        nyttVedtak(januar)
        nyPeriode(mars)
        assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)

        this@OppdaterteArbeidsgiveropplysningerTest.håndterOverstyrArbeidsgiveropplysninger(1.januar, listOf(OverstyrtArbeidsgiveropplysning(a1, 32000.månedlig)))

        assertEquals(3, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
        val forespørsel = observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last()
        assertEquals(
            PersonObserver.Inntekt,
            forespørsel.forespurteOpplysninger.first { it is PersonObserver.Inntekt }
        )
    }

    @Test
    fun `Sender oppdatert forespørsel ved nytt vilkårsgrunnlag pga korrigerende inntektsmelding -- revurdering`() {
        nyttVedtak(januar)
        nyPeriode(mars)

        assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            beregnetInntekt = 32000.månedlig
        )

        assertVarsel(Varselkode.RV_IM_4, 1.vedtaksperiode.filter())
        assertEquals(3, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
        val forespørsel = observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last()
        assertEquals(
            PersonObserver.Inntekt,
            forespørsel.forespurteOpplysninger.first { it is PersonObserver.Inntekt }
        )
    }

    @Test
    fun `sender ikke oppdatert forespørsel for en periode som har mottatt inntektsmelding`() {
        nyPeriode(januar)
        nyPeriode(mars)
        håndterArbeidsgiveropplysninger(
            listOf(1.mars til 16.mars),
            vedtaksperiodeIdInnhenter = 2.vedtaksperiode
        )

        assertTilstand(2.vedtaksperiode, TilstandType.AVVENTER_BLOKKERENDE_PERIODE)
        assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)

        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode)

        assertTilstand(2.vedtaksperiode, TilstandType.AVVENTER_BLOKKERENDE_PERIODE)
        assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
    }
}
