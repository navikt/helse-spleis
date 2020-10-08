package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.spleis.e2e.er
import no.nav.helse.spleis.e2e.sant
import no.nav.helse.spleis.e2e.usant
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje.Companion.annullerteDager
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje.Companion.sykedager
import no.nav.helse.testhelpers.TestEvent.Companion.sykmelding
import no.nav.helse.testhelpers.TestEvent.Companion.testkilde
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class SjekkAv16DagersGapTest {

    @Test
    fun `Tidslinje uten gap`() {
        val tidslinje = syk(1.januar til 2.januar) + syk(17.januar til 18.januar)
        assertFalse(tidslinje.harNyArbeidsgiverperiodeEtter(2.januar))
    }

    @Test
    fun `Tidslinje med 15 dagers gap`() {
        val tidslinje = syk(1.januar til 2.januar) + syk(18.januar til 19.januar)
        assertFalse(tidslinje.harNyArbeidsgiverperiodeEtter(2.januar))
    }

    @Test
    fun `Tidslinje med 16 dagers gap`() {
        val tidslinje = (syk(1.januar til 2.januar)
            + syk(19.januar til 25.januar))
        assertTrue(tidslinje.harNyArbeidsgiverperiodeEtter(2.januar))
    }

    @Test
    fun `Annullering regnes som sykedag - vi bryr oss egentlig ikke, annullering skal behandles i Infotrygd`() {
        val tidslinje = (syk(1.januar til 2.januar)
            + annullering(19.januar til 25.januar))
        tidslinje.harNyArbeidsgiverperiodeEtter(2.januar) er sant

        val tidslinje2 = (syk(1.januar til 2.januar)
            + annullering(12.januar til 25.januar))
        tidslinje2.harNyArbeidsgiverperiodeEtter(2.januar) er usant
    }


    private fun syk(periode: Periode) =
        sykedager(periode.start, periode.endInclusive, 100.0, sykmelding)

    private fun annullering(periode: Periode) =
        annullerteDager(periode.start til periode.endInclusive, testkilde)

}
