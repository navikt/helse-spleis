package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelse.Testhendelse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month

internal class StudieTest {
    companion object {
        private val testKildeHendelse = Testhendelse(rapportertdato = LocalDateTime.of(2019, Month.JULY, 1, 0, 0))
    }

    @Test
    fun `studiedager`() {
        val studiedager = Sykdomstidslinje.studiedager(1.juli, 7.juli,
            testKildeHendelse
        )
        assertEquals(7, studiedager.length())
        assertEquals(0, studiedager.antallSykedagerHvorViTellerMedHelg())
        assertEquals(0, studiedager.antallSykedagerHvorViIkkeTellerMedHelg())
    }

    private val Int.juli get() = LocalDate.of(2019, Month.JULY, this)
}
