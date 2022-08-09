package no.nav.helse.hendelser

import java.time.LocalDateTime
import no.nav.helse.dsl.ArbeidsgiverHendelsefabrikk
import no.nav.helse.februar
import no.nav.helse.januar
import no.nav.helse.juli
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.somFødselsnummer
import no.nav.helse.sykdomstidslinje.Dag.SykHelgedag
import no.nav.helse.sykdomstidslinje.Dag.Sykedag
import no.nav.helse.sykdomstidslinje.Dag.UkjentDag
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class SykmeldingTest {

    private companion object {
        const val UNG_PERSON_FNR_2018 = "12029240045"
        val hendelsefabrikk = ArbeidsgiverHendelsefabrikk(
            aktørId = "12345",
            organisasjonsnummer = "987654321",
            fødselsnummer = UNG_PERSON_FNR_2018.somFødselsnummer(),
            fødselsdato = 12.februar(1992)
        )
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
        assertFalse(sykmelding.valider(Periode(1.januar, 31.januar), MaskinellJurist()).hasErrorsOrWorse())
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
        assertFalse(sykmelding.valider(sykmelding.periode(), MaskinellJurist()).hasErrorsOrWorse())
    }

    @Test
    fun `sykmelding eldre enn 6 måneder får error`() {
        sykmelding(Sykmeldingsperiode(1.januar, 12.januar, 100.prosent), mottatt = 13.juli.atStartOfDay())
        assertTrue(sykmelding.valider(sykmelding.periode(), MaskinellJurist()).hasErrorsOrWorse())
    }

    private fun sykmelding(vararg sykeperioder: Sykmeldingsperiode, mottatt: LocalDateTime? = null) {
        val tidligsteFom = Sykmeldingsperiode.periode(sykeperioder.toList())?.start?.atStartOfDay()
        val sisteTom = Sykmeldingsperiode.periode(sykeperioder.toList())?.endInclusive?.atStartOfDay()
        sykmelding = hendelsefabrikk.lagSykmelding(
            sykeperioder = sykeperioder,
            sykmeldingSkrevet = tidligsteFom ?: LocalDateTime.now(),
            mottatt = mottatt ?: sisteTom ?: LocalDateTime.now()
        )
    }

}
