package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.forrigeDag
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje.Companion.ghostdager
import no.nav.helse.testhelpers.FiktivInntekt
import no.nav.helse.testhelpers.faktaavklarteInntekter
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FaktaavklarteInntekterTest {

    @Test
    fun ghosttidslinje() {
        val skjæringstidspunkt = 1.januar
        val a1Fom = skjæringstidspunkt

        val inntekter = listOf(FiktivInntekt("a1", 1000.månedlig, a1Fom til 10.januar)).faktaavklarteInntekter(skjæringstidspunkt)

        assertEquals(Sykdomstidslinje(), inntekter.ghosttidslinje("a2", skjæringstidspunkt))
        assertEquals(Sykdomstidslinje(), inntekter.ghosttidslinje("a2", skjæringstidspunkt.forrigeDag))
        assertEquals(ghostdager(skjæringstidspunkt til 31.januar), inntekter.ghosttidslinje("a1", 31.januar))
        assertEquals(ghostdager(skjæringstidspunkt til 5.januar), inntekter.ghosttidslinje("a1", 5.januar))
    }

    @Test
    fun `fastsatt årsinntekt - tilkommen inntekt`() {
        val skjæringstidspunkt = 1.januar
        val a1Fom = skjæringstidspunkt
        val a2Fom = skjæringstidspunkt.plusDays(1)

        val inntekter = listOf(
            FiktivInntekt("a1", 1000.månedlig, a1Fom til 10.januar),
            FiktivInntekt("a2", 2000.månedlig, a2Fom til LocalDate.MAX)
        ).faktaavklarteInntekter(skjæringstidspunkt)

        assertEquals(ghostdager(skjæringstidspunkt til 31.januar), inntekter.ghosttidslinje("a1", 31.januar))
        assertEquals(ghostdager(a2Fom til 31.januar), inntekter.ghosttidslinje("a2", 31.januar))
    }
}