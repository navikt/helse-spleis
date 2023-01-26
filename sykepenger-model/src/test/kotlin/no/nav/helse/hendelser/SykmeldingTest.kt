package no.nav.helse.hendelser

import java.time.LocalDateTime
import no.nav.helse.dsl.ArbeidsgiverHendelsefabrikk
import no.nav.helse.januar
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.somPersonidentifikator
import no.nav.helse.sykdomstidslinje.Dag.SykHelgedag
import no.nav.helse.sykdomstidslinje.Dag.Sykedag
import no.nav.helse.sykdomstidslinje.Dag.UkjentDag
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class SykmeldingTest {

    private companion object {
        const val UNG_PERSON_FNR_2018 = "12029240045"
        val hendelsefabrikk = ArbeidsgiverHendelsefabrikk(
            aktørId = "12345",
            personidentifikator = UNG_PERSON_FNR_2018.somPersonidentifikator(),
            organisasjonsnummer = "987654321"
        )
    }

    private lateinit var sykmelding: Sykmelding

    @Test
    fun `oppdaterer perioder`() {
        sykmelding(Sykmeldingsperiode(10.januar, 15.januar, 100.prosent))

        sykmelding.oppdaterSykmeldingsperioder(emptyList()).also { result ->
            assertEquals(listOf(10.januar til 15.januar), result)
        }

        sykmelding.oppdaterSykmeldingsperioder(
            listOf(1.januar til 2.januar)
        ).also { result ->
            assertEquals(listOf(
                1.januar til 2.januar,
                10.januar til 15.januar
            ), result)
        }

        sykmelding.oppdaterSykmeldingsperioder(
            listOf(17.januar til 20.januar)
        ).also { result ->
            assertEquals(listOf(
                10.januar til 15.januar,
                17.januar til 20.januar
            ), result)
        }

        sykmelding.oppdaterSykmeldingsperioder(listOf(
            1.januar til 2.januar,
            6.januar til 10.januar,
            15.januar til 20.januar,
            23.januar til 25.januar
        )).also { result ->
            assertEquals(listOf(
                1.januar til 2.januar,
                6.januar til 20.januar,
                23.januar til 25.januar
            ), result)
        }
    }
    @Test
    fun `oppdaterer perioder - trimmet dager - en dag igjen`() {
        sykmelding(Sykmeldingsperiode(10.januar, 15.januar, 100.prosent))
        sykmelding.trimLeft(14.januar)
        sykmelding.oppdaterSykmeldingsperioder(emptyList()).also { result ->
            assertEquals(listOf(15.januar til 15.januar), result)
        }
    }
    @Test
    fun `oppdaterer perioder - trimmet forbi`() {
        sykmelding(Sykmeldingsperiode(10.januar, 15.januar, 100.prosent))
        sykmelding.trimLeft(15.januar)
        val perioder = listOf(1.januar til 2.januar)
        sykmelding.oppdaterSykmeldingsperioder(perioder).also { result ->
            assertEquals(perioder, result)
        }
    }

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
        assertFalse(sykmelding.valider(Periode(1.januar, 31.januar), MaskinellJurist()).harFunksjonelleFeilEllerVerre())
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

    private fun sykmelding(vararg sykeperioder: Sykmeldingsperiode, mottatt: LocalDateTime? = null) {
        val tidligsteFom = Sykmeldingsperiode.periode(sykeperioder.toList())?.start?.atStartOfDay()
        val sisteTom = Sykmeldingsperiode.periode(sykeperioder.toList())?.endInclusive?.atStartOfDay()
        sykmelding = hendelsefabrikk.lagSykmelding(
            sykeperioder = sykeperioder,
            sykmeldingSkrevet = tidligsteFom ?: LocalDateTime.now()
        )
    }

}
