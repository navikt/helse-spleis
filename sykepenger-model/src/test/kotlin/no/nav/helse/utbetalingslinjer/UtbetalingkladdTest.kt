package no.nav.helse.utbetalingslinjer

import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.utbetalingslinjer.Fagområde.Sykepenger
import no.nav.helse.utbetalingslinjer.Fagområde.SykepengerRefusjon
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class UtbetalingkladdTest {

    @Test
    fun `kladd opphører tidligere periode dersom kladd slutter før`() {
        val kladd = Utbetalingkladd(
            periode = 5.januar til 20.januar,
            arbeidsgiveroppdrag = Oppdrag("orgnr", SykepengerRefusjon),
            personoppdrag = Oppdrag("fnr", Sykepenger)
        )

        assertTrue(kladd.opphører(5.januar til 30.januar))
        assertTrue(kladd.opphører(21.januar til 30.januar))
        assertFalse(kladd.opphører(5.januar til 20.januar))
        assertFalse(kladd.opphører(1.januar til 4.januar))
    }
}