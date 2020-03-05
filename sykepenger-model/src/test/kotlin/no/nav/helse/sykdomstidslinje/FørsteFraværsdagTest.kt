package no.nav.helse.sykdomstidslinje

import no.nav.helse.*
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Søknad
import no.nav.helse.sykdomstidslinje.dag.Dag
import no.nav.helse.sykdomstidslinje.dag.Egenmeldingsdag
import no.nav.helse.sykdomstidslinje.dag.SykHelgedag
import no.nav.helse.sykdomstidslinje.dag.Sykedag
import no.nav.helse.testhelpers.desember
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.helse.tournament.historiskDagturnering
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.util.*

internal class FørsteFraværsdagTest {

    @Test
    internal fun `kaster exception for ugyldige situasjoner`() {
        assertUgyldigTilstand(1.utenlandsdager)
        assertUgyldigTilstand(1.studieDager)
        assertUgyldigTilstand(1.feriedager)
        assertUgyldigTilstand(1.permisjonsdager)
        assertUgyldigTilstand(1.implisittDager)
        assertUgyldigTilstand(1.arbeidsdager)
    }

    @Test
    internal fun `utgangspunkt for beregning er sykedag, egenmeldingsdag eller syk helgedag`() {
        assertDagenErUtgangspunktForBeregning(sykedag)
        assertDagenErUtgangspunktForBeregning(egenmeldingsdag)
        assertDagenErUtgangspunktForBeregning(sykHelgedag)
    }

    @Test
    internal fun `utgangspunkt for beregning av ytelse er første egenmeldingsdag, sykedag eller sykhelgdag i en sammenhengende periode`() {
        assertFørsteDagErUtgangspunktForBeregning(2.sykedager)
        assertFørsteDagErUtgangspunktForBeregning(2.egenmeldingsdager)

        perioder(2.sykedager, 2.implisittDager, 2.sykedager) { _, _, periode3 ->
            assertFørsteDagErUtgangspunktForBeregning(periode3, this)
        }
        perioder(2.sykedager, 2.arbeidsdager, 2.feriedager, 2.sykedager) { _, _, _, periode4 ->
            assertFørsteDagErUtgangspunktForBeregning(periode4, this)
        }
        perioder(2.sykedager, 2.arbeidsdager, 2.permisjonsdager, 2.sykedager) { _, _, _, periode4 ->
            assertFørsteDagErUtgangspunktForBeregning(periode4, this)
        }
        perioder(2.sykedager, 2.feriedager, 2.sykedager) { periode1, _, _ ->
            assertFørsteDagErUtgangspunktForBeregning(periode1, this)
        }
        perioder(2.sykedager, 2.permisjonsdager, 2.sykedager) { periode1, _, _ ->
            assertFørsteDagErUtgangspunktForBeregning(periode1, this)
        }
        perioder(2.sykedager, 2.implisittDager, 1.sykHelgdager) { _, _ , sisteSykedager ->
            assertFørsteDagErUtgangspunktForBeregning(sisteSykedager, this)
        }
        perioder(2.sykedager, 2.implisittDager, 1.egenmeldingsdager) { _, _ , sisteSykedager ->
            assertFørsteDagErUtgangspunktForBeregning(sisteSykedager, this)
        }
    }

    @Test
    internal fun `tidslinjer som slutter med ignorerte dager`() {
        perioder(2.sykedager, 2.feriedager) { periode1, _ ->
            assertFørsteDagErUtgangspunktForBeregning(periode1, this)
        }
        perioder(2.sykedager, 2.permisjonsdager) { periode1, _ ->
            assertFørsteDagErUtgangspunktForBeregning(periode1, this)
        }
    }

    @Test
    internal fun `ferie i framtiden`() {
        perioder(2.sykedager, 2.implisittDager, 2.feriedager) { sykedager, _, _ ->
            assertFørsteDagErUtgangspunktForBeregning(sykedager, this)
        }
    }

    @Test
    internal fun `sykedager etterfulgt av arbeidsdager`() {
        perioder(2.sykedager, 2.arbeidsdager) { sykedager, _ ->
            assertFørsteDagErUtgangspunktForBeregning(sykedager, this)
        }
    }

    @Test
    internal fun `sykedager etterfulgt av implisittdager`() {
        perioder(2.sykedager, 2.implisittDager) { sykedager, _ ->
            assertFørsteDagErUtgangspunktForBeregning(sykedager, this)
        }
    }

    @Test
    internal fun `søknad med arbeidgjenopptatt gir ikke feil`() {
        perioder(2.sykedager, 2.implisittDager) { sykedager, _ ->
            assertFørsteDagErUtgangspunktForBeregning(sykedager, this)
        }
    }

    @Test
    internal fun `sykeperiode starter på første fraværsdag`() {
        val sykmelding = sykmelding(Triple(4.februar(2020), 21.februar(2020), 100))
        val søknad = søknad(Søknad.Periode.Sykdom(4.februar(2020), 21.februar(2020), 100))
        val inntektsmelding = inntektsmelding(listOf(
            Periode(20.januar(2020), 20.januar(2020)),
            Periode(4.februar(2020), 18.februar(2020))
        ), førsteFraværsdag = 4.februar(2020))
        assertFørsteFraværsdag(sykmelding, søknad, inntektsmelding, 21.februar(2020))
    }

