package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.sykdomstidslinje.Dag.*
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.juli
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.util.*

internal class SykmeldingTest {

    private companion object {
        internal const val UNG_PERSON_FNR_2018 = "12020052345"
    }

    private lateinit var sykmelding: Sykmelding

    @Test
    internal fun `sykdomsgrad som er 100% støttes`() {
        sykmelding(Sykmeldingsperiode(1.januar, 10.januar, 100), Sykmeldingsperiode(12.januar, 16.januar, 100))
        assertEquals(8 + 3, sykmelding.sykdomstidslinje().filterIsInstance<Sykedag>().size)
        assertEquals(4, sykmelding.sykdomstidslinje().filterIsInstance<SykHelgedag>().size)
        assertEquals(1, sykmelding.sykdomstidslinje().filterIsInstance<UkjentDag>().size)
    }

    @Test
    internal fun `sykdomsgrad under 100% støttes`() {
        sykmelding(Sykmeldingsperiode(1.januar, 10.januar, 50), Sykmeldingsperiode(12.januar, 16.januar, 100))
        assertFalse(sykmelding.valider(Periode(1.januar, 31.januar)).hasErrors())
    }

    @Test
    internal fun `sykeperioder mangler`() {
        assertThrows<Aktivitetslogg.AktivitetException> { sykmelding() }
    }

    @Test
    internal fun `overlappende sykeperioder`() {
        assertThrows<Aktivitetslogg.AktivitetException> {
            sykmelding(Sykmeldingsperiode(10.januar, 12.januar, 100), Sykmeldingsperiode(1.januar, 12.januar, 100))
        }
    }

    @Test
    internal fun `sykmelding ikke eldre enn 6 måneder får ikke error`() {
        sykmelding(Sykmeldingsperiode(1.januar, 12.januar, 100), mottatt = 1.juli.atStartOfDay())
        assertFalse(sykmelding.valider(sykmelding.periode()).hasErrors())
    }

    @Test
    internal fun `sykmelding eldre enn 6 måneder får error`() {
        sykmelding(Sykmeldingsperiode(1.januar, 12.januar, 100), mottatt = 2.juli.atStartOfDay())
        assertTrue(sykmelding.valider(sykmelding.periode()).hasErrors())
    }

    private fun sykmelding(vararg sykeperioder: Sykmeldingsperiode, mottatt: LocalDateTime? = null) {
        val tidligsteFom = sykeperioder.map { it.fom }.min()?.atStartOfDay()
        sykmelding = Sykmelding(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = "12345",
            orgnummer = "987654321",
            sykeperioder = listOf(*sykeperioder),
            mottatt = mottatt ?: tidligsteFom ?: LocalDateTime.now()
        )
    }

}
