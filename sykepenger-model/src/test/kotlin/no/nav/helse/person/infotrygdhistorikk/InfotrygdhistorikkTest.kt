package no.nav.helse.person.infotrygdhistorikk

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.dto.deserialisering.InfotrygdhistorikkInnDto
import no.nav.helse.februar
import no.nav.helse.hendelser.somPeriode
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
import no.nav.helse.testhelpers.tidslinjeOf
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingslinjer.Utbetalingsak
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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
        aktivitetslogg.barn().also {
            historikk.oppfriskNødvendig(it, tidligsteDato)
            assertEquals("${tidligsteDato.minusYears(4)}", it.behov.first().detaljer()["historikkFom"])
            assertEquals("${LocalDate.now()}", it.behov.first().detaljer()["historikkTom"])
        }
        aktivitetslogg.barn().also {
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
            ArbeidsgiverUtbetalingsperiode("orgnr", 1.januar, 31.januar, 100.prosent, 25000.månedlig)
        )
        val nå = LocalDateTime.now()
        val gammel = nå.minusHours(24)
        assertTrue(historikk.oppdaterHistorikk(historikkelement(perioder, oppdatert = gammel)))
        assertFalse(historikk.oppdaterHistorikk(historikkelement(perioder, oppdatert = nå)))
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
                    ArbeidsgiverUtbetalingsperiode("orgnr", 1.januar, 31.januar, 100.prosent, 25000.månedlig)
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
            ArbeidsgiverUtbetalingsperiode("orgnr", 1.januar, 31.januar, 100.prosent, 1154.daglig),
            ArbeidsgiverUtbetalingsperiode("orgnr", 1.februar, 28.februar, 100.prosent, 1154.daglig),
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
                            PersonData.InfotrygdhistorikkElementData.ArbeidsgiverutbetalingsperiodeData("orgnr", 1.februar, 28.februar, 100.0, 1154),
                            PersonData.InfotrygdhistorikkElementData.ArbeidsgiverutbetalingsperiodeData("orgnr", 1.januar, 31.januar, 100.0, 1154)
                        ),
                        personutbetalingsperioder = emptyList(),
                        inntekter = emptyList(),
                        arbeidskategorikoder = emptyMap(),
                        oppdatert = nå
                    ).tilDto()
                )
            )
        )
        assertEquals(1, historikk.inspektør.elementer())
        assertFalse(historikk.oppdaterHistorikk(historikkelement(perioder)))
        assertEquals(1, historikk.inspektør.elementer())
    }

    @Test
    fun `tom historikk validerer`() {
        assertTrue(historikk.valider(aktivitetslogg, 1.januar til 31.januar, 1.januar, "ag1"))
        assertFalse(aktivitetslogg.harFunksjonelleFeilEllerVerre())
    }

    @Test
    fun `nyere opplysninger i Infotrygd`() {
        historikk.oppdaterHistorikk(
            historikkelement(
                listOf(
                    ArbeidsgiverUtbetalingsperiode("ag1", 1.februar, 15.februar, 100.prosent, 1500.daglig),
                    Friperiode(15.mars, 20.mars)
                ), inntekter = listOf(Inntektsopplysning("ag1", 1.februar, 1000.daglig, true))
            )
        )
        aktivitetslogg.barn().also {
            assertTrue(historikk.valider(it, 1.januar til 31.januar, 1.januar, "ag1"))
            assertFalse(it.harFunksjonelleFeilEllerVerre())
            it.assertVarsel(Varselkode.RV_IT_1)
        }
        aktivitetslogg.barn().also {
            assertFalse(historikk.valider(it, 20.februar til 28.februar, 20.februar, "ag1"))
            assertTrue(it.harVarslerEllerVerre())
            it.assertFunksjonellFeil(Varselkode.RV_IT_37)
        }
        aktivitetslogg.barn().also {
            assertTrue(historikk.valider(it, 1.mai til 5.mai, 1.mai, "ag1"))
            assertFalse(it.harVarslerEllerVerre())
        }
    }

    @Test
    fun skjæringstidspunkt() {
        historikk.oppdaterHistorikk(
            historikkelement(
                listOf(
                    ArbeidsgiverUtbetalingsperiode("ag1", 5.januar, 10.januar, 100.prosent, 25000.månedlig),
                    Friperiode(11.januar, 12.januar),
                    ArbeidsgiverUtbetalingsperiode("ag2", 13.januar, 15.januar, 100.prosent, 25000.månedlig),
                    ArbeidsgiverUtbetalingsperiode("ag1", 16.januar, 20.januar, 100.prosent, 25000.månedlig),
                    ArbeidsgiverUtbetalingsperiode("ag1", 1.februar, 28.februar, 100.prosent, 25000.månedlig)
                )
            )
        )
        assertEquals(5.januar, historikk.skjæringstidspunkt(emptyList()).beregnSkjæringstidspunkt(5.januar til 31.januar))
        assertEquals(1.januar, historikk.skjæringstidspunkt(listOf(2.S, 3.S)).beregnSkjæringstidspunkt(januar))
    }

    @Test
    fun `har endret historikk når historikk er tom`() {
        assertFalse(historikk.harEndretHistorikk(lagUtbetaling()))
    }

    @Test
    fun `har endret historikk dersom utbetaling er eldre enn siste element`() {
        val utbetaling = lagUtbetaling()
        historikk.oppdaterHistorikk(historikkelement())
        assertTrue(historikk.harEndretHistorikk(utbetaling))
    }

    @Test
    fun `har ikke endret historikk dersom utbetaling er nyere enn siste element`() {
        historikk.oppdaterHistorikk(historikkelement())
        val utbetaling = lagUtbetaling()
        assertFalse(historikk.harEndretHistorikk(utbetaling))
    }

    @Test
    fun `Infotrygdutbetaling før spleisutbetaling`() {
        historikk.oppdaterHistorikk(
            historikkelement(
                listOf(
                    ArbeidsgiverUtbetalingsperiode("ag1", 1.januar, 25.januar, 100.prosent, 25000.månedlig),
                    Friperiode(26.januar, 31.januar),
                )
            )
        )
        assertFalse(historikk.harEndretHistorikk(lagUtbetaling()))
    }

    @Test
    fun `Infotrygdutbetaling etter spleisutbetaling`() {
        val utbetaling = lagUtbetaling()
        historikk.oppdaterHistorikk(
            historikkelement(
                listOf(
                    ArbeidsgiverUtbetalingsperiode("ag1", 1.januar, 25.januar, 100.prosent, 25000.månedlig),
                    Friperiode(26.januar, 31.januar),
                )
            )
        )
        assertTrue(historikk.harEndretHistorikk(utbetaling))
    }

    @Test
    fun `Ny inntekt registrert i infotrygd`() {
        historikk.oppdaterHistorikk(
            historikkelement(
                listOf(
                    ArbeidsgiverUtbetalingsperiode("ag1", 1.januar, 25.januar, 100.prosent, 25000.månedlig)
                )
            )
        )
        val utbetaling = lagUtbetaling()
        historikk.oppdaterHistorikk(
            historikkelement(
                listOf(
                    ArbeidsgiverUtbetalingsperiode("ag1", 1.januar, 25.januar, 100.prosent, 25000.månedlig)
                )
            )
        )
        assertFalse(historikk.harEndretHistorikk(utbetaling))
        historikk.oppdaterHistorikk(
            historikkelement(
                perioder = listOf(ArbeidsgiverUtbetalingsperiode("ag1", 1.januar, 25.januar, 100.prosent, 25000.månedlig)),
                inntekter = listOf(Inntektsopplysning("ag1", 1.januar, 1000.daglig, true))
            )
        )
        assertFalse(historikk.harEndretHistorikk(utbetaling))
    }

    @Test
    fun `Nye arbeidskategorikoder registrert i infotrygd`() {
        historikk.oppdaterHistorikk(
            historikkelement(
                listOf(
                    ArbeidsgiverUtbetalingsperiode("ag1", 1.januar, 25.januar, 100.prosent, 25000.månedlig)
                )
            )
        )
        val utbetaling = lagUtbetaling()
        historikk.oppdaterHistorikk(
            historikkelement(
                listOf(
                    ArbeidsgiverUtbetalingsperiode("ag1", 1.januar, 25.januar, 100.prosent, 25000.månedlig)
                )
            )
        )
        assertFalse(historikk.harEndretHistorikk(utbetaling))
        historikk.oppdaterHistorikk(
            historikkelement(
                perioder = listOf(ArbeidsgiverUtbetalingsperiode("ag1", 1.januar, 25.januar, 100.prosent, 25000.månedlig)),
                arbeidskategorikoder = mapOf("123" to LocalDate.now())
            )
        )
        assertFalse(historikk.harEndretHistorikk(utbetaling))
    }

    private fun lagUtbetaling() = Utbetaling.lagUtbetaling(
        utbetalinger = emptyList(),
        fødselsnummer = "",
        organisasjonsnummer = "",
        utbetalingstidslinje = tidslinjeOf(),
        periode = 1.januar.somPeriode(),
        utbetalingsaker = listOf(Utbetalingsak(1.januar, listOf(1.januar.somPeriode()))),
        aktivitetslogg = Aktivitetslogg(),
        maksdato = 1.januar,
        forbrukteSykedager = 0,
        gjenståendeSykedager = 0
    ).first

    private fun historikkelement(
        perioder: List<Infotrygdperiode> = emptyList(),
        inntekter: List<Inntektsopplysning> = emptyList(),
        arbeidskategorikoder: Map<String, LocalDate> = emptyMap(),
        hendelseId: UUID = UUID.randomUUID(),
        oppdatert: LocalDateTime = LocalDateTime.now()
    ) =
        InfotrygdhistorikkElement.opprett(
            oppdatert = oppdatert,
            hendelseId = hendelseId,
            perioder = perioder,
            inntekter = inntekter,
            arbeidskategorikoder = arbeidskategorikoder
        )
}
