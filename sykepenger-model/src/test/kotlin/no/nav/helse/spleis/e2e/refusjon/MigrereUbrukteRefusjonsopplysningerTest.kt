package no.nav.helse.spleis.e2e.refusjon

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.Toggle
import no.nav.helse.dsl.AbstractDslTest
import no.nav.helse.dsl.TestPerson.Companion.INNTEKT
import no.nav.helse.februar
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.refusjon.RefusjonsservitørView
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

}