package no.nav.helse.sykdomstidlinje.test

import no.nav.helse.Testhendelse
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.testhelpers.Uke
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class HendelserTest {

    @Test
    fun `kan hente ut hendelser av en tidslinje`() {
        val hendelse = Testhendelse()

        val tidslinje = Sykdomstidslinje.sykedager(Uke(1).mandag, Uke(1).fredag, hendelse)
        val hendelser = tidslinje.hendelser()

        assertEquals(1, hendelser.size)
        assertEquals(hendelse, hendelser.first())
    }

    @Test
    fun `får med hendelser fra erstattede dager`() {
        val hendelse = Testhendelse()
        val hendelse2 = Testhendelse()

        val tidslinje = Sykdomstidslinje.sykedager(Uke(1).mandag, Uke(1).fredag, hendelse) + Sykdomstidslinje.ikkeSykedager(Uke(1).mandag, Uke(1).fredag, hendelse2)
        val hendelser = tidslinje.hendelser()

        assertEquals(2, hendelser.size)
        assertThat(hendelser).containsExactlyInAnyOrder(hendelse, hendelse2)
    }
}
