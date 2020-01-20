package no.nav.helse.hendelser

import no.nav.helse.hendelser.ModelVilkårsgrunnlag.Inntekt
import no.nav.helse.hendelser.ModelVilkårsgrunnlag.Måned
import no.nav.helse.person.Aktivitetslogger
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

internal class ModelVilkårsgrunnlagTest {

    @Test
    internal fun `skal kunne beregne avvik mellom innmeldt lønn fra inntektsmelding og lønn fra inntektskomponenten`() {
        val vilkårsgrunnlag = vilkårsgrunnlag((1..12)
            .map { Måned(YearMonth.of(2018, it), listOf(Inntekt(1000.0))) })

        assertFalse(vilkårsgrunnlag.harAvvikIOppgittInntekt(1000.00))
        assertTrue(vilkårsgrunnlag.harAvvikIOppgittInntekt(1250.01))
        assertFalse(vilkårsgrunnlag.harAvvikIOppgittInntekt(1250.00))
        assertTrue(vilkårsgrunnlag.harAvvikIOppgittInntekt(749.99))
        assertFalse(vilkårsgrunnlag.harAvvikIOppgittInntekt(750.00))
    }

    private fun vilkårsgrunnlag(inntektsmåneder: List<Måned>) = ModelVilkårsgrunnlag(
        hendelseId = UUID.randomUUID(),
        vedtaksperiodeId = "123456789",
        aktørId = "987654321",
        fødselsnummer = "12345678901",
        orgnummer = "orgnummer",
        rapportertDato = LocalDateTime.now(),
        inntektsmåneder = inntektsmåneder,
        erEgenAnsatt = false,
        aktivitetslogger = Aktivitetslogger()
    )
}
