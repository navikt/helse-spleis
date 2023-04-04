package no.nav.helse.spleis.e2e

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.TestPerson.Companion.AKTØRID
import no.nav.helse.dsl.TestPerson.Companion.INNTEKT
import no.nav.helse.dsl.TestPerson.Companion.UNG_PERSON_FNR_2018
import no.nav.helse.dsl.nyPeriode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class VedtaksperiodeVenterTest: AbstractDslTest() {

    @Test
    fun `Venter på tidligere periode som ikke har fått inntektsmelding`(){
        a1 {
            val søknadIdJanuar = UUID.randomUUID()
            nyPeriode(1.januar til 31.januar, søknadId = søknadIdJanuar)

            assertEquals(2, observatør.vedtaksperiodeVenter.size)
            val søknadIdMars = UUID.randomUUID()
            nyPeriode(1.mars til 31.mars, søknadId = søknadIdMars)
            assertEquals(6, observatør.vedtaksperiodeVenter.size)

            val inntektsmeldingIdMars = håndterInntektsmelding(listOf(1.mars til 16.mars))
            assertEquals(8, observatør.vedtaksperiodeVenter.size)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
            val venterTil = inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.oppdatert.plusDays(180)
            val forventetVedtaksperiode1 = PersonObserver.VedtaksperiodeVenterEvent(
                fødselsnummer = UNG_PERSON_FNR_2018.toString(),
                aktørId = AKTØRID,
                organisasjonsnummer = a1,
                vedtaksperiodeId = 1.vedtaksperiode,
                hendelser = setOf(søknadIdJanuar),
                ventetSiden = inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.oppdatert,
                venterTil = venterTil,
                venterPå = PersonObserver.VedtaksperiodeVenterEvent.VenterPå(
                    vedtaksperiodeId = 1.vedtaksperiode,
                    organisasjonsnummer = a1,
                    venteårsak = PersonObserver.VedtaksperiodeVenterEvent.Venteårsak(
                        hva = "INNTEKTSMELDING",
                        hvorfor = null
                    )
                )
            )
            val forventetVedtaksperiode2 = PersonObserver.VedtaksperiodeVenterEvent(
                fødselsnummer = UNG_PERSON_FNR_2018.toString(),
                aktørId = AKTØRID,
                organisasjonsnummer = a1,
                vedtaksperiodeId = 2.vedtaksperiode,
                hendelser = setOf(søknadIdMars, inntektsmeldingIdMars),
                ventetSiden = inspektør.vedtaksperioder(2.vedtaksperiode).inspektør.oppdatert,
                venterTil = venterTil,
                venterPå = PersonObserver.VedtaksperiodeVenterEvent.VenterPå(
                    vedtaksperiodeId = 1.vedtaksperiode,
                    organisasjonsnummer = a1,
                    venteårsak = PersonObserver.VedtaksperiodeVenterEvent.Venteårsak(
                        hva = "INNTEKTSMELDING",
                        hvorfor = null
                    )
                )
            )
            assertEquals(forventetVedtaksperiode1, observatør.vedtaksperiodeVenter.last{
                it.vedtaksperiodeId == 1.vedtaksperiode
            })
            assertEquals(forventetVedtaksperiode2, observatør.vedtaksperiodeVenter.last {
                it.vedtaksperiodeId == 2.vedtaksperiode
            })
        }
    }

    @Test
    fun `HAR_SYKMELDING_SOM_OVERLAPPER_PÅ_ANDRE_ARBEIDSGIVERE`(){
        a1 {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar))
        }
        a2 {
            val søknadId = UUID.randomUUID()
            nyPeriode(1.januar til 31.januar, søknadId = søknadId)
            assertEquals(2, observatør.vedtaksperiodeVenter.size)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            val inntektsmeldingId = håndterInntektsmelding(listOf(1.januar til 16.januar))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)
            assertEquals(3, observatør.vedtaksperiodeVenter.size)

            val forventet = PersonObserver.VedtaksperiodeVenterEvent(
                fødselsnummer = UNG_PERSON_FNR_2018.toString(),
                aktørId = AKTØRID,
                organisasjonsnummer = a2,
                vedtaksperiodeId = 1.vedtaksperiode,
                hendelser = setOf(søknadId, inntektsmeldingId),
                ventetSiden = inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.oppdatert,
                venterTil = inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.oppdatert.plusDays(90),
                venterPå = PersonObserver.VedtaksperiodeVenterEvent.VenterPå(
                    vedtaksperiodeId = 1.vedtaksperiode,
                    organisasjonsnummer = a2,
                    venteårsak = PersonObserver.VedtaksperiodeVenterEvent.Venteårsak(
                        hva = "SØKNAD",
                        hvorfor = "HAR_SYKMELDING_SOM_OVERLAPPER_PÅ_ANDRE_ARBEIDSGIVERE"
                    )
                )
            )
            assertEquals(forventet, observatør.vedtaksperiodeVenter.last())
        }
    }

    @Test
    fun `Periode i avventer innteksmelding`() {
        a1 {
            val søknadId = UUID.randomUUID()
            nyPeriode(1.januar til 31.januar, søknadId = søknadId)
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            val forventet = PersonObserver.VedtaksperiodeVenterEvent(
                fødselsnummer = UNG_PERSON_FNR_2018.toString(),
                aktørId = AKTØRID,
                organisasjonsnummer = a1,
                vedtaksperiodeId = 1.vedtaksperiode,
                hendelser = setOf(søknadId),
                ventetSiden = inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.oppdatert,
                venterTil = inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.oppdatert.plusDays(180),
                venterPå = PersonObserver.VedtaksperiodeVenterEvent.VenterPå(
                    vedtaksperiodeId = 1.vedtaksperiode,
                    organisasjonsnummer = a1,
                    venteårsak = PersonObserver.VedtaksperiodeVenterEvent.Venteårsak(
                        hva = "INNTEKTSMELDING",
                        hvorfor = null
                    )
                )
            )
            assertEquals(forventet, observatør.vedtaksperiodeVenter.last())
        }
    }

    @Test
    fun `En periode i Avsluttet Uten Utbetaling som eneste periode får en Infotrygd-utbetaling foran seg vil nå utbetales` () {
        a1 {
            val søknadId = UUID.randomUUID()
            nyPeriode(16.januar til 31.januar, søknadId = søknadId)
            val søknadId2 = UUID.randomUUID()
            håndterSøknad(Sykdom(1.januar, 16.januar, 100.prosent), søknadId = søknadId2)
            assertForkastetPeriodeTilstander(2.vedtaksperiode, START, TIL_INFOTRYGD)
            val infotrygdUtbetaling = ArbeidsgiverUtbetalingsperiode(a1, 1.januar, 16.januar, 100.prosent, INNTEKT)
            håndterUtbetalingshistorikkEtterInfotrygdendring(listOf(infotrygdUtbetaling))
            val forventet = PersonObserver.VedtaksperiodeVenterEvent(
                fødselsnummer = UNG_PERSON_FNR_2018.toString(),
                aktørId = AKTØRID,
                organisasjonsnummer = a1,
                vedtaksperiodeId = 1.vedtaksperiode,
                hendelser = setOf(søknadId, søknadId2),
                ventetSiden = inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.oppdatert,
                venterTil = LocalDateTime.MAX,
                venterPå = PersonObserver.VedtaksperiodeVenterEvent.VenterPå(
                    vedtaksperiodeId = 1.vedtaksperiode,
                    organisasjonsnummer = a1,
                    venteårsak = PersonObserver.VedtaksperiodeVenterEvent.Venteårsak(
                        hva = "HJELP",
                        hvorfor = "VIL_UTBETALES"
                    )
                )
            )
            assertEquals(forventet, observatør.vedtaksperiodeVenter.last())
        }
    }

    @Test
    fun `En periode i Avsluttet Uten Utbetaling som eneste periode som fortsatt ikke skal utbetales skriker ikke om hjelp`() {
        a1 {
            nyPeriode(16.januar til 31.januar)
            assertSisteTilstand(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            val venteHendelseFør = observatør.vedtaksperiodeVenter.toList()
            håndterPåminnelse(1.vedtaksperiode, AVSLUTTET_UTEN_UTBETALING)
            assertEquals(venteHendelseFør, observatør.vedtaksperiodeVenter)
        }
    }

    @Test
    fun `Om perioden man venter på har en timeout bør den brukes som venter til`() {
        a1 {
            håndterSøknad(Sykdom(1.januar, 31.januar, 100.prosent))
            håndterSøknad(Sykdom(1.mars, 31.mars, 100.prosent))
            håndterInntektsmelding(listOf(1.mars til 16.mars))
            assertSisteTilstand(1.vedtaksperiode, AVVENTER_INNTEKTSMELDING)
            assertSisteTilstand(2.vedtaksperiode, AVVENTER_BLOKKERENDE_PERIODE)

            val januarVenterTil = inspektør.vedtaksperioder(1.vedtaksperiode).inspektør.oppdatert.plusDays(180)
            val januarVenter = observatør.vedtaksperiodeVenter.last { it.vedtaksperiodeId == 1.vedtaksperiode }
            assertEquals(1.vedtaksperiode, januarVenter.venterPå.vedtaksperiodeId)
            assertEquals(januarVenterTil, januarVenter.venterTil)
            val marsVenter = observatør.vedtaksperiodeVenter.last { it.vedtaksperiodeId == 2.vedtaksperiode }
            assertEquals(1.vedtaksperiode, marsVenter.venterPå.vedtaksperiodeId)
            assertEquals(januarVenterTil, marsVenter.venterTil)
        }
    }
}