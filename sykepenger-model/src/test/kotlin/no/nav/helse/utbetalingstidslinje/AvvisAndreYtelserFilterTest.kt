package no.nav.helse.utbetalingstidslinje

import java.util.UUID
import no.nav.helse.hendelser.somPeriode
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.testhelpers.NAV
import no.nav.helse.testhelpers.tidslinjeOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class AvvisAndreYtelserFilterTest {

    @Test
    fun `Avslår andre ytelser`() {
        val filter = AvvisAndreYtelserFilter(
            foreldrepenger = listOf(1.januar.somPeriode()),
            svangerskapspenger = listOf(2.januar.somPeriode()),
            pleiepenger = listOf(3.januar.somPeriode()),
            dagpenger = listOf(4.januar.somPeriode()),
            arbeidsavklaringspenger = listOf(5.januar.somPeriode()),
            opplæringspenger = listOf(8.januar.somPeriode()),
            omsorgspenger = listOf(9.januar.somPeriode()),
        )

        val tidslinjer = listOf(
            Arbeidsgiverberegning(
                orgnummer = "a1",
                vedtaksperioder = listOf(
                    Vedtaksperiodeberegning(
                        vedtaksperiodeId = UUID.randomUUID(),
                        utbetalingstidslinje = tidslinjeOf(9.NAV)
                    )
                ),
                ghostOgAndreInntektskilder = emptyList()
            )
        )
        val result = filter.filter(tidslinjer, januar).single().samletVedtaksperiodetidslinje.inspektør

        assertEquals(7, result.avvistDagTeller)
        assertEquals(Begrunnelse.AndreYtelserForeldrepenger, result.begrunnelse(1.januar).single())
        assertEquals(Begrunnelse.AndreYtelserSvangerskapspenger, result.begrunnelse(2.januar).single())
        assertEquals(Begrunnelse.AndreYtelserPleiepenger, result.begrunnelse(3.januar).single())
        assertEquals(Begrunnelse.AndreYtelserDagpenger, result.begrunnelse(4.januar).single())
        assertEquals(Begrunnelse.AndreYtelserAap, result.begrunnelse(5.januar).single())
        assertEquals(Begrunnelse.AndreYtelserOpplaringspenger, result.begrunnelse(8.januar).single())
        assertEquals(Begrunnelse.AndreYtelserOmsorgspenger, result.begrunnelse(9.januar).single())
    }
}
