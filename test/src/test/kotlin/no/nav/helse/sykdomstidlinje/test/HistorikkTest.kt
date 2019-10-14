package no.nav.helse.sykdomstidlinje.test

import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class HistorikkTest {

    companion object {
        private val uke1Mandag = LocalDate.of(2019, 9, 23)
        private val uke1Tirsdag = LocalDate.of(2019, 9, 24)
        private val uke1Onsdag = LocalDate.of(2019, 9, 25)
        private val uke1Fredag = LocalDate.of(2019, 9, 27)
    }

    @Test
    fun `dager med overlapp gir historikk`() {
        val sykmeldingsperiode = Sykdomstidslinje.sykedager(uke1Mandag, uke1Onsdag, Testhendelse())
        val søknadsperiode = Sykdomstidslinje.sykedager(uke1Mandag, uke1Onsdag, Testhendelse())

        val periode = sykmeldingsperiode + søknadsperiode

        val dager = periode.flatten()

        assertEquals(1, dager[0].dagerErstattet().size)
        assertEquals(1, dager[1].dagerErstattet().size)
        assertEquals(1, dager[2].dagerErstattet().size)
    }

    @Test
    fun `dager uten overlapp gir ikke historikk`() {
        val sykmeldingsperiode = Sykdomstidslinje.sykedager(uke1Mandag, uke1Tirsdag, Testhendelse())
        val søknadsperiode = Sykdomstidslinje.sykedager(uke1Onsdag, uke1Fredag, Testhendelse())

        val periode = sykmeldingsperiode + søknadsperiode

        val dager = periode.flatten()

        assertEquals(0, dager[0].dagerErstattet().size)
        assertEquals(0, dager[1].dagerErstattet().size)
        assertEquals(0, dager[2].dagerErstattet().size)
        assertEquals(0, dager[3].dagerErstattet().size)
        assertEquals(0, dager[4].dagerErstattet().size)
    }

    @Disabled
    @Test
    fun `bevarer historikk fra forrige overlapp`() {
        val sykmeldingsperiode = Sykdomstidslinje.sykedager(uke1Mandag, uke1Fredag, Testhendelse())
        val søknadsperiode = Sykdomstidslinje.sykedager(uke1Mandag, uke1Fredag, Testhendelse())
        val inntektsmeldingsperiode = Sykdomstidslinje.ferie(uke1Mandag, uke1Onsdag, Testhendelse())

        val periode = sykmeldingsperiode + søknadsperiode + inntektsmeldingsperiode

        val dager = periode.flatten()

        assertEquals(2, dager[0].dagerErstattet().size)
        assertEquals(2, dager[1].dagerErstattet().size)
        assertEquals(2, dager[2].dagerErstattet().size)
        assertEquals(1, dager[3].dagerErstattet().size)
        assertEquals(1, dager[4].dagerErstattet().size)
    }
}
