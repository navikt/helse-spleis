package no.nav.helse.person.infotrygdhistorikk

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.april
import no.nav.helse.februar
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.mars
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Inntektshistorikk
import no.nav.helse.person.Person
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.serde.PersonData
import no.nav.helse.serde.PersonData.InfotrygdhistorikkElementData.Companion.tilModellObjekt
import no.nav.helse.somPersonidentifikator
import no.nav.helse.sykepengegrunnlag
import no.nav.helse.testhelpers.A
import no.nav.helse.testhelpers.S
import no.nav.helse.testhelpers.opphold
import no.nav.helse.testhelpers.resetSeed
import no.nav.helse.testhelpers.somVilkårsgrunnlagHistorikk
import no.nav.helse.testhelpers.tidslinjeOf
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingstidslinje.Alder.Companion.alder
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler
import no.nav.helse.utbetalingstidslinje.Inntekter
import no.nav.helse.utbetalingstidslinje.UtbetalingstidslinjeBuilder
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
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
        assertTrue(historikk.oppfriskNødvendig(aktivitetslogg, tidligsteDato))
        assertTrue(aktivitetslogg.behov().isNotEmpty()) { aktivitetslogg.toString() }
        assertEquals(0, historikk.inspektør.elementer())
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
        assertEquals(0, historikk.inspektør.elementer())
    }

    @Test
    fun `tømme historikk - med tom data`() {
        val tidsstempel = LocalDateTime.now()
        historikk.oppdaterHistorikk(historikkelement(oppdatert = tidsstempel))
        historikk.tøm()
        assertFalse(historikk.oppfriskNødvendig(aktivitetslogg, tidligsteDato))
        assertFalse(aktivitetslogg.behov().isNotEmpty()) { aktivitetslogg.toString() }
        assertEquals(1, historikk.inspektør.elementer())
    }

    @Test
    fun `tømme historikk - med ulagret data`() {
        val tidsstempel = LocalDateTime.now()
        historikk.oppdaterHistorikk(historikkelement(
            oppdatert = tidsstempel,
            perioder = listOf(Friperiode(1.januar,  10.januar))
        ))
        historikk.tøm()
        assertFalse(historikk.oppfriskNødvendig(aktivitetslogg, tidligsteDato))
        assertFalse(aktivitetslogg.behov().isNotEmpty()) { aktivitetslogg.toString() }
        assertEquals(1, historikk.inspektør.elementer())
    }

    @Test
    fun `tømme historikk - med flere ulagret data`() {
        val tidsstempel1 = LocalDateTime.now().minusHours(1)
        val tidsstempel2 = LocalDateTime.now()
        historikk.oppdaterHistorikk(historikkelement(
            oppdatert = tidsstempel1,
            perioder = listOf(Friperiode(1.januar,  5.januar))
        ))
        historikk.oppdaterHistorikk(historikkelement(
            oppdatert = tidsstempel2,
            perioder = listOf(Friperiode(1.januar,  10.januar))
        ))
        historikk.tøm()
        assertFalse(historikk.oppfriskNødvendig(aktivitetslogg, tidligsteDato))
        assertFalse(aktivitetslogg.behov().isNotEmpty()) { aktivitetslogg.toString() }
        assertEquals(1, historikk.inspektør.elementer())
        assertTrue(tidsstempel2 < historikk.inspektør.opprettet(0))
        assertEquals(tidsstempel2, historikk.inspektør.oppdatert(0))
    }

    @Test
    fun `tømme historikk - etter lagring av tom inntektliste`() {
        val tidsstempel = LocalDateTime.now()
        historikk.oppdaterHistorikk(historikkelement(
            oppdatert = tidsstempel,
            inntekter = emptyList()
        ))
        historikk.addInntekter(Person("", "01010112345".somPersonidentifikator(), 1.januar(1950).alder, MaskinellJurist()), aktivitetslogg)
        historikk.tøm()
        assertFalse(historikk.oppfriskNødvendig(aktivitetslogg, tidligsteDato))
        assertFalse(aktivitetslogg.behov().isNotEmpty()) { aktivitetslogg.toString() }
        assertEquals(1, historikk.inspektør.elementer())
    }

    @Test
    fun `tømme historikk - med lagret inntekter`() {
        val tidsstempel = LocalDateTime.now()
        historikk.oppdaterHistorikk(historikkelement(
            oppdatert = tidsstempel,
            inntekter = listOf(Inntektsopplysning("orgnr", 1.januar, 1000.daglig, true))
        ))
        historikk.addInntekter(Person("", "10101012345".somPersonidentifikator(), 1.januar(1950).alder, MaskinellJurist()), aktivitetslogg)
        historikk.tøm()
        assertFalse(historikk.oppfriskNødvendig(aktivitetslogg, tidligsteDato))
        assertFalse(aktivitetslogg.behov().isNotEmpty()) { aktivitetslogg.toString() }
        assertEquals(1, historikk.inspektør.elementer())
        assertEquals(tidsstempel, historikk.inspektør.oppdatert(0))
    }

    @Test
    fun `tømme historikk - med lagret vilkårsgrunnlag`() {
        val tidsstempel = LocalDateTime.now()
        historikk.oppdaterHistorikk(historikkelement(
            oppdatert = tidsstempel,
            perioder = listOf(Friperiode(1.januar,  10.januar))
        ))
        historikk.lagreVilkårsgrunnlag(
            skjæringstidspunkt = 1.januar,
            vilkårsgrunnlagHistorikk = VilkårsgrunnlagHistorikk(),
            kanOverskriveVilkårsgrunnlag = { false },
            sykepengegrunnlagFor = INGEN::sykepengegrunnlag
        )
        historikk.tøm()
        assertFalse(historikk.oppfriskNødvendig(aktivitetslogg, tidligsteDato))
        assertFalse(aktivitetslogg.behov().isNotEmpty()) { aktivitetslogg.toString() }
        assertEquals(1, historikk.inspektør.elementer())
        assertTrue(tidsstempel < historikk.inspektør.opprettet(0))
        assertEquals(tidsstempel, historikk.inspektør.oppdatert(0))
    }

    @Test
    fun `tømme historikk - med og ulagret lagret data`() {
        val tidsstempel1 = LocalDateTime.now().minusDays(1)
        val tidsstempel2 = LocalDateTime.now()
        historikk.oppdaterHistorikk(historikkelement(
            oppdatert = tidsstempel1,
            perioder = listOf(Friperiode(1.januar,  10.januar))
        ))
        historikk.lagreVilkårsgrunnlag(
            skjæringstidspunkt = 1.januar,
            vilkårsgrunnlagHistorikk = VilkårsgrunnlagHistorikk(),
            kanOverskriveVilkårsgrunnlag = { false },
            sykepengegrunnlagFor = INGEN::sykepengegrunnlag
        )
        historikk.oppdaterHistorikk(historikkelement(
            oppdatert = tidsstempel2,
            perioder = listOf(ArbeidsgiverUtbetalingsperiode("orgnr", 1.januar,  10.januar, 100.prosent, 1000.daglig))
        ))
        historikk.tøm()
        assertFalse(historikk.oppfriskNødvendig(aktivitetslogg, tidligsteDato))
        assertFalse(aktivitetslogg.behov().isNotEmpty()) { aktivitetslogg.toString() }
        assertEquals(2, historikk.inspektør.elementer())
        assertTrue(tidsstempel2 < historikk.inspektør.opprettet(0))
        assertEquals(tidsstempel2, historikk.inspektør.oppdatert(0))
        assertTrue(tidsstempel1 < historikk.inspektør.opprettet(1))
        assertEquals(tidsstempel1, historikk.inspektør.oppdatert(1))
    }

    @Test
    fun `må oppfriske gammel historikk`() {
        historikk.oppdaterHistorikk(historikkelement(oppdatert = LocalDateTime.now().minusHours(24)))
        assertTrue(historikk.oppfriskNødvendig(aktivitetslogg, tidligsteDato))
        assertTrue(aktivitetslogg.behov().isNotEmpty())
        assertEquals(1, historikk.inspektør.elementer())
    }

    @Test
    fun `kan bestemme tidspunkt selv`() {
        val tidsstempel = LocalDateTime.now().minusHours(24)
        historikk.oppdaterHistorikk(historikkelement(oppdatert = tidsstempel))
        assertFalse(historikk.oppfriskNødvendig(aktivitetslogg, tidligsteDato, tidsstempel.minusMinutes(1)))
        assertFalse(aktivitetslogg.behov().isNotEmpty())
        assertEquals(1, historikk.inspektør.elementer())
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
            ArbeidsgiverUtbetalingsperiode("orgnr", 1.januar,  31.januar, 100.prosent, 25000.månedlig)
        )
        val nå = LocalDateTime.now()
        val gammel = nå.minusHours(24)
        assertTrue(historikk.oppdaterHistorikk(historikkelement(perioder, oppdatert = gammel)))
        assertFalse(historikk.oppdaterHistorikk(historikkelement(perioder, oppdatert = nå)))
        assertFalse(historikk.oppfriskNødvendig(aktivitetslogg, tidligsteDato))
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
        historikk.oppdaterHistorikk(historikkelement(listOf(
            ArbeidsgiverUtbetalingsperiode("orgnr", 1.januar,  31.januar, 100.prosent, 25000.månedlig)
        )))
        historikk.utbetalingstidslinje().also {
            assertEquals(1.januar til 31.januar, it.periode())
        }
    }

    @Test
    fun `rekkefølge respekteres ved deserialisering`() {
        val perioder = listOf(
            ArbeidsgiverUtbetalingsperiode("orgnr", 1.januar,  31.januar, 100.prosent, 25000.månedlig),
            ArbeidsgiverUtbetalingsperiode("orgnr", 1.februar,  28.februar, 100.prosent, 25000.månedlig),
            Friperiode(1.mars,  31.mars)
        )
        val nå = LocalDateTime.now()
        historikk = listOf(PersonData.InfotrygdhistorikkElementData(
            id = UUID.randomUUID(),
            tidsstempel = nå,
            hendelseId = UUID.randomUUID(),
            ferieperioder = listOf(PersonData.InfotrygdhistorikkElementData.FerieperiodeData(1.mars, 31.mars)),
            arbeidsgiverutbetalingsperioder = listOf(
                PersonData.InfotrygdhistorikkElementData.ArbeidsgiverutbetalingsperiodeData("orgnr", 1.februar, 28.februar, 100, 25000.0),
                PersonData.InfotrygdhistorikkElementData.ArbeidsgiverutbetalingsperiodeData("orgnr", 1.januar, 31.januar, 100, 25000.0)
            ),
            personutbetalingsperioder = emptyList(),
            ukjenteperioder = emptyList(),
            inntekter = emptyList(),
            arbeidskategorikoder = emptyMap(),
            ugyldigePerioder = emptyList(),
            harStatslønn = false,
            oppdatert = nå,
            lagretInntekter = false,
            lagretVilkårsgrunnlag = false
        )).tilModellObjekt()
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
        historikk.oppdaterHistorikk(historikkelement(listOf(
            ArbeidsgiverUtbetalingsperiode("ag1", 1.februar,  15.februar, 100.prosent, 1500.daglig),
            Friperiode(15.mars,  20.mars)
        ), inntekter = listOf(Inntektsopplysning("ag1", 1.februar, 1000.daglig, true))))
        aktivitetslogg.barn().also {
            assertFalse(historikk.valider(it, 1.januar til 31.januar, 1.januar, "ag1"))
            assertTrue(it.harFunksjonelleFeilEllerVerre())
        }
        aktivitetslogg.barn().also {
            assertTrue(historikk.valider(it, 20.februar til 28.februar, 20.februar, "ag1"))
            assertFalse(it.harVarslerEllerVerre())
        }
        aktivitetslogg.barn().also {
            assertTrue(historikk.valider(it, 1.april til 5.april, 1.april, "ag1"))
            assertFalse(it.harVarslerEllerVerre())
        }
    }

    @Test
    fun skjæringstidspunkt() {
        historikk.oppdaterHistorikk(historikkelement(listOf(
            ArbeidsgiverUtbetalingsperiode("ag1", 5.januar,  10.januar, 100.prosent, 25000.månedlig),
            Friperiode(11.januar,  12.januar),
            ArbeidsgiverUtbetalingsperiode("ag2", 13.januar,  15.januar, 100.prosent, 25000.månedlig),
            ArbeidsgiverUtbetalingsperiode("ag1", 16.januar,  20.januar, 100.prosent, 25000.månedlig),
            ArbeidsgiverUtbetalingsperiode("ag1", 1.februar,  28.februar, 100.prosent, 25000.månedlig)
        )))
        assertEquals(5.januar, historikk.skjæringstidspunkt(5.januar til 31.januar, emptyList()))
        assertEquals(1.januar, historikk.skjæringstidspunkt(1.januar til 31.januar, listOf(2.S, 3.S)))
    }

    @Test
    fun `skjæringstidspunkt med låst periode`() {
        val sykdomstidslinje = 28.S + 3.A + 16.S
        sykdomstidslinje.lås(1.januar til 31.januar)
        historikk.oppdaterHistorikk(historikkelement(listOf(
            ArbeidsgiverUtbetalingsperiode("ag1", 29.januar,  31.januar, 100.prosent, 25000.månedlig)
        )))
        assertEquals(1.januar, historikk.skjæringstidspunkt(1.februar til 16.februar, listOf(sykdomstidslinje)))
    }

    @Test
    fun `skjæringstidspunkt for orgnr med låst periode`() {
        val sykdomstidslinje = 28.S + 3.A + 16.S
        sykdomstidslinje.lås(1.januar til 31.januar)
        historikk.oppdaterHistorikk(historikkelement(listOf(
            ArbeidsgiverUtbetalingsperiode("ag1", 29.januar,  31.januar, 100.prosent, 25000.månedlig)
        )))
        assertEquals(1.januar, historikk.skjæringstidspunkt("ag1", 1.februar til 16.februar, sykdomstidslinje))
    }

    @Test
    fun `skjæringstidspunkter med låst periode`() {
        val sykdomstidslinje = 28.S + 3.A + 16.S
        sykdomstidslinje.lås(1.januar til 31.januar)
        historikk.oppdaterHistorikk(historikkelement(listOf(
            ArbeidsgiverUtbetalingsperiode("ag1", 29.januar,  31.januar, 100.prosent, 25000.månedlig)
        )))
        assertEquals(listOf(1.januar), historikk.skjæringstidspunkter(listOf(sykdomstidslinje)))
    }

    @Test
    fun skjæringstidspunkter() {
        historikk.oppdaterHistorikk(historikkelement(listOf(
            ArbeidsgiverUtbetalingsperiode("ag1", 5.januar,  10.januar, 100.prosent, 25000.månedlig),
            Friperiode(11.januar,  12.januar),
            ArbeidsgiverUtbetalingsperiode("ag2", 13.januar,  15.januar, 100.prosent, 25000.månedlig),
            ArbeidsgiverUtbetalingsperiode("ag1", 16.januar,  20.januar, 100.prosent, 25000.månedlig),
            ArbeidsgiverUtbetalingsperiode("ag1", 1.februar,  28.februar, 100.prosent, 25000.månedlig)
        )))
        assertEquals(listOf(1.februar, 5.januar), historikk.skjæringstidspunkter(emptyList()))
        assertEquals(listOf(1.februar, 1.januar), historikk.skjæringstidspunkter(listOf(2.S, 3.S)))
    }

    @Test
    fun `har endret historikk når historikk er tom`() {
        assertFalse(historikk.harEndretHistorikk(utbetaling()))
    }

    @Test
    fun `har endret historikk dersom utbetaling er eldre enn siste element`() {
        val utbetaling = utbetaling()
        historikk.oppdaterHistorikk(historikkelement())
        assertTrue(historikk.harEndretHistorikk(utbetaling))
    }

    @Test
    fun `har ikke endret historikk om ugyldige perioder er lik`() {
        assertTrue(historikk.oppdaterHistorikk(historikkelement(ugyldigePerioder = listOf(UgyldigPeriode(1.januar, 1.januar, 100)))))
        assertFalse(historikk.oppdaterHistorikk(historikkelement(ugyldigePerioder = listOf(UgyldigPeriode(1.januar, 1.januar, 100)))))
    }

    @Test
    fun `har ikke endret historikk dersom utbetaling er nyere enn siste element`() {
        historikk.oppdaterHistorikk(historikkelement())
        val utbetaling = utbetaling()
        assertFalse(historikk.harEndretHistorikk(utbetaling))
    }

    @Test
    fun `tar ikke med nyere historikk i beregning av utbetalingstidslinje`() {
        historikk.oppdaterHistorikk(historikkelement(listOf(
            ArbeidsgiverUtbetalingsperiode("ag1", 1.februar, 10.februar, 100.prosent, 25000.månedlig),
            Friperiode(11.februar,  15.februar),
        )))
        val sykdomstidslinje = 31.S
        val builder = UtbetalingstidslinjeBuilder(Inntekter(
            organisasjonsnummer = "a1",
            vilkårsgrunnlagHistorikk = mapOf(
                1.januar to Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), 1.januar, UUID.randomUUID(), 25000.månedlig)
            ).somVilkårsgrunnlagHistorikk("a1"),
            regler = ArbeidsgiverRegler.Companion.NormalArbeidstaker,
            subsumsjonObserver = SubsumsjonObserver.NullObserver
        ))
        val utbetalingstidslinje = historikk.build("ag1", sykdomstidslinje, builder, SubsumsjonObserver.NullObserver)
        assertEquals(1.januar til 31.januar, utbetalingstidslinje.periode())
    }

    @Test
    fun `tar ikke med eldre historikk i beregning av utbetalingstidslinje`() {
        historikk.oppdaterHistorikk(historikkelement(listOf(
            ArbeidsgiverUtbetalingsperiode("ag1", 1.januar, 25.januar, 100.prosent, 25000.månedlig),
            Friperiode(26.januar,  31.januar),
        )))
        val sykdomstidslinje = 31.opphold + 28.S
        val builder = UtbetalingstidslinjeBuilder(Inntekter(
            organisasjonsnummer = "a1",
            vilkårsgrunnlagHistorikk = mapOf(
                1.januar to Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), 1.januar, UUID.randomUUID(), 25000.månedlig)
            ).somVilkårsgrunnlagHistorikk("a1"),
            regler = ArbeidsgiverRegler.Companion.NormalArbeidstaker,
            subsumsjonObserver = SubsumsjonObserver.NullObserver
        ))
        val utbetalingstidslinje = historikk.build("ag1", sykdomstidslinje, builder, SubsumsjonObserver.NullObserver)
        assertEquals(1.februar til 28.februar, utbetalingstidslinje.periode())
    }

    private fun utbetaling() = Utbetaling.lagUtbetaling(
        utbetalinger = emptyList(),
        fødselsnummer = "",
        beregningId = UUID.randomUUID(),
        organisasjonsnummer = "",
        utbetalingstidslinje = tidslinjeOf(),
        sisteDato = 1.januar,
        aktivitetslogg = Aktivitetslogg(),
        maksdato = 1.januar,
        forbrukteSykedager = 0,
        gjenståendeSykedager = 0,
        forrige = null
    )

    private fun historikkelement(
        perioder: List<Infotrygdperiode> = emptyList(),
        inntekter: List<Inntektsopplysning> = emptyList(),
        arbeidskategorikoder: Map<String, LocalDate> = emptyMap(),
        ugyldigePerioder: List<UgyldigPeriode> = emptyList(),
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
}
