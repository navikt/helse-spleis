package no.nav.helse.person

import no.nav.helse.hendelser.til
import no.nav.helse.januar
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DagerUtenNavAnsvaravklaringTest {

    @Test
    fun `En påstartet telling er det samme som en fortsatt eller ferdig avklart telling`() {
        val en = DagerUtenNavAnsvaravklaring(false, listOf(1.januar til 10.januar))
        val to = DagerUtenNavAnsvaravklaring(false, listOf(1.januar til 12.januar))
        val tre = DagerUtenNavAnsvaravklaring(true, listOf(1.januar til 16.januar))
        assertTrue(en.samme(en))
        assertTrue(en.samme(to))
        assertTrue(en.samme(tre))

        assertTrue(to.samme(en))
        assertTrue(to.samme(to))
        assertTrue(to.samme(tre))

        assertTrue(tre.samme(en))
        assertTrue(tre.samme(to))
        assertTrue(tre.samme(tre))
    }

    @Test
    fun `En miks av avklart på tvers av spleis og infotrygd`() {
        val spleis = DagerUtenNavAnsvaravklaring(true, listOf(1.januar til 16.januar))
        val infotrygd = DagerUtenNavAnsvaravklaring(true, listOf(1.januar til 1.januar))
        assertFalse(spleis.samme(infotrygd))
        assertFalse(infotrygd.samme(spleis))
    }
}
