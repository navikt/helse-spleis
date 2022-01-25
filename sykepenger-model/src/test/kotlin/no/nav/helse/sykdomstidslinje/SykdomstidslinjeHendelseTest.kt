package no.nav.helse.sykdomstidslinje

import no.nav.helse.februar
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.testhelpers.A
import no.nav.helse.testhelpers.S
import no.nav.helse.testhelpers.resetSeed
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class SykdomstidslinjeHendelseTest {
    private companion object {
        private val MELDING = UUID.randomUUID()
        private const val FØDSELSNUMMER = "fnr"
        private const val AKTØR = "aktørId"
        private const val ORGNR = "orgnr"
    }

    @BeforeEach
    fun setup() {
        resetSeed()
    }

    @Test
    fun kontekst() {
        assertEquals(mapOf(
            "meldingsreferanseId" to MELDING.toString(),
            "aktørId" to AKTØR,
            "fødselsnummer" to FØDSELSNUMMER,
            "organisasjonsnummer" to ORGNR
        ), Testhendelse().toSpesifikkKontekst().kontekstMap)
    }

    @Test
    fun oppdaterFom() {
        val hendelse = Testhendelse(31.S)
        assertEquals(1.januar til 10.januar, hendelse.oppdaterFom(5.januar til 10.januar))
        hendelse.trimLeft(10.januar)
        assertEquals(11.januar til 31.januar, hendelse.oppdaterFom(15.januar til 31.januar))
        hendelse.trimLeft(31.januar)
        assertEquals(1.februar til 5.februar, hendelse.oppdaterFom(1.februar til 5.februar))
    }

    @Test
    fun `oppdaterer ikke fom til arbeidsdager`() {
        val hendelse = Testhendelse(15.A + 16.S)
        assertEquals(1.januar til 10.januar, hendelse.oppdaterFom(5.januar til 10.januar))
        hendelse.trimLeft(10.januar)
        assertEquals(15.januar til 31.januar, hendelse.oppdaterFom(15.januar til 31.januar))
        hendelse.trimLeft(31.januar)
        assertEquals(1.februar til 5.februar, hendelse.oppdaterFom(1.februar til 5.februar))
    }

    private class Testhendelse(private val sykdomstidslinje: Sykdomstidslinje = Sykdomstidslinje()) : SykdomstidslinjeHendelse(MELDING, LocalDateTime.now()) {
        override fun aktørId() = AKTØR
        override fun fødselsnummer() = FØDSELSNUMMER
        override fun organisasjonsnummer() = ORGNR
        override fun sykdomstidslinje(): Sykdomstidslinje = sykdomstidslinje
        override fun valider(periode: Periode): Aktivitetslogg = throw RuntimeException("Brukes ikke i testene")
        override fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver) = throw RuntimeException("Brukes ikke i testene")
    }
}
