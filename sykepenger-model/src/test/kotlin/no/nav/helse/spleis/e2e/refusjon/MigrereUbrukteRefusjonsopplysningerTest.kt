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
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MigrereUbrukteRefusjonsopplysningerTest : AbstractDslTest() {

    private lateinit var forrigeUbrukteRefusjonsopplysninger: RefusjonsservitørView
    private val inntektsmeldingId = UUID.randomUUID()
    private val inntektsmeldingMottatt = LocalDateTime.now()

    private fun setup1og2() {
        håndterSøknad(januar)
        håndterInntektsmelding(
            arbeidsgiverperioder = listOf(1.januar til 16.januar),
            refusjon = Inntektsmelding.Refusjon(beløp = INNTEKT, opphørsdato = 28.februar),
            beregnetInntekt = INNTEKT,
            id = inntektsmeldingId,
            mottatt = inntektsmeldingMottatt
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

}