package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.SøknadArbeidsgiver
import no.nav.helse.person.SykdomstidslinjeVisitor
import no.nav.helse.sykdomstidslinje.dag.*
import no.nav.helse.testhelpers.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*
import kotlin.reflect.KClass

internal class ForeldetSykedagTest {

    companion object {
        private const val UNG_PERSON_FNR_2018 = "12020052345"
        private const val AKTØRID = "42"
        private const val ORGNUMMER = "987654321"
    }

    @Test internal fun `omgående innsending`() {
        undersøke(søknad(1.mars)).also {
            assertEquals(28, it.dagerTeller)
            assertEquals(20, it.dagstypeTeller[Sykedag::class])
            assertEquals(8, it.dagstypeTeller[SykHelgedag::class])
            assertNull(it.dagstypeTeller[ForeldetSykedag::class])
        }
    }

    @Test internal fun `siste dag innlevering`() {
        undersøke(søknad(30.april)).also {
            assertEquals(28, it.dagerTeller)
            assertEquals(20, it.dagstypeTeller[Sykedag::class])
            assertEquals(8, it.dagstypeTeller[SykHelgedag::class])
            assertNull(it.dagstypeTeller[ForeldetSykedag::class])
        }
    }

    @Test internal fun `Noen dager er ugyldige`() {
        undersøke(søknad(1.mai)).also {
            assertEquals(28, it.dagerTeller)
            assertEquals(10, it.dagstypeTeller[Sykedag::class])
            assertEquals(10, it.dagstypeTeller[ForeldetSykedag::class])
            assertEquals(8, it.dagstypeTeller[SykHelgedag::class])
        }
    }

    @Test internal fun `Alle dager er ugyldige`() {
        undersøke(søknad(1.juni)).also {
            assertEquals(28, it.dagerTeller)
            assertNull(it.dagstypeTeller[Sykedag::class])
            assertEquals(20, it.dagstypeTeller[ForeldetSykedag::class])
            assertEquals(8, it.dagstypeTeller[SykHelgedag::class])
        }
    }

    @Test internal fun `Søknad til arbeidsgiver lager Sykedager og SykHelgedag`() {
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

    private class TestInspektør(søknad: SykdomstidslinjeHendelse) : SykdomstidslinjeVisitor {
        internal var dagerTeller = 0
        internal val dagstypeTeller = mutableMapOf<KClass<out Dag>, Int>()

        init {
            søknad.sykdomstidslinje().accept(this)
        }

        override fun preVisitSykdomstidslinje(tidslinje: Sykdomstidslinje) {
            dagerTeller = 0
        }

        private fun oppdateringTeller(klasse: KClass<out Dag>) {
            dagerTeller += 1
            dagstypeTeller.compute(klasse) { _, value ->
                1 + (value ?: 0)
            }
        }

        override fun visitArbeidsdag(dag: Arbeidsdag.Søknad) = oppdateringTeller(Arbeidsdag::class)
        override fun visitArbeidsdag(dag: Arbeidsdag.Inntektsmelding) = oppdateringTeller(Arbeidsdag::class)

        override fun visitPermisjonsdag(dag: Permisjonsdag.Aareg) = oppdateringTeller(Permisjonsdag::class)

        override fun visitEgenmeldingsdag(dag: Egenmeldingsdag.Søknad) = oppdateringTeller(Egenmeldingsdag::class)
        override fun visitEgenmeldingsdag(dag: Egenmeldingsdag.Inntektsmelding) = oppdateringTeller(Egenmeldingsdag::class)

        override fun visitFeriedag(dag: Feriedag.Søknad) = oppdateringTeller(Feriedag::class)
        override fun visitFeriedag(dag: Feriedag.Inntektsmelding) = oppdateringTeller(Feriedag::class)

        override fun visitImplisittDag(dag: ImplisittDag) = oppdateringTeller(ImplisittDag::class)

        override fun visitForeldetSykedag(dag: ForeldetSykedag) = oppdateringTeller(ForeldetSykedag::class)

        override fun visitSykHelgedag(dag: SykHelgedag.Søknad) = oppdateringTeller(SykHelgedag::class)

        override fun visitSykedag(dag: Sykedag.Søknad) = oppdateringTeller(Sykedag::class)
        override fun visitSykedag(dag: Sykedag.Sykmelding) = oppdateringTeller(Sykedag::class)
    }
}
