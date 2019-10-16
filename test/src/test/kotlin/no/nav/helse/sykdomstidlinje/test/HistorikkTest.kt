package no.nav.helse.sykdomstidlinje.test

import no.nav.helse.Testhendelse
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.testhelpers.fredag
import no.nav.helse.testhelpers.mandag
import no.nav.helse.testhelpers.onsdag
import no.nav.helse.testhelpers.tirsdag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class HistorikkTest {

    @Test
    fun `dager med overlapp gir historikk`() {
        val sykmeldingsperiode = Sykdomstidslinje.sykedager(1.mandag, 1.onsdag, Testhendelse())
        val søknadsperiode = Sykdomstidslinje.sykedager(1.mandag, 1.onsdag, Testhendelse())

        val periode = sykmeldingsperiode + søknadsperiode

        val dager = periode.flatten()

        assertEquals(1, dager[0].dagerErstattet().size)
        assertEquals(1, dager[1].dagerErstattet().size)
        assertEquals(1, dager[2].dagerErstattet().size)
    }

    @Test
    fun `dager uten overlapp gir ikke historikk`() {
        val sykmeldingsperiode = Sykdomstidslinje.sykedager(1.mandag, 1.tirsdag, Testhendelse())
        val søknadsperiode = Sykdomstidslinje.sykedager(1.onsdag, 1.fredag, Testhendelse())

        val periode = sykmeldingsperiode + søknadsperiode

        val dager = periode.flatten()

        assertEquals(0, dager[0].dagerErstattet().size)
        assertEquals(0, dager[1].dagerErstattet().size)
        assertEquals(0, dager[2].dagerErstattet().size)
        assertEquals(0, dager[3].dagerErstattet().size)
        assertEquals(0, dager[4].dagerErstattet().size)
    }

    @Test
    fun `bevarer historikk fra forrige overlapp`() {
        val sykmeldingsperiode = Sykdomstidslinje.sykedager(1.mandag, 1.fredag, Testhendelse())
        val søknadsperiode = Sykdomstidslinje.sykedager(1.mandag, 1.fredag, Testhendelse())
        val inntektsmeldingsperiode = Sykdomstidslinje.ferie(1.mandag, 1.onsdag, Testhendelse())

        val periode = sykmeldingsperiode + søknadsperiode + inntektsmeldingsperiode

        val dager = periode.flatten()

        assertEquals(2, dager[0].dagerErstattet().size)
        assertEquals(2, dager[1].dagerErstattet().size)
        assertEquals(2, dager[2].dagerErstattet().size)
        assertEquals(1, dager[3].dagerErstattet().size)
        assertEquals(1, dager[4].dagerErstattet().size)
    }
}
