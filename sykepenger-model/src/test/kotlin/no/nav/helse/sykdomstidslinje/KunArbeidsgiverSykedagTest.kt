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

internal class KunArbeidsgiverSykedagTest {

    companion object {
        private const val UNG_PERSON_FNR_2018 = "12020052345"
        private const val AKTØRID = "42"
        private const val ORGNUMMER = "987654321"
    }

    @Test internal fun `omgående innsending`() {
        undersøke(søknad(1.mars)).also {
            assertEquals(28, it.dagerTeller)
            assertEquals(20, it.dagstypeTeller["Sykedag"])
            assertEquals(8, it.dagstypeTeller["SykHelgedag"])
            assertNull(it.dagstypeTeller["KunArbeidsgiverSykedag"])
        }
    }

    @Test internal fun `siste dag innlevering`() {
        undersøke(søknad(30.april)).also {
            assertEquals(28, it.dagerTeller)
            assertEquals(20, it.dagstypeTeller["Sykedag"])
            assertEquals(8, it.dagstypeTeller["SykHelgedag"])
            assertNull(it.dagstypeTeller["KunArbeidsgiverSykedag"])
        }
    }

    @Test internal fun `Noen dager er ugyldige`() {
        undersøke(søknad(1.mai)).also {
            assertEquals(28, it.dagerTeller)
            assertEquals(10, it.dagstypeTeller["Sykedag"])
            assertEquals(10, it.dagstypeTeller["KunArbeidsgiverSykedag"])
            assertEquals(8, it.dagstypeTeller["SykHelgedag"])
        }
    }

    @Test internal fun `Alle dager er ugyldige`() {
        undersøke(søknad(1.juni)).also {
            assertEquals(28, it.dagerTeller)
            assertNull(it.dagstypeTeller["Sykedag"])
            assertEquals(20, it.dagstypeTeller["KunArbeidsgiverSykedag"])
            assertEquals(8, it.dagstypeTeller["SykHelgedag"])
        }
    }

    @Test internal fun `Søknad til arbeidsgiver lager kun KunArbeidsgiverSykedag og SykHelgedag`() {
        undersøke(søknadArbeidsgiver()).also {
            assertEquals(28, it.dagerTeller)
            assertNull(it.dagstypeTeller["Sykedag"])
            assertEquals(20, it.dagstypeTeller["KunArbeidsgiverSykedag"])
            assertEquals(8, it.dagstypeTeller["SykHelgedag"])
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
        internal val dagstypeTeller = mutableMapOf<String, Int>()

        init {
            søknad.sykdomstidslinje().accept(this)
        }

        override fun preVisitSykdomstidslinje(tidslinje: Sykdomstidslinje) {
            dagerTeller = 0
        }

        private fun oppdateringTeller(dag: Dag) {
            dagerTeller += 1
            dag.javaClass.canonicalName.split('.').filterNot { it in listOf("Søknad", "Inntektsmelding", "Sykmelding") }.last().also {
                dagstypeTeller.set(it, dagstypeTeller.getOrDefault(it, 0).plus(1))
            }
        }

        override fun visitArbeidsdag(dag: Arbeidsdag.Søknad) = oppdateringTeller(dag)
        override fun visitArbeidsdag(dag: Arbeidsdag.Inntektsmelding) = oppdateringTeller(dag)

        override fun visitPermisjonsdag(dag: Permisjonsdag.Aareg) = oppdateringTeller(dag)

        override fun visitEgenmeldingsdag(dag: Egenmeldingsdag.Søknad) = oppdateringTeller(dag)
        override fun visitEgenmeldingsdag(dag: Egenmeldingsdag.Inntektsmelding) = oppdateringTeller(dag)

        override fun visitFeriedag(dag: Feriedag.Søknad) = oppdateringTeller(dag)
        override fun visitFeriedag(dag: Feriedag.Inntektsmelding) = oppdateringTeller(dag)

        override fun visitImplisittDag(dag: ImplisittDag) = oppdateringTeller(dag)

        override fun visitKunArbeidsgiverSykedag(dag: KunArbeidsgiverSykedag) = oppdateringTeller(dag)

        override fun visitSykHelgedag(dag: SykHelgedag.Søknad) = oppdateringTeller(dag)

        override fun visitSykedag(dag: Sykedag.Søknad) = oppdateringTeller(dag)
        override fun visitSykedag(dag: Sykedag.Sykmelding) = oppdateringTeller(dag)
    }
}
