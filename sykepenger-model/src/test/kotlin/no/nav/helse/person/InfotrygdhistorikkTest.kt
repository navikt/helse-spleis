package no.nav.helse.person

import no.nav.helse.hendelser.til
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
        historikk.oppdaterHistorikk(historikkelement(tidsstempel = tidsstempel))
        historikk.tøm()
        assertTrue(historikk.oppfriskNødvendig(aktivitetslogg))
        assertTrue(aktivitetslogg.behov().isNotEmpty()) { aktivitetslogg.toString() }
        assertEquals(1, inspektør.elementer())
        assertEquals(tidsstempel, inspektør.opprettet(0))
        assertEquals(LocalDateTime.MIN, inspektør.oppdatert(0))
    }

    @Test
    fun `tømme historikk - med data`() {
        val tidsstempel = LocalDateTime.now()
        historikk.oppdaterHistorikk(historikkelement(
            tidsstempel = tidsstempel,
            perioder = listOf(Infotrygdhistorikk.Friperiode(1.januar til 10.januar))
        ))
        historikk.tøm()
        assertTrue(historikk.oppfriskNødvendig(aktivitetslogg))
        assertTrue(aktivitetslogg.behov().isNotEmpty()) { aktivitetslogg.toString() }
        assertEquals(2, inspektør.elementer())
        assertTrue(tidsstempel < inspektør.opprettet(0))
        assertEquals(LocalDateTime.MIN, inspektør.oppdatert(0))
        assertEquals(tidsstempel, inspektør.opprettet(1))
        assertEquals(tidsstempel, inspektør.oppdatert(1))
    }

    @Test
    fun `må oppfriske gammel historikk`() {
        historikk.oppdaterHistorikk(historikkelement(tidsstempel = LocalDateTime.now().minusHours(24)))
        assertTrue(historikk.oppfriskNødvendig(aktivitetslogg))
        assertTrue(aktivitetslogg.behov().isNotEmpty())
        assertEquals(1, inspektør.elementer())
    }

    @Test
    fun `kan bestemme tidspunkt selv`() {
        val tidsstempel = LocalDateTime.now().minusHours(24)
        historikk.oppdaterHistorikk(historikkelement(tidsstempel = tidsstempel))
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
            Infotrygdhistorikk.Utbetalingsperiode("orgnr", 1.januar til 31.januar, 100.prosent, 25000.månedlig)
        )
        val nå = LocalDateTime.now()
        val gammel = nå.minusHours(24)
        historikk.oppdaterHistorikk(historikkelement(perioder, tidsstempel = gammel))
        historikk.oppdaterHistorikk(historikkelement(perioder, tidsstempel = nå))
        assertFalse(historikk.oppfriskNødvendig(aktivitetslogg))
        assertEquals(1, inspektør.elementer())
        assertEquals(gammel, inspektør.opprettet(0))
        assertEquals(nå, inspektør.oppdatert(0))
    }

    @Test
    fun `tom historikk validerer`() {
        assertTrue(historikk.valider(aktivitetslogg, 1.januar til 31.januar, 1.januar))
        assertFalse(aktivitetslogg.hasErrorsOrWorse())
    }

    private fun historikkelement(
        perioder: List<Infotrygdhistorikk.Infotrygdperiode> = emptyList(),
        inntekter: List<Infotrygdhistorikk.Inntektsopplysning> = emptyList(),
        arbeidskategorikoder: Map<String, LocalDate> = emptyMap(),
        tidsstempel: LocalDateTime = LocalDateTime.now()
    ) =
        Infotrygdhistorikk.Element.opprett(
            tidsstempel = tidsstempel,
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

        override fun preVisitInfotrygdhistorikkElement(id: UUID, tidsstempel: LocalDateTime, oppdatert: LocalDateTime) {
            elementer.add(Triple(id, tidsstempel, oppdatert))
        }
    }
}
