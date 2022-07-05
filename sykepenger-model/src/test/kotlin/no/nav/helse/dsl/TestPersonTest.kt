package no.nav.helse.dsl

import no.nav.helse.inspectors.PersonInspektør
import no.nav.helse.person.Person
import no.nav.helse.spleis.e2e.TestObservatør
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class TestPersonTest {

    @Test
    fun `oppretter standardperson`() {
        val strategi = { person: Person -> PersonInspektør(person) }
        val testperson = TestPerson(TestObservatør())
        val inspektør = testperson.inspiser(strategi)
        assertEquals(TestPerson.UNG_PERSON_FNR_2018, inspektør.fødselsnummer)
        assertEquals(TestPerson.UNG_PERSON_FDATO_2018, inspektør.fødselsdato)
        assertEquals(TestPerson.AKTØRID, inspektør.aktørId)
        assertNull(inspektør.dødsdato)
    }
}