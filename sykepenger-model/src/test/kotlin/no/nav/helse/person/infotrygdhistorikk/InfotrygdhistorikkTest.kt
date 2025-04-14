package no.nav.helse.person.infotrygdhistorikk

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import no.nav.helse.dto.deserialisering.InfotrygdhistorikkInnDto
import no.nav.helse.februar
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mai
import no.nav.helse.mars
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.infotrygdhistorikk.InfotrygdhistorikkElementTest.Companion.eksisterendeInfotrygdHistorikkelement
import no.nav.helse.serde.PersonData
import no.nav.helse.spleis.e2e.assertFunksjonellFeil
import no.nav.helse.spleis.e2e.assertVarsel
import no.nav.helse.testhelpers.S
import no.nav.helse.testhelpers.resetSeed
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class InfotrygdhistorikkTest {
    private companion object {
        private val tidligsteDato = 1.januar
    }

    private lateinit var historikk: Infotrygdhistorikk
    private lateinit var aktivitetslogg: Aktivitetslogg

    @BeforeEach
    fun setup() {
        resetSeed(1.januar)
        historikk = Infotrygdhistorikk()
        aktivitetslogg = Aktivitetslogg()
    }

    @Test
    fun `må oppfriske tom historikk`() {
        historikk.oppfriskNødvendig(aktivitetslogg, tidligsteDato)
        assertTrue(aktivitetslogg.behov.isNotEmpty()) { aktivitetslogg.toString() }
        assertEquals(0, historikk.inspektør.elementer())
    }

    @Test
    fun `kan justere perioden for oppfrisk`() {
        Aktivitetslogg().also {
            historikk.oppfriskNødvendig(it, tidligsteDato)
            assertEquals("${tidligsteDato.minusYears(4)}", it.behov.first().detaljer()["historikkFom"])
            assertEquals("${LocalDate.now()}", it.behov.first().detaljer()["historikkTom"])
        }
        Aktivitetslogg().also {
            historikk.oppfriskNødvendig(it, 1.februar)
            assertEquals("${1.februar.minusYears(4)}", it.behov.first().detaljer()["historikkFom"])
            assertEquals("${LocalDate.now()}", it.behov.first().detaljer()["historikkTom"])
        }
    }

    @Test
    fun `tømme historikk - ingen data`() {
        historikk.tøm()
        historikk.oppfriskNødvendig(aktivitetslogg, tidligsteDato)
        assertTrue(aktivitetslogg.behov.isNotEmpty()) { aktivitetslogg.toString() }
        assertEquals(0, historikk.inspektør.elementer())
    }

    @Test
    fun `tømme historikk - med tom data`() {
        val tidsstempel = LocalDateTime.now()
        historikk.oppdaterHistorikk(historikkelement(oppdatert = tidsstempel))
        historikk.tøm()
        historikk.oppfriskNødvendig(aktivitetslogg, tidligsteDato)
        assertTrue(aktivitetslogg.behov.isNotEmpty()) { aktivitetslogg.toString() }
        assertEquals(1, historikk.inspektør.elementer())
    }

    @Test
    fun `tømme historikk - med ulagret data`() {
        val tidsstempel = LocalDateTime.now()
        historikk.oppdaterHistorikk(
            historikkelement(
                oppdatert = tidsstempel,
                perioder = listOf(Friperiode(1.januar, 10.januar))
            )
        )
        historikk.tøm()
        historikk.oppfriskNødvendig(aktivitetslogg, tidligsteDato)
        assertTrue(aktivitetslogg.behov.isNotEmpty()) { aktivitetslogg.toString() }
        assertEquals(1, historikk.inspektør.elementer())
    }

    @Test
    fun `tømme historikk - med flere ulagret data`() {
        val tidsstempel1 = LocalDateTime.now().minusHours(1)
        val tidsstempel2 = LocalDateTime.now()
        historikk.oppdaterHistorikk(
            historikkelement(
                oppdatert = tidsstempel1,
                perioder = listOf(Friperiode(1.januar, 5.januar))
            )
        )
        historikk.oppdaterHistorikk(
            historikkelement(
                oppdatert = tidsstempel2,
                perioder = listOf(Friperiode(1.januar, 10.januar))
            )
        )
        historikk.tøm()
        historikk.oppfriskNødvendig(aktivitetslogg, tidligsteDato)
        assertTrue(aktivitetslogg.behov.isNotEmpty()) { aktivitetslogg.toString() }
        assertEquals(1, historikk.inspektør.elementer())
        assertTrue(tidsstempel2 < historikk.inspektør.opprettet(0))
        assertEquals(tidsstempel2, historikk.inspektør.oppdatert(0))
    }

    @Test
    fun `tømme historikk - etter lagring av tom inntektliste`() {
        val historikk = Infotrygdhistorikk.gjenopprett(
            InfotrygdhistorikkInnDto(
                listOf(
                    eksisterendeInfotrygdHistorikkelement(),
                    eksisterendeInfotrygdHistorikkelement()
                )
            )
        )
        historikk.tøm()
        historikk.oppfriskNødvendig(aktivitetslogg, tidligsteDato)
        assertTrue(aktivitetslogg.behov.isNotEmpty()) { aktivitetslogg.toString() }
        assertEquals(1, historikk.inspektør.elementer())
    }

    @Test
    fun `trenger ikke oppfriske gammel historikk`() {
        historikk.oppdaterHistorikk(historikkelement(oppdatert = LocalDateTime.now().minusHours(24)))
        historikk.oppfriskNødvendig(aktivitetslogg, tidligsteDato)
        assertTrue(aktivitetslogg.behov.isNotEmpty())
        assertEquals(1, historikk.inspektør.elementer())
    }

    @Test
    fun `kan bestemme tidspunkt selv`() {
        val tidsstempel = LocalDateTime.now().minusHours(24)
        historikk.oppdaterHistorikk(historikkelement(oppdatert = tidsstempel))
        historikk.oppfriskNødvendig(aktivitetslogg, tidligsteDato)
        assertTrue(aktivitetslogg.behov.isNotEmpty())
        assertEquals(1, historikk.inspektør.elementer())
    }

    @Test
    fun `oppfrisker ikke ny historikk`() {
        historikk.oppdaterHistorikk(historikkelement())
        historikk.oppfriskNødvendig(aktivitetslogg, tidligsteDato)
        assertTrue(aktivitetslogg.behov.isNotEmpty())
    }

    @Test
    fun `oppdaterer tidspunkt når ny historikk er lik gammel`() {
        val perioder = listOf(
            ArbeidsgiverUtbetalingsperiode("orgnr", 1.januar, 31.januar)
        )
        val nå = LocalDateTime.now()
        val gammel = nå.minusHours(24)
        assertEquals(1.januar, historikk.oppdaterHistorikk(historikkelement(perioder, oppdatert = gammel)))
        assertNull(historikk.oppdaterHistorikk(historikkelement(perioder, oppdatert = nå)))
        historikk.oppfriskNødvendig(aktivitetslogg, tidligsteDato)
        assertEquals(1, historikk.inspektør.elementer())
        assertTrue(nå < historikk.inspektør.opprettet(0))
        assertEquals(nå, historikk.inspektør.oppdatert(0))
    }

    @Test
    fun `tom utbetalingstidslinje`() {
        assertTrue(historikk.utbetalingstidslinje().isEmpty())
    }

    @Test
    fun `utbetalingstidslinje kuttes ikke`() {
        historikk.oppdaterHistorikk(
            historikkelement(
                listOf(
                    ArbeidsgiverUtbetalingsperiode("orgnr", 1.januar, 31.januar)
                )
            )
        )
        historikk.utbetalingstidslinje().also {
            assertEquals(januar, it.periode())
        }
    }

    @Test
    fun `rekkefølge respekteres ved deserialisering`() {
        val perioder = listOf(
            ArbeidsgiverUtbetalingsperiode("orgnr", 1.januar, 31.januar),
            ArbeidsgiverUtbetalingsperiode("orgnr", 1.februar, 28.februar),
            Friperiode(1.mars, 31.mars)
        )
        val nå = LocalDateTime.now()
        historikk = Infotrygdhistorikk.gjenopprett(
            InfotrygdhistorikkInnDto(
                elementer = listOf(
                    PersonData.InfotrygdhistorikkElementData(
                        id = UUID.randomUUID(),
                        tidsstempel = nå,
                        hendelseId = UUID.randomUUID(),
                        ferieperioder = listOf(PersonData.InfotrygdhistorikkElementData.FerieperiodeData(1.mars, 31.mars)),
                        arbeidsgiverutbetalingsperioder = listOf(
                            PersonData.InfotrygdhistorikkElementData.ArbeidsgiverutbetalingsperiodeData("orgnr", 1.februar, 28.februar),
                            PersonData.InfotrygdhistorikkElementData.ArbeidsgiverutbetalingsperiodeData("orgnr", 1.januar, 31.januar)
                        ),
                        personutbetalingsperioder = emptyList(),
                        oppdatert = nå
                    ).tilDto()
                )
            )
        )
        assertEquals(1, historikk.inspektør.elementer())
        assertNull(historikk.oppdaterHistorikk(historikkelement(perioder)))
        assertEquals(1, historikk.inspektør.elementer())
    }

    @Test
    fun `tom historikk validerer`() {
        assertTrue(historikk.validerMedFunksjonellFeil(aktivitetslogg, 1.januar til 31.januar))
        assertFalse(aktivitetslogg.harFunksjonelleFeilEllerVerre())
    }

    @Test
    fun `nyere opplysninger i Infotrygd`() {
        historikk.oppdaterHistorikk(
            historikkelement(
                listOf(
                    ArbeidsgiverUtbetalingsperiode("ag1", 1.februar, 15.februar),
                    Friperiode(15.mars, 20.mars)
                )
            )
        )
        Aktivitetslogg().also {
            historikk.validerNyereOpplysninger(it, 1.januar til 31.januar)
            assertFalse(it.harFunksjonelleFeilEllerVerre())
            it.assertVarsel(Varselkode.RV_IT_1)
        }
        Aktivitetslogg().also {
            assertFalse(historikk.validerMedFunksjonellFeil(it, 20.februar til 28.februar))
            assertTrue(it.harVarslerEllerVerre())
            it.assertFunksjonellFeil(Varselkode.RV_IT_37)
        }
        Aktivitetslogg().also {
            assertTrue(historikk.validerMedFunksjonellFeil(it, 1.mai til 5.mai))
            assertFalse(it.harVarslerEllerVerre())
        }
    }

    @Test
    fun skjæringstidspunkt() {
        historikk.oppdaterHistorikk(
            historikkelement(
                listOf(
                    ArbeidsgiverUtbetalingsperiode("ag1", 5.januar, 10.januar),
                    Friperiode(11.januar, 12.januar),
                    ArbeidsgiverUtbetalingsperiode("ag2", 13.januar, 15.januar),
                    ArbeidsgiverUtbetalingsperiode("ag1", 16.januar, 20.januar),
                    ArbeidsgiverUtbetalingsperiode("ag1", 1.februar, 28.februar)
                )
            )
        )
        assertEquals(5.januar, historikk.skjæringstidspunkt(emptyList()).sisteOrNull(5.januar til 31.januar))
        assertEquals(1.januar, historikk.skjæringstidspunkt(listOf(2.S, 3.S)).sisteOrNull(januar))
    }

    private fun historikkelement(
        perioder: List<Infotrygdperiode> = emptyList(),
        hendelseId: UUID = UUID.randomUUID(),
        oppdatert: LocalDateTime = LocalDateTime.now()
    ) =
        InfotrygdhistorikkElement.opprett(
            oppdatert = oppdatert,
            hendelseId = MeldingsreferanseId(hendelseId),
            perioder = perioder
        )
}
