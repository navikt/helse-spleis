package no.nav.helse.serde

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import java.math.BigDecimal
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

internal fun assertDeepEquals(one: Any?, other: Any?) {
    ModelDeepEquals().assertDeepEquals(one, other, "ROOT")
}

private class ModelDeepEquals {
    val checkLog = mutableListOf<Pair<Any, Any>>()
    fun assertDeepEquals(one: Any?, other: Any?, fieldName: String) {
        if (one == null && other == null) return
        assertFalse(one == null || other == null, "For field $fieldName: $one or $other is null")
        requireNotNull(one)
        if (one::class.qualifiedName == null) return
        checkLog.forEach {
            if (it.first == one && it.second == other) return // vil ev. feile hos den som la i checkLog
        }
        checkLog.add(one to other!!)

        if (one is Collection<*> && other is Collection<*>) {
            assertCollectionEquals(one, other, fieldName)
        } else if (one is Map<*, *> && other is Map<*, *>) {
            assertMapEquals(one, other, fieldName)
        } else {
            assertObjectEquals(one, other)
        }
    }

    private fun assertObjectEquals(one: Any, other: Any) {
        assertEquals(one::class, other::class)
        if (one is Enum<*> && other is Enum<*>) {
            assertEquals(one, other)
        }
        if (one::class.qualifiedName!!.startsWith("no.nav.helse.")) {
            assertHelseObjectEquals(one, other)
        } else {
            if (one is BigDecimal && other is BigDecimal) {
                assertEquals(one.toLong(), other.toLong())
            } else {
                assertEquals(one, other, {
                    "TODO"
                })
            }
        }
    }

    private fun assertHelseObjectEquals(one: Any, other: Any) {
        one::class.memberProperties.map { it.apply { isAccessible = true } }.forEach { prop ->
            if (!prop.name.toLowerCase().endsWith("observers")) {
                assertDeepEquals(prop.call(one), prop.call(other), prop.name)
            }
        }
    }

    private fun assertMapEquals(one: Map<*, *>, other: Map<*, *>, fieldName: String) {
        assertEquals(one.size, other.size)
        one.keys.forEach {
            assertDeepEquals(one[it], other[it], fieldName)
        }
    }

    private fun assertCollectionEquals(one: Collection<*>, other: Collection<*>, fieldName: String) {
        assertEquals(one.size, other.size, "Failure for fieldName $fieldName")
        (one.toTypedArray() to other.toTypedArray()).forEach { i1, i2 ->
            this.assertDeepEquals(i1, i2, fieldName)
        }
    }

    private fun Pair<Array<*>, Array<*>>.forEach(block: (Any?, Any?) -> Unit) {
        first.forEachIndexed { i, any -> block(any, second[i]) }
    }
}
