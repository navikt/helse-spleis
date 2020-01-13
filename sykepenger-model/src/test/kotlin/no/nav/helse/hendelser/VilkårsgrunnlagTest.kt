package no.nav.helse.hendelser

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.TestConstants.objectMapper
import no.nav.helse.person.ArbeidstakerHendelse.Hendelsestype
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.YearMonth

internal class VilkårsgrunnlagTest {

    @Test
    fun `skal kunne beregne avvik mellom innmeldt lønn fra inntektsmelding og lønn fra inntektskomponenten`() {
        val vilkårsgrunnlag = requireNotNull(
            Vilkårsgrunnlag.Builder().build(
                behov(YearMonth.of(2018, 1), YearMonth.of(2018, 12))
            )
        )

        assertEquals(false, vilkårsgrunnlag.harAvvikIOppgittInntekt(100.0))
        assertEquals(true, vilkårsgrunnlag.harAvvikIOppgittInntekt(74.0))
    }

    private fun behov(start: YearMonth, slutt: YearMonth) = objectMapper.valueToTree<JsonNode>(
        mapOf(
            "@id" to "behovsid",
            "@final" to true,
            "fødselsnummer" to "fødselsnummer",
            "organisasjonsnummer" to "orgnummer",
            "@opprettet" to LocalDateTime.now(),
            "hendelse" to Hendelsestype.Vilkårsgrunnlag,
            "@behov" to listOf("Inntektsberegning", "EgenAnsatt"),
            "aktørId" to "123",
            "vedtaksperiodeId" to "vedtaksperiodeId",
            "beregningStart" to start,
            "beregningSlutt" to slutt,
            "@løsning" to mapOf("Inntektsberegning" to (1.rangeTo(12)).map {
                Vilkårsgrunnlag.Måned(
                    årMåned = YearMonth.of(2018, it),
                    inntektsliste = listOf(
                        Vilkårsgrunnlag.Inntekt(
                            beløp = 100.0,
                            inntektstype = Vilkårsgrunnlag.Inntektstype.LOENNSINNTEKT,
                            orgnummer = "123456789"
                        )
                    )
                )
            })
        )
    ).toString()
}