    @Test
    internal fun `sykeperioden starter etter første fraværsdag`() {
        val sykmelding = sykmelding(Triple(29.januar(2020), 16.februar(2020), 100))
        val søknad = søknad(Søknad.Periode.Sykdom(29.januar(2020), 16.februar(2020), 100))
        val inntektsmelding = inntektsmelding(listOf(
            Periode(13.januar(2020), 17.januar(2020)),
            Periode(20.januar(2020), 30.januar(2020))
        ), førsteFraværsdag = 20.januar(2020))
        assertFørsteFraværsdag(sykmelding, søknad, inntektsmelding, 16.februar(2020))
    }

    @Test
    internal fun `arbeidsgiverperiode med enkeltdager før første fraværsdag`() {
        val sykmelding = sykmelding(Triple(12.februar(2020), 19.februar(2020), 100))
        val søknad = søknad(Søknad.Periode.Sykdom(12.februar(2020), 19.februar(2020), 100))
        val inntektsmelding = inntektsmelding(listOf(
            Periode(14.januar(2020), 14.januar(2020)),
            Periode(28.januar(2020), 28.januar(2020)),
            Periode(3.februar(2020), 16.februar(2020))
        ), førsteFraværsdag = 3.februar(2020))
        assertFørsteFraværsdag(sykmelding, søknad, inntektsmelding, 19.februar(2020))
    }

    private fun assertFørsteFraværsdag(sykmelding: Sykmelding, søknad: Søknad, inntektsmelding: Inntektsmelding, tom: LocalDate) {
        val tidslinje = listOf(
            sykmelding.sykdomstidslinje(),
            søknad.sykdomstidslinje(),
            inntektsmelding.sykdomstidslinje()
        ).merge(historiskDagturnering)

        val førsteFraværsdag = tidslinje.førsteFraværsdag()
        assertEquals(inntektsmelding.førsteFraværsdag, førsteFraværsdag) {
            "Forventet første fraværsdag ${inntektsmelding.førsteFraværsdag}. " +
                "Fikk $førsteFraværsdag\n" +
                "Tidslinje:\n$tidslinje"
        }
    }

    private fun sykmelding(vararg sykeperioder: Triple<LocalDate, LocalDate, Int>): Sykmelding {
        return Sykmelding(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = AKTØRID,
            orgnummer = ORGNUMMER,
            sykeperioder = listOf(*sykeperioder)
        )
    }

    private fun søknad(vararg perioder: Søknad.Periode): Søknad {
        return Søknad(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = AKTØRID,
            orgnummer = ORGNUMMER,
            perioder = listOf(*perioder),
            harAndreInntektskilder = false
        )
    }

    private fun inntektsmelding(
        arbeidsgiverperioder: List<Periode>,
        ferieperioder: List<Periode> = emptyList(),
        refusjonBeløp: Double = INNTEKT,
        beregnetInntekt: Double = INNTEKT,
        førsteFraværsdag: LocalDate = 1.januar,
        refusjonOpphørsdato: LocalDate = 31.desember,  // Employer paid
        endringerIRefusjon: List<LocalDate> = emptyList()
    ): Inntektsmelding {
        return Inntektsmelding(
            meldingsreferanseId = UUID.randomUUID(),
            refusjon = Inntektsmelding.Refusjon(refusjonOpphørsdato, refusjonBeløp, endringerIRefusjon),
            orgnummer = ORGNUMMER,
            fødselsnummer = UNG_PERSON_FNR_2018,
            aktørId = AKTØRID,
            førsteFraværsdag = førsteFraværsdag,
            beregnetInntekt = beregnetInntekt,
            arbeidsgiverperioder = arbeidsgiverperioder,
            ferieperioder = ferieperioder
        )
    }

    private companion object {
        private const val UNG_PERSON_FNR_2018 = "12020052345"
        private const val AKTØRID = "42"
        private const val ORGNUMMER = "987654321"
        private const val INNTEKT = 31000.00

        private val sykedag = Sykedag.Søknad(LocalDate.now(), 100.0)
        private val egenmeldingsdag = Egenmeldingsdag.Søknad(LocalDate.now())
        private val sykHelgedag = SykHelgedag.Søknad(LocalDate.now(), 100.0)

        private fun assertDagenErUtgangspunktForBeregning(dagen: Dag) {
            assertDagenErUtgangspunktForBeregning(dagen.dagen, dagen)
        }

        private fun assertDagenErUtgangspunktForBeregning(
            dagen: LocalDate,
            sykdomstidslinje: ConcreteSykdomstidslinje
        ) {
            val førsteFraværsdag = sykdomstidslinje.førsteFraværsdag()
            assertEquals(dagen, førsteFraværsdag) { "Forventet $dagen, men fikk $førsteFraværsdag.\nTidslinjen:\n$sykdomstidslinje"}
        }

        private fun assertFørsteDagErUtgangspunktForBeregning(sykdomstidslinje: ConcreteSykdomstidslinje) {
            assertEquals(sykdomstidslinje.førsteDag(), sykdomstidslinje.førsteFraværsdag())
        }

        private fun assertFørsteDagErUtgangspunktForBeregning(
            perioden: ConcreteSykdomstidslinje,
            sykdomstidslinje: ConcreteSykdomstidslinje
        ) {
            assertDagenErUtgangspunktForBeregning(perioden.førsteDag(), sykdomstidslinje)
        }

        private fun assertUgyldigTilstand(sykdomstidslinje: ConcreteSykdomstidslinje) {
            assertThrows<IllegalStateException> {
                sykdomstidslinje.førsteFraværsdag()
            }
        }
    }
}
