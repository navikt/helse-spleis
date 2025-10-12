package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import java.time.LocalDate.EPOCH
import no.nav.helse.desember
import no.nav.helse.erHelg
import no.nav.helse.forrigeDag
import no.nav.helse.januar
import no.nav.helse.september
import no.nav.helse.utbetalingstidslinje.MaksimumSykepengedagerregler.Companion.NormalArbeidstaker
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class MaksdatokontekstTest {

    @Test
    fun tilbakestill() {
        val kontekst = Maksdatokontekst.tomKontekst(NormalArbeidstaker, EPOCH)
        assertEquals(kontekst, kontekst
            .inkrementer(1.januar)
            .medOppholdsdag(2.januar)
            .medAvslåttDag(3.januar, Begrunnelse.SykepengedagerOppbrukt)
            .tilbakestill())
    }

    @Test
    fun inkrementer() {
        val kontekst = Maksdatokontekst.tomKontekst(NormalArbeidstaker, EPOCH)
        val forventet = kontekst.copy(
            vurdertTilOgMed = 3.januar,
            betalteDager = setOf(1.januar, 3.januar)
        )
        assertEquals(forventet, kontekst
            .inkrementer(1.januar)
            .medOppholdsdag(2.januar)
            .inkrementer(3.januar))
    }

    @Test
    fun `forskyver treårsvindu`() {
        val kontekst = Maksdatokontekst.tomKontekst(NormalArbeidstaker, EPOCH)
        val forventet = kontekst.copy(
            vurdertTilOgMed = 3.januar,
            betalteDager = setOf(2.januar, 3.januar),
            startdatoTreårsvindu = 2.januar
        )
        assertEquals(forventet, kontekst
            .inkrementer(1.januar)
            .inkrementer(2.januar)
            .dekrementer(3.januar, 2.januar))
    }

    @Test
    fun `gjenstående dager`() {
        val sekstisyvårsdagen = 2.januar
        val kontekst = Maksdatokontekst
            .tomKontekst(NormalArbeidstaker, sekstisyvårsdagen)
            .nyMaksdatosak(1.januar, EPOCH)
            .inkrementer(2.januar)
            .inkrementer(3.januar)

        assertEquals(245, kontekst.gjenståendeDagerUnder67År)
        assertEquals(59, kontekst.gjenståendeDagerOver67År)
        assertEquals(setOf(1.januar, 2.januar, 3.januar), kontekst.betalteDager)
        assertEquals(setOf(3.januar), kontekst.betalteDagerOver67)
    }

    @Test
    fun `maksdato under 67 - ingen forbrukte dager`() {
        val sekstisyvårsdagen = 1.januar(2021)
        val kontekst = medBetalteDager(antallBetalteDager = 0, sekstisyvårsdagen, sisteVurderteDag = 16.januar)
        assertEquals(28.desember, kontekst.beregnMaksdato(1.januar(2024), null).maksdato)
    }

    @Test
    fun `maksdato under 67 - alle dager forbrukt`() {
        val sekstisyvårsdagen = 1.januar(2021)
        val kontekst = medBetalteDager(antallBetalteDager = 248, sekstisyvårsdagen, sisteVurderteDag = 31.januar, sisteBetalteDag = 10.januar)
        assertEquals(10.januar, kontekst.beregnMaksdato(sekstisyvårsdagen.plusYears(3), null).maksdato)
    }

    @Test
    fun `maksdato under 67 inntreffer på dødsdato`() {
        val sekstisyvårsdagen = 1.januar(2021)
        val dødsdato = 31.januar
        val kontekst = medBetalteDager(antallBetalteDager = 10, sekstisyvårsdagen, sisteVurderteDag = 17.januar)
        assertEquals(dødsdato, kontekst.beregnMaksdato(sekstisyvårsdagen.plusYears(3), dødsdato).maksdato)
    }

    @Test
    fun `maksdato under 67 inntreffer etter 67`() {
        val sekstisyvårsdagen = 27.september
        val kontekst = medBetalteDager(antallBetalteDager = 0, sekstisyvårsdagen, sisteVurderteDag = 16.januar)
        assertEquals(20.desember, kontekst.beregnMaksdato(sekstisyvårsdagen.plusYears(3), null).maksdato)
    }

    @Test
    fun `maksdato over 67 inntreffer på 70 årsdagen`() {
        val syttiårsdagen = 7.januar
        val sekstisyvårsdagen = syttiårsdagen.minusYears(3)
        val kontekst = medBetalteDager(antallBetalteDager = 0, sekstisyvårsdagen, sisteVurderteDag = 1.januar)
        assertEquals(5.januar, kontekst.beregnMaksdato(syttiårsdagen, null).maksdato)
    }

    @Test
    fun `dødsdato rett før 70 år - maksdato inntreffer på dødsdato`() {
        val syttiårsdagen = 7.januar
        val sekstisyvårsdagen = syttiårsdagen.minusYears(3)
        val dødsdato = 3.januar
        val kontekst = medBetalteDager(antallBetalteDager = 0, sekstisyvårsdagen, sisteVurderteDag = 1.januar)
        assertEquals(dødsdato, kontekst.beregnMaksdato(syttiårsdagen, dødsdato).maksdato)
    }

    private fun medBetalteDager(antallBetalteDager: Int, sekstisyvårsdagen: LocalDate, sisteVurderteDag: LocalDate = 1.januar, sisteBetalteDag: LocalDate = sisteVurderteDag): Maksdatokontekst {
        val tomKontekst = Maksdatokontekst
            .tomKontekst(NormalArbeidstaker, sekstisyvårsdagen)
        if (antallBetalteDager == 0) return tomKontekst.copy(vurdertTilOgMed = sisteVurderteDag)
        val betalteDager = buildList {
            var dag = sisteBetalteDag
            while (size < antallBetalteDager) {
                if (!dag.erHelg()) add(0, dag)
                dag = dag.forrigeDag
            }
        }
        return tomKontekst
            .copy(
                vurdertTilOgMed = sisteVurderteDag,
                startdatoSykepengerettighet = betalteDager.first(),
                startdatoTreårsvindu = EPOCH,
                betalteDager = betalteDager.toSet()
            )
    }
}
