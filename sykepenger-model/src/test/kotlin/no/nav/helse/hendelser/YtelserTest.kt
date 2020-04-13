package no.nav.helse.hendelser

import no.nav.helse.juli
import no.nav.helse.juni
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.UtbetalingsdagVisitor
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class YtelserTest {

    @Test
    fun `siste utbetalte dag fra tom sykepengehistorikk`() {
        val ytelser = ytelser()
        assertNull(ytelser.utbetalingshistorikk().sisteUtbetalteDag())
    }

    @Test
    fun `startdato fra sykepengehistorikk med én periode`() {
        val ytelser = ytelser(
            utbetalinger = listOf(
                Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(1.juni, 2.juni, 1000)
            )
        )

        assertEquals(2.juni, ytelser.utbetalingshistorikk().sisteUtbetalteDag())
    }

    @Test
    fun `siste utbetalte dag fra sykepengehistorikk med flere periode`() {
        val ytelser = ytelser(
            utbetalinger = listOf(
                Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(1.juni, 2.juni, 1000),
                Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(1.juni, 2.juli, 1200)
            )
        )

        assertEquals(2.juli, ytelser.utbetalingshistorikk().sisteUtbetalteDag())
    }

    @Test
    fun `tidslinje fra sykepengehistorikk med overlappende perioder`() {
        val sykepengehistorikkHendelse = ytelser(
            utbetalinger = listOf(
                Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(1.juni, 2.juni, 1000),
                Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(1.juni, 3.juni, 1200)
            )
        )

        assertEquals(3.juni, sykepengehistorikkHendelse.utbetalingshistorikk().sisteUtbetalteDag())
    }

    @Test
    fun `sykepengehistorikk blir ignorert dersom gap er nøyaktig 26 uker`() {
        val ytelser = ytelser(
            utbetalinger = listOf(
                Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(1.august(2019), 31.august(2019), 1000)
            )
        )

        assertNull(ytelser.utbetalingshistorikk().sisteUtbetalteDag(1.mars(2020)))
    }

    @Test
    fun `sykepengehistorikk blir ikke ignorert dersom gap er 26 uker-1 dag`() {
        val ytelser = ytelser(
            utbetalinger = listOf(
                Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(1.august(2019), 1.september(2019), 1000)
            )
        )

        assertEquals(1.september(2019), ytelser.utbetalingshistorikk().sisteUtbetalteDag(1.mars(2020)))
    }

    @Test
    fun `sykepengehistorikk før gap større enn 26 uker blir ignorert`() {
        val historikkSenereEnn26UkerGap =
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(30.oktober(2019), 1.desember(2019), 1000)
        val historikkTidligereEnn26UkerGap =
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(1.april(2019), 30.april(2019), 1000)
        val ytelser = ytelser(
            utbetalinger = listOf(
                historikkSenereEnn26UkerGap,
                historikkTidligereEnn26UkerGap
            )
        )

        assertFalse(ytelser.valider(2.mai(2020)).hasErrors())
        assertEquals(historikkSenereEnn26UkerGap.tom, ytelser.utbetalingshistorikk().sisteUtbetalteDag(2.mai(2020)))
        val inspektør =
            Inspektør().apply { ytelser.utbetalingshistorikk().utbetalingstidslinje(2.mai(2020)).accept(this) }
        assertEquals(historikkSenereEnn26UkerGap.tom, inspektør.sisteDag)
        assertEquals(historikkSenereEnn26UkerGap.fom, inspektør.førsteDag)
    }

    @Test
    fun `ukjente perioder tidligere enn gap på 26 uker blir ignorert`() {
        val historikkSenereEnn26UkerGap =
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(30.oktober(2019), 1.desember(2019), 1000)
        val historikkTidligereEnn26UkerGap = Utbetalingshistorikk.Periode.Ukjent(1.april(2019), 30.april(2019), 1000)
        val ytelser = ytelser(
            utbetalinger = listOf(
                historikkSenereEnn26UkerGap,
                historikkTidligereEnn26UkerGap
            )
        )

        assertFalse(ytelser.valider(2.mai(2020)).hasErrors())
        assertEquals(historikkSenereEnn26UkerGap.tom, ytelser.utbetalingshistorikk().sisteUtbetalteDag(2.mai(2020)))
        val inspektør =
            Inspektør().apply { ytelser.utbetalingshistorikk().utbetalingstidslinje(2.mai(2020)).accept(this) }
        assertEquals(historikkSenereEnn26UkerGap.tom, inspektør.sisteDag)
        assertEquals(historikkSenereEnn26UkerGap.fom, inspektør.førsteDag)
    }

    @Test
    fun `ukjente perioder er ugyldige`() {
        val refusjonTilArbeidsgiver =
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(30.oktober(2019), 1.desember(2019), 1000)
        val ukjentPeriode = Utbetalingshistorikk.Periode.Ukjent(1.april(2019), 31.mai(2019), 1000)
        val ytelser = ytelser(
            utbetalinger = listOf(
                refusjonTilArbeidsgiver,
                ukjentPeriode
            )
        )

        assertTrue(ytelser.valider(2.mai(2020)).hasErrors())
    }

    @Test
    fun `ukjent peride fullstendig overlappet av refusjonTilAG skal gi error`() {
        val refusjonTilArbeidsgiver =
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(30.oktober(2019), 1.desember(2019), 1000)
        val ukjentPeriode = Utbetalingshistorikk.Periode.Ukjent(5.november(2019), 10.november(2019), 1000)
        val ytelser = ytelser(
            utbetalinger = listOf(
                refusjonTilArbeidsgiver,
                ukjentPeriode
            )
        )

        assertTrue(ytelser.valider(15.mai(2020)).hasErrors())
    }

    @Test
    fun `ukjent peride fullstendig overlappet av refusjonTilAG skal gi error - motsatt rekkefølge`() {
        val refusjonTilArbeidsgiver =
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(30.oktober(2019), 1.desember(2019), 1000)
        val ukjentPeriode = Utbetalingshistorikk.Periode.Ukjent(5.november(2019), 10.november(2019), 1000)
        val ytelser = ytelser(
            utbetalinger = listOf(
                ukjentPeriode,
                refusjonTilArbeidsgiver
            )
        )

        assertTrue(ytelser.valider(15.mai(2020)).hasErrors())
    }

    @Test
    fun `ukjent peride overlapper med slutten på refusjonTilAG skal gi error`() {
        val refusjonTilArbeidsgiver =
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(30.oktober(2019), 10.november(2019), 1000)
        val ukjentPeriode = Utbetalingshistorikk.Periode.Ukjent(5.november(2019), 1.desember(2019), 1000)
        val ytelser = ytelser(
            utbetalinger = listOf(
                ukjentPeriode,
                refusjonTilArbeidsgiver
            )
        )

        assertTrue(ytelser.valider(15.mai(2020)).hasErrors())
    }

    @Test
    fun `ukjent peride overlapper med slutten på refusjonTilAG skal gi error - motsatt rekkefølge`() {
        val refusjonTilArbeidsgiver =
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(30.oktober(2019), 10.november(2019), 1000)
        val ukjentPeriode = Utbetalingshistorikk.Periode.Ukjent(5.november(2019), 1.desember(2019), 1000)
        val ytelser = ytelser(
            utbetalinger = listOf(
                refusjonTilArbeidsgiver,
                ukjentPeriode
            )
        )

        assertTrue(ytelser.valider(15.mai(2020)).hasErrors())
    }

    @Test
    fun `ukjent peride overlapper med starten på refusjonTilAG skal gi error`() {
        val refusjonTilArbeidsgiver =
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(5.november(2019), 1.desember(2019), 1000)
        val ukjentPeriode = Utbetalingshistorikk.Periode.Ukjent(30.oktober(2019), 10.november(2019), 1000)
        val ytelser = ytelser(
            utbetalinger = listOf(
                refusjonTilArbeidsgiver,
                ukjentPeriode
            )
        )

        assertTrue(ytelser.valider(15.mai(2020)).hasErrors())
    }

    @Test
    fun `ukjent peride overlapper med starten på refusjonTilAG skal gi error - motsatt rekkefølge`() {
        val refusjonTilArbeidsgiver =
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(5.november(2019), 1.desember(2019), 1000)
        val ukjentPeriode = Utbetalingshistorikk.Periode.Ukjent(30.oktober(2019), 10.november(2019), 1000)
        val ytelser = ytelser(
            utbetalinger = listOf(
                ukjentPeriode,
                refusjonTilArbeidsgiver
            )
        )

        assertTrue(ytelser.valider(15.mai(2020)).hasErrors())
    }

    @Test
    fun `tidligste fom skal brukes i filtrering`() {
        val refusjonTilArbeidsgiver1 =
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(1.november(2019), 30.november(2019), 1000)
        val refusjonTilArbeidsgiver2 =
            Utbetalingshistorikk.Periode.RefusjonTilArbeidsgiver(10.november(2019), 20.november(2019), 1000)
        val ukjentPeriode = Utbetalingshistorikk.Periode.Ukjent(1.mai(2019), 7.mai(2019), 1000)
        val ytelser = ytelser(
            utbetalinger = listOf(
                refusjonTilArbeidsgiver1,
                refusjonTilArbeidsgiver2,
                ukjentPeriode
            )
        )

        assertTrue(ytelser.valider(15.mai(2020)).hasErrors())
    }

    private class Inspektør : UtbetalingsdagVisitor {
        var førsteDag: LocalDate? = null
        var sisteDag: LocalDate? = null

        private fun visitDag(dag: Utbetalingstidslinje.Utbetalingsdag) {
            førsteDag = førsteDag ?: dag.dato
            sisteDag = dag.dato
        }

        override fun visitArbeidsgiverperiodeDag(dag: Utbetalingstidslinje.Utbetalingsdag.ArbeidsgiverperiodeDag) {
            visitDag(dag)
        }

        override fun visitNavDag(dag: Utbetalingstidslinje.Utbetalingsdag.NavDag) {
            visitDag(dag)
        }

        override fun visitNavHelgDag(dag: Utbetalingstidslinje.Utbetalingsdag.NavHelgDag) {
            visitDag(dag)
        }

        override fun visitArbeidsdag(dag: Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag) {
            visitDag(dag)
        }

        override fun visitFridag(dag: Utbetalingstidslinje.Utbetalingsdag.Fridag) {
            visitDag(dag)
        }

        override fun visitAvvistDag(dag: Utbetalingstidslinje.Utbetalingsdag.AvvistDag) {
            visitDag(dag)
        }

        override fun visitForeldetDag(dag: Utbetalingstidslinje.Utbetalingsdag.ForeldetDag) {
            visitDag(dag)
        }

        override fun visitUkjentDag(dag: Utbetalingstidslinje.Utbetalingsdag.UkjentDag) {
            visitDag(dag)
        }
    }

    private fun ytelser(
        utbetalinger: List<Utbetalingshistorikk.Periode> = listOf(),
        foreldrepenger: Periode? = null,
        svangerskapspenger: Periode? = null
    ) = Aktivitetslogg().let {
        Ytelser(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId.toString(),
            utbetalingshistorikk = Utbetalingshistorikk(
                utbetalinger = utbetalinger,
                inntektshistorikk = emptyList(),
                graderingsliste = emptyList(),
                aktivitetslogg = it
            ),
            foreldrepermisjon = Foreldrepermisjon(foreldrepenger, svangerskapspenger, it),
            aktivitetslogg = it
        )
    }

    private companion object {
        private val organisasjonsnummer = "123456789"
        private val aktørId = "987654321"
        private val fødselsnummer = "01010112345"
        private val vedtaksperiodeId = UUID.randomUUID()
    }
}
