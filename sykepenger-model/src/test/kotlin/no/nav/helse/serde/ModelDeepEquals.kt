package no.nav.helse.serde

import org.junit.jupiter.api.Assertions
import java.math.BigDecimal
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

internal fun assertDeepEquals(one: Any?, other: Any?) {
    ModelDeepEquals().assertDeepEquals(one, other)
}

private class ModelDeepEquals {
    val checkLog = mutableListOf<Pair<Any, Any>>()
    fun assertDeepEquals(one: Any?, other: Any?) {
        if (one == null && other == null) return
        Assertions.assertFalse(one == null || other == null)
        requireNotNull(one)
        if (one::class.qualifiedName == null) return
        checkLog.forEach {
            if (it.first == one && it.second == other) return // vil ev. feile hos den som la i checkLog
        }
        checkLog.add(one to other!!)

        if (one is Collection<*> && other is Collection<*>) {
            assertCollectionEquals(one, other)
        } else if (one is Map<*, *> && other is Map<*, *>) {
            assertMapEquals(one, other)
        } else {
            assertObjectEquals(one, other)
        }
    }

    private fun assertObjectEquals(one: Any, other: Any) {
        Assertions.assertEquals(one::class, other::class)
        if (one is Enum<*> && other is Enum<*>) {
            Assertions.assertEquals(one, other)
        }
        if (one::class.qualifiedName!!.startsWith("no.nav.helse.")) {
            assertHelseObjectEquals(one, other)
        } else {
            if (one is BigDecimal && other is BigDecimal) {
                Assertions.assertEquals(one.toLong(), other.toLong())
            } else {
                Assertions.assertEquals(one, other, {
                    "TODO"
                })
            }
        }
    }

    private fun assertHelseObjectEquals(one: Any, other: Any) {
        one::class.memberProperties.map { it.apply { isAccessible = true } }.forEach { prop ->
            if (!prop.name.toLowerCase().endsWith("observers")) {
                assertDeepEquals(prop.call(one), prop.call(other))
            }
        }
    }

    private fun assertMapEquals(one: Map<*, *>, other: Map<*, *>) {
        Assertions.assertEquals(one.size, other.size)
        one.keys.forEach {
            assertDeepEquals(one[it], other[it])
        }
    }

    private fun assertCollectionEquals(one: Collection<*>, other: Collection<*>) {
        Assertions.assertEquals(one.size, other.size)
        (one.toTypedArray() to other.toTypedArray()).forEach(this::assertDeepEquals)
    }

    private fun Pair<Array<*>, Array<*>>.forEach(block: (Any?, Any?) -> Unit) {
        first.forEachIndexed { i, any -> block(any, second[i]) }
    }
}
