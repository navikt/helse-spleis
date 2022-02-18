package no.nav.helse.person

import no.nav.helse.januar
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class OpptjeningTest {

    @Test
    fun `Tom liste med arbeidsforhold betyr at du ikke oppfyller opptjeningskrav`() {
        val arbeidsforhold = emptyList<Arbeidsforholdhistorikk.Arbeidsforhold>()
        val opptjening = Opptjening(arbeidsforhold, 1.januar)

        assertFalse(opptjening.erOppfylt())
    }

    @Test
    fun `Én dags opptjening oppfyller ikke krav til opptjening`() {
        val arbeidsforhold = listOf(Arbeidsforholdhistorikk.Arbeidsforhold(ansattFom = 1.januar, ansattTom = null, deaktivert = false))
        val opptjening = Opptjening(arbeidsforhold, 2.januar)

        assertFalse(opptjening.erOppfylt())
    }

    @Test
    fun `27 dager opptjening oppfyller ikke krav til opptjening`() {
        val arbeidsforhold = listOf(Arbeidsforholdhistorikk.Arbeidsforhold(ansattFom = 1.januar, ansattTom = null, deaktivert = false))
        val opptjening = Opptjening(arbeidsforhold, 1.januar.plusDays(27))

        assertFalse(opptjening.erOppfylt())
    }

    @Test
    fun `28 dager opptjening oppfyller krav til opptjening`() {
        val arbeidsforhold = listOf(
            Arbeidsforholdhistorikk.Arbeidsforhold(ansattFom = 1.januar, ansattTom = null, deaktivert = false)
        )
        val opptjening = Opptjening(arbeidsforhold, 1.januar.plusDays(28))

        assertTrue(opptjening.erOppfylt())
    }

    @Test
    fun `Opptjening skal ikke bruke deaktiverte arbeidsforhold`() {
        val arbeidsforhold = listOf(Arbeidsforholdhistorikk.Arbeidsforhold(ansattFom = 1.januar, ansattTom = null, deaktivert = true))
        val opptjening = Opptjening(arbeidsforhold, 1.januar.plusDays(28))

        assertFalse(opptjening.erOppfylt())
    }

    @Test
    fun `Opptjening skal ikke koble sammen om deaktiverte arbeidsforhold fører til gap`() {
        val arbeidsforhold = listOf(
            Arbeidsforholdhistorikk.Arbeidsforhold(ansattFom = 1.januar, ansattTom = 10.januar, deaktivert = false),
            Arbeidsforholdhistorikk.Arbeidsforhold(ansattFom = 11.januar, ansattTom = 14.januar, deaktivert = true),
            Arbeidsforholdhistorikk.Arbeidsforhold(ansattFom = 15.januar, ansattTom = null, deaktivert = false)
        )
        val opptjening = Opptjening(arbeidsforhold, 1.januar.plusDays(28))

        assertFalse(opptjening.erOppfylt())
    }

    @Test
    fun `to tilstøtende arbeidsforhold`() {
        val arbeidsforhold = listOf(
            Arbeidsforholdhistorikk.Arbeidsforhold(ansattFom = 1.januar, ansattTom = 10.januar, deaktivert = false),
            Arbeidsforholdhistorikk.Arbeidsforhold(ansattFom = 11.januar, ansattTom = null, deaktivert = false)
        )
        val opptjening = Opptjening(arbeidsforhold, 1.januar.plusDays(28))

        assertTrue(opptjening.erOppfylt())
    }

    @Test
    fun `Opptjening kobler sammen gap selvom rekkefølgen ikke er kronologisk`() {
        val arbeidsforhold = listOf(
            Arbeidsforholdhistorikk.Arbeidsforhold(ansattFom = 1.januar, ansattTom = 10.januar, deaktivert = false),
            Arbeidsforholdhistorikk.Arbeidsforhold(ansattFom = 15.januar, ansattTom = null, deaktivert = false),
            Arbeidsforholdhistorikk.Arbeidsforhold(ansattFom = 11.januar, ansattTom = 14.januar, deaktivert = false)
        )
        val opptjening = Opptjening(arbeidsforhold, 1.januar.plusDays(28))

        assertTrue(opptjening.erOppfylt())
    }
}
