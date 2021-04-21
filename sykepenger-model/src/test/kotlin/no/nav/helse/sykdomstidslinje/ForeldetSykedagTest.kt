package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.SøknadArbeidsgiver
import no.nav.helse.person.SykdomstidslinjeVisitor
import no.nav.helse.sykdomstidslinje.Dag.*
import no.nav.helse.testhelpers.*
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import no.nav.helse.økonomi.Økonomi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.reflect.KClass

internal class ForeldetSykedagTest {

    companion object {
        private const val UNG_PERSON_FNR_2018 = "12020052345"
        private const val AKTØRID = "42"
        private const val ORGNUMMER = "987654321"
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

    @Test fun `Søknad til arbeidsgiver lager Sykedager og SykHelgedag`() {
        undersøke(søknadArbeidsgiver()).also {
            assertEquals(28, it.dagerTeller)
            assertEquals(20, it.dagstypeTeller[Sykedag::class])
            assertEquals(8, it.dagstypeTeller[SykHelgedag::class])
        }
    }

    private fun søknad(sendtTilNAV: LocalDate): Søknad {
        return Søknad(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = AKTØRID,
            orgnummer = ORGNUMMER,
            perioder = listOf(Sykdom(18.januar, 14.februar, 100.prosent)), // 10 sykedag januar & februar
            andreInntektskilder = emptyList(),
            sendtTilNAV = sendtTilNAV.atStartOfDay(),
            permittert = false,
            merknaderFraSykmelding = emptyList(),
            sykmeldingSkrevet = LocalDateTime.now()
        )
    }

    private fun søknadArbeidsgiver(): SøknadArbeidsgiver {
        return SøknadArbeidsgiver(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = AKTØRID,
            orgnummer = ORGNUMMER,
            perioder = listOf(SøknadArbeidsgiver.Søknadsperiode(18.januar, 14.februar, 100.prosent)), // 10 sykedag januar & februar
            sykmeldingSkrevet = LocalDateTime.now()
        )
    }

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
        override fun visitDag(dag: ProblemDag, dato: LocalDate, kilde: SykdomstidslinjeHendelse.Hendelseskilde, melding: String) = inkrementer(dag::class)


    }
}
