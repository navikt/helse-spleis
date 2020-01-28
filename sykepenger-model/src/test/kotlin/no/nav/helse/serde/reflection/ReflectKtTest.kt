package no.nav.helse.serde.reflection

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertAll
import kotlin.reflect.full.memberProperties

internal const val aktørId = "12345"
internal const val fnr = "12020052345"
internal const val orgnummer = "987654321"
internal const val vedtaksperiodeId = "1"

internal inline fun <reified T : Any, reified U : Any> assertMembers(
    skalMappes: List<String> = emptyList(),
    skalIkkeMappes: List<String> = emptyList()
) {
    val actualMembers = T::class.memberProperties.map { it.name }
    val actualReflectMembers = U::class.memberProperties.map { it.name }
    val expectedMembers = skalMappes + skalIkkeMappes

    assertAll(
        { assertTrue(actualMembers.containsAll(expectedMembers)) { "Mangler følgende felter ${expectedMembers - actualMembers}" } },
        { assertTrue(expectedMembers.containsAll(actualMembers)) { "Ukjente felter ${actualMembers - expectedMembers}. Skal mappes til JSON?" } },
        { assertTrue(actualReflectMembers.containsAll(skalMappes)) { "Mangler følgende felter ${skalMappes - actualReflectMembers} i reflect" } },
        { assertTrue(skalMappes.containsAll(actualReflectMembers)) { "Ukjente felter i reflect ${actualReflectMembers - skalMappes}" } }
    )
}
