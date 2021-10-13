package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Inntektshistorikk
import no.nav.helse.person.Refusjonshistorikk
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.testhelpers.*
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
        val refusjonsgjødsler = Refusjonsgjødsler(tidslinje = utbetalingstidslinje, refusjonshistorikk = refusjonshistorikk(refusjon(1.januar, 1431.daglig)))
        val aktivitetslogg = Aktivitetslogg()
        refusjonsgjødsler.gjødsle(aktivitetslogg)
        assertRefusjonArbeidsgiver(utbetalingstidslinje, 1431.0)
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `Gjødsler utbetalingslinje uten refusjon i januar`() {
        val utbetalingstidslinje = (16.U + 10.S).utbetalingstidslinje(inntektsopplysning(1.januar, 1431.daglig))
        val refusjonsgjødsler = Refusjonsgjødsler(tidslinje = utbetalingstidslinje, refusjonshistorikk = refusjonshistorikk(refusjon(1.januar, null)))
        val aktivitetslogg = Aktivitetslogg()
        refusjonsgjødsler.gjødsle(aktivitetslogg)
        assertRefusjonArbeidsgiver(utbetalingstidslinje, 0.0)
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }


    @Test
    fun `Gjødsler utbetalingslinje med full refusjon i februar`() {
        resetSeed(1.februar)
        val utbetalingstidslinje = (16.U + 10.S).utbetalingstidslinje(inntektsopplysning(1.februar, 2308.daglig))
        val refusjonsgjødsler = Refusjonsgjødsler(tidslinje = utbetalingstidslinje, refusjonshistorikk = refusjonshistorikk(refusjon(1.februar, 1154.daglig)))
        val aktivitetslogg = Aktivitetslogg()
        refusjonsgjødsler.gjødsle(aktivitetslogg)
        assertRefusjonArbeidsgiver(utbetalingstidslinje, 1154.0)
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `Gjødsler utbetalingslinje hvor vi ikke finner noe refusjon (Infotrygd⁉) - Legger på warning og antar full refusjon`() {
        resetSeed(1.februar)
        val utbetalingstidslinje = (16.U + 10.S).utbetalingstidslinje(inntektsopplysning(1.februar, 2308.daglig))
        val refusjonsgjødsler = Refusjonsgjødsler(tidslinje = utbetalingstidslinje, refusjonshistorikk = refusjonshistorikk())
        val aktivitetslogg = Aktivitetslogg()
        refusjonsgjødsler.gjødsle(aktivitetslogg)
        // Helger i arbeidsgiverperioden har inntekt
        assertRefusjonArbeidsgiver(utbetalingstidslinje[1.februar til 16.februar], 2308.0)
        assertRefusjonArbeidsgiver(utbetalingstidslinje[17.februar til 18.februar], 0.0)
        assertRefusjonArbeidsgiver(utbetalingstidslinje[19.februar til 23.februar], 2308.0)
        assertRefusjonArbeidsgiver(utbetalingstidslinje[24.februar til 25.februar], 0.0)
        assertRefusjonArbeidsgiver(utbetalingstidslinje[26.februar til 26.februar], 2308.0)
        assertTrue(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `Utbetalingstidslinje med flere sammenhengende sykdomsperioder henter riktig refusjon for perioden`() {
        resetSeed(1.februar)
        val utbetalingstidslinje = (16.U + 10.S + 2.A + 10.S).utbetalingstidslinje(
            inntektsopplysning(1.februar, 2308.daglig) + inntektsopplysning(1.mars, 2500.daglig)
        )
        val refusjonsgjødsler = Refusjonsgjødsler(
            tidslinje = utbetalingstidslinje,
            refusjonshistorikk = refusjonshistorikk(refusjon(1.februar, 2308.daglig), refusjon(1.mars, 2500.daglig))
        )
        val aktivitetslogg = Aktivitetslogg()
        refusjonsgjødsler.gjødsle(aktivitetslogg)
        assertRefusjonArbeidsgiver(utbetalingstidslinje[1.februar til 26.februar], 2308.0)
        assertRefusjonArbeidsgiver(utbetalingstidslinje[27.februar til 28.februar], null)
        assertRefusjonArbeidsgiver(utbetalingstidslinje[1.mars til 10.mars], 2500.0)
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `Utbetalingstidslinje med flere sammenhengende sykdomsperioder og oppdelt arbeidsgiverperiode henter riktig refusjon for perioden`() {
        resetSeed(1.februar)
        val utbetalingstidslinje = (8.U + 10.A + 8.U + 2.S).utbetalingstidslinje(
            inntektsopplysning(19.februar, 2308.daglig)
        )
        val refusjonsgjødsler = Refusjonsgjødsler(
            tidslinje = utbetalingstidslinje,
            refusjonshistorikk = refusjonshistorikk(
                refusjon(
                    førsteFraværsdag = 19.februar,
                    beløp = 2308.daglig,
                    arbeidsgiverperioder = listOf(1.februar til 8.februar, 19.februar til 26.februar)
                )
            )
        )
        val aktivitetslogg = Aktivitetslogg()
        refusjonsgjødsler.gjødsle(aktivitetslogg)
        assertRefusjonArbeidsgiver(utbetalingstidslinje[1.februar til 18.februar], null)
        assertRefusjonArbeidsgiver(utbetalingstidslinje[19.februar til 28.februar], 2308.0)
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `Utbetalingstidslinje hvor dagen før en arbeidsgiverperiode er en fridag`() {
        val utbetalingstidslinje = (8.U + 26.opphold + 16.U + 2.S).utbetalingstidslinje(
            inntektsopplysning(4.februar, 2308.daglig)
        )
        val refusjonsgjødsler = Refusjonsgjødsler(
            tidslinje = utbetalingstidslinje,
            refusjonshistorikk = refusjonshistorikk(
                refusjon(
                    førsteFraværsdag = 4.februar,
                    beløp = 2308.daglig,
                    arbeidsgiverperioder = listOf(4.februar til 19.februar)
                )
            )
        )
        val aktivitetslogg = Aktivitetslogg()
        assertDoesNotThrow { refusjonsgjødsler.gjødsle(aktivitetslogg) }
    }

    private companion object {

        operator fun Iterable<Utbetalingstidslinje.Utbetalingsdag>.get(periode: Periode) = filter { it.dato in periode }

        private fun assertRefusjonArbeidsgiver(utbetalingstidslinje: Iterable<Utbetalingstidslinje.Utbetalingsdag>, forventetUkedagbeløp: Double?) {
            utbetalingstidslinje.forEach { utbetalingsdag ->
                assertEquals(forventetUkedagbeløp, utbetalingsdag.økonomi.arbeidsgiverRefusjonsbeløp())
            }
        }

        private fun refusjonshistorikk(vararg refusjoner: Refusjonshistorikk.Refusjon): Refusjonshistorikk {
            val refusjonshistorikk = Refusjonshistorikk()
            refusjoner.forEach { refusjonshistorikk.leggTilRefusjon(it) }
            return refusjonshistorikk
        }

        private fun refusjon(
            førsteFraværsdag: LocalDate,
            beløp: Inntekt?,
            arbeidsgiverperioder: List<Periode> = listOf(førsteFraværsdag til førsteFraværsdag.plusDays(15))
        ) = Refusjonshistorikk.Refusjon(
            meldingsreferanseId = UUID.randomUUID(),
            førsteFraværsdag = førsteFraværsdag,
            arbeidsgiverperioder = arbeidsgiverperioder,
            beløp = beløp,
            sisteRefusjonsdag = null,
            endringerIRefusjon = emptyList()
        )

        private fun Økonomi.arbeidsgiverRefusjonsbeløp() = medData { _, arbeidsgiverRefusjonsbeløp, _, _, _, _, _, _, _ -> arbeidsgiverRefusjonsbeløp }

        private fun inntektsopplysning(skjæringstidspunkt: LocalDate, inntekt: Inntekt) = mapOf(
            skjæringstidspunkt to Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), skjæringstidspunkt, UUID.randomUUID(), inntekt)
        )

        private fun Sykdomstidslinje.utbetalingstidslinje(
            inntektsopplysning: Map<LocalDate, Inntektshistorikk.Inntektsopplysning?>,
            strategi: Forlengelsestrategi = Forlengelsestrategi.Ingen
        ): Utbetalingstidslinje {
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
