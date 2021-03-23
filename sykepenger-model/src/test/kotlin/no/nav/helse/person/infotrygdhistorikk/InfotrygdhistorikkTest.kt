package no.nav.helse.person.infotrygdhistorikk

import no.nav.helse.hendelser.til
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.InfotrygdhistorikkVisitor
import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class InfotrygdhistorikkTest {

    private lateinit var historikk: Infotrygdhistorikk
    private lateinit var aktivitetslogg: Aktivitetslogg
    private val inspektør get() = InfotrygdhistorikkInspektør(historikk)

    @BeforeEach
    fun setup() {
        historikk = Infotrygdhistorikk()
        aktivitetslogg = Aktivitetslogg()
    }

    @Test
    fun `må oppfriske tom historikk`() {
        assertTrue(historikk.oppfriskNødvendig(aktivitetslogg))
        assertTrue(aktivitetslogg.behov().isNotEmpty()) { aktivitetslogg.toString() }
        assertEquals(0, inspektør.elementer())
    }

    @Test
    fun `tømme historikk - ingen data`() {
        historikk.tøm()
        assertTrue(historikk.oppfriskNødvendig(aktivitetslogg))
        assertTrue(aktivitetslogg.behov().isNotEmpty()) { aktivitetslogg.toString() }
        assertEquals(0, inspektør.elementer())
    }

    @Test
    fun `tømme historikk - med tom data`() {
        val tidsstempel = LocalDateTime.now()
        historikk.oppdaterHistorikk(historikkelement(oppdatert = tidsstempel))
        historikk.tøm()
        assertTrue(historikk.oppfriskNødvendig(aktivitetslogg))
        assertTrue(aktivitetslogg.behov().isNotEmpty()) { aktivitetslogg.toString() }
        assertEquals(1, inspektør.elementer())
        assertTrue(tidsstempel < inspektør.opprettet(0))
        assertEquals(LocalDateTime.MIN, inspektør.oppdatert(0))
    }

    @Test
    fun `tømme historikk - med data`() {
        val tidsstempel = LocalDateTime.now()
        historikk.oppdaterHistorikk(historikkelement(
            oppdatert = tidsstempel,
            perioder = listOf(Friperiode(1.januar til 10.januar))
        ))
        historikk.tøm()
        assertTrue(historikk.oppfriskNødvendig(aktivitetslogg))
        assertTrue(aktivitetslogg.behov().isNotEmpty()) { aktivitetslogg.toString() }
        assertEquals(2, inspektør.elementer())
        assertTrue(tidsstempel < inspektør.opprettet(0))
        assertEquals(LocalDateTime.MIN, inspektør.oppdatert(0))
        assertTrue(tidsstempel < inspektør.opprettet(1))
        assertEquals(tidsstempel, inspektør.oppdatert(1))
    }

    @Test
    fun `må oppfriske gammel historikk`() {
        historikk.oppdaterHistorikk(historikkelement(oppdatert = LocalDateTime.now().minusHours(24)))
        assertTrue(historikk.oppfriskNødvendig(aktivitetslogg))
        assertTrue(aktivitetslogg.behov().isNotEmpty())
        assertEquals(1, inspektør.elementer())
    }

    @Test
    fun `kan bestemme tidspunkt selv`() {
        val tidsstempel = LocalDateTime.now().minusHours(24)
        historikk.oppdaterHistorikk(historikkelement(oppdatert = tidsstempel))
        assertFalse(historikk.oppfriskNødvendig(aktivitetslogg, tidsstempel.minusMinutes(1)))
        assertFalse(aktivitetslogg.behov().isNotEmpty())
        assertEquals(1, inspektør.elementer())
    }

    @Test
    fun `oppfrisker ikke ny historikk`() {
        historikk.oppdaterHistorikk(historikkelement())
        assertFalse(historikk.oppfriskNødvendig(aktivitetslogg))
        assertFalse(aktivitetslogg.behov().isNotEmpty())
    }

    @Test
    fun `oppdaterer tidspunkt når ny historikk er lik gammel`() {
        val perioder = listOf(
            Utbetalingsperiode("orgnr", 1.januar til 31.januar, 100.prosent, 25000.månedlig)
        )
        val nå = LocalDateTime.now()
        val gammel = nå.minusHours(24)
        historikk.oppdaterHistorikk(historikkelement(perioder, oppdatert = gammel))
        historikk.oppdaterHistorikk(historikkelement(perioder, oppdatert = nå))
        assertFalse(historikk.oppfriskNødvendig(aktivitetslogg))
        assertEquals(1, inspektør.elementer())
        assertTrue(nå < inspektør.opprettet(0))
        assertEquals(nå, inspektør.oppdatert(0))
    }

    @Test
    fun `tom historikk validerer`() {
        assertTrue(historikk.valider(aktivitetslogg, 1.januar til 31.januar, 1.januar))
        assertFalse(aktivitetslogg.hasErrorsOrWorse())
    }

    @Test
    fun `overlapper ikke med tom historikk`() {
        assertFalse(historikk.overlapperMed(aktivitetslogg, 1.januar til 31.januar))
        assertFalse(aktivitetslogg.hasErrorsOrWorse())
    }

    @Test
    fun `overlappende utbetalinger`() {
        historikk.oppdaterHistorikk(historikkelement(listOf(
            Utbetalingsperiode("orgnr", 5.januar til 10.januar, 100.prosent, 25000.månedlig)
        )))
        aktivitetslogg.barn().also {
            assertTrue(historikk.overlapperMed(it, 10.januar til 31.januar))
            assertTrue(it.hasErrorsOrWorse())
        }
        aktivitetslogg.barn().also {
            assertTrue(historikk.overlapperMed(it, 1.januar til 5.januar))
            assertTrue(it.hasErrorsOrWorse())
        }
        aktivitetslogg.barn().also {
            assertFalse(historikk.overlapperMed(it, 1.januar til 4.januar))
            assertFalse(it.hasErrorsOrWorse())
        }
        aktivitetslogg.barn().also {
            assertFalse(historikk.overlapperMed(it, 11.januar til 31.januar))
            assertFalse(it.hasErrorsOrWorse())
        }
    }

    @Test
    fun `overlapper ikke med ferie eller ukjent`() {
        historikk.oppdaterHistorikk(historikkelement(listOf(
            Friperiode(5.januar til 10.januar),
            Ukjent(15.januar til 20.januar)
        )))
        assertFalse(historikk.overlapperMed(aktivitetslogg, 1.januar til 31.januar))
    }

    private fun historikkelement(
        perioder: List<Infotrygdperiode> = emptyList(),
        inntekter: List<Inntektsopplysning> = emptyList(),
        arbeidskategorikoder: Map<String, LocalDate> = emptyMap(),
        hendelseId: UUID = UUID.randomUUID(),
        oppdatert: LocalDateTime = LocalDateTime.now()
    ) =
        Infotrygdhistorikk.Element.opprett(
            oppdatert = oppdatert,
            hendelseId = hendelseId,
            perioder = perioder,
            inntekter = inntekter,
            arbeidskategorikoder = arbeidskategorikoder
        )

    private class InfotrygdhistorikkInspektør(historikk: Infotrygdhistorikk) : InfotrygdhistorikkVisitor {
        private var elementer = mutableListOf<Triple<UUID, LocalDateTime, LocalDateTime>>()

        init {
            historikk.accept(this)
        }

        fun elementer() = elementer.size
        fun opprettet(indeks: Int) = elementer.elementAt(indeks).second
        fun oppdatert(indeks: Int) = elementer.elementAt(indeks).third

        override fun preVisitInfotrygdhistorikkElement(id: UUID, tidsstempel: LocalDateTime, oppdatert: LocalDateTime, hendelseId: UUID?) {
            elementer.add(Triple(id, tidsstempel, oppdatert))
        }
    }
}
