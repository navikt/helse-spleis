package no.nav.helse.spleis.e2e.arbeidsgiveropplysninger

import no.nav.helse.februar
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.spleis.e2e.assertTilstand
import no.nav.helse.spleis.e2e.håndterSykmelding
import no.nav.helse.spleis.e2e.håndterSøknad
import no.nav.helse.spleis.e2e.nyPeriode
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class TrengerPotensieltArbeidsgiveropplysningerTest : AbstractEndToEndTest() {

    @Test
    fun `sender ut event TrengerPotensieltArbeidsgiveropplysninger når vi ankommer AvsluttetUtenUtbetaling`() {
        nyPeriode(1.januar til 16.januar)
        assertEquals(0, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
        assertEquals(1, observatør.trengerPotensieltArbeidsgiveropplysningerVedtaksperioder.size)

        val expectedPotensiellForespørsel = PersonObserver.TrengerPotensieltArbeidsgiveropplysningerEvent(
            ORGNUMMER,
            inspektør.vedtaksperiodeId(1.vedtaksperiode),
            1.januar,
            sykmeldingsperioder = listOf(1.januar til 16.januar),
            egenmeldingsperioder = emptyList(),
            førsteFraværsdager = listOf(mapOf("organisasjonsnummer" to ORGNUMMER, "førsteFraværsdag" to 1.januar))
        )

        assertEquals(expectedPotensiellForespørsel, observatør.trengerPotensieltArbeidsgiveropplysningerVedtaksperioder.single())
    }

    @Test
    fun `sender sykmeldingsperioder som er knyttet til samme AGP, men ikke etter gjeldende forespørsel`() {
        nyPeriode(1.januar til 1.januar)
        nyPeriode(1.februar til 2.februar)
        nyPeriode(3.februar til 3.februar)
        nyPeriode(9.februar til 9.februar)
        nyPeriode(7.februar til 7.februar)

        assertTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertTilstand(2.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertTilstand(3.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertTilstand(4.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
        assertTilstand(5.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)

        assertEquals(0, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
        assertEquals(5, observatør.trengerPotensieltArbeidsgiveropplysningerVedtaksperioder.size)

        val expectedPotensiellForespørsel1 = PersonObserver.TrengerPotensieltArbeidsgiveropplysningerEvent(
            ORGNUMMER,
            inspektør.vedtaksperiodeId(4.vedtaksperiode),
            9.februar,
            sykmeldingsperioder = listOf(
                1.februar til 2.februar,
                3.februar til 3.februar,
                9.februar til 9.februar
            ),
            egenmeldingsperioder = emptyList(),
            førsteFraværsdager = listOf(mapOf("organisasjonsnummer" to ORGNUMMER, "førsteFraværsdag" to 9.februar))
        )

        assertEquals(expectedPotensiellForespørsel1, observatør.trengerPotensieltArbeidsgiveropplysningerVedtaksperioder[3])

        val expectedPotensiellForespørsel2 = PersonObserver.TrengerPotensieltArbeidsgiveropplysningerEvent(
            ORGNUMMER,
            inspektør.vedtaksperiodeId(5.vedtaksperiode),
            7.februar,
            sykmeldingsperioder = listOf(
                1.februar til 2.februar,
                3.februar til 3.februar,
                7.februar til 7.februar
            ),
            egenmeldingsperioder = emptyList(),
            førsteFraværsdager = listOf(mapOf("organisasjonsnummer" to ORGNUMMER, "førsteFraværsdag" to 7.februar))
        )

        assertEquals(expectedPotensiellForespørsel2, observatør.trengerPotensieltArbeidsgiveropplysningerVedtaksperioder.last())
    }

    @Test
    fun `sender egenmeldingsperioder fra søknader som er knyttet til samme AGP`() {
        håndterSykmelding(Sykmeldingsperiode(5.januar, 16.januar))
        håndterSøknad(
            Søknad.Søknadsperiode.Sykdom(5.januar, 16.januar, 100.prosent),
            egenmeldinger = listOf(Søknad.Søknadsperiode.Arbeidsgiverdag(1.januar, 4.januar)
        ))

        // Langt gap fra forrige periode
        håndterSykmelding(Sykmeldingsperiode(5.mars, 8.mars))
        håndterSøknad(
            Søknad.Søknadsperiode.Sykdom(5.mars, 8.mars, 100.prosent),
            egenmeldinger = listOf(Søknad.Søknadsperiode.Arbeidsgiverdag(1.mars, 2.mars)
        ))

        // Kort gap fra forrige periode
        håndterSykmelding(Sykmeldingsperiode(14.mars, 16.mars))
        håndterSøknad(
            Søknad.Søknadsperiode.Sykdom(14.mars, 16.mars, 100.prosent),
            egenmeldinger = listOf(Søknad.Søknadsperiode.Arbeidsgiverdag(13.mars, 13.mars)
        ))

        assertEquals(0, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
        assertEquals(3, observatør.trengerPotensieltArbeidsgiveropplysningerVedtaksperioder.size)

        assertEquals(listOf(1.januar til 4.januar), observatør.trengerPotensieltArbeidsgiveropplysningerVedtaksperioder[0].egenmeldingsperioder)
        assertEquals(listOf(1.mars til 2.mars), observatør.trengerPotensieltArbeidsgiveropplysningerVedtaksperioder[1].egenmeldingsperioder)

        val expectedPotensiellForespørsel = PersonObserver.TrengerPotensieltArbeidsgiveropplysningerEvent(
            ORGNUMMER,
            inspektør.vedtaksperiodeId(3.vedtaksperiode),
            13.mars,
            sykmeldingsperioder = listOf(
                5.mars til 8.mars,
                14.mars til 16.mars
            ),
            egenmeldingsperioder = listOf(1.mars til 2.mars, 13.mars til 13.mars),
            førsteFraværsdager = listOf(mapOf("organisasjonsnummer" to ORGNUMMER, "førsteFraværsdag" to 13.mars))
        )
        assertEquals(expectedPotensiellForespørsel, observatør.trengerPotensieltArbeidsgiveropplysningerVedtaksperioder.last())
    }

    @Test
    fun `Skal ikke sende med egenmeldingsdager etter vedtaksperioden sin potensielle forespørsel`() {
        håndterSykmelding(Sykmeldingsperiode(10.januar, 16.januar))
        håndterSøknad(
            Søknad.Søknadsperiode.Sykdom(10.januar, 16.januar, 100.prosent),
            egenmeldinger = listOf(Søknad.Søknadsperiode.Arbeidsgiverdag(9.januar, 9.januar)
        ))
        håndterSykmelding(Sykmeldingsperiode(2.januar, 5.januar))
        håndterSøknad(
            Søknad.Søknadsperiode.Sykdom(2.januar, 5.januar, 100.prosent),
            egenmeldinger = listOf(Søknad.Søknadsperiode.Arbeidsgiverdag(1.januar, 1.januar)
        ))

        assertEquals(listOf(1.januar til 1.januar), observatør.trengerPotensieltArbeidsgiveropplysningerVedtaksperioder.last().egenmeldingsperioder)
    }

    @Test
    fun `Skal sende med første fraværsdager for alle arbeidsgivere på skjæringstidspunktet`() {
        nyPeriode(1.januar til 31.januar, orgnummer = a1)
        nyPeriode(2.januar til 2.januar, orgnummer = a2)

        assertEquals(listOf(
            mapOf("organisasjonsnummer" to a1, "førsteFraværsdag" to 1.januar),
            mapOf("organisasjonsnummer" to a2, "førsteFraværsdag" to 2.januar),
        ), observatør.trengerPotensieltArbeidsgiveropplysningerVedtaksperioder.last().førsteFraværsdager)
    }
}