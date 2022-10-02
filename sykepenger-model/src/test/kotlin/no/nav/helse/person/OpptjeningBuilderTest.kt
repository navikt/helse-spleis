package no.nav.helse.person

import no.nav.helse.april
import no.nav.helse.februar
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.serde.AbstractBuilder
import no.nav.helse.serde.JsonBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class OpptjeningBuilderTest {

    @Test
    fun `serialiserer en opptjening med et arbeidsgiverOpptjeningsgrunnlag`() {
        val arbeidsgiverOpptjeningsgrunnlag = listOf(
            Opptjening.ArbeidsgiverOpptjeningsgrunnlag(
                orgnummer = "orgnummer",
                ansattPerioder = listOf(Arbeidsforholdhistorikk.Arbeidsforhold(ansattFom = 1.januar, ansattTom = null, deaktivert = false))
            )
        )

        val opptjening = Opptjening.gjenopprett(arbeidsgiverOpptjeningsgrunnlag, 1.januar til 1.mai)
        val opptjeningMap = mutableMapOf<String, Any>()
        val opptjeningState = JsonBuilder.OpptjeningState(opptjeningMap)

        class TestBuilder: AbstractBuilder()
        val testBuilder = TestBuilder()
        testBuilder.pushState(opptjeningState)
        opptjeningState.builder(testBuilder)
        opptjening.accept(testBuilder)

        assertEquals(
            mapOf(
                "arbeidsforhold" to listOf(
                    mapOf(
                        "orgnummer" to "orgnummer",
                        "ansattPerioder" to listOf(
                            mapOf("ansattFom" to 1.januar, "ansattTom" to null, "deaktivert" to false)
                        )
                    )
                ),
                "opptjeningFom" to 1.januar,
                "opptjeningTom" to 1.mai
            ),
            opptjeningMap
        )
    }

    @Test
    fun `serialiserer en optjening med en liste av arbeidsgiverOpptjeningsgrunnlag`() {
        val arbeidsgiverOpptjeningsgrunnlag = listOf(
            Opptjening.ArbeidsgiverOpptjeningsgrunnlag(
                orgnummer = "orgnummer",
                ansattPerioder = listOf(
                    Arbeidsforholdhistorikk.Arbeidsforhold(ansattFom = 1.mars, ansattTom = 31.mars, deaktivert = false),
                    Arbeidsforholdhistorikk.Arbeidsforhold(ansattFom = 1.april, ansattTom = null, deaktivert = false)
                )
            ), Opptjening.ArbeidsgiverOpptjeningsgrunnlag(
                orgnummer = "orgnummer2",
                ansattPerioder = listOf(
                    Arbeidsforholdhistorikk.Arbeidsforhold(ansattFom = 1.januar, ansattTom = 31.januar, deaktivert = false),
                    Arbeidsforholdhistorikk.Arbeidsforhold(ansattFom = 1.februar, ansattTom = null, deaktivert = true)
                )
            )
        )

        val opptjening = Opptjening.gjenopprett(arbeidsgiverOpptjeningsgrunnlag, 1.januar til 1.mai)
        val opptjeningMap = mutableMapOf<String, Any>()
        val opptjeningState = JsonBuilder.OpptjeningState(opptjeningMap)

        class TestBuilder: AbstractBuilder()
        val testBuilder = TestBuilder()
        testBuilder.pushState(opptjeningState)
        opptjeningState.builder(testBuilder)
        opptjening.accept(testBuilder)

        assertEquals(
            mapOf(
                "arbeidsforhold" to listOf(
                    mapOf(
                        "orgnummer" to "orgnummer",
                        "ansattPerioder" to listOf(
                            mapOf("ansattFom" to 1.mars, "ansattTom" to 31.mars, "deaktivert" to false),
                            mapOf("ansattFom" to 1.april, "ansattTom" to null, "deaktivert" to false)
                        )
                    ),
                    mapOf(
                        "orgnummer" to "orgnummer2",
                        "ansattPerioder" to listOf(
                            mapOf("ansattFom" to 1.januar, "ansattTom" to 31.januar, "deaktivert" to false),
                            mapOf("ansattFom" to 1.februar, "ansattTom" to null, "deaktivert" to true)
                        )
                    )
                ),
                "opptjeningFom" to 1.januar,
                "opptjeningTom" to 1.mai
            ),
            opptjeningMap
        )
    }
}
