package no.nav.helse.serde.reflection

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertAll
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.memberProperties

internal const val aktørId = "12345"
internal const val fnr = "12020052345"
internal const val orgnummer = "987654321"
internal const val vedtaksperiodeId = "1"

internal inline fun <reified T : Any, reified U : Any> assertMembers(
    subClasses: Pair<String, String>? = null,
    skalMappes: List<String> = emptyList(),
    skalIkkeMappes: List<String> = emptyList()
) {
    val actualClass = subClasses?.first?.let { getNestedClass(T::class, it) } ?: T::class
    val reflectClass = subClasses?.second?.let { getNestedClass(U::class, it) } ?: U::class

    val actualMembers = actualClass.allMemberProperties.map { it.name }
    val reflectMembers = reflectClass.memberProperties.map { it.name }
    val expectedMembers = skalMappes + skalIkkeMappes

    assertAll(
        { assertTrue(actualMembers.containsAll(expectedMembers)) { "Mangler følgende felter ${expectedMembers - actualMembers} i ${actualClass.simpleName}" } },
        { assertTrue(expectedMembers.containsAll(actualMembers)) { "Ukjente felter ${actualMembers - expectedMembers} i ${actualClass.simpleName}. Skal mappes til JSON?" } },
        { assertTrue(reflectMembers.containsAll(skalMappes)) { "Mangler følgende felter ${skalMappes - reflectMembers} i ${reflectClass.simpleName}" } },
        { assertTrue(skalMappes.containsAll(reflectMembers)) { "Ukjente felter ${reflectMembers - skalMappes} i ${reflectClass.simpleName}" } }
    )
}

private val KClass<*>.allMemberProperties: List<KProperty1<*, *>>
    get() = memberProperties + allSuperclasses.flatMap { it.memberProperties }

private fun getNestedClass(kClass: KClass<*>, nestedClassName: String) =
    getNestedClasses(kClass).single { it.simpleName == nestedClassName }

private fun getNestedClasses(kClass: KClass<*>): List<KClass<*>> =
    kClass.nestedClasses.fold(emptyList()) { nestedClasses, nestedClass ->
        nestedClasses + nestedClass + getNestedClasses(nestedClass)
    }
