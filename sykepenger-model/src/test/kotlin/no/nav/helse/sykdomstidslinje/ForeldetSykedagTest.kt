package no.nav.helse.sykdomstidslinje

import java.time.LocalDate
import no.nav.helse.april
import no.nav.helse.dsl.ArbeidsgiverHendelsefabrikk
import no.nav.helse.februar
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.januar
import no.nav.helse.juni
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.person.SykdomstidslinjeVisitor
import no.nav.helse.somPersonidentifikator
import no.nav.helse.sykdomstidslinje.Dag.Arbeidsdag
import no.nav.helse.sykdomstidslinje.Dag.ArbeidsgiverHelgedag
import no.nav.helse.sykdomstidslinje.Dag.Arbeidsgiverdag
import no.nav.helse.sykdomstidslinje.Dag.Feriedag
import no.nav.helse.sykdomstidslinje.Dag.ForeldetSykedag
import no.nav.helse.sykdomstidslinje.Dag.FriskHelgedag
import no.nav.helse.sykdomstidslinje.Dag.ProblemDag
import no.nav.helse.sykdomstidslinje.Dag.SykHelgedag
import no.nav.helse.sykdomstidslinje.Dag.Sykedag
import no.nav.helse.sykdomstidslinje.Dag.UkjentDag
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.Økonomi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

internal class ForeldetSykedagTest {

    companion object {
        private const val UNG_PERSON_FNR_2018 = "12029240045"
        private const val AKTØRID = "42"
        private const val ORGNUMMER = "987654321"
        private val hendelefabrikk = ArbeidsgiverHendelsefabrikk(
            personidentifikator = UNG_PERSON_FNR_2018.somPersonidentifikator(),
            aktørId = AKTØRID,
            organisasjonsnummer = ORGNUMMER,
            fødselsdato = 12.februar(1992)
        )
    }

    @Test fun `omgående innsending`() {
        undersøke(søknad(1.mars)).also {
            assertEquals(28, it.dagerTeller)
            assertEquals(20, it.dagstypeTeller[Sykedag::class])
            assertEquals(8, it.dagstypeTeller[SykHelgedag::class])
            assertNull(it.dagstypeTeller[ForeldetSykedag::class])
        }
    }

    @Test fun `siste dag innlevering`() {
        undersøke(søknad(30.april)).also {
            assertEquals(28, it.dagerTeller)
            assertEquals(20, it.dagstypeTeller[Sykedag::class])
            assertEquals(8, it.dagstypeTeller[SykHelgedag::class])
            assertNull(it.dagstypeTeller[ForeldetSykedag::class])
        }
    }

    @Test fun `Noen dager er ugyldige`() {
        undersøke(søknad(1.mai)).also {
            assertEquals(28, it.dagerTeller)
            assertEquals(10, it.dagstypeTeller[Sykedag::class])
            assertEquals(10, it.dagstypeTeller[ForeldetSykedag::class])
            assertEquals(8, it.dagstypeTeller[SykHelgedag::class])
        }
    }

    @Test fun `Alle dager er ugyldige`() {
        undersøke(søknad(1.juni)).also {
            assertEquals(28, it.dagerTeller)
            assertNull(it.dagstypeTeller[Sykedag::class])
            assertEquals(20, it.dagstypeTeller[ForeldetSykedag::class])
            assertEquals(8, it.dagstypeTeller[SykHelgedag::class])
        }
    }

    private fun søknad(sendtTilNAV: LocalDate) = hendelefabrikk.lagSøknad(
        perioder = arrayOf(Sykdom(18.januar, 14.februar, 100.prosent)), // 10 sykedag januar & februar
        sendtTilNAVEllerArbeidsgiver = sendtTilNAV
    )

    private fun undersøke(søknad: SykdomstidslinjeHendelse): TestInspektør {
        return TestInspektør(søknad)
    }

    private class TestInspektør(søknad: SykdomstidslinjeHendelse) : SykdomstidslinjeVisitor {
        var dagerTeller = 0
        val dagstypeTeller = mutableMapOf<KClass<out Dag>, Int>()

        init {
            søknad.sykdomstidslinje().accept(this)
        }

        override fun preVisitSykdomstidslinje(
            tidslinje: Sykdomstidslinje,
            låstePerioder: List<Periode>
        ) {
            dagerTeller = 0
        }

        private fun inkrementer(klasse: KClass<out Dag>) {
            dagerTeller += 1
            dagstypeTeller.compute(klasse) { _, value ->
                1 + (value ?: 0)
            }
        }

        override fun visitDag(dag: UkjentDag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) = inkrementer(dag::class)
        override fun visitDag(dag: Arbeidsdag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) = inkrementer(dag::class)
        override fun visitDag(
            dag: Arbeidsgiverdag,
            dato: LocalDate,
            økonomi: Økonomi,
            kilde: SykdomstidslinjeHendelse.Hendelseskilde
        ) = inkrementer(dag::class)
        override fun visitDag(dag: Feriedag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) = inkrementer(dag::class)
        override fun visitDag(dag: FriskHelgedag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) = inkrementer(dag::class)
        override fun visitDag(
            dag: ArbeidsgiverHelgedag,
            dato: LocalDate,
            økonomi: Økonomi,
            kilde: SykdomstidslinjeHendelse.Hendelseskilde
        ) = inkrementer(dag::class)
        override fun visitDag(
            dag: Sykedag,
            dato: LocalDate,
            økonomi: Økonomi,
            kilde: SykdomstidslinjeHendelse.Hendelseskilde
        ) = inkrementer(dag::class)
        override fun visitDag(
            dag: ForeldetSykedag,
            dato: LocalDate,
            økonomi: Økonomi,
            kilde: SykdomstidslinjeHendelse.Hendelseskilde
        ) = inkrementer(dag::class)
        override fun visitDag(
            dag: SykHelgedag,
            dato: LocalDate,
            økonomi: Økonomi,
            kilde: SykdomstidslinjeHendelse.Hendelseskilde
        ) = inkrementer(dag::class)
        override fun visitDag(
            dag: ProblemDag,
            dato: LocalDate,
            kilde: SykdomstidslinjeHendelse.Hendelseskilde,
            other: SykdomstidslinjeHendelse.Hendelseskilde?,
            melding: String
        ) = inkrementer(dag::class)


    }
}
