package no.nav.helse.sykdomstidlinje.test

import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class EgenmeldingsdagTest {

    companion object {
        private val uke1Mandag = LocalDate.of(2019, 9, 23)
        private val uke1Onsdag = LocalDate.of(2019, 9, 25)
        private val uke1Fredag = LocalDate.of(2019, 9, 27)
        private val uke2Mandag = LocalDate.of(2019, 9, 30)

        private val testKildeHendelse = Testhendelse(LocalDateTime.of(2019, 10, 14, 20, 0))
    }

    @Test
    fun `egenmeldingsdager kan legges til tidslinjen`() {
        val egenmeldingsdager = Sykdomstidslinje.egenmeldingsdager(uke1Mandag, uke1Onsdag, testKildeHendelse)
        Assertions.assertEquals(3, egenmeldingsdager.length())
        Assertions.assertEquals(0, egenmeldingsdager.antallSykedagerHvorViTellerMedHelg())
        Assertions.assertEquals(0, egenmeldingsdager.antallSykedagerHvorViIkkeTellerMedHelg())
    }

    @Test
    fun `egenmeldingsdager over helg teller som egenmeldingsdager på arbeidsdager`() {
        val egenmeldingsdager = Sykdomstidslinje.egenmeldingsdager(uke1Fredag, uke2Mandag, testKildeHendelse)
        Assertions.assertEquals(4, egenmeldingsdager.length())
        Assertions.assertEquals(0, egenmeldingsdager.antallSykedagerHvorViTellerMedHelg())
        Assertions.assertEquals(0, egenmeldingsdager.antallSykedagerHvorViIkkeTellerMedHelg())
    }

}
