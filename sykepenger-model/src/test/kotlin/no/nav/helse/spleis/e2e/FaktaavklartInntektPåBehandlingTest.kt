package no.nav.helse.spleis.e2e

import java.time.Year
import java.util.UUID
import no.nav.helse.assertForventetFeil
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.INNTEKT
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.selvstendig
import no.nav.helse.hendelser.Arbeidsgiveropplysning
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.inntekt.ArbeidstakerFaktaavklartInntekt.ArbeistakerFaktaavklartInntektView
import no.nav.helse.person.inntekt.SelvstendigFaktaavklartInntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull

internal class FaktaavklartInntektPåBehandlingTest : AbstractDslTest() {

    @Test
    fun `Selvstendig får faktaavklart inntekt fra søknaden`() {
        selvstendig {
            val søknadId = UUID.randomUUID()
            håndterFørstegangssøknadSelvstendig(januar, søknadId = søknadId)

            val faktaavklartInntekt = inspektør.faktaavklartInntekt(1.vedtaksperiode) as? SelvstendigFaktaavklartInntekt.SelvstendigFaktaavklartInntektView
            assertNotNull(faktaavklartInntekt)
            assertEquals(460589.årlig, faktaavklartInntekt.beløp)
            assertEquals(søknadId, faktaavklartInntekt.hendelseId)
        }
    }

    @Test
    fun `Selvstendig får faktaavklart inntekt fra korrigerende søknad`() {
        selvstendig {
            val søknadId = UUID.randomUUID()
            håndterFørstegangssøknadSelvstendig(januar, søknadId = søknadId)

            (inspektør.faktaavklartInntekt(1.vedtaksperiode) as? SelvstendigFaktaavklartInntekt.SelvstendigFaktaavklartInntektView).also { faktaavklartInntekt ->
                assertNotNull(faktaavklartInntekt)
                assertEquals(460589.årlig, faktaavklartInntekt.beløp)
                assertEquals(søknadId, faktaavklartInntekt.hendelseId)
            }

            val korrigerendeSøknadId = UUID.randomUUID()
            håndterFørstegangssøknadSelvstendig(
                periode = januar,
                søknadId = korrigerendeSøknadId,
                pensjonsgivendeInntekter = listOf(
                    Søknad.PensjonsgivendeInntekt(Year.of(2017), 500000.årlig, INGEN, INGEN, INGEN, erFerdigLignet = true),
                    Søknad.PensjonsgivendeInntekt(Year.of(2016), 450000.årlig, INGEN, INGEN, INGEN, erFerdigLignet = true),
                    Søknad.PensjonsgivendeInntekt(Year.of(2015), 450000.årlig, INGEN, INGEN, INGEN, erFerdigLignet = true)
                )
            )

            (inspektør.faktaavklartInntekt(1.vedtaksperiode) as? SelvstendigFaktaavklartInntekt.SelvstendigFaktaavklartInntektView).also { faktaavklartInntekt ->
                assertNotNull(faktaavklartInntekt)
                assertEquals(460589.årlig, faktaavklartInntekt.beløp)
                assertForventetFeil(
                    forklaring = "Tar tydeligvis ikke inn korrigerende inntektsopplysninger fra selvstendigsøknad",
                    nå = {
                        assertEquals(460589.årlig, faktaavklartInntekt.beløp)
                        assertEquals(søknadId, faktaavklartInntekt.hendelseId)
                    },
                    ønsket = {
                        assertEquals(477319.årlig, faktaavklartInntekt.beløp)
                        assertEquals(korrigerendeSøknadId, faktaavklartInntekt.hendelseId)
                    }
                )
            }
        }
    }

    @Test
    fun `Arbeidstaker får faktaavklart inntekt fra arbeidsgiveropplysninger`() {
        a1 {
            håndterSøknad(januar)
            assertNull(inspektør.faktaavklartInntekt(1.vedtaksperiode))
            håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), vedtaksperiodeId = 1.vedtaksperiode)

            val faktaavklartInntekt = inspektør.faktaavklartInntekt(1.vedtaksperiode) as? ArbeistakerFaktaavklartInntektView
            assertNotNull(faktaavklartInntekt)
            assertEquals(INNTEKT, faktaavklartInntekt.beløp)
        }
    }

    @Test
    fun `Arbeidstaker får faktaavklart inntekt fra korrigerende arbeidsgiveropplysninger`() {
        a1 {
            håndterSøknad(januar)
            val hendelseIdArbeidsgiveropplysninger = håndterArbeidsgiveropplysninger(listOf(1.januar til 16.januar), vedtaksperiodeId = 1.vedtaksperiode)
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode)
            håndterUtbetalt()

            (inspektør.faktaavklartInntekt(1.vedtaksperiode) as? ArbeistakerFaktaavklartInntektView).also { faktaavklartInntekt ->
                assertNotNull(faktaavklartInntekt)
                assertEquals(INNTEKT, faktaavklartInntekt.beløp)
                assertEquals(hendelseIdArbeidsgiveropplysninger, faktaavklartInntekt.hendelseId)
            }

            val hendelseIdKorrigerendeArbeidsgiveropplysninger = håndterKorrigerteArbeidsgiveropplysninger(1.vedtaksperiode, Arbeidsgiveropplysning.OppgittInntekt(INNTEKT * 1.1))
            (inspektør.faktaavklartInntekt(1.vedtaksperiode) as? ArbeistakerFaktaavklartInntektView).also { faktaavklartInntekt ->
                assertNotNull(faktaavklartInntekt)
                assertEquals(INNTEKT * 1.1, faktaavklartInntekt.beløp)
                assertEquals(hendelseIdKorrigerendeArbeidsgiveropplysninger, faktaavklartInntekt.hendelseId)
            }
        }
    }
}
