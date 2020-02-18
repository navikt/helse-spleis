package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelser.SendtSøknad
import no.nav.helse.testhelpers.Uke
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class EgenmeldingsdagTest {
    @Test
    fun `egenmeldingsdager kan legges til tidslinjen`() {
        val egenmeldingsdager = ConcreteSykdomstidslinje.egenmeldingsdager(Uke(1).mandag, Uke(1).onsdag, SendtSøknad.SøknadDagFactory)
        Assertions.assertEquals(3, egenmeldingsdager.length(), "Skal ha 3 egenmeldingsdager")
    }

    @Test
    fun `egenmeldingsdager over helg teller som egenmeldingsdager på arbeidsdager`() {
        val egenmeldingsdager = ConcreteSykdomstidslinje.egenmeldingsdager(Uke(1).fredag, Uke(2).mandag, SendtSøknad.SøknadDagFactory)
        Assertions.assertEquals(4, egenmeldingsdager.length(), "Skal ha 4 egenmeldingsdager")
    }

}
