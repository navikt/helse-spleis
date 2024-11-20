package no.nav.helse.spleis.e2e.refusjon

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.Toggle
import no.nav.helse.assertForventetFeil
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.OverstyrtArbeidsgiveropplysning
import no.nav.helse.dsl.TestPerson.Companion.INNTEKT
import no.nav.helse.februar
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.refusjon.RefusjonsservitørView
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
internal class MigrereUbrukteRefusjonsopplysningerTest : AbstractDslTest() {

    private lateinit var forrigeUbrukteRefusjonsopplysninger: RefusjonsservitørView
    private val inntektsmeldingId1 = UUID.randomUUID()
    private val inntektsmeldingMottatt1 = LocalDateTime.now()
    private val inntektsmeldingId2 = UUID.randomUUID()
    private val inntektsmeldingMottatt2 = inntektsmeldingMottatt1.plusSeconds(1)

    private fun setup1og2() {
        håndterSøknad(januar)
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(1.januar til 16.januar),
            refusjon = Inntektsmelding.Refusjon(beløp = INNTEKT, opphørsdato = 28.februar),
            beregnetInntekt = INNTEKT,
            id = inntektsmeldingId1,
            mottatt = inntektsmeldingMottatt1
        )
    }

    @Test
    @Order(1)
    fun `Endring i refusjon frem i tid fra inntektsmelding - med toggle`() = Toggle.LagreUbrukteRefusjonsopplysninger.enable {
        a1 {
            setup1og2()
            forrigeUbrukteRefusjonsopplysninger = inspektør.ubrukteRefusjonsopplysninger
        }
    }

    @Test
    @Order(2)
    fun `Endring i refusjon frem i tid fra inntektsmelding - uten toggle`() = Toggle.LagreUbrukteRefusjonsopplysninger.disable {
        a1 {
            setup1og2()
            migrerUbrukteRefusjonsopplysninger()
            assertEquals(forrigeUbrukteRefusjonsopplysninger, inspektør.ubrukteRefusjonsopplysninger)
        }
    }

    private fun setup3og4() {
        håndterSøknad(januar)
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(1.januar til 16.januar),
            refusjon = Inntektsmelding.Refusjon(beløp = INNTEKT * 2, opphørsdato = 28.februar),
            beregnetInntekt = INNTEKT,
            id = inntektsmeldingId1,
            mottatt = inntektsmeldingMottatt1
        )
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(1.januar til 16.januar),
            refusjon = Inntektsmelding.Refusjon(beløp = INNTEKT/2, opphørsdato = 28.februar),
            beregnetInntekt = INNTEKT,
            id = inntektsmeldingId2,
            mottatt = inntektsmeldingMottatt2
        )
    }

    @Test
    @Order(3)
    fun `Endring i refusjon frem i tid fra flere inntektsmeldinger - med toggle`() = Toggle.LagreUbrukteRefusjonsopplysninger.enable {
        a1 {
            setup3og4()
            forrigeUbrukteRefusjonsopplysninger = inspektør.ubrukteRefusjonsopplysninger
        }
    }

    @Test
    @Order(4)
    fun `Endring i refusjon frem i tid fra flere inntektsmeldinger - uten toggle`() = Toggle.LagreUbrukteRefusjonsopplysninger.disable {
        a1 {
            setup3og4()
            migrerUbrukteRefusjonsopplysninger()
            assertEquals(forrigeUbrukteRefusjonsopplysninger, inspektør.ubrukteRefusjonsopplysninger)
        }
    }

    private fun setup5og6() {
        håndterSøknad(januar)
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(1.januar til 16.januar),
            refusjon = Inntektsmelding.Refusjon(beløp = INNTEKT, opphørsdato = 28.februar),
            beregnetInntekt = INNTEKT,
            id = inntektsmeldingId1,
            mottatt = inntektsmeldingMottatt1,
            harOpphørAvNaturalytelser = true
        )
    }

    @Test
    @Order(5)
    fun `Inntektsmeldingen støttes ikke - med toggle`() = Toggle.LagreUbrukteRefusjonsopplysninger.enable {
        a1 {
            setup5og6()
            forrigeUbrukteRefusjonsopplysninger = inspektør.ubrukteRefusjonsopplysninger
        }
    }

    @Test
    @Order(6)
    fun `Inntektsmeldingen støttes ikke - uten toggle`() = Toggle.LagreUbrukteRefusjonsopplysninger.disable {
        a1 {
            setup5og6()
            migrerUbrukteRefusjonsopplysninger()
            assertEquals(forrigeUbrukteRefusjonsopplysninger, inspektør.ubrukteRefusjonsopplysninger)
        }
    }

    private fun setup7og8() {
        a1{
            håndterSøknad(januar)
            håndterInntektsmelding(
                arbeidsgiverperioder = listOf(1.januar til 16.januar),
                refusjon = Inntektsmelding.Refusjon(beløp = INNTEKT, opphørsdato = 28.februar),
                beregnetInntekt = INNTEKT,
                id = inntektsmeldingId1,
                mottatt = inntektsmeldingMottatt1
            )
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterUtbetalingsgodkjenning(1.vedtaksperiode, godkjent = false)
            håndterUtbetalingshistorikkEtterInfotrygdendring(ArbeidsgiverUtbetalingsperiode(a1, 1.januar, 31.januar, 100.prosent, INNTEKT))
        }
    }

    @Test
    @Order(7)
    fun `Periode er kastet og utbetalt i Infotrygd - med toggle`() = Toggle.LagreUbrukteRefusjonsopplysninger.enable {
        a1 {
            setup7og8()
            forrigeUbrukteRefusjonsopplysninger = inspektør.ubrukteRefusjonsopplysninger
        }
    }

    @Test
    @Order(8)
    fun `Periode er kastet og utbetalt i Infotrygd - uten toggle`() = Toggle.LagreUbrukteRefusjonsopplysninger.disable {
        a1 {
            setup7og8()
            migrerUbrukteRefusjonsopplysninger()
            assertEquals(forrigeUbrukteRefusjonsopplysninger, inspektør.ubrukteRefusjonsopplysninger)
        }
    }

    private fun setup9og10() {
        a1{
            håndterSøknad(januar)
            håndterInntektsmelding(
                arbeidsgiverperioder = listOf(1.januar til 16.januar),
                beregnetInntekt = INNTEKT,
                id = inntektsmeldingId1,
                mottatt = inntektsmeldingMottatt1
            )
            håndterVilkårsgrunnlag(1.vedtaksperiode)
            håndterYtelser(1.vedtaksperiode)
            håndterSimulering(1.vedtaksperiode)
            håndterOverstyrArbeidsgiveropplysninger(1.januar, listOf(OverstyrtArbeidsgiveropplysning(
                orgnummer = a1,
                inntekt = INNTEKT,
                forklaring = "foo",
                subsumsjon = null,
                refusjonsopplysninger = listOf(Triple(1.januar, 31.januar, INNTEKT), Triple(1.februar, null, Inntekt.INGEN))
            )))
        }
    }

    @Test
    @Order(9)
    fun `Saksbehandler overstyrer refusjon frem i tid - med toggle`() = Toggle.LagreUbrukteRefusjonsopplysninger.enable {
        a1 {
            setup9og10()
            forrigeUbrukteRefusjonsopplysninger = inspektør.ubrukteRefusjonsopplysninger
        }
    }

    @Test
    @Order(10)
    fun `Saksbehandler overstyrer refusjon frem i tid - uten toggle`() = Toggle.LagreUbrukteRefusjonsopplysninger.disable {
        a1 {
            setup9og10()
            migrerUbrukteRefusjonsopplysninger()
            assertForventetFeil(
                forklaring = "Har ikke migrert dette ennå",
                nå = { assertEquals(RefusjonsservitørView(emptyMap()), inspektør.ubrukteRefusjonsopplysninger) },
                ønsket = { assertEquals(forrigeUbrukteRefusjonsopplysninger, inspektør.ubrukteRefusjonsopplysninger) }
            )
        }
    }
}