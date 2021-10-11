package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.til
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Inntektshistorikk
import no.nav.helse.person.Refusjonshistorikk
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.testhelpers.*
import no.nav.helse.testhelpers.U
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Økonomi
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class RefusjonsgjødslerTest {

    @BeforeEach
    fun før() {
        resetSeed()
    }

    @Test
    fun `Gjødsler utbetalingslinje med full refusjon i januar`() {
        val utbetalingstidslinje = (16.U + 10.S).utbetalingstidslinje(inntektsopplysning(1.januar, 2862.daglig))
        val refusjonsgjødsler = Refusjonsgjødsler(tidslinjer = listOf(utbetalingstidslinje), refusjonshistorikk = refusjonshistorikk(refusjon(1.januar, 1431.daglig)))
        val aktivitetslogg = Aktivitetslogg()
        refusjonsgjødsler.gjødsle(aktivitetslogg)
        assertRefusjonArbeidsgiver(utbetalingstidslinje, 1431.0)
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `Gjødsler utbetalingslinje uten refusjon i januar`() {
        val utbetalingstidslinje = (16.U + 10.S).utbetalingstidslinje(inntektsopplysning(1.januar, 1431.daglig))
        val refusjonsgjødsler = Refusjonsgjødsler(tidslinjer = listOf(utbetalingstidslinje), refusjonshistorikk = refusjonshistorikk(refusjon(1.januar, null)))
        val aktivitetslogg = Aktivitetslogg()
        refusjonsgjødsler.gjødsle(aktivitetslogg)
        assertRefusjonArbeidsgiver(utbetalingstidslinje, 0.0)
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }


    @Test
    fun `Gjødsler utbetalingslinje med full refusjon i februar`() {
        resetSeed(1.februar)
        val utbetalingstidslinje = (16.U + 10.S).utbetalingstidslinje(inntektsopplysning(1.februar, 2308.daglig))
        val refusjonsgjødsler = Refusjonsgjødsler(tidslinjer = listOf(utbetalingstidslinje), refusjonshistorikk = refusjonshistorikk(refusjon(1.februar, 1154.daglig)))
        val aktivitetslogg = Aktivitetslogg()
        refusjonsgjødsler.gjødsle(aktivitetslogg)
        assertRefusjonArbeidsgiver(utbetalingstidslinje, 1154.0)
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `Gjødsler utbetalingslinje hvor vi ikke finner noe refusjon (Infotrygd⁉) - Legger på warning og antar full refusjon`() {
        resetSeed(1.februar)
        val utbetalingstidslinje = (16.U + 10.S).utbetalingstidslinje(inntektsopplysning(1.februar, 2308.daglig))
        val refusjonsgjødsler = Refusjonsgjødsler(tidslinjer = listOf(utbetalingstidslinje), refusjonshistorikk = refusjonshistorikk())
        val aktivitetslogg = Aktivitetslogg()
        refusjonsgjødsler.gjødsle(aktivitetslogg)
        assertRefusjonArbeidsgiver(utbetalingstidslinje, 2308.0)
        assertTrue(aktivitetslogg.hasWarningsOrWorse())
    }

    private companion object {

        private fun assertRefusjonArbeidsgiver(utbetalingstidslinje: Iterable<Utbetalingstidslinje.Utbetalingsdag>, forventetUkedagbeløp: Double) {
            utbetalingstidslinje.forEach { utbetalingsdag ->
                when {
                    utbetalingsdag.økonomi.aktuellDagsinntekt() == 0.0 -> assertEquals(0.0, utbetalingsdag.økonomi.arbeidsgiverRefusjonsbeløp())
                    else -> assertEquals(forventetUkedagbeløp, utbetalingsdag.økonomi.arbeidsgiverRefusjonsbeløp())
                }
            }
        }

        private fun refusjonshistorikk(vararg refusjoner: Refusjonshistorikk.Refusjon) : Refusjonshistorikk {
            val refusjonshistorikk = Refusjonshistorikk()
            refusjoner.forEach { refusjonshistorikk.leggTilRefusjon(it) }
            return refusjonshistorikk
        }

        private fun refusjon(førsteFraværsdag: LocalDate, beløp: Inntekt?) = Refusjonshistorikk.Refusjon(
            meldingsreferanseId = UUID.randomUUID(),
            førsteFraværsdag = førsteFraværsdag,
            arbeidsgiverperioder = listOf(førsteFraværsdag til førsteFraværsdag.plusDays(15)),
            beløp = beløp,
            opphørsdato = null,
            endringerIRefusjon = emptyList()
        )

        private fun Økonomi.arbeidsgiverRefusjonsbeløp() = medData { _, arbeidsgiverRefusjonsbeløp, _, _, _, _, _, _, _ ->  arbeidsgiverRefusjonsbeløp }
        private fun Økonomi.aktuellDagsinntekt() = medData { _, _, _, _, _, aktuellDagsinntekt, _, _, _ -> aktuellDagsinntekt }

        private fun inntektsopplysning(skjæringstidspunkt: LocalDate, inntekt: Inntekt) = mapOf(
            skjæringstidspunkt to Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), skjæringstidspunkt, UUID.randomUUID(), inntekt)
        )
        private fun Sykdomstidslinje.utbetalingstidslinje(
            inntektsopplysning: Map<LocalDate, Inntektshistorikk.Inntektsopplysning?>,
            strategi: Forlengelsestrategi = Forlengelsestrategi.Ingen
        ) : Utbetalingstidslinje {
            val tidslinje = UtbetalingstidslinjeBuilder(
                skjæringstidspunkter = inntektsopplysning.keys.toList(),
                inntektPerSkjæringstidspunkt = inntektsopplysning
            ).apply { forlengelsestrategi(strategi) }.result(this, periode()!!)
            verifiserRekkefølge(tidslinje)
            return tidslinje
        }

        private fun verifiserRekkefølge(tidslinje: Utbetalingstidslinje) {
            tidslinje.windowed(2).forEach { (forrige, neste) ->
                assertTrue(neste.dato > forrige.dato) { "Rekkefølgen er ikke riktig: ${neste.dato} skal være nyere enn ${forrige.dato}" }
            }
        }
    }
}
