package no.nav.helse.person.infotrygdhistorikk

import no.nav.helse.hendelser.til
import no.nav.helse.person.*
import no.nav.helse.serde.PersonData
import no.nav.helse.serde.PersonData.InfotrygdhistorikkElementData.Companion.tilModellObjekt
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.testhelpers.*
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class InfotrygdhistorikkTest {
    private companion object {
        private val tidligsteDato = 1.januar
    }
    private lateinit var historikk: Infotrygdhistorikk
    private lateinit var aktivitetslogg: Aktivitetslogg
    private val inspektør get() = InfotrygdhistorikkInspektør(historikk)

    @BeforeEach
    fun setup() {
        resetSeed(1.januar)
        historikk = Infotrygdhistorikk()
        aktivitetslogg = Aktivitetslogg()
    }

    @Test
    fun `må oppfriske tom historikk`() {
        assertTrue(historikk.oppfriskNødvendig(aktivitetslogg, tidligsteDato))
        assertTrue(aktivitetslogg.behov().isNotEmpty()) { aktivitetslogg.toString() }
        assertEquals(0, inspektør.elementer())
    }

    @Test
    fun `kan justere perioden for oppfrisk`() {
        aktivitetslogg.barn().also {
            historikk.oppfriskNødvendig(it, tidligsteDato)
            assertEquals("${tidligsteDato.minusYears(4)}", it.behov().first().detaljer()["historikkFom"])
            assertEquals("${LocalDate.now()}", it.behov().first().detaljer()["historikkTom"])
        }
        aktivitetslogg.barn().also {
            historikk.oppfriskNødvendig(it, 1.februar)
            assertEquals("${1.februar.minusYears(4)}", it.behov().first().detaljer()["historikkFom"])
            assertEquals("${LocalDate.now()}", it.behov().first().detaljer()["historikkTom"])
        }
    }

    @Test
    fun `tømme historikk - ingen data`() {
        historikk.tøm()
        assertTrue(historikk.oppfriskNødvendig(aktivitetslogg, tidligsteDato))
        assertTrue(aktivitetslogg.behov().isNotEmpty()) { aktivitetslogg.toString() }
        assertEquals(0, inspektør.elementer())
    }

    @Test
    fun `tømme historikk - med tom data`() {
        val tidsstempel = LocalDateTime.now()
        historikk.oppdaterHistorikk(historikkelement(oppdatert = tidsstempel))
        historikk.tøm()
        assertTrue(historikk.oppfriskNødvendig(aktivitetslogg, tidligsteDato))
        assertTrue(aktivitetslogg.behov().isNotEmpty()) { aktivitetslogg.toString() }
        assertEquals(0, inspektør.elementer())
    }

    @Test
    fun `tømme historikk - med ulagret data`() {
        val tidsstempel = LocalDateTime.now()
        historikk.oppdaterHistorikk(historikkelement(
            oppdatert = tidsstempel,
            perioder = listOf(Friperiode(1.januar til 10.januar))
        ))
        historikk.tøm()
        assertTrue(historikk.oppfriskNødvendig(aktivitetslogg, tidligsteDato))
        assertTrue(aktivitetslogg.behov().isNotEmpty()) { aktivitetslogg.toString() }
        assertEquals(0, inspektør.elementer())
    }

    @Test
    fun `tømme historikk - etter lagring av tom inntektliste`() {
        val tidsstempel = LocalDateTime.now()
        historikk.oppdaterHistorikk(historikkelement(
            oppdatert = tidsstempel,
            inntekter = emptyList()
        ))
        historikk.addInntekter(Person("", ""), aktivitetslogg)
        historikk.tøm()
        assertTrue(historikk.oppfriskNødvendig(aktivitetslogg, tidligsteDato))
        assertTrue(aktivitetslogg.behov().isNotEmpty()) { aktivitetslogg.toString() }
        assertEquals(0, inspektør.elementer())
    }

    @Test
    fun `tømme historikk - med lagret inntekter`() {
        val tidsstempel = LocalDateTime.now()
        historikk.oppdaterHistorikk(historikkelement(
            oppdatert = tidsstempel,
            inntekter = listOf(Inntektsopplysning("orgnr", 1.januar, 1000.daglig, true))
        ))
        historikk.addInntekter(Person("", ""), aktivitetslogg)
        historikk.tøm()
        assertTrue(historikk.oppfriskNødvendig(aktivitetslogg, tidligsteDato))
        assertTrue(aktivitetslogg.behov().isNotEmpty()) { aktivitetslogg.toString() }
        assertEquals(2, inspektør.elementer())
        assertEquals(LocalDateTime.MIN, inspektør.oppdatert(0))
        assertEquals(tidsstempel, inspektør.oppdatert(1))
    }

    @Test
    fun `tømme historikk - med lagret vilkårsgrunnlag`() {
        val tidsstempel = LocalDateTime.now()
        historikk.oppdaterHistorikk(historikkelement(
            oppdatert = tidsstempel,
            perioder = listOf(Friperiode(1.januar til 10.januar))
        ))
        historikk.lagreVilkårsgrunnlag(1.januar, Periodetype.OVERGANG_FRA_IT, VilkårsgrunnlagHistorikk())
        historikk.tøm()
        assertTrue(historikk.oppfriskNødvendig(aktivitetslogg, tidligsteDato))
        assertTrue(aktivitetslogg.behov().isNotEmpty()) { aktivitetslogg.toString() }
        assertEquals(2, inspektør.elementer())
        assertTrue(tidsstempel < inspektør.opprettet(0))
        assertEquals(LocalDateTime.MIN, inspektør.oppdatert(0))
        assertTrue(tidsstempel < inspektør.opprettet(1))
        assertEquals(tidsstempel, inspektør.oppdatert(1))
    }

    @Test
    fun `tømme historikk - med og ulagret lagret data`() {
        val tidsstempel1 = LocalDateTime.now().minusDays(1)
        val tidsstempel2 = LocalDateTime.now()
        historikk.oppdaterHistorikk(historikkelement(
            oppdatert = tidsstempel1,
            perioder = listOf(Friperiode(1.januar til 10.januar))
        ))
        historikk.lagreVilkårsgrunnlag(1.januar, Periodetype.OVERGANG_FRA_IT, VilkårsgrunnlagHistorikk())
        historikk.oppdaterHistorikk(historikkelement(
            oppdatert = tidsstempel2,
            perioder = listOf(Utbetalingsperiode("orgnr", 1.januar til 10.januar, 100.prosent, 1000.daglig))
        ))
        historikk.tøm()
        assertTrue(historikk.oppfriskNødvendig(aktivitetslogg, tidligsteDato))
        assertTrue(aktivitetslogg.behov().isNotEmpty()) { aktivitetslogg.toString() }
        assertEquals(2, inspektør.elementer())
        assertTrue(tidsstempel2 < inspektør.opprettet(0))
        assertEquals(LocalDateTime.MIN, inspektør.oppdatert(0))
        assertTrue(tidsstempel2 < inspektør.opprettet(1))
        assertEquals(tidsstempel1, inspektør.oppdatert(1))
    }

    @Test
    fun `må oppfriske gammel historikk`() {
        historikk.oppdaterHistorikk(historikkelement(oppdatert = LocalDateTime.now().minusHours(24)))
        assertTrue(historikk.oppfriskNødvendig(aktivitetslogg, tidligsteDato))
        assertTrue(aktivitetslogg.behov().isNotEmpty())
        assertEquals(1, inspektør.elementer())
    }

    @Test
    fun `kan bestemme tidspunkt selv`() {
        val tidsstempel = LocalDateTime.now().minusHours(24)
        historikk.oppdaterHistorikk(historikkelement(oppdatert = tidsstempel))
        assertFalse(historikk.oppfriskNødvendig(aktivitetslogg, tidligsteDato, tidsstempel.minusMinutes(1)))
        assertFalse(aktivitetslogg.behov().isNotEmpty())
        assertEquals(1, inspektør.elementer())
    }

    @Test
    fun `oppfrisker ikke ny historikk`() {
        historikk.oppdaterHistorikk(historikkelement())
        assertFalse(historikk.oppfriskNødvendig(aktivitetslogg, tidligsteDato))
        assertFalse(aktivitetslogg.behov().isNotEmpty())
    }

    @Test
    fun `oppdaterer tidspunkt når ny historikk er lik gammel`() {
        val perioder = listOf(
            Utbetalingsperiode("orgnr", 1.januar til 31.januar, 100.prosent, 25000.månedlig)
        )
        val nå = LocalDateTime.now()
        val gammel = nå.minusHours(24)
        assertTrue(historikk.oppdaterHistorikk(historikkelement(perioder, oppdatert = gammel)))
        assertFalse(historikk.oppdaterHistorikk(historikkelement(perioder, oppdatert = nå)))
        assertFalse(historikk.oppfriskNødvendig(aktivitetslogg, tidligsteDato))
        assertEquals(1, inspektør.elementer())
        assertTrue(nå < inspektør.opprettet(0))
        assertEquals(nå, inspektør.oppdatert(0))
    }

    @Test
    fun `tom utbetalingstidslinje`() {
        assertTrue(historikk.utbetalingstidslinje().isEmpty())
    }

    @Test
    fun `utbetalingstidslinje kuttes ikke`() {
        historikk.oppdaterHistorikk(historikkelement(listOf(
            Utbetalingsperiode("orgnr", 1.januar til 31.januar, 100.prosent, 25000.månedlig)
        )))
        historikk.utbetalingstidslinje().also {
            assertEquals(1.januar til 31.januar, it.periode())
        }
    }

    @Test
    fun `rekkefølge respekteres ved deserialisering`() {
        val perioder = listOf(
            Utbetalingsperiode("orgnr", 1.januar til 31.januar, 100.prosent, 25000.månedlig),
            Utbetalingsperiode("orgnr", 1.februar til 28.februar, 100.prosent, 25000.månedlig),
            Friperiode(1.mars til 31.mars)
        )
        val nå = LocalDateTime.now()
        historikk.oppdaterHistorikk(historikkelement(perioder))
        historikk = listOf(PersonData.InfotrygdhistorikkElementData(
            id = UUID.randomUUID(),
            tidsstempel = nå,
            hendelseId = UUID.randomUUID(),
            ferieperioder = listOf(PersonData.InfotrygdhistorikkElementData.FerieperiodeData(1.mars, 31.mars)),
            utbetalingsperioder = listOf(
                PersonData.InfotrygdhistorikkElementData.UtbetalingsperiodeData("orgnr", 1.februar, 28.februar, 100, 25000.0),
                PersonData.InfotrygdhistorikkElementData.UtbetalingsperiodeData("orgnr", 1.januar, 31.januar, 100, 25000.0)
            ),
            ukjenteperioder = emptyList(),
            inntekter = emptyList(),
            arbeidskategorikoder = emptyMap(),
            ugyldigePerioder = emptyList(),
            harStatslønn = false,
            oppdatert = nå,
            lagretInntekter = false,
            lagretVilkårsgrunnlag = false
        )).tilModellObjekt()
        assertEquals(1, inspektør.elementer())
        assertFalse(historikk.oppdaterHistorikk(historikkelement(perioder)))
        assertEquals(1, inspektør.elementer())
    }

    @Test
    fun `tom historikk validerer`() {
        assertTrue(historikk.valider(aktivitetslogg, Periodetype.FØRSTEGANGSBEHANDLING, 1.januar til 31.januar, 1.januar))
        assertFalse(aktivitetslogg.hasErrorsOrWorse())
    }

    @Test
    fun `overlapper ikke med tom historikk`() {
        assertTrue(historikk.validerOverlappende(aktivitetslogg, 1.januar til 31.januar, 1.januar))
        assertFalse(aktivitetslogg.hasErrorsOrWorse())
    }

    @Test
    fun `overlappende utbetalinger`() {
        historikk.oppdaterHistorikk(historikkelement(listOf(
            Utbetalingsperiode("orgnr", 5.januar til 10.januar, 100.prosent, 25000.månedlig)
        )))
        aktivitetslogg.barn().also {
            assertFalse(historikk.validerOverlappende(it, 10.januar til 31.januar, 10.januar))
            assertTrue(it.hasErrorsOrWorse())
        }
        aktivitetslogg.barn().also {
            assertFalse(historikk.validerOverlappende(it, 1.januar til 5.januar, 1.januar))
            assertTrue(it.hasErrorsOrWorse())
        }
        aktivitetslogg.barn().also {
            assertTrue(historikk.validerOverlappende(it, 1.januar til 4.januar, 1.januar))
            assertFalse(it.hasErrorsOrWorse())
        }
        aktivitetslogg.barn().also {
            assertTrue(historikk.validerOverlappende(it, 11.januar til 31.januar, 11.januar))
            assertFalse(it.hasErrorsOrWorse())
        }
    }

    @Test
    fun `overlapper ikke med ferie eller ukjent`() {
        historikk.oppdaterHistorikk(historikkelement(listOf(
            Friperiode(5.januar til 10.januar),
            UkjentInfotrygdperiode(15.januar til 20.januar)
        )))
        assertTrue(historikk.validerOverlappende(aktivitetslogg, 1.januar til 31.januar, 1.januar))
    }

    @Test
    fun `hensyntar ikke ugyldige perioder i overlapp-validering`() {
        historikk.oppdaterHistorikk(historikkelement(ugyldigePerioder = listOf(1.januar to null)))
        assertTrue(historikk.validerOverlappende(aktivitetslogg, 1.januar til 31.januar, 1.januar))
    }

    @Test
    fun `hensyntar ikke statslønn i overlapp-validering`() {
        historikk.oppdaterHistorikk(historikkelement(harStatslønn = true))
        assertTrue(historikk.validerOverlappende(aktivitetslogg, 1.januar til 31.januar, 1.januar))
        assertFalse(aktivitetslogg.hasWarningsOrWorse())
    }

    @Test
    fun `statslønn lager error ved Overgang fra IT`() {
        historikk.oppdaterHistorikk(historikkelement(harStatslønn = true))
        aktivitetslogg.barn().also {
            assertTrue(historikk.valider(it, Periodetype.FØRSTEGANGSBEHANDLING, 1.januar til 31.januar, 1.januar))
            assertFalse(it.hasWarningsOrWorse())
        }
        aktivitetslogg.barn().also {
            assertTrue(historikk.valider(it, Periodetype.FORLENGELSE, 1.januar til 31.januar, 1.januar))
            assertFalse(it.hasWarningsOrWorse())
        }
        aktivitetslogg.barn().also {
            assertTrue(historikk.valider(it, Periodetype.INFOTRYGDFORLENGELSE, 1.januar til 31.januar, 1.januar))
            assertFalse(it.hasWarningsOrWorse())
        }
        aktivitetslogg.barn().also {
            assertFalse(historikk.valider(it, Periodetype.OVERGANG_FRA_IT, 1.januar til 31.januar, 1.januar))
            assertTrue(it.hasErrorsOrWorse())
        }
    }

    @Test
    fun `siste sykepengedag - tom historikk`() {
        historikk.oppdaterHistorikk(historikkelement())
        assertNull(historikk.sisteSykepengedag("ag1"))
    }

    @Test
    fun `siste sykepengedag - tom historikk for ag`() {
        historikk.oppdaterHistorikk(historikkelement(listOf(
            Utbetalingsperiode("ag1", 1.januar til 10.januar, 100.prosent, 1500.daglig)
        )))
        assertNull(historikk.sisteSykepengedag("ag2"))
    }

    @Test
    fun `siste sykepengedag - ekskluderer ferie`() {
        historikk.oppdaterHistorikk(historikkelement(listOf(
            Utbetalingsperiode("ag1", 1.januar til 10.januar, 100.prosent, 1500.daglig),
            Utbetalingsperiode("ag2", 10.januar til 20.januar, 100.prosent, 1500.daglig),
            Friperiode(20.januar til 31.januar)
        )))
        assertEquals(10.januar, historikk.sisteSykepengedag("ag1"))
        assertEquals(20.januar, historikk.sisteSykepengedag("ag2"))
    }

    @Test
    fun `historikk for - tom historikk`() {
        assertEquals(0, historikk.historikkFor("ornr", Sykdomstidslinje()).count())
        assertEquals(1, historikk.historikkFor("ornr", 1.S).count())
    }

    @Test
    fun `historikk for`() {
        historikk.oppdaterHistorikk(historikkelement(listOf(
            Utbetalingsperiode("ag1", 1.januar til 10.januar, 100.prosent, 1500.daglig),
            Utbetalingsperiode("ag2", 10.januar til 20.januar, 100.prosent, 1500.daglig),
            Friperiode(20.januar til 31.januar)
        )))
        assertEquals(22, SykdomstidslinjeInspektør(historikk.historikkFor("ag1", Sykdomstidslinje())).dager.filterNot { it.value is Dag.UkjentDag }.size)
        assertEquals(12, SykdomstidslinjeInspektør(historikk.historikkFor("ag3", Sykdomstidslinje())).dager.filterNot { it.value is Dag.UkjentDag }.size)
    }

    @Test
    fun `har betalt - tom historikk`() {
        assertFalse(historikk.harBetalt("ag1", 1.januar))
    }

    @Test
    fun `har betalt`() {
        historikk.oppdaterHistorikk(historikkelement(listOf(
            Utbetalingsperiode("ag1", 1.januar til 10.januar, 100.prosent, 1500.daglig),
            Utbetalingsperiode("ag2", 10.januar til 20.januar, 100.prosent, 1500.daglig),
            Friperiode(20.januar til 31.januar)
        )))
        assertTrue(historikk.harBetalt("ag1", 1.januar))
        assertFalse(historikk.harBetalt("ag2", 1.januar))
        assertTrue(historikk.harBetalt("ag2", 10.januar))
        assertFalse(historikk.harBetalt("ag1", 20.januar))
    }

    @Test
    fun `skjæringstidspunkt`() {
        historikk.oppdaterHistorikk(historikkelement(listOf(
            Utbetalingsperiode("ag1", 5.januar til 10.januar, 100.prosent, 25000.månedlig),
            Friperiode(11.januar til 12.januar),
            Utbetalingsperiode("ag2", 13.januar til 15.januar, 100.prosent, 25000.månedlig),
            Utbetalingsperiode("ag1", 16.januar til 20.januar, 100.prosent, 25000.månedlig),
            Utbetalingsperiode("ag1", 1.februar til 28.februar, 100.prosent, 25000.månedlig)
        )))
        assertEquals(5.januar, historikk.skjæringstidspunkt(5.januar til 31.januar, emptyList()))
        assertEquals(1.januar, historikk.skjæringstidspunkt(1.januar til 31.januar, listOf(2.S, 3.S)))
    }

    @Test
    fun `skjæringstidspunkter`() {
        historikk.oppdaterHistorikk(historikkelement(listOf(
            Utbetalingsperiode("ag1", 5.januar til 10.januar, 100.prosent, 25000.månedlig),
            Friperiode(11.januar til 12.januar),
            Utbetalingsperiode("ag2", 13.januar til 15.januar, 100.prosent, 25000.månedlig),
            Utbetalingsperiode("ag1", 16.januar til 20.januar, 100.prosent, 25000.månedlig),
            Utbetalingsperiode("ag1", 1.februar til 28.februar, 100.prosent, 25000.månedlig)
        )))
        assertEquals(listOf(1.februar, 5.januar), historikk.skjæringstidspunkter(emptyList()))
        assertEquals(listOf(1.februar, 1.januar), historikk.skjæringstidspunkter(listOf(2.S, 3.S)))
    }

    private fun historikkelement(
        perioder: List<Infotrygdperiode> = emptyList(),
        inntekter: List<Inntektsopplysning> = emptyList(),
        arbeidskategorikoder: Map<String, LocalDate> = emptyMap(),
        ugyldigePerioder: List<Pair<LocalDate?, LocalDate?>> = emptyList(),
        hendelseId: UUID = UUID.randomUUID(),
        harStatslønn: Boolean = false,
        oppdatert: LocalDateTime = LocalDateTime.now()
    ) =
        InfotrygdhistorikkElement.opprett(
            oppdatert = oppdatert,
            hendelseId = hendelseId,
            perioder = perioder,
            inntekter = inntekter,
            arbeidskategorikoder = arbeidskategorikoder,
            ugyldigePerioder = ugyldigePerioder,
            harStatslønn = harStatslønn
        )

    private class InfotrygdhistorikkInspektør(historikk: Infotrygdhistorikk) : InfotrygdhistorikkVisitor {
        private var elementer = mutableListOf<Triple<UUID, LocalDateTime, LocalDateTime>>()

        init {
            historikk.accept(this)
        }

        fun elementer() = elementer.size
        fun opprettet(indeks: Int) = elementer.elementAt(indeks).second
        fun oppdatert(indeks: Int) = elementer.elementAt(indeks).third

        override fun preVisitInfotrygdhistorikkElement(
            id: UUID,
            tidsstempel: LocalDateTime,
            oppdatert: LocalDateTime,
            hendelseId: UUID?,
            lagretInntekter: Boolean,
            lagretVilkårsgrunnlag: Boolean,
            harStatslønn: Boolean
        ) {
            elementer.add(Triple(id, tidsstempel, oppdatert))
        }
    }
}
