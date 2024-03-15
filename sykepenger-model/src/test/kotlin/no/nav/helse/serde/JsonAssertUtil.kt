package no.nav.helse.serde

import no.nav.helse.person.Person
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode

internal fun assertPersonEquals(expected: Person, actual: Person) {
    val expectedJson = expected.dto().tilPersonData().tilSerialisertPerson().json
    val actualJson = actual.dto().tilPersonData().tilSerialisertPerson().json
    JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.STRICT)
}
