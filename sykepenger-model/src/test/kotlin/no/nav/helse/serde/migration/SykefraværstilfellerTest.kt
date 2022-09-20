package no.nav.helse.serde.migration

import no.nav.helse.desember
import no.nav.helse.februar
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.serde.migration.Sykefraværstilfeller.sykefraværstilfeller
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SykefraværstilfellerTest {

    @Test
    fun `sammenhengende periode hvor tidligste skjæringstidspunkt er før perioden`(){
        val vedtaksperioder = listOf(
            Sykefraværstilfeller.Vedtaksperiode(skjæringstidspunkt = 1.januar, periode = 1.januar til 31.januar),
            Sykefraværstilfeller.Vedtaksperiode(skjæringstidspunkt = 15.desember(2017), periode = 1.februar til 28.februar),
            Sykefraværstilfeller.Vedtaksperiode(skjæringstidspunkt = 1.januar, periode = 1.mars til 31.mars)
        )
        assertEquals(mapOf(15.desember(2017) to (1.januar til 31.mars)), sykefraværstilfeller(vedtaksperioder))
    }
}