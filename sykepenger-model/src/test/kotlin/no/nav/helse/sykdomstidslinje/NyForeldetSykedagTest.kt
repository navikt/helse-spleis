package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.SøknadArbeidsgiver
import no.nav.helse.person.NySykdomstidslinjeVisitor
import no.nav.helse.sykdomstidslinje.NyDag.*
import no.nav.helse.testhelpers.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.reflect.KClass

internal class NyForeldetSykedagTest {

    companion object {
        private const val UNG_PERSON_FNR_2018 = "12020052345"
        private const val AKTØRID = "42"
        private const val ORGNUMMER = "987654321"
    }

    @Test internal fun `omgående innsending`() {
        undersøke(søknad(1.mars)).also {
            assertEquals(28, it.dagerTeller)
            assertEquals(20, it.dagstypeTeller[NySykedag::class])
            assertEquals(8, it.dagstypeTeller[NySykHelgedag::class])
            assertNull(it.dagstypeTeller[NyForeldetSykedag::class])
        }
    }

    @Test internal fun `siste dag innlevering`() {
        undersøke(søknad(30.april)).also {
            assertEquals(28, it.dagerTeller)
            assertEquals(20, it.dagstypeTeller[NySykedag::class])
            assertEquals(8, it.dagstypeTeller[NySykHelgedag::class])
            assertNull(it.dagstypeTeller[NyForeldetSykedag::class])
        }
    }

    @Test internal fun `Noen dager er ugyldige`() {
        undersøke(søknad(1.mai)).also {
            assertEquals(28, it.dagerTeller)
            assertEquals(10, it.dagstypeTeller[NySykedag::class])
            assertEquals(10, it.dagstypeTeller[NyForeldetSykedag::class])
            assertEquals(8, it.dagstypeTeller[NySykHelgedag::class])
        }
    }

    @Test internal fun `Alle dager er ugyldige`() {
        undersøke(søknad(1.juni)).also {
            assertEquals(28, it.dagerTeller)
            assertNull(it.dagstypeTeller[NySykedag::class])
            assertEquals(20, it.dagstypeTeller[NyForeldetSykedag::class])
            assertEquals(8, it.dagstypeTeller[NySykHelgedag::class])
        }
    }

    @Test internal fun `Søknad til arbeidsgiver lager Sykedager og NySykHelgedag`() {
        undersøke(søknadArbeidsgiver()).also {
            assertEquals(28, it.dagerTeller)
            assertEquals(20, it.dagstypeTeller[NySykedag::class])
            assertEquals(8, it.dagstypeTeller[NySykHelgedag::class])
        }
    }

    private fun søknad(sendtTilNAV: LocalDate): Søknad {
        return Søknad(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = AKTØRID,
            orgnummer = ORGNUMMER,
            perioder = listOf(Sykdom(18.januar, 14.februar, 100)), // 10 sykedag januar & februar
            harAndreInntektskilder = false,
            sendtTilNAV = sendtTilNAV.atStartOfDay(),
            permittert = false
        )
    }

    private fun søknadArbeidsgiver(): SøknadArbeidsgiver {
        return SøknadArbeidsgiver(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = AKTØRID,
            orgnummer = ORGNUMMER,
            perioder = listOf(SøknadArbeidsgiver.Søknadsperiode(18.januar, 14.februar, 100)) // 10 sykedag januar & februar
        )
    }

    private fun undersøke(søknad: SykdomstidslinjeHendelse): TestInspektør {
        return TestInspektør(søknad)
    }

    private class TestInspektør(søknad: SykdomstidslinjeHendelse) : NySykdomstidslinjeVisitor {
        internal var dagerTeller = 0
        internal val dagstypeTeller = mutableMapOf<KClass<out NyDag>, Int>()

        init {
            søknad.nySykdomstidslinje().accept(this)
        }

        override fun preVisitNySykdomstidslinje(tidslinje: NySykdomstidslinje, låstePerioder: List<Periode>, id: UUID, tidsstempel: LocalDateTime) {
            dagerTeller = 0
        }

        private fun inkrementer(klasse: KClass<out NyDag>) {
            dagerTeller += 1
            dagstypeTeller.compute(klasse) { _, value ->
                1 + (value ?: 0)
            }
        }

        override fun visitDag(dag: NyUkjentDag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) = inkrementer(dag::class)
        override fun visitDag(dag: NyArbeidsdag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) = inkrementer(dag::class)
        override fun visitDag(dag: NyArbeidsgiverdag, dato: LocalDate, grad: Grad, kilde: SykdomstidslinjeHendelse.Hendelseskilde) = inkrementer(dag::class)
        override fun visitDag(dag: NyFeriedag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) = inkrementer(dag::class)
        override fun visitDag(dag: NyFriskHelgedag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde) = inkrementer(dag::class)
        override fun visitDag(dag: NyArbeidsgiverHelgedag, dato: LocalDate, grad: Grad, kilde: SykdomstidslinjeHendelse.Hendelseskilde) = inkrementer(dag::class)
        override fun visitDag(dag: NySykedag, dato: LocalDate, grad: Grad, kilde: SykdomstidslinjeHendelse.Hendelseskilde) = inkrementer(dag::class)
        override fun visitDag(dag: NyForeldetSykedag, dato: LocalDate, grad: Grad, kilde: SykdomstidslinjeHendelse.Hendelseskilde) = inkrementer(dag::class)
        override fun visitDag(dag: NySykHelgedag, dato: LocalDate, grad: Grad, kilde: SykdomstidslinjeHendelse.Hendelseskilde) = inkrementer(dag::class)
        override fun visitDag(dag: ProblemDag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde, melding: String) = inkrementer(dag::class)


    }
}
