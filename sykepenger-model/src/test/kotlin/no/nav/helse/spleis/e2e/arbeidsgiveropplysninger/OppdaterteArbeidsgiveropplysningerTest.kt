package no.nav.helse.spleis.e2e.arbeidsgiveropplysninger

import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.OverstyrtArbeidsgiveropplysning
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
import no.nav.helse.person.EventSubscription
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.tilstandsmaskin.TilstandType
import no.nav.helse.spleis.e2e.AktivitetsloggFilter.Companion.filter
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class OppdaterteArbeidsgiveropplysningerTest : AbstractDslTest() {

    @Test
    fun `Søknad fra annen arbeidsgiver flytter skjæringstidspunktet i AVVENTER_INNTEKTSMELDING`() {
        a1 { nyPeriode(2.januar til 31.januar) }
        a2 { nyPeriode(januar) }

        a1 {
            assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.filter { it.opplysninger.vedtaksperiodeId == 1.vedtaksperiode }.size)
        }
        a2 {
            assertEquals(1, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.filter { it.opplysninger.vedtaksperiodeId == 1.vedtaksperiode }.size)
        }

        a1 {
            val expectedForespørsel = EventSubscription.TrengerArbeidsgiveropplysningerEvent(
                EventSubscription.TrengerArbeidsgiveropplysninger(
                    personidentifikator = UNG_PERSON_FNR_2018,
                    arbeidstaker = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a1),
                    vedtaksperiodeId = 1.vedtaksperiode,
                    skjæringstidspunkt = 1.januar,
                    sykmeldingsperioder = listOf(2.januar til 31.januar),
                    egenmeldingsperioder = emptyList(),
                    førsteFraværsdager = listOf(
                        EventSubscription.FørsteFraværsdag(Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a2), 1.januar),
                        EventSubscription.FørsteFraværsdag(Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a1), 2.januar)
                    ),
                    forespurteOpplysninger = setOf(
                        EventSubscription.Inntekt,
                        EventSubscription.Refusjon,
                        EventSubscription.Arbeidsgiverperiode
                    )
                )
            )
            val actualForespørsel = observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last {
                it.opplysninger.vedtaksperiodeId == 1.vedtaksperiode
            }
            assertEquals(expectedForespørsel, actualForespørsel)
        }
    }

    @Test
    fun `Søknad fra annen arbeidsgiver flytter skjæringstidspunktet, skal ikke be om nye opplysninger i annen tilstand enn AVVENTER_INNTEKTSMELDING`() {
        a1 { nyttVedtak(2.januar til 31.januar) }
        a2 { nyPeriode(januar) }

        a1 {
            assertEquals(1, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.filter { it.opplysninger.vedtaksperiodeId == 1.vedtaksperiode }.size)
        }
        a2 {
            assertEquals(1, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.filter { it.opplysninger.vedtaksperiodeId == 1.vedtaksperiode }.size)
        }
    }

    @Test
    fun `oppdaterte opplysninger for mars når ag2 tetter gapet`() {
        a1 {
            nyPeriode(januar)
            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                vedtaksperiodeId = 1.vedtaksperiode
            )
            håndterVilkårsgrunnlagFlereArbeidsgivere(1.vedtaksperiode, a1, a2)
            assertVarsel(Varselkode.RV_VV_2, 1.vedtaksperiode.filter())
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()
        }

        a1 { nyPeriode(mars) }
        a2 { nyPeriode(februar) }

        assertEquals(4, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
        a1 {
            assertEquals(1, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.filter { it.opplysninger.vedtaksperiodeId == 1.vedtaksperiode }.size)
        }
        a2 {
            assertEquals(1, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.filter { it.opplysninger.vedtaksperiodeId == 1.vedtaksperiode }.size)
        }
        a1 {
            val arbeidsgiveropplysningerEventer = observatør.trengerArbeidsgiveropplysningerVedtaksperioder.filter {
                it.opplysninger.vedtaksperiodeId == 2.vedtaksperiode
            }
            assertEquals(2, arbeidsgiveropplysningerEventer.size)
            arbeidsgiveropplysningerEventer.last().also { trengerArbeidsgiveropplysningerEvent ->
                val expectedForespørsel = EventSubscription.TrengerArbeidsgiveropplysningerEvent(
                    EventSubscription.TrengerArbeidsgiveropplysninger(
                        personidentifikator = UNG_PERSON_FNR_2018,
                        arbeidstaker = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a1),
                        vedtaksperiodeId = 2.vedtaksperiode,
                        skjæringstidspunkt = 1.januar,
                        sykmeldingsperioder = listOf(mars),
                        egenmeldingsperioder = emptyList(),
                        førsteFraværsdager = listOf(
                            EventSubscription.FørsteFraværsdag(Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a2), 1.februar),
                            EventSubscription.FørsteFraværsdag(Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a1), 1.mars)
                        ),
                        forespurteOpplysninger = setOf(
                            EventSubscription.Refusjon,
                            EventSubscription.Arbeidsgiverperiode
                        )
                    )
                )
                assertEquals(expectedForespørsel, trengerArbeidsgiveropplysningerEvent)
            }
        }
    }

    @Test
    fun `Overlappende søknad fører til oppdatert forespørsel`() {
        a1 { nyPeriode(20.januar til 20.februar) }
        a2 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
            håndterSøknad(Søknad.Søknadsperiode.Sykdom(1.januar, 31.januar, 100.prosent), Søknad.Søknadsperiode.Arbeid(18.januar, 31.januar))
        }
        a1 { assertEquals(20.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode)) }
        a2 { assertEquals(1.januar, inspektør.skjæringstidspunkt(1.vedtaksperiode)) }
        a2 {
            assertEquals(1, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.filter { it.opplysninger.vedtaksperiodeId == 1.vedtaksperiode }.size)
        }
        a1 {
            assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.filter { it.opplysninger.vedtaksperiodeId == 1.vedtaksperiode }.size)
        }
    }

    @Test
    fun `Sender oppdatert forespørsel når vi vi får inn ny inntekt på en forrige periode`() {
        a1 {
            nyPeriode(januar)
            nyPeriode(10.februar til 10.mars)

            assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), vedtaksperiodeId = 1.vedtaksperiode)

            assertEquals(3, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
            val expectedForespørsel = EventSubscription.TrengerArbeidsgiveropplysningerEvent(
                EventSubscription.TrengerArbeidsgiveropplysninger(
                    personidentifikator = UNG_PERSON_FNR_2018,
                    arbeidstaker = Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a1),
                    vedtaksperiodeId = 2.vedtaksperiode,
                    skjæringstidspunkt = 10.februar,
                    sykmeldingsperioder = listOf(10.februar til 10.mars),
                    egenmeldingsperioder = emptyList(),
                    førsteFraværsdager = listOf(EventSubscription.FørsteFraværsdag(Behandlingsporing.Yrkesaktivitet.Arbeidstaker(a1), 10.februar)),
                    forespurteOpplysninger = setOf(
                        EventSubscription.Inntekt,
                        EventSubscription.Refusjon
                    )
                )
            )
            assertEquals(expectedForespørsel, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last())
        }
    }


    @Test
    fun `Sender ikke med skjønnsmessig inntekt ved oppdatert forespørsel`() {
        a1 {
            nyttVedtak(januar)
            nyPeriode(mars)
            assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)

            håndterSkjønnsmessigFastsettelse(1.januar, listOf(OverstyrtArbeidsgiveropplysning(a1, INNTEKT / 2)))
            assertEquals(3, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
            val forespørsel = observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last()
            assertEquals(
                EventSubscription.Inntekt,
                forespørsel.opplysninger.forespurteOpplysninger.first { it is EventSubscription.Inntekt }
            )
        }
    }

    @Test
    fun `Sender oppdatert forespørsel ved nytt vilkårsgrunnlag pga saksbehandleroverstyrt inntekt`() {
        a1 {
            nyttVedtak(januar)
            nyPeriode(mars)
            assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)

            håndterOverstyrArbeidsgiveropplysninger(1.januar, listOf(OverstyrtArbeidsgiveropplysning(a1, 32000.månedlig)))

            assertEquals(3, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
            val forespørsel = observatør.trengerArbeidsgiveropplysningerVedtaksperioder.last()
            assertEquals(
                EventSubscription.Inntekt,
                forespørsel.opplysninger.forespurteOpplysninger.first { it is EventSubscription.Inntekt }
            )
        }
    }

    @Test
    fun `Sender oppdatert forespørsel ved nytt vilkårsgrunnlag pga korrigerende inntektsmelding -- revurdering`() {
        a1 {
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
                EventSubscription.Inntekt,
                forespørsel.opplysninger.forespurteOpplysninger.first { it is EventSubscription.Inntekt }
            )
        }
    }

    @Test
    fun `sender ikke oppdatert forespørsel for en periode som har mottatt inntektsmelding`() {
        a1 {
            nyPeriode(januar)
            nyPeriode(mars)
            håndterArbeidsgiveropplysninger(
                listOf(1.mars til 16.mars),
                vedtaksperiodeId = 2.vedtaksperiode
            )

            assertTilstand(2.vedtaksperiode, TilstandType.AVVENTER_BLOKKERENDE_PERIODE)
            assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)

            håndterArbeidsgiveropplysninger(
                listOf(1.januar til 16.januar),
                vedtaksperiodeId = 1.vedtaksperiode
            )
            håndterVilkårsgrunnlag(1.vedtaksperiode)

            assertTilstand(2.vedtaksperiode, TilstandType.AVVENTER_BLOKKERENDE_PERIODE)
            assertEquals(2, observatør.trengerArbeidsgiveropplysningerVedtaksperioder.size)
        }
    }
}
