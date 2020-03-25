package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Søknad.Periode.Sykdom
import no.nav.helse.person.NySykdomstidslinjeVisitor
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

    private fun søknad(sendtTilNAV: LocalDate): Søknad {
        return Søknad(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = AKTØRID,
            orgnummer = ORGNUMMER,
            perioder = listOf(Sykdom(18.januar, 14.februar, 100)), // 10 sykedag januar & februar
            harAndreInntektskilder = false,
            sendtTilNAV = sendtTilNAV.atStartOfDay()
        )
    }

    private fun undersøke(søknad: Søknad): TestInspektør {
        return TestInspektør(søknad)
    }

    private class TestInspektør(søknad: Søknad): NySykdomstidslinjeVisitor {
        internal var dagerTeller = 0
        internal val dagstypeTeller = mutableMapOf<String, Int>()
        init {
            søknad.nySykdomstidslinje().accept(this)
        }

        override fun preVisitSykdomstidslinje(tidslinje: NySykdomstidslinje) {
            dagerTeller = 0
        }

        private fun oppdateringTeller(dag: Dag) {
            dagerTeller += 1
            dag.javaClass.canonicalName.split('.').filterNot { it == "Søknad" }.last().also {
                dagstypeTeller.set(it, dagstypeTeller.getOrDefault(it, 0).plus(1))
            }
        }

        override fun visitArbeidsdag(arbeidsdag: Arbeidsdag.Søknad) {
            oppdateringTeller(arbeidsdag)
        }

        override fun visitEgenmeldingsdag(egenmeldingsdag: Egenmeldingsdag.Søknad) {
            oppdateringTeller(egenmeldingsdag)
        }

        override fun visitFeriedag(feriedag: Feriedag.Søknad) {
            oppdateringTeller(feriedag)
        }

        override fun visitImplisittDag(implisittDag: ImplisittDag) {
            oppdateringTeller(implisittDag)
        }

        override fun visitKunArbeidsgiverSykedag(sykedag: KunArbeidsgiverSykedag) {
            oppdateringTeller(sykedag)
        }

        override fun visitSykHelgedag(sykHelgedag: SykHelgedag.Søknad) {
            oppdateringTeller(sykHelgedag)
        }

        override fun visitSykedag(sykedag: Sykedag.Søknad) {
            oppdateringTeller(sykedag)
        }

    }
}
