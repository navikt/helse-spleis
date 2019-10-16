package no.nav.helse.sykdomstidlinje.test

import no.nav.helse.Testhendelse
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.testhelpers.fredag
import no.nav.helse.testhelpers.mandag
import no.nav.helse.testhelpers.onsdag
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class EgenmeldingsdagTest {
    private val testKildeHendelse = Testhendelse(4.fredag.atTime(12, 0))

    @Test
    fun `egenmeldingsdager kan legges til tidslinjen`() {
        val egenmeldingsdager = Sykdomstidslinje.egenmeldingsdager(1.mandag, 1.onsdag, testKildeHendelse)
        Assertions.assertEquals(3, egenmeldingsdager.length(), "Skal ha 3 egenmeldingsdager")
        Assertions.assertEquals(3, egenmeldingsdager.antallSykedagerHvorViTellerMedHelg(), "Skal ha 0 dager hvor vi teller med helg")
        Assertions.assertEquals(0, egenmeldingsdager.antallSykedagerHvorViIkkeTellerMedHelg(), "Skal ha 0 dager hvor vi ikke teller med helg")
    }

    @Test
    fun `egenmeldingsdager over helg teller som egenmeldingsdager på arbeidsdager`() {
        val egenmeldingsdager = Sykdomstidslinje.egenmeldingsdager(1.fredag, 2.mandag, testKildeHendelse)
        Assertions.assertEquals(4, egenmeldingsdager.length(), "Skal ha 4 egenmeldingsdager")
        Assertions.assertEquals(4, egenmeldingsdager.antallSykedagerHvorViTellerMedHelg(), "Skal ha 0 dager hvor vi teller med helg")
        Assertions.assertEquals(0, egenmeldingsdager.antallSykedagerHvorViIkkeTellerMedHelg(), "Skal ha 0 dager hvor vi ikke teller med helg")
    }

}
