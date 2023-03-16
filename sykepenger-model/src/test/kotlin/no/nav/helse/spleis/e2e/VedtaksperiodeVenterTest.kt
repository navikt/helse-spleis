package no.nav.helse.spleis.e2e

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.TestPerson.Companion.AKTØRID
import no.nav.helse.dsl.TestPerson.Companion.UNG_PERSON_FNR_2018
import no.nav.helse.dsl.nyPeriode
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING
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
            val forventetVedtaksperiode1 = PersonObserver.VedtaksperiodeVenterEvent(
                fødselsnummer = UNG_PERSON_FNR_2018.toString(),
                aktørId = AKTØRID,
                organisasjonsnummer = a1,
                vedtaksperiodeId = 1.vedtaksperiode,
                hendelser = setOf(søknadIdJanuar),
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
            val forventetVedtaksperiode2 = PersonObserver.VedtaksperiodeVenterEvent(
                fødselsnummer = UNG_PERSON_FNR_2018.toString(),
                aktørId = AKTØRID,
                organisasjonsnummer = a1,
                vedtaksperiodeId = 2.vedtaksperiode,
                hendelser = setOf(søknadIdMars, inntektsmeldingIdMars),
                ventetSiden = inspektør.vedtaksperioder(2.vedtaksperiode).inspektør.oppdatert,
                venterTil = LocalDateTime.MAX,
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
                venterTil = LocalDateTime.MAX,
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
}