package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.testhelpers.desember
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.september
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class OpptjeningvurderingTest {

    private companion object {
        private const val ORGNUMMER = "345"
        private val SKJÆRINGSTIDSPUNKT = 1.januar
    }

    private lateinit var aktivitetslogg: Aktivitetslogg

    @Test
    fun `27 dager opptjening gir ikke rett til opptjening`() {
        assertTrue(undersøke(listOf(Opptjeningvurdering.Arbeidsforhold(ORGNUMMER, 5.desember(2017)))) {
            assertEquals(27, it.antallOpptjeningsdager)
            assertFalse(it.harOpptjening())
        })
    }

    @Test
    fun `arbeidsforhold nyere enn skjæringstidspunkt`() {
        assertTrue(undersøke(listOf(Opptjeningvurdering.Arbeidsforhold(ORGNUMMER, SKJÆRINGSTIDSPUNKT.plusDays(1)))) {
            assertEquals(0, it.antallOpptjeningsdager)
            assertFalse(it.harOpptjening())
        })
    }

    @Test
    fun `arbeidsforhold avsluttet før skjæringstidspunkt`() {
        assertTrue(undersøke(listOf(Opptjeningvurdering.Arbeidsforhold(ORGNUMMER, LocalDate.EPOCH, SKJÆRINGSTIDSPUNKT.minusDays(1)))) {
            assertEquals(0, it.antallOpptjeningsdager)
            assertFalse(it.harOpptjening())
        })
    }

    @Test
    fun `28 dager opptjening fører til OK opptjening`() {
        assertFalse(undersøke(listOf(Opptjeningvurdering.Arbeidsforhold(ORGNUMMER, 4.desember(2017)))) {
            assertEquals(28, it.antallOpptjeningsdager)
            assertTrue(it.harOpptjening())
        })
    }

    @Test
    fun `flere arbeidsforhold i samme bedrift`() {
        assertFalse(undersøke(listOf(
            Opptjeningvurdering.Arbeidsforhold(ORGNUMMER, 4.desember(2017)),
            Opptjeningvurdering.Arbeidsforhold(ORGNUMMER, 4.desember(2017), 1.januar(2018)),
            Opptjeningvurdering.Arbeidsforhold(ORGNUMMER, 1.januar(2018), 1.september(2018))
        )) {
            assertEquals(28, it.antallOpptjeningsdager)
            assertTrue(it.harOpptjening())
        })
    }

    @Test
    fun `ingen arbeidsforhold gir 0 opptjente dager`() {
        assertTrue(undersøke(emptyList()) {
            assertEquals(0, it.antallOpptjeningsdager)
            assertFalse(it.harOpptjening())
        })
    }

    private fun undersøke(arbeidsforhold: List<Opptjeningvurdering.Arbeidsforhold>, test: (Opptjeningvurdering) -> Unit): Boolean {
        aktivitetslogg = Aktivitetslogg()
        val opptjeningvurdering = Opptjeningvurdering(arbeidsforhold)
        return opptjeningvurdering.valider(aktivitetslogg, SKJÆRINGSTIDSPUNKT).hasErrorsOrWorse().also {
            test(opptjeningvurdering)
        }
    }
}
