package no.nav.helse.spleis.e2e.arbeidsgiveropplysninger

import java.util.UUID
import kotlin.reflect.KClass
import no.nav.helse.april
import no.nav.helse.assertForventetFeil
import no.nav.helse.desember
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.UNG_PERSON_FNR_2018
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.februar
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Ferie
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_8
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IV_10
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_VV_2
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INFOTRYGDHISTORIKK
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.tilstandsmaskin.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.tilstandsmaskin.TilstandType.START
import no.nav.helse.somOrganisasjonsnummer
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.AktivitetsloggFilter
import no.nav.helse.spleis.e2e.OverstyrtArbeidsgiveropplysning
import no.nav.helse.spleis.e2e.TestObservatør
import no.nav.helse.spleis.e2e.assertSisteTilstand
import no.nav.helse.spleis.e2e.assertTilstand
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.assertVarsel
import no.nav.helse.spleis.e2e.assertVarsler
import no.nav.helse.spleis.e2e.forlengVedtak
import no.nav.helse.spleis.e2e.håndterArbeidsgiveropplysninger
import no.nav.helse.spleis.e2e.håndterInntektsmelding
import no.nav.helse.spleis.e2e.håndterOverstyrArbeidsgiveropplysninger
import no.nav.helse.spleis.e2e.håndterPåminnelse
import no.nav.helse.spleis.e2e.håndterSimulering
import no.nav.helse.spleis.e2e.håndterSkjønnsmessigFastsettelse
import no.nav.helse.spleis.e2e.håndterSykepengegrunnlagForArbeidsgiver
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.håndterUtbetalingsgodkjenning
import no.nav.helse.spleis.e2e.håndterUtbetalt
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlag
import no.nav.helse.spleis.e2e.håndterVilkårsgrunnlagFlereArbeidsgivere
import no.nav.helse.spleis.e2e.håndterYtelser
import no.nav.helse.spleis.e2e.nullstillTilstandsendringer
import no.nav.helse.spleis.e2e.nyPeriode
import no.nav.helse.spleis.e2e.nyeVedtak
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class TrengerArbeidsgiveropplysningerTest : AbstractEndToEndTest() {

    @Test
    fun `En annen vedtaksperiode håndterer innteksmelding, så forespørsel bli aldri kvittert ut`()  {
        håndterSøknad(1.januar til 16.januar)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)

        håndterSøknad(17.januar til 31.januar)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        // Sendes forespørsel for periode nummer 2
        assertEtterspurt(2.vedtaksperiode.id(a1), PersonObserver.Inntekt::class, PersonObserver.Refusjon::class, PersonObserver.Arbeidsgiverperiode::class)

        // Arbeidsgiver svarer ikke på forespørselen, men sender en "gammel" inntektsmelding & opplyser om begrunnelseForReduksjonEllerIkkeUtbetalt
        val inntektsmeldingId = håndterInntektsmelding(listOf(1.januar til 16.januar), begrunnelseForReduksjonEllerIkkeUtbetalt = "En eller annen kul verdi")
        // Da er det periode nummer 1 som håndterer denne inntektsmeldingen
        assertEquals(inntektsmeldingId to 1.vedtaksperiode.id(a1), observatør.inntektsmeldingHåndtert.single())

        assertVarsler(listOf(RV_IM_8), 1.vedtaksperiode.filter(a1))

        assertForventetFeil(
            forklaring = "Her burde forespørselen kvitteres ut",
            nå = { assertTrue(observatør.trengerIkkeArbeidsgiveropplysningerVedtaksperioder.isEmpty()) },
            ønsket = { assertEquals(2.vedtaksperiode.id(a1), observatør.trengerIkkeArbeidsgiveropplysningerVedtaksperioder.singleOrNull()?.vedtaksperiodeId) }
        )
    }

    @Test
    fun `Vi går videre med skatt, så forespørsel bli aldri kvittert ut`() {
        håndterSøknad(1.januar til 31.januar)
        assertEtterspurt(1.vedtaksperiode.id(a1), PersonObserver.Inntekt::class, PersonObserver.Refusjon::class, PersonObserver.Arbeidsgiverperiode::class)
        this@TrengerArbeidsgiveropplysningerTest.håndterPåminnelse(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING, flagg = setOf("ønskerInntektFraAOrdningen"))
        this@TrengerArbeidsgiveropplysningerTest.håndterSykepengegrunnlagForArbeidsgiver(1.januar)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
        assertTrue(observatør.inntektsmeldingHåndtert.isEmpty())

        assertVarsler(listOf(RV_IV_10), 1.vedtaksperiode.filter(a1))

        assertForventetFeil(
            forklaring = "Her burde forespørselen kvitteres ut",
            nå = { assertTrue(observatør.trengerIkkeArbeidsgiveropplysningerVedtaksperioder.isEmpty()) },
            ønsket = { assertEquals(1.vedtaksperiode.id(a1), observatør.trengerIkkeArbeidsgiveropplysningerVedtaksperioder.singleOrNull()?.vedtaksperiodeId) }
        )
    }

    @Test
    fun `syk fra ghost samme måned som skjæringstidspunktet`() {
        håndterSøknad(28.januar til 28.februar)
        håndterArbeidsgiveropplysninger(listOf(28.januar til 12.februar), vedtaksperiodeIdInnhenter = 1.vedtaksperiode)
        håndterVilkårsgrunnlagFlereArbeidsgivere(vedtaksperiodeIdInnhenter = 1.vedtaksperiode, a1, a2, orgnummer = a1)
        this@TrengerArbeidsgiveropplysningerTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        this@TrengerArbeidsgiveropplysningerTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
        assertVarsler(listOf(RV_VV_2), 1.vedtaksperiode.filter())

        håndterSøknad(28.januar til 28.februar, orgnummer = a2)
        assertEtterspurt(1.vedtaksperiode.id(a2), PersonObserver.Refusjon::class, PersonObserver.Arbeidsgiverperiode::class)
    }

    @Test
    fun `syk fra ghost annen måned som skjæringstidspunktet`() {
        håndterSøknad(28.januar til 28.februar)
        håndterArbeidsgiveropplysninger(listOf(28.januar til 12.februar), vedtaksperiodeIdInnhenter = 1.vedtaksperiode)
        håndterVilkårsgrunnlagFlereArbeidsgivere(vedtaksperiodeIdInnhenter = 1.vedtaksperiode, a1, a2, orgnummer = a1)
        this@TrengerArbeidsgiveropplysningerTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        this@TrengerArbeidsgiveropplysningerTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()
        assertVarsler(listOf(RV_VV_2), 1.vedtaksperiode.filter())

        håndterSøknad(1.februar til 28.februar, orgnummer = a2)
        assertEtterspurt(1.vedtaksperiode.id(a2), PersonObserver.Refusjon::class, PersonObserver.Arbeidsgiverperiode::class)
    }

    @Test
    fun `Skal høre på arbeidsgiver når hen sier at egenmeldinger ikke gjelder`() {
        håndterSøknad(Sykdom(3.januar, 17.januar, 100.prosent), egenmeldinger = listOf(1.januar til 2.januar))
        håndterArbeidsgiveropplysninger(listOf(3.januar til 17.januar), vedtaksperiodeIdInnhenter = 1.vedtaksperiode)

        assertEquals(1, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
        assertEquals(emptyList<Periode>(), inspektør.vedtaksperioder(1.vedtaksperiode).egenmeldingsdager)
    }

    @Test
    fun `Skal ikke sende forespørsel for korte perioder etter at arbeidsgiver har sendt riktig AGP`()  {
        håndterSøknad(Sykdom(6.januar, 17.januar, 100.prosent), egenmeldinger = listOf(1.januar til 5.januar))
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        assertEquals(1, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
        assertEquals(listOf(1.januar til 16.januar), inspektør.arbeidsgiverperiode(1.vedtaksperiode))
        assertEquals(listOf(1.januar til 5.januar), inspektør.egenmeldingsdager(1.vedtaksperiode))

        håndterSøknad(Sykdom(22.januar, 25.januar, 100.prosent), egenmeldinger = listOf(21.januar til 21.januar))
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
        assertEquals(listOf(1.januar til 16.januar), inspektør.arbeidsgiverperiode(2.vedtaksperiode))
        assertEquals(listOf(21.januar til 21.januar), inspektør.egenmeldingsdager(2.vedtaksperiode))

        håndterArbeidsgiveropplysninger(
            listOf(6.januar til 17.januar),
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertEquals(listOf(6.januar til 17.januar, 22.januar til 25.januar), inspektør.arbeidsgiverperiode(1.vedtaksperiode))
        assertEquals(emptyList<Periode>(), inspektør.egenmeldingsdager(1.vedtaksperiode))

        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertEquals(listOf(6.januar til 17.januar, 22.januar til 25.januar), inspektør.arbeidsgiverperiode(2.vedtaksperiode))
        assertEquals(emptyList<Periode>(), inspektør.egenmeldingsdager(2.vedtaksperiode))

        assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
    }

    @Test
    fun `flere korte perioder - sender ikke ut ny oppdatert forespørsel ved mottak av im`() {
        håndterSøknad(Sykdom(1.januar, 5.januar, 100.prosent))
        håndterSøknad(Sykdom(6.januar, 7.januar, 100.prosent))
        håndterSøknad(Sykdom(8.januar, 13.januar, 100.prosent))
        håndterSøknad(Sykdom(14.januar, 21.januar, 100.prosent))
        håndterSøknad(Sykdom(22.januar, 26.januar, 100.prosent))
        håndterSøknad(Sykdom(27.januar, 28.januar, 100.prosent))
        håndterSøknad(Sykdom(29.januar, 31.januar, 100.prosent))
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(3.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertSisteTilstand(4.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
        assertSisteTilstand(5.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        assertSisteTilstand(6.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        assertSisteTilstand(7.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
        assertEquals(1, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
    }

    @Test
    fun `første fraværsdag vi mottar i IM blir feil når det er ferie første dag i sykmeldingsperioden etter kort gap`() {
        nyttVedtak(januar)

        håndterSøknad(Sykdom(12.februar, 28.februar, 100.prosent), Ferie(12.februar, 12.februar))
        observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last().let { event ->
            assertEquals(listOf(12.februar til 28.februar), event.sykmeldingsperioder)
            assertEquals(13.februar, event.skjæringstidspunkt)
            assertEquals(listOf(PersonObserver.FørsteFraværsdag(Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a1), 13.februar)), event.førsteFraværsdager)
            assertFalse(event.forespurteOpplysninger.any { it is PersonObserver.Arbeidsgiverperiode })
        }
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        håndterArbeidsgiveropplysninger(emptyList(), vedtaksperiodeIdInnhenter = 2.vedtaksperiode)
        assertSisteTilstand(2.vedtaksperiode, AVVENTER_VILKÅRSPRØVING)
    }

    @Test
    fun `sender ut event TrengerArbeidsgiveropplysninger når vi ankommer AvventerInntektsmelding`() {
        nyPeriode(januar)
        assertEquals(1, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
    }

    @Test
    fun `ber ikke om arbeidsgiveropplysninger på ghost når riktig inntektsmelding kommer`()  {
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2, orgnummer = a1)
        assertVarsel(RV_VV_2, 1.vedtaksperiode.filter(orgnummer = a1))
        this@TrengerArbeidsgiveropplysningerTest.håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        this@TrengerArbeidsgiveropplysningerTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        håndterSøknad(Sykdom(1.februar, 10.februar, 100.prosent), orgnummer = a2)
        håndterSøknad(Sykdom(11.februar, 28.februar, 100.prosent), orgnummer = a2)

        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = a1)

        assertEquals(1, observatør.inntektsmeldingHåndtert.size)
        assertEquals(1, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.count { it.yrkesaktivitetssporing.somOrganisasjonsnummer == a1 })
        assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.count { it.yrkesaktivitetssporing.somOrganisasjonsnummer == a2 })

        håndterArbeidsgiveropplysninger(
            listOf(1.februar til 16.februar),
            orgnummer = a2,
            vedtaksperiodeIdInnhenter = 2.vedtaksperiode
        )

        assertEquals(2, observatør.inntektsmeldingHåndtert.size)
        assertEquals(1, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.count { it.yrkesaktivitetssporing.somOrganisasjonsnummer == a1 })
        assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.count { it.yrkesaktivitetssporing.somOrganisasjonsnummer == a2 })
    }

    @Test
    fun `ber ikke om arbeidsgiveropplysninger på forlengelse med forskjøvet agp`() {
        håndterSøknad(Sykdom(1.januar, 15.januar, 100.prosent), Ferie(1.januar, 11.januar))
        håndterSøknad(Sykdom(16.januar, 29.januar, 100.prosent), Ferie(16.januar, 16.januar))
        håndterSøknad(Sykdom(30.januar, 8.februar, 100.prosent))
        assertEquals(1, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
        assertEquals(2.vedtaksperiode.id(a1), observatør.trengerArbeidsgiveropplysningerVedtaksperioder.single().vedtaksperiodeId)
    }

    @Test
    fun `auu håndterer dager før forlengelsen håndterer inntekt`() {
        håndterSøknad(Sykdom(1.januar, 15.januar, 100.prosent), Ferie(1.januar, 11.januar))
        håndterSøknad(Sykdom(16.januar, 29.januar, 100.prosent), Ferie(16.januar, 16.januar))
        håndterInntektsmelding(listOf(12.januar til 27.januar))
        assertEquals(1, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
        assertEquals(2.vedtaksperiode.id(a1), observatør.trengerArbeidsgiveropplysningerVedtaksperioder.single().vedtaksperiodeId)
    }

    @Test
    fun `skal ikke be om arbeidsgiverperiode når det er mindre en 16 dagers gap`() {
        val inntektsmeldingId = UUID.randomUUID()
        nyttVedtak(januar, inntektsmeldingId = inntektsmeldingId)
        nyPeriode(16.februar til 15.mars)

        assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)

        val expectedForespurteOpplysninger = setOf(
            PersonObserver.Inntekt,
            PersonObserver.Refusjon
        )
        val actualForespurteOpplysninger =
            observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last().forespurteOpplysninger
        assertEquals(expectedForespurteOpplysninger, actualForespurteOpplysninger)
    }

    @Test
    fun `skal ikke be om arbeidsgiveropplysninger ved forlengelse`() {
        nyttVedtak(januar)
        forlengVedtak(februar)

        assertEquals(1, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
    }

    @Test
    fun `skal ikke be om arbeidsgiveropplysninger ved forlengelse selv når inntektsmeldingen ikke har kommet enda`() {
        nyPeriode(januar)
        nyPeriode(februar)

        assertEquals(1, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
    }

    @Test
    fun `skal be om arbeidsgiverperiode ved 16 dagers gap`() {
        val inntektsmeldingId = UUID.randomUUID()
        nyttVedtak(januar, inntektsmeldingId = inntektsmeldingId)
        nyPeriode(17.februar til 17.mars)

        val expectedForespurteOpplysninger = setOf(
            PersonObserver.Inntekt,
            PersonObserver.Refusjon,
            PersonObserver.Arbeidsgiverperiode
        )
        val actualForespurteOpplysninger =
            observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last().forespurteOpplysninger
        assertEquals(expectedForespurteOpplysninger, actualForespurteOpplysninger)

        assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
    }

    @Test
    fun `sender med skjæringstidspunkt i eventet`() {
        nyPeriode(januar)
        val trengerArbeidsgiveropplysningerEvent = observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last()

        assertEquals(1.januar, trengerArbeidsgiveropplysningerEvent.skjæringstidspunkt)
    }

    @Test
    fun `sender med begge sykmeldingsperiodene når vi har en kort periode som forlenges av en lang`() {
        nyPeriode(1.januar til 16.januar)
        nyPeriode(17.januar til 31.januar)

        val trengerArbeidsgiveropplysningerEvent = observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last()
        val expectedSykmeldingsperioder = listOf(
            1.januar til 16.januar,
            17.januar til 31.januar
        )
        assertEquals(expectedSykmeldingsperioder, trengerArbeidsgiveropplysningerEvent.sykmeldingsperioder)

        assertEquals(1, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
    }

    @Test
    fun `sender ikke med begge sykmeldingsperiodene når vi har et gap større enn 16 dager mellom dem`() {
        nyPeriode(januar)
        nyPeriode(17.februar til 17.mars)

        val trengerArbeidsgiveropplysningerEvent = observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last()
        val expectedSykmeldingsperioder = listOf(17.februar til 17.mars)
        assertEquals(expectedSykmeldingsperioder, trengerArbeidsgiveropplysningerEvent.sykmeldingsperioder)

        assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
    }

    @Test
    fun `sender med riktig sykmeldingsperioder når arbeidsgiverperioden er stykket opp i flere korte perioder`() {
        nyPeriode(1.januar til 7.januar)
        nyPeriode(9.januar til 14.januar)
        nyPeriode(16.januar til 21.januar)

        val expectedForespurteOpplysninger = setOf(
            PersonObserver.Inntekt,
            PersonObserver.Refusjon,
            PersonObserver.Arbeidsgiverperiode
        )

        val trengerArbeidsgiveropplysningerEvent = observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last()
        assertEquals(expectedForespurteOpplysninger, trengerArbeidsgiveropplysningerEvent.forespurteOpplysninger)

        val expectedSykmeldingsperioder = listOf(
            1.januar til 7.januar,
            9.januar til 14.januar,
            16.januar til 21.januar
        )
        assertEquals(expectedSykmeldingsperioder, trengerArbeidsgiveropplysningerEvent.sykmeldingsperioder)
        assertEquals(1, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
    }

    @Test
    fun `ber ikke om inntekt når vi allerede har inntekt på skjæringstidspunktet -- med arbeidsgiverperiode`() {
        nyeVedtak(januar, a1, a2)
        forlengVedtak(februar, orgnummer = a2)
        nyPeriode(mars, a1)

        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, orgnummer = a1)

        assertEquals(5, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
        assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.count { it.vedtaksperiodeId == 1.vedtaksperiode.id(a1) })
        assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.count { it.vedtaksperiodeId == 1.vedtaksperiode.id(a2) })
        assertEquals(1, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.count { it.vedtaksperiodeId == 2.vedtaksperiode.id(a1) })

        val actualForespurteOpplysninger =
            observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last().forespurteOpplysninger
        val expectedForespurteOpplysninger = setOf(
            PersonObserver.Refusjon,
            PersonObserver.Arbeidsgiverperiode
        )
        assertEquals(expectedForespurteOpplysninger, actualForespurteOpplysninger)
    }

    @Test
    fun `ber ikke om inntekt og AGP når vi har inntekt på skjæringstidspunkt og det er mindre enn 16 dagers gap`() {
        nyeVedtak(januar, a1, a2)
        forlengVedtak(1.februar til 10.februar, orgnummer = a2)
        nyPeriode(11.februar til 28.februar, a1)

        assertEquals(5, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
        assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.count { it.vedtaksperiodeId == 1.vedtaksperiode.id(a1) })
        assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.count { it.vedtaksperiodeId == 1.vedtaksperiode.id(a2) })
        assertEquals(1, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.count { it.vedtaksperiodeId == 2.vedtaksperiode.id(a1) })

        val actualForespurteOpplysninger =
            observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last().forespurteOpplysninger
        val expectedForespurteOpplysninger = setOf(
            PersonObserver.Refusjon
        )
        assertEquals(expectedForespurteOpplysninger, actualForespurteOpplysninger)
    }

    @Test
    fun `sender ikke med Inntekt når vi allerede har en inntektsmelding lagt til grunn på skjæringstidspunktet`() {
        nyeVedtak(januar, a1, a2)
        forlengVedtak(februar, orgnummer = a2)
        nyPeriode(mars, a1)

        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, orgnummer = a1)

        assertEquals(5, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
        assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.count { it.vedtaksperiodeId == 1.vedtaksperiode.id(a1) })
        assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.count { it.vedtaksperiodeId == 1.vedtaksperiode.id(a2) })
        assertEquals(1, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.count { it.vedtaksperiodeId == 2.vedtaksperiode.id(a1) })
        val actualForespurtOpplysning = observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last().forespurteOpplysninger

        val expectedForespurteOpplysninger = setOf(
            PersonObserver.Refusjon,
            PersonObserver.Arbeidsgiverperiode
        )

        assertEquals(expectedForespurteOpplysninger, actualForespurtOpplysning)
    }

    @Test
    fun `sender med første fraværsdag på alle arbeidsgivere for skjæringstidspunktet`()  {
        nyeVedtakMedUlikFom(
            mapOf(
                a1 to (januar),
                a2 to (2.januar til 31.januar)
            )
        )
        assertEquals(4, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
        val actualForespørsel = observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last()
        val expectedForespørsel = PersonObserver.TrengerArbeidsgiveropplysningerEvent(
            personidentifikator = UNG_PERSON_FNR_2018,
            yrkesaktivitetssporing = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a2),
            vedtaksperiodeId = 1.vedtaksperiode.id(a2),
            skjæringstidspunkt = 1.januar,
            sykmeldingsperioder = listOf(2.januar til 31.januar),
            egenmeldingsperioder = emptyList(),
            førsteFraværsdager = listOf(
                PersonObserver.FørsteFraværsdag(Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a1), 1.januar),
                PersonObserver.FørsteFraværsdag(Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a2), 2.januar)
            ),
            forespurteOpplysninger = setOf(
                PersonObserver.Inntekt,
                PersonObserver.Refusjon,
                PersonObserver.Arbeidsgiverperiode
            )
        )
        assertEquals(expectedForespørsel, actualForespørsel)
    }

    @Test
    fun `sender ikke med Inntekt når vi allerede har inntekt fra skatt lagt til grunn på skjæringstidspunktet`() {
        nyeVedtakMedUlikFom(
            mapOf(
                a1 to (31.desember(2017) til 31.januar),
                a2 to (januar)
            )
        )
        assertVarsler(listOf(RV_VV_2), 1.vedtaksperiode.filter(a1))
        forlengVedtak(februar, orgnummer = a1)
        nyPeriode(mars, a2)
        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, orgnummer = a2)

        assertEquals(5, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
        assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.count { it.vedtaksperiodeId == 1.vedtaksperiode.id(a1) })
        assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.count { it.vedtaksperiodeId == 1.vedtaksperiode.id(a2) })
        assertEquals(0, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.count { it.vedtaksperiodeId == 2.vedtaksperiode.id(a1) })
        assertEquals(1, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.count { it.vedtaksperiodeId == 2.vedtaksperiode.id(a2) })

        val actualForespurtOpplysning =
            observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last().forespurteOpplysninger
        val expectedForespurteOpplysninger = setOf(
            PersonObserver.Refusjon,
            PersonObserver.Arbeidsgiverperiode
        )
        assertEquals(expectedForespurteOpplysninger, actualForespurtOpplysning)
    }

    @Test
    fun `be om arbeidsgiverperiode ved forlengelse av en kort periode`() {
        nyPeriode(1.januar til 16.januar)
        nyPeriode(17.januar til 31.januar)

        val trengerArbeidsgiveropplysningerEvent = observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last()
        val expectedForespurteOpplysninger = setOf(
            PersonObserver.Inntekt,
            PersonObserver.Refusjon,
            PersonObserver.Arbeidsgiverperiode
        )

        assertEquals(expectedForespurteOpplysninger, trengerArbeidsgiveropplysningerEvent.forespurteOpplysninger)
        assertEquals(1, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
    }

    @Test
    fun `be om arbeidsgiverperiode når en kort periode har et lite gap til ny periode`() {
        nyPeriode(1.januar til 16.januar)
        nyPeriode(20.januar til 31.januar)

        val trengerArbeidsgiveropplysningerEvent = observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last()

        val expectedForespurteOpplysninger = setOf(
            PersonObserver.Inntekt,
            PersonObserver.Refusjon,
        )

        assertEquals(expectedForespurteOpplysninger, trengerArbeidsgiveropplysningerEvent.forespurteOpplysninger)
        assertEquals(1, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
    }

    @Test
    fun `blir syk fra ghost`()  {
        håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent), orgnummer = a1)
        håndterArbeidsgiveropplysninger(
            listOf(1.januar til 16.januar),
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2, orgnummer = a1)
        this@TrengerArbeidsgiveropplysningerTest.håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        this@TrengerArbeidsgiveropplysningerTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        håndterSøknad(Sykdom(1.februar, 28.februar, 100.prosent), orgnummer = a2)
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, orgnummer = a2)

        val arbeidsgiveropplysningerEvents = observatør.trengerArbeidsgiveropplysningerVedtaksperioder
        assertEquals(2, arbeidsgiveropplysningerEvents.size)
        val trengerArbeidsgiveropplysningerEvent = arbeidsgiveropplysningerEvents.last()

        val expectedForespurteOpplysninger = setOf(
            PersonObserver.Refusjon,
            PersonObserver.Arbeidsgiverperiode
        )
        assertEquals(expectedForespurteOpplysninger, trengerArbeidsgiveropplysningerEvent.forespurteOpplysninger)

        nullstillTilstandsendringer()
        håndterArbeidsgiveropplysninger(
            listOf(1.februar til 16.februar),
            orgnummer = a2,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode
        )
        assertVarsler(listOf(RV_VV_2), AktivitetsloggFilter.arbeidsgiver(a1))
        assertVarsler(emptyList(), AktivitetsloggFilter.arbeidsgiver(a2))

        assertTilstander(1.vedtaksperiode, AVSLUTTET, AVVENTER_REVURDERING, AVVENTER_HISTORIKK_REVURDERING, orgnummer = a1)
        assertTilstander(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, orgnummer = a2)
    }

    @Test
    fun `skal kun sende med refusjonsopplysninger som overlapper med, eller er etter, vedtaksperioden som ikke trenger inntektsopplysninger fra arbeidsgiver`() {
        nyPeriode(januar, a1)
        nyPeriode(januar, a2)
        håndterArbeidsgiveropplysninger(
            arbeidsgiverperioder = listOf(1.januar til 16.januar),
            beregnetInntekt = INNTEKT,
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )
        håndterArbeidsgiveropplysninger(
            arbeidsgiverperioder = listOf(1.januar til 16.januar),
            beregnetInntekt = INNTEKT,
            refusjon = Inntektsmelding.Refusjon(
                beløp = INNTEKT,
                opphørsdato = null,
                endringerIRefusjon = listOf(
                    Inntektsmelding.Refusjon.EndringIRefusjon(18000.månedlig, 10.februar),
                    Inntektsmelding.Refusjon.EndringIRefusjon(17000.månedlig, 1.mars),
                    Inntektsmelding.Refusjon.EndringIRefusjon(16000.månedlig, 10.mars),
                    Inntektsmelding.Refusjon.EndringIRefusjon(15000.månedlig, 1.april)
                )
            ),
            orgnummer = a2,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )
        fraVilkårsprøvingTilGodkjent()
        forlengVedtak(februar, orgnummer = a1)

        nyPeriode(mars, a2)

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET, orgnummer = a1)

        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, orgnummer = a2)

        val trengerArbeidsgiveropplysningerEvents = observatør.trengerArbeidsgiveropplysningerVedtaksperioder

        val trengerArbeidsgiveropplysningerEvent = trengerArbeidsgiveropplysningerEvents.last()

        val expectedForespurteOpplysninger = setOf(
            PersonObserver.Refusjon,
            PersonObserver.Arbeidsgiverperiode
        )

        assertEquals(
            expectedForespurteOpplysninger,
            trengerArbeidsgiveropplysningerEvent.forespurteOpplysninger
        )
    }

    @Test
    fun `skal sende med riktig refusjonsopplysninger ved ingen refusjon`() {
        nyPeriode(januar, a1)
        nyPeriode(januar, a2)
        håndterArbeidsgiveropplysninger(
            arbeidsgiverperioder = listOf(1.januar til 16.januar),
            beregnetInntekt = INNTEKT,
            orgnummer = a1,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )
        håndterArbeidsgiveropplysninger(
            arbeidsgiverperioder = listOf(1.januar til 16.januar),
            beregnetInntekt = INNTEKT,
            refusjon = Inntektsmelding.Refusjon(
                beløp = INNTEKT,
                opphørsdato = 15.mars
            ),
            orgnummer = a2,
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )
        fraVilkårsprøvingTilGodkjent()
        forlengVedtak(februar, orgnummer = a1)
        nyPeriode(mars, a2)

        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a1)
        assertSisteTilstand(1.vedtaksperiode, AVSLUTTET, orgnummer = a2)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET, orgnummer = a1)

        assertTilstander(2.vedtaksperiode, START, AVVENTER_INNTEKTSMELDING, orgnummer = a2)

        val trengerArbeidsgiveropplysningerEvents = observatør.trengerArbeidsgiveropplysningerVedtaksperioder

        val trengerArbeidsgiveropplysningerEvent = trengerArbeidsgiveropplysningerEvents.last()

        val expectedForespurteOpplysninger = setOf(
            PersonObserver.Refusjon,
            PersonObserver.Arbeidsgiverperiode
        )

        assertEquals(
            expectedForespurteOpplysninger,
            trengerArbeidsgiveropplysningerEvent.forespurteOpplysninger
        )
    }

    @Test
    fun `Skal ikke be om arbeidsgiveropplysninger for perioder innenfor arbeidsgiverperioden`() {
        nyPeriode(1.januar til 10.januar)
        assertTrue(observatør.trengerArbeidsgiveropplysningerVedtaksperioder.isEmpty())
        assertTilstander(1.vedtaksperiode, START, AVVENTER_INFOTRYGDHISTORIKK, AVVENTER_INNTEKTSMELDING, AVVENTER_BLOKKERENDE_PERIODE, AVSLUTTET_UTEN_UTBETALING)
    }

    @Test
    fun `Skal ikke sende ut forespørsel for en periode som allerede har mottatt inntektsmelding`() {
        håndterInntektsmelding(
            listOf(1.januar til 16.januar)
        )
        nyPeriode(januar)
        assertEquals(0, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
    }

    @Test
    fun `Periode etter kort gap skal ikke sende forespørsel dersom inntektsmeldingen allerede er mottatt`() {
        nyttVedtak(januar)

        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            førsteFraværsdag = 10.februar
        )
        håndterSykmelding(Sykmeldingsperiode(10.februar, 5.mars))
        håndterSøknad(Sykdom(10.februar, 5.mars, sykmeldingsgrad = 100.prosent))

        assertEquals(1, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
    }

    @Test
    fun `Skal ikke sende ut forespørsel for en periode som allerede har mottatt inntektsmelding -- selv om håndteringen feiler`() {
        håndterInntektsmelding(
            listOf(1.januar til 16.januar),
            opphørAvNaturalytelser = listOf(Inntektsmelding.OpphørAvNaturalytelse(1000.månedlig, 1.januar, "BIL"))
        )
        nyPeriode(januar)
        assertEquals(0, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
    }

    @Test
    fun `Skal sende ut forespørsel for en periode dersom inntektsmeldingReplay ikke bærer noen frukter`() {
        håndterInntektsmelding(
            listOf(1.januar til 16.januar)
        )
        nyPeriode(februar)
        assertEquals(1, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
    }

    @Test
    fun `Skal sende egenmeldingsdager fra søknad i forespørsel`() {
        håndterSykmelding(Sykmeldingsperiode(5.januar, 31.januar))
        håndterSøknad(Sykdom(5.januar, 31.januar, 100.prosent), egenmeldinger = listOf(1.januar til 4.januar))
        assertEquals(5.januar til 31.januar, inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.periode)

        val expectedEgenmeldinger = listOf(1.januar til 4.januar)
        assertEquals(expectedEgenmeldinger, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last().egenmeldingsperioder)
    }

    @Test
    fun `Sender med egenmeldingsdager fra kort søknad`() {
        håndterSykmelding(Sykmeldingsperiode(5.januar, 10.januar))
        håndterSøknad(Sykdom(5.januar, 10.januar, 100.prosent), egenmeldinger = listOf(4.januar til 4.januar))

        nyPeriode(11.januar til 31.januar)

        val exeptectedVedtaksperiode = 5.januar til 10.januar
        assertEquals(exeptectedVedtaksperiode, inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.periode)

        val expectedEgenmeldinger = listOf(4.januar til 4.januar)
        assertEquals(expectedEgenmeldinger, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last().egenmeldingsperioder)
    }

    @Test
    fun `Skal ikke sende med skjønnsfastsatt sykpengegrunnlag som inntektForrigeSkjæringstidspunkt`() {
        val inntektsmeldingId = UUID.randomUUID()
        nyttVedtak(januar, inntektsmeldingId = inntektsmeldingId)
        this@TrengerArbeidsgiveropplysningerTest.håndterSkjønnsmessigFastsettelse(1.januar, listOf(OverstyrtArbeidsgiveropplysning(a1, INNTEKT * 2)))
        this@TrengerArbeidsgiveropplysningerTest.håndterYtelser(1.vedtaksperiode)
        håndterSimulering(1.vedtaksperiode)
        this@TrengerArbeidsgiveropplysningerTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode)
        håndterUtbetalt()

        nyPeriode(16.februar til 15.mars)

        assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)

        val expectedForespurteOpplysninger = setOf(
            PersonObserver.Inntekt,
            PersonObserver.Refusjon,

            )
        val actualForespurteOpplysninger =
            observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last().forespurteOpplysninger
        assertEquals(expectedForespurteOpplysninger, actualForespurteOpplysninger)
    }

    @Test
    fun `Skal sende med saksbehandlerinntekt som inntektForrigeSkjæringstidspunkt`() {
        nyttVedtak(januar)
        this@TrengerArbeidsgiveropplysningerTest.håndterOverstyrArbeidsgiveropplysninger(
            skjæringstidspunkt = 1.januar,
            arbeidsgiveropplysninger = listOf(
                OverstyrtArbeidsgiveropplysning(a1, 32000.månedlig, listOf(Triple(1.januar, null, 32000.månedlig)))
            )
        )
        this@TrengerArbeidsgiveropplysningerTest.håndterYtelser(1.vedtaksperiode)

        nyPeriode(16.februar til 15.mars)

        assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)

        val expectedForespurteOpplysninger = setOf(
            PersonObserver.Inntekt,
            PersonObserver.Refusjon,
        )
        val actualForespurteOpplysninger =
            observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last().forespurteOpplysninger
        assertEquals(expectedForespurteOpplysninger, actualForespurteOpplysninger)
    }

    @Test
    fun `Skal ikke sende med inntektForrigeSkjæringstidspunkt fra annen arbeidsgiver`() {
        nyttVedtak(januar, orgnummer = a1)
        nyPeriode(mars, orgnummer = a2)

        assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)

        val expectedForespurteOpplysninger = setOf(
            PersonObserver.Inntekt,
            PersonObserver.Refusjon,
            PersonObserver.Arbeidsgiverperiode
        )
        val actualForespurteOpplysninger =
            observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last().forespurteOpplysninger
        assertEquals(expectedForespurteOpplysninger, actualForespurteOpplysninger)
    }

    @Test
    fun `Sender kun med refusjonsopplysninger som overlapper med eller er nyere enn forespørselsperioden`() {
        nyPeriode(januar)
        håndterArbeidsgiveropplysninger(
            arbeidsgiverperioder = listOf(1.januar til 16.januar),
            refusjon = Inntektsmelding.Refusjon(
                INNTEKT, 1.april, endringerIRefusjon = listOf(
                Inntektsmelding.Refusjon.EndringIRefusjon(29000.månedlig, 20.januar),
                Inntektsmelding.Refusjon.EndringIRefusjon(28000.månedlig, 30.januar),
                Inntektsmelding.Refusjon.EndringIRefusjon(27000.månedlig, 20.februar)
            )
            ),
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode)

        nyPeriode(15.februar til 28.februar)
        val expectedForespurteOpplysninger = setOf(
            PersonObserver.Inntekt,
            PersonObserver.Refusjon,
        )
        val actualForespurteOpplysninger =
            observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last().forespurteOpplysninger
        assertEquals(expectedForespurteOpplysninger, actualForespurteOpplysninger)
    }

    @Test
    fun `Sender med riktige refusjonsopplysninger ved opphør av refusjon før perioden`() {
        nyPeriode(januar)
        håndterArbeidsgiveropplysninger(
            arbeidsgiverperioder = listOf(1.januar til 16.januar),
            refusjon = Inntektsmelding.Refusjon(INNTEKT, 10.februar, endringerIRefusjon = emptyList()),
            vedtaksperiodeIdInnhenter = 1.vedtaksperiode,
        )
        håndterVilkårsgrunnlag(1.vedtaksperiode)

        nyPeriode(15.februar til 28.februar)
        val expectedForespurteOpplysninger = setOf(
            PersonObserver.Inntekt,
            PersonObserver.Refusjon
        )
        val actualForespurteOpplysninger =
            observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last().forespurteOpplysninger
        assertEquals(expectedForespurteOpplysninger, actualForespurteOpplysninger)
    }

    @Test
    fun `Sender med tom liste med refusjonsopplysninger når vi mangler vilkårsgrunnlag på forrige skjæringstidspunkt`() {
        nyPeriode(januar)
        nyPeriode(15.februar til 28.februar)

        val expectedForespurteOpplysninger = setOf(
            PersonObserver.Inntekt,
            PersonObserver.Refusjon
        )
        val actualForespurteOpplysninger =
            observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last().forespurteOpplysninger
        assertEquals(expectedForespurteOpplysninger, actualForespurteOpplysninger)
    }

    @Test
    fun `Sender trenger_ikke_opplysninger_fra_arbeidsgiver-event for out-of-order som er kant i kant`() {
        nyPeriode(februar)
        nyPeriode(januar)

        assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
        assertEquals(1, observatør.trengerIkkeArbeidsgiveropplysningerVedtaksperioder.size)
    }

    @Test
    fun `Sender ikke trenger_ikke_opplysninger_fra_arbeidsgiver-event for out-of-order med gap`() {
        nyPeriode(februar)
        nyPeriode(1.januar til 30.januar)

        assertEquals(3, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
        assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.count { it.vedtaksperiodeId == 1.vedtaksperiode.id(a1) })
        assertEquals(1, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.count { it.vedtaksperiodeId == 2.vedtaksperiode.id(a1) })
        assertEquals(0, observatør.trengerIkkeArbeidsgiveropplysningerVedtaksperioder.size)
    }

    @Test
    fun `Sender ikke trenger_ikke_opplysninger_fra_arbeidsgiver-event for out-of-order som ikke fører til en ny forespørsel`() {
        nyPeriode(februar)
        nyPeriode(25.januar til 31.januar)

        assertEquals(0, observatør.trengerIkkeArbeidsgiveropplysningerVedtaksperioder.size)
        assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        assertSisteTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
        assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.count { it.vedtaksperiodeId == 1.vedtaksperiode.id(a1) })
        assertEquals(0, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.count { it.vedtaksperiodeId == 2.vedtaksperiode.id(a1) })
    }

    @Test
    fun `Skal ikke sende ut forespørsler dersom vi er innenfor arbeidsgiverperioden`() {
        nyPeriode(1.januar til 2.januar)
        nyPeriode(3.januar til 6.januar)

        håndterInntektsmelding(
            emptyList(),
            førsteFraværsdag = 1.januar,
            begrunnelseForReduksjonEllerIkkeUtbetalt = "ManglerOpptjening",
        )

        assertVarsel(Varselkode.RV_IM_8, 1.vedtaksperiode.filter())
        assertEquals(0, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
        assertEquals(0, observatør.trengerIkkeArbeidsgiveropplysningerVedtaksperioder.size)
    }

    @Test
    fun `forlengelse av auu som slutter på en lørdag skal be om agp`() {
        nyPeriode(4.januar til 20.januar)
        assertEquals(1, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
        nyPeriode(21.januar til 31.januar)
        assertEquals(1, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)

        val expectedForespurteOpplysninger = setOf(
            PersonObserver.Inntekt,
            PersonObserver.Refusjon,
            PersonObserver.Arbeidsgiverperiode
        )
        val actualForespurteOpplysninger =
            observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last().forespurteOpplysninger
        assertEquals(expectedForespurteOpplysninger, actualForespurteOpplysninger)
    }

    @Test
    fun `Sender ikke med sykmeldingsperioder som er etter skjæringstidspunktet`() {
        nyPeriode(januar)
        nyPeriode(februar)
        this@TrengerArbeidsgiveropplysningerTest.håndterPåminnelse(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)

        val actualJanuarForespørsel = observatør.trengerArbeidsgiveropplysningerVedtaksperioder[1]
        assertEquals(listOf(januar), actualJanuarForespørsel.sykmeldingsperioder)
    }

    @Test
    fun `Sender ikke med tidligere sykmeldingsperioder knyttet til vedtaksperiodens AGP når vi ikke spør om AGP i forespørsel`() {
        nyPeriode(1.januar til 17.januar)
        nyPeriode(20.januar til 31.januar)

        val actualForespørsel = observatør.trengerArbeidsgiveropplysningerVedtaksperioder[1]
        assertEquals(listOf(20.januar til 31.januar), actualForespørsel.sykmeldingsperioder)
    }

    @Test
    fun `Skal ikke sende med egenmeldingsperioder når vi ikke ber om AGP`() {
        nyttVedtak(1.januar til 17.januar)
        håndterSykmelding(Sykmeldingsperiode(20.januar, 31.januar))
        håndterSøknad(
            Sykdom(20.januar, 31.januar, 100.prosent),
            egenmeldinger = listOf(19.januar til 19.januar)
        )
        assertEquals(20.januar til 31.januar, inspektør.vedtaksperioder(2.vedtaksperiode).inspektør.periode)

        håndterArbeidsgiveropplysninger(emptyList(), vedtaksperiodeIdInnhenter = 2.vedtaksperiode)
        håndterVilkårsgrunnlag(2.vedtaksperiode)
        this@TrengerArbeidsgiveropplysningerTest.håndterYtelser(2.vedtaksperiode)

        val actualForespørsel = observatør.trengerArbeidsgiveropplysningerVedtaksperioder[1]
        assertForventetFeil(
            forklaring = "Trenger ikke sende med egenmeldingsperioder når vi ikke ber om AGP, burde være sikker på at egenmeldinger ikke kommer fra søknad på perioden uten ny AGP",
            nå = {
                assertEquals(listOf(19.januar til 19.januar), actualForespørsel.egenmeldingsperioder)
            },
            ønsket = {
                assertEquals(emptyList<Periode>(), actualForespørsel.egenmeldingsperioder)
            }
        )
    }

    @Test
    fun `dersom kort periode allerede har fått inntektsmelding trenger ikke neste periode å be om AGP`() {
        nyPeriode(1.januar til 16.januar)
        håndterInntektsmelding(listOf(1.januar til 16.januar))
        nyPeriode(18.januar til 31.januar)

        val expectedForespurteOpplysninger = setOf(
            PersonObserver.Inntekt,
            PersonObserver.Refusjon,
        )
        val actualForespørsel = observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last()
        assertEquals(expectedForespurteOpplysninger, actualForespørsel.forespurteOpplysninger)
    }

    @Test
    fun `egenmeldinger som hadde strukket perioden utover AGP skal føre til forespørsel om arbeidsgiveropplysninger`() {
        nyPeriode(2.januar til 16.januar)
        håndterSøknad(Sykdom(25.januar, 25.januar, 100.prosent), egenmeldinger = listOf(24.januar til 24.januar))

        assertTilstand(2.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
        assertEquals(1, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
    }

    private fun nyeVedtakMedUlikFom(sykefraværHosArbeidsgiver: Map<String, Periode>) {
        val ag1Periode = sykefraværHosArbeidsgiver[a1]!!
        val ag2Periode = sykefraværHosArbeidsgiver[a2]!!
        nyPeriode(ag1Periode.start til ag1Periode.endInclusive, a1)
        nyPeriode(ag2Periode.start til ag2Periode.endInclusive, a2)

        håndterInntektsmelding(
            listOf(ag1Periode.start til ag1Periode.start.plusDays(15)),
            beregnetInntekt = INNTEKT,
            orgnummer = a1
        )
        håndterInntektsmelding(
            listOf(ag2Periode.start til ag2Periode.start.plusDays(15)),
            beregnetInntekt = INNTEKT,
            orgnummer = a2
        )

        fraVilkårsprøvingTilGodkjent()
    }

    private fun fraVilkårsprøvingTilGodkjent() {
        håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2, orgnummer = a1)
        this@TrengerArbeidsgiveropplysningerTest.håndterYtelser(1.vedtaksperiode, orgnummer = a1)
        håndterSimulering(1.vedtaksperiode, orgnummer = a1)
        this@TrengerArbeidsgiveropplysningerTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a1)
        håndterUtbetalt(orgnummer = a1)

        this@TrengerArbeidsgiveropplysningerTest.håndterYtelser(1.vedtaksperiode, orgnummer = a2)
        håndterSimulering(1.vedtaksperiode, orgnummer = a2)
        this@TrengerArbeidsgiveropplysningerTest.håndterUtbetalingsgodkjenning(1.vedtaksperiode, orgnummer = a2)
        håndterUtbetalt(orgnummer = a2)
    }

    private fun assertEtterspurt(vedtaksperiode: UUID, vararg forventet: KClass<out PersonObserver.ForespurtOpplysning>) =
        observatør.assertEtterspurt(vedtaksperiode, *forventet)

    internal companion object {
        fun TestObservatør.assertEtterspurt(vedtaksperiode: UUID, vararg forventet: KClass<out PersonObserver.ForespurtOpplysning>) {
            val forespurteOpplysninger = trengerArbeidsgiveropplysningerVedtaksperioder.lastOrNull { it.vedtaksperiodeId == vedtaksperiode }?.forespurteOpplysninger ?: emptyList()
            assertEquals(forventet.toSet(), forespurteOpplysninger.map { it::class }.toSet())
        }
    }
}
