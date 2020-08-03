package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Søknad
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.sykdomstidslinje.SykdomshistorikkTest.TestEvent.TestSykmelding
import no.nav.helse.sykdomstidslinje.SykdomshistorikkTest.TestEvent.TestSøknad
import no.nav.helse.testhelpers.TestSykdomstidslinje
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.jobbTil
import no.nav.helse.testhelpers.sykTil
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class SykdomshistorikkTest {
    private var historikk = Sykdomshistorikk()
    private val tidslinje: Sykdomstidslinje get() = historikk.sykdomstidslinje()

    @Test
    fun `Legger på to hendelser`() {

        historikk.håndter(TestSykmelding((1.januar jobbTil 12.januar)))
        historikk.håndter(TestSykmelding((13.januar sykTil 20.januar)))

        assertEquals(10, tidslinje.filterIsInstance<Dag.Arbeidsdag>().size)
        assertEquals(2, tidslinje.filterIsInstance<Dag.FriskHelgedag>().size)
        assertEquals(3, tidslinje.filterIsInstance<Dag.SykHelgedag>().size)
        assertEquals(5, tidslinje.filterIsInstance<Dag.Sykedag>().size)
    }

    @Test
    fun `sykmelding til 12 januar blir sendt til Infotrygd`() {
        historikk.håndter(TestSykmelding((1.januar jobbTil 12.januar)))
        historikk.håndter(TestSykmelding((20.januar sykTil 25.januar)))

        val søknad = TestSøknad((20.januar sykTil 25.januar))
        historikk.håndter(søknad)

        historikk.fjernDager(Periode(1.januar, 12.januar))

        assertEquals(0, tidslinje.filterIsInstance<Dag.Arbeidsdag>().size)
        assertEquals(0, tidslinje.filterIsInstance<Dag.FriskHelgedag>().size)
        assertEquals(2, tidslinje.filterIsInstance<Dag.SykHelgedag>().size)
        assertEquals(4, tidslinje.filterIsInstance<Dag.Sykedag>().size)
        assertEquals(4, historikk.size)

        historikk.fjernDager(Periode(13.januar, 19.januar))

        assertEquals(4, historikk.size)
    }

    @Test
    fun `Nytt element i sykdomshistorikk er tomt`() {
        historikk.håndter(TestSykmelding(1.januar sykTil 15.januar))
        historikk.tøm()
        assertEquals(0, historikk.sykdomstidslinje().length())
    }

    internal sealed class TestEvent(
        private val sykdomstidslinje: TestSykdomstidslinje,
        melding: Melding
    ) : SykdomstidslinjeHendelse(UUID.randomUUID(), melding) {

        private val UNG_PERSON_FNR_2018 = "12020052345"
        private val AKTØRID = "42"
        private val ORGNUMMER = "987654321"

        override fun organisasjonsnummer() = ORGNUMMER
        override fun aktørId() = AKTØRID
        override fun fødselsnummer() = UNG_PERSON_FNR_2018

        override fun sykdomstidslinje(tom: LocalDate) = sykdomstidslinje()
        override fun sykdomstidslinje() = sykdomstidslinje.asSykdomstidslinje(kilde = kilde)
        override fun valider(periode: Periode) = Aktivitetslogg()
        override fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver) {}

        internal class TestSykmelding(tidslinje: TestSykdomstidslinje) : TestEvent(tidslinje, Sykmelding::class)
        internal class TestInntektsmelding(tidslinje: TestSykdomstidslinje) : TestEvent(tidslinje, Inntektsmelding::class)
        internal class TestSøknad(tidslinje: TestSykdomstidslinje) : TestEvent(tidslinje, Søknad::class)
    }
}
