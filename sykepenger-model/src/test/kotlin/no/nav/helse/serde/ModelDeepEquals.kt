package no.nav.helse.serde

import no.nav.helse.sykdomstidslinje.Dag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import java.math.BigDecimal

internal fun assertDeepEquals(one: Any?, other: Any?) {
    ModelDeepEquals().assertDeepEquals(one, other, listOf("ROOT"))
}

private class ModelDeepEquals {
    val checkLog = mutableListOf<Pair<Any, Any>>()
    fun assertDeepEquals(one: Any?, other: Any?, path: List<String>) {
        if (one == null && other == null) return
        assertFalse(one == null || other == null, "For field $path: $one or $other is null")
        requireNotNull(one)
        if (one::class.qualifiedName == null) return
        checkLog.forEach {
            if (it.first == one && it.second == other) return // vil ev. feile hos den som la i checkLog
        }
        checkLog.add(one to other!!)

        if (one is Collection<*> && other is Collection<*>) {
            assertCollectionEquals(one, other, path)
        } else if (one is Map<*, *> && other is Map<*, *>) {
            // Sykdomstidslinje lagrer ikke UkjentDager, så vi ignorerer de for beregnetSykdomstidslinje. NotLikeThis
            if (path.last() == "dager") {
                assertMapEquals(one.filtrerUkjentDager(), other.filtrerUkjentDager(), path)
            } else {
                assertMapEquals(one, other, path)
            }
        } else {
            assertObjectEquals(one, other, path)
        }
    }

    private fun Map<*, *>.filtrerUkjentDager() = filterValues { it !is Dag.UkjentDag }

    private fun assertObjectEquals(one: Any, other: Any, path: List<String>) {
        assertEqualWithMessage(one::class, other::class, path)
        if (one is Enum<*> && other is Enum<*>) {
            assertEqualWithMessage(one, other, path)
        }
        if (one::class.qualifiedName!!.startsWith("no.nav.helse.")) {
            assertHelseObjectEquals(one, other, path)
        } else {
            if (one is BigDecimal && other is BigDecimal) {
                assertEqualWithMessage(one.toLong(), other.toLong(), path)
            } else if (one is Pair<*, *> && other is Pair<*, *>) {
                assertDeepEquals(one.first, other.first, path)
                assertDeepEquals(one.second, other.second, path)
            } else if (one is Triple<*, *, *> && other is Triple<*, *, *>) {
                assertDeepEquals(one.first, other.first, path)
                assertDeepEquals(one.second, other.second, path)
                assertDeepEquals(one.third, other.third, path)
            } else {
                assertEqualWithMessage(one, other, path)
            }
        }
    }

    private fun assertEqualWithMessage(
        one: Any,
        other: Any,
        path: List<String>
    ) {
        assertEquals(one, other, path.toString())
    }

    private fun assertHelseObjectEquals(one: Any, other: Any, path: List<String>) {
        one::class.java.declaredFields.map { it.apply { isAccessible = true } }.forEach { prop ->
            if (!prop.name.toLowerCase().endsWith("observers") && prop.name.toLowerCase() != "forrigehendelse" && prop.name != "vilkårsgrunnlagHistorikk") {
                assertDeepEquals(prop[one], prop[other], path + prop.name)
            }
        }
    }

    private fun assertMapEquals(one: Map<*, *>, other: Map<*, *>, path: List<String>) {
        assertEquals(one.size, other.size, "$path")
        one.keys.forEach {
            assertDeepEquals(one[it], other[it], path + "[${it}]")
        }
    }

    private fun assertCollectionEquals(one: Collection<*>, other: Collection<*>, path: List<String>) {
        assertEquals(one.size, other.size, path.toString())
        one.toTypedArray().zip(other.toTypedArray()).forEachIndexed { index, pair ->
            this.assertDeepEquals(
                pair.first,
                pair.second,
                path + "[${index}]"
            )
        }
    }
}
