package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelser.Testhendelse
import no.nav.helse.testhelpers.Uke
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class HistorikkTest {

    @Test
    fun `dager med overlapp gir historikk`() {
        val sykmeldingsperiode = ConcreteSykdomstidslinje.sykedager(Uke(1).mandag, Uke(1).onsdag,
            Testhendelse()
        )
        val søknadsperiode = ConcreteSykdomstidslinje.sykedager(Uke(1).mandag, Uke(1).onsdag,
            Testhendelse()
        )

        val periode = sykmeldingsperiode + søknadsperiode

        val dager = periode.flatten()

        assertEquals(1, dager[0].dagerErstattet().size)
        assertEquals(1, dager[1].dagerErstattet().size)
        assertEquals(1, dager[2].dagerErstattet().size)
    }

    @Test
    fun `dager uten overlapp gir ikke historikk`() {
        val sykmeldingsperiode = ConcreteSykdomstidslinje.sykedager(Uke(1).mandag, Uke(1).tirsdag,
            Testhendelse()
        )
        val søknadsperiode = ConcreteSykdomstidslinje.sykedager(Uke(1).onsdag, Uke(1).fredag,
            Testhendelse()
        )

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
        val sykmeldingsperiode = ConcreteSykdomstidslinje.sykedager(Uke(1).mandag, Uke(1).fredag,
            Testhendelse()
        )
        val søknadsperiode = ConcreteSykdomstidslinje.sykedager(Uke(1).mandag, Uke(1).fredag,
            Testhendelse()
        )
        val inntektsmeldingsperiode = ConcreteSykdomstidslinje.ferie(Uke(1).mandag, Uke(1).onsdag,
            Testhendelse()
        )

        val periode = sykmeldingsperiode + søknadsperiode + inntektsmeldingsperiode

        val dager = periode.flatten()

        assertEquals(2, dager[0].dagerErstattet().size)
        assertEquals(2, dager[1].dagerErstattet().size)
        assertEquals(2, dager[2].dagerErstattet().size)
        assertEquals(1, dager[3].dagerErstattet().size)
        assertEquals(1, dager[4].dagerErstattet().size)
    }
}
