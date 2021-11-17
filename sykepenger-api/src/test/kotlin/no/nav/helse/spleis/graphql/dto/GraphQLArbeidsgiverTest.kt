package no.nav.helse.spleis.graphql.dto

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class GraphQLArbeidsgiverTest {

    @Test
    internal fun `safeSlice - returnerer en slice av listen`() {
        val liste = listOf(1, 2, 3, 4, 5)
        assertEquals(listOf(3, 4), liste.safeSlice(2, 2))
        assertEquals(listOf(5), liste.safeSlice(1, 4))
        assertEquals(listOf(1, 2, 3, 4, 5), liste.safeSlice(5, 0))
    }

    @Test
    internal fun `safeSlice - returnerer de n første elementene dersom from er null og first er n`() {
        val liste = listOf(1, 2, 3, 4, 5)
        assertEquals(listOf(1, 2), liste.safeSlice(2, null))
        assertEquals(listOf(1, 2, 3, 4), liste.safeSlice(4, null))
        assertEquals(emptyList<Int>(), liste.safeSlice(0, null))
    }

    @Test
    internal fun `safeSlice - returnerer resten av listen fom from dersom first er null`() {
        val liste = listOf(1, 2, 3, 4, 5)
        assertEquals(listOf(3, 4, 5), liste.safeSlice(null, 2))
        assertEquals(listOf(5), liste.safeSlice(null, 4))
        assertEquals(listOf(1, 2, 3, 4, 5), liste.safeSlice(null, 0))
    }

    @Test
    internal fun `safeSlice - returnerer tom liste når slicen som etterspørres er utenfor den opprinnelige listen`() {
        val liste = listOf(1, 2, 3, 4, 5)
        assertEquals(emptyList<Int>(), liste.safeSlice(1, 5))
        assertEquals(emptyList<Int>(), liste.safeSlice(1000, 1000))
    }

}
