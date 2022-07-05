package no.nav.helse.dsl

import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.inspectors.PersonInspektør
import no.nav.helse.inspectors.TestArbeidsgiverInspektør
import no.nav.helse.januar
import no.nav.helse.person.Person
import no.nav.helse.spleis.e2e.TestObservatør
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class TestPersonTest {
    private companion object {
        private val a1 = "a1"
        private val a2 = "a2"
        private val agInspektør = { orgnummer: String -> { person: Person -> TestArbeidsgiverInspektør(person, orgnummer) } }
    }
    private lateinit var observatør: TestObservatør
    private lateinit var testperson: TestPerson

    private fun inspektør(orgnummer: String) = testperson.inspiser(agInspektør(orgnummer))

    @BeforeEach
    fun setup() {
        observatør = TestObservatør()
        testperson = TestPerson(observatør)
    }

    @Test
    fun `oppretter standardperson`() {
        val strategi = { person: Person -> PersonInspektør(person) }
        val inspektør = testperson.inspiser(strategi)
        assertEquals(TestPerson.UNG_PERSON_FNR_2018, inspektør.fødselsnummer)
        assertEquals(TestPerson.UNG_PERSON_FDATO_2018, inspektør.fødselsdato)
        assertEquals(TestPerson.AKTØRID, inspektør.aktørId)
        assertNull(inspektør.dødsdato)
    }

    @Test
    fun `kan sende sykmelding`() {
        testperson.arbeidsgiver(a1).håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        assertEquals(1, inspektør(a1).sykmeldingsperioder().size)
    }

    @Test
    fun `kan sende sykmelding via testblokk`() {
        val ag1 = testperson.arbeidsgiver(a1) {
            håndterSykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100.prosent))
        }
        assertEquals(1, ag1.inspektør.sykmeldingsperioder().size)
    }
}