package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.til
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.testhelpers.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class ArbeidsgiverperiodeBuilderTest {
    private lateinit var builder: ArbeidsgiverperiodeBuilder
    private var arbeidsgiverperiode: Arbeidsgiverperiode? = null

    @BeforeEach
    fun reset() {
        arbeidsgiverperiode = null
        resetSeed()
    }

    @Test
    fun `gir hele arbeidsgiverperioden for en dag`() {
        val agp = 1.januar til 16.januar
        val tidslinje = 16.U + 3.S + 2.F + 5.S + 2.F + 13.A + 2.S
        arbeidsgiverperiode(tidslinje)
        tidslinje.periode()?.forEach { dato -> assertArbeidsgiverperiode(agp, dato) }
        assertNull(builder.result(31.desember(2017) til 31.desember(2017)))
        assertNull(builder.result(tidslinje.sisteDag().plusDays(1) til tidslinje.sisteDag().plusDays(1)))
    }

    @Test
    fun `gir hele påbegynte arbeidsgiverperioden for en dag`() {
        val tidslinje = 10.U + 16.A + 2.F + 5.S + 2.F + 13.A + 2.S
        arbeidsgiverperiode(tidslinje)
        (1.januar til 10.januar).forEach { dato -> assertArbeidsgiverperiode(1.januar til 10.januar, dato) }
        (11.januar til 28.januar).forEach { assertNull(builder.result(it til it)) }
        val agp = listOf((29.januar til 2.februar), (18.februar til 19.februar)).flatMap { it.toList() }
        (29.januar til tidslinje.sisteDag()).forEach { dato -> assertArbeidsgiverperiode(agp, dato) }
    }

    private fun assertArbeidsgiverperiode(periode: Iterable<LocalDate>, dag: LocalDate) {
        assertEquals(periode.toList(), builder.result(dag til dag)?.toList()) {
            "Fant ikke arbeidsgiverperiode for $dag"
        }
    }

    private fun arbeidsgiverperiode(tidslinje: Sykdomstidslinje) {
        builder = ArbeidsgiverperiodeBuilder().also { it.build(tidslinje, tidslinje.periode()!!) }
    }
}
