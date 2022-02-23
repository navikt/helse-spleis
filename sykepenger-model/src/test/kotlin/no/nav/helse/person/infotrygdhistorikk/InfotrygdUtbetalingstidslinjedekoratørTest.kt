package no.nav.helse.person.infotrygdhistorikk

import no.nav.helse.februar
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.Inntektshistorikk
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.testhelpers.S
import no.nav.helse.testhelpers.resetSeed
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler.Companion.NormalArbeidstaker
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverperiodeBuilder
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiodeteller
import no.nav.helse.utbetalingstidslinje.Inntekter
import no.nav.helse.utbetalingstidslinje.UtbetalingstidslinjeBuilder
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

internal class InfotrygdUtbetalingstidslinjedekoratørTest {

    @BeforeEach
    fun setup() {
        resetSeed()
    }

    @Test
    fun `ekskluderer dager før første dag`() {
        val builder = UtbetalingstidslinjeBuilder(Inntekter(
            skjæringstidspunkter = listOf(1.januar),
            inntektPerSkjæringstidspunkt = mapOf(
                1.januar to Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), 1.januar, UUID.randomUUID(), 25000.månedlig)
            ),
            regler = NormalArbeidstaker,
            subsumsjonObserver = MaskinellJurist()
        ))
        val dekoratør = InfotrygdUtbetalingstidslinjedekoratør(builder, 1.februar)
        val tidslinje = 31.S + 28.S
        tidslinje.accept(ArbeidsgiverperiodeBuilder(Arbeidsgiverperiodeteller.NormalArbeidstaker, dekoratør, MaskinellJurist()))
        assertEquals(1.februar til 28.februar, builder.result().periode())
    }
}
