package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.sykdomstidslinje.Dag.*
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.juli
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.util.*

internal class SykmeldingTest {

    private companion object {
        const val UNG_PERSON_FNR_2018 = "12020052345"
    }

    private lateinit var sykmelding: Sykmelding

    @Test
    fun `sykdomsgrad som er 100 prosent støttes`() {
        sykmelding(Sykmeldingsperiode(1.januar, 10.januar, 100.prosent), Sykmeldingsperiode(12.januar, 16.januar, 100.prosent))
        assertEquals(8 + 3, sykmelding.sykdomstidslinje().filterIsInstance<Sykedag>().size)
        assertEquals(4, sykmelding.sykdomstidslinje().filterIsInstance<SykHelgedag>().size)
        assertEquals(1, sykmelding.sykdomstidslinje().filterIsInstance<UkjentDag>().size)
    }

    @Test
    fun `sykdomsgrad under 100 prosent støttes`() {
        sykmelding(Sykmeldingsperiode(1.januar, 10.januar, 50.prosent), Sykmeldingsperiode(12.januar, 16.januar, 100.prosent))
        assertFalse(sykmelding.valider(Periode(1.januar, 31.januar)).hasErrorsOrWorse())
    }

    @Test
    fun `sykeperioder mangler`() {
        assertThrows<Aktivitetslogg.AktivitetException> { sykmelding() }
    }

    @Test
    fun `overlappende sykeperioder`() {
        assertThrows<Aktivitetslogg.AktivitetException> {
            sykmelding(Sykmeldingsperiode(10.januar, 12.januar, 100.prosent), Sykmeldingsperiode(1.januar, 12.januar, 100.prosent))
        }
    }

    @Test
    fun `sykmelding ikke eldre enn 6 måneder får ikke error`() {
        sykmelding(Sykmeldingsperiode(1.januar, 12.januar, 100.prosent), mottatt = 12.juli.atStartOfDay())
        assertFalse(sykmelding.valider(sykmelding.periode()).hasErrorsOrWorse())
    }

    @Test
    fun `sykmelding eldre enn 6 måneder får error`() {
        sykmelding(Sykmeldingsperiode(1.januar, 12.januar, 100.prosent), mottatt = 13.juli.atStartOfDay())
        assertTrue(sykmelding.valider(sykmelding.periode()).hasErrorsOrWorse())
    }

    private fun sykmelding(vararg sykeperioder: Sykmeldingsperiode, mottatt: LocalDateTime? = null) {
        val tidligsteFom = Sykmeldingsperiode.periode(sykeperioder.toList())?.start?.atStartOfDay()
        val sisteTom = Sykmeldingsperiode.periode(sykeperioder.toList())?.endInclusive?.atStartOfDay()
        sykmelding = Sykmelding(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = "12345",
            orgnummer = "987654321",
            sykeperioder = sykeperioder.toList(),
            sykmeldingSkrevet = tidligsteFom ?: LocalDateTime.now(),
            mottatt = mottatt ?: sisteTom ?: LocalDateTime.now()
        )
    }

}
