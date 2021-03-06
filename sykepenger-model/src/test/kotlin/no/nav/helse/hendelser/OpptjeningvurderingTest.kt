package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.testhelpers.desember
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.november
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
        assertFalse(undersøke(listOf(Arbeidsforhold(ORGNUMMER, 5.desember(2017)))) {
            assertEquals(27, it.antallOpptjeningsdager)
            assertFalse(it.harOpptjening())
        })
    }

    @Test
    fun `tom eldre enn fom`() {
        assertTrue(undersøke(listOf(
            Arbeidsforhold(ORGNUMMER, 1.november(2017)),
            Arbeidsforhold(ORGNUMMER, 10.desember(2017), 1.desember(2017))
        )) {
            assertEquals(61, it.antallOpptjeningsdager)
            assertTrue(it.harOpptjening())
            assertFalse(aktivitetslogg.hasErrorsOrWorse())
            assertTrue(aktivitetslogg.hasWarningsOrWorse())
        })
    }

    @Test
    fun `arbeidsforhold nyere enn skjæringstidspunkt`() {
        assertFalse(undersøke(listOf(Arbeidsforhold(ORGNUMMER, SKJÆRINGSTIDSPUNKT.plusDays(1)))) {
            assertEquals(0, it.antallOpptjeningsdager)
            assertFalse(it.harOpptjening())
        })
    }

    @Test
    fun `arbeidsforhold avsluttet før skjæringstidspunkt`() {
        assertFalse(undersøke(listOf(Arbeidsforhold(ORGNUMMER, LocalDate.EPOCH, SKJÆRINGSTIDSPUNKT.minusDays(1)))) {
            assertEquals(0, it.antallOpptjeningsdager)
            assertFalse(it.harOpptjening())
        })
    }

    @Test
    fun `28 dager opptjening fører til OK opptjening`() {
        assertTrue(undersøke(listOf(Arbeidsforhold(ORGNUMMER, 4.desember(2017)))) {
            assertEquals(28, it.antallOpptjeningsdager)
            assertTrue(it.harOpptjening())
        })
    }

    @Test
    fun `flere arbeidsforhold i samme bedrift`() {
        assertTrue(undersøke(listOf(
            Arbeidsforhold(ORGNUMMER, 4.desember(2017)),
            Arbeidsforhold(ORGNUMMER, 4.desember(2017), 1.januar(2018)),
            Arbeidsforhold(ORGNUMMER, 1.januar(2018), 1.september(2018))
        )) {
            assertEquals(28, it.antallOpptjeningsdager)
            assertTrue(it.harOpptjening())
        })
    }

    @Test
    fun `ingen arbeidsforhold gir 0 opptjente dager`() {
        assertFalse(undersøke(emptyList()) {
            assertEquals(0, it.antallOpptjeningsdager)
            assertFalse(it.harOpptjening())
        })
    }

    private fun undersøke(arbeidsforhold: List<Arbeidsforhold>, test: (Opptjeningvurdering) -> Unit): Boolean {
        aktivitetslogg = Aktivitetslogg()
        val opptjeningvurdering = Opptjeningvurdering(arbeidsforhold)
        return opptjeningvurdering.valider(aktivitetslogg, SKJÆRINGSTIDSPUNKT).also {
            test(opptjeningvurdering)
        }
    }
}
