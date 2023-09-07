package no.nav.helse.spleis.e2e.arbeidsgiveropplysninger

import no.nav.helse.EnableToggle
import no.nav.helse.Toggle
import no.nav.helse.assertForventetFeil
import no.nav.helse.desember
import no.nav.helse.februar
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.november
import no.nav.helse.oktober
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.TilstandType
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertTilstander
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.nyPeriode
import no.nav.helse.spleis.e2e.nyttVedtak
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@EnableToggle(Toggle.OPPDATERE_FORESPØRSLER::class)
internal class OppdaterteArbeidsgiveropplysningerTest: AbstractEndToEndTest() {
    @Test
    fun `Sender ny forespørsel når korrigerende søknad kommer før vi har fått svar på forrige forespørsel -- flytter skjæringstidspunktet`() {
        nyPeriode(2.januar til 31.januar)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(2.januar, 31.januar, 100.prosent), egenmeldinger = listOf(Søknad.Søknadsperiode.Arbeidsgiverdag(1.januar, 1.januar)))

        val expectedForespørsel = PersonObserver.TrengerArbeidsgiveropplysningerEvent(
            organisasjonsnummer = ORGNUMMER,
            vedtaksperiodeId = 1.vedtaksperiode.id(ORGNUMMER),
            skjæringstidspunkt = 1.januar,
            sykmeldingsperioder = listOf(2.januar til 31.januar),
            egenmeldingsperioder = listOf(1.januar til 1.januar),
            forespurteOpplysninger = listOf(
                PersonObserver.Inntekt(PersonObserver.Inntektsforslag(beregningsmåneder = listOf(oktober(2017), november(2017), desember(2017)), forrigeInntekt = null)),
                PersonObserver.Refusjon(forslag = emptyList()),
                PersonObserver.Arbeidsgiverperiode
            )
        )

        assertTilstander(1.vedtaksperiode,
            TilstandType.START,
            TilstandType.AVVENTER_INFOTRYGDHISTORIKK,
            TilstandType.AVVENTER_INNTEKTSMELDING
        )
        assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
        val actualForespørsel = observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last()
        assertEquals(expectedForespørsel, actualForespørsel)
    }

    @Test
    fun `Sender ny forespørsel når korrigerende søknad kommer før vi har fått svar på forrige forespørsel -- flytter ikke skjæringstidspunktet`() {
        håndterSykmelding(Sykmeldingsperiode(5.januar, 31.januar))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(5.januar, 31.januar, 100.prosent), egenmeldinger = listOf(Søknad.Søknadsperiode.Arbeidsgiverdag(1.januar, 1.januar)))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(5.januar, 31.januar, 100.prosent), egenmeldinger = listOf(Søknad.Søknadsperiode.Arbeidsgiverdag(1.januar, 2.januar)))

        val expectedForespørsel = PersonObserver.TrengerArbeidsgiveropplysningerEvent(
            organisasjonsnummer = ORGNUMMER,
            vedtaksperiodeId = 1.vedtaksperiode.id(ORGNUMMER),
            skjæringstidspunkt = 5.januar,
            sykmeldingsperioder = listOf(5.januar til 31.januar),
            egenmeldingsperioder = listOf(1.januar til 2.januar),
            forespurteOpplysninger = listOf(
                PersonObserver.Inntekt(PersonObserver.Inntektsforslag(beregningsmåneder = listOf(oktober(2017), november(2017), desember(2017)), forrigeInntekt = null)),
                PersonObserver.Refusjon(forslag = emptyList()),
                PersonObserver.Arbeidsgiverperiode
            )
        )

        assertTilstander(1.vedtaksperiode,
            TilstandType.START,
            TilstandType.AVVENTER_INFOTRYGDHISTORIKK,
            TilstandType.AVVENTER_INNTEKTSMELDING
        )
        assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
        val actualForespørsel = observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last()
        assertEquals(expectedForespørsel, actualForespørsel)
    }

    @Test
    fun `Korrigerende søknad fjerner ikke egenmeldingsdager, er det tiltenkt`() {
        håndterSykmelding(Sykmeldingsperiode(2.januar, 31.januar))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(2.januar, 31.januar, 100.prosent), egenmeldinger = listOf(Søknad.Søknadsperiode.Arbeidsgiverdag(1.januar, 1.januar)))
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(2.januar, 31.januar, 100.prosent))

        val actualForespørsel = observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last()
        assertForventetFeil(
            forklaring = "Vi fjerner ikke egenmeldingsdager dersom korrigerende søknad ikke har egenmeldinger, er dette tiltenkt?",
            nå = {
                Assertions.assertEquals(listOf(1.januar til 1.januar), actualForespørsel.egenmeldingsperioder)
            },
            ønsket = {
                Assertions.fail("""\_(ツ)_/¯""")
            }
        )
    }

    @Test
    fun `Søknad fra annen arbeidsgiver flytter skjæringstidspunktet i AVVENTER_INNTEKTSMELDING`() {
        nyPeriode(2.januar til 31.januar, orgnummer = a1)
        nyPeriode(1.januar til 31.januar, orgnummer = a2)

        assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.filter { it.vedtaksperiodeId == 1.vedtaksperiode.id(a1) }.size)
        assertEquals(1, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.filter { it.vedtaksperiodeId == 1.vedtaksperiode.id(a2) }.size)

        val expectedForespørsel = PersonObserver.TrengerArbeidsgiveropplysningerEvent(
            organisasjonsnummer = a1,
            vedtaksperiodeId = 1.vedtaksperiode.id(a1),
            skjæringstidspunkt = 1.januar,
            sykmeldingsperioder = listOf(2.januar til 31.januar),
            egenmeldingsperioder = emptyList(),
            forespurteOpplysninger = listOf(
                PersonObserver.Inntekt(PersonObserver.Inntektsforslag(beregningsmåneder = listOf(oktober(2017), november(2017), desember(2017)), forrigeInntekt = null)),
                PersonObserver.Refusjon(forslag = emptyList()),
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
        nyttVedtak(2.januar, 31.januar, orgnummer = a1)
        nyPeriode(1.januar til 31.januar, orgnummer = a2)

        assertEquals(1, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.filter { it.vedtaksperiodeId == 1.vedtaksperiode.id(a1) }.size)
        assertEquals(1, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.filter { it.vedtaksperiodeId == 1.vedtaksperiode.id(a2) }.size)
    }

    @Test
    fun `Overlappende søknad med arbeid gjenopptatt slik at skjæringstidspunkt ikke flyttes hos annen arbeidsgiver skal ikke føre til oppdatert forespørsel`() {
        nyPeriode(20.januar til 20.februar, orgnummer = a1)
        håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar), orgnummer = a2)
        håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent), Søknad.Søknadsperiode.Arbeid(18.januar, 31.januar), orgnummer = a2)

        assertEquals(20.januar, inspektør(a1).skjæringstidspunkt(1.vedtaksperiode))
        assertEquals(1.januar, inspektør(a2).skjæringstidspunkt(1.vedtaksperiode))
        assertEquals(1, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.filter { it.vedtaksperiodeId == 1.vedtaksperiode.id(a2) }.size)

        assertEquals(1, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.filter { it.vedtaksperiodeId == 1.vedtaksperiode.id(a1) }.size)
    }
}