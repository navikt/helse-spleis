package no.nav.helse.utbetalingslinjer

import no.nav.helse.Toggle
import no.nav.helse.hendelser.til
import no.nav.helse.hendelser.utbetaling.*
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.helse.person.UtbetalingVisitor
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingslinjer.Oppdragstatus.AKSEPTERT
import no.nav.helse.utbetalingslinjer.Utbetaling.Companion.aktive
import no.nav.helse.utbetalingstidslinje.MaksimumUtbetaling
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Isolated
internal class UtbetalingTest {

    private lateinit var aktivitetslogg: Aktivitetslogg

    private companion object {
        private const val UNG_PERSON_FNR_2018 = "12020052345"
        private const val ORGNUMMER = "987654321"
    }

    private val Oppdrag.inspektør get() = OppdragInspektør(this)

    @BeforeEach
    private fun initEach() {
        aktivitetslogg = Aktivitetslogg()
    }

    @Test
    fun `utbetalinger kan konverters til sykdomstidslinje`() {
        val tidslinje = tidslinjeOf(16.AP, 3.NAV, 2.HELG, 5.NAV, 2.HELG, 5.NAV)
        beregnUtbetalinger(tidslinje)
        val utbetaling = opprettUtbetaling(tidslinje, sisteDato = 21.januar)

        val inspektør = SykdomstidslinjeInspektør(Utbetaling.sykdomstidslinje(listOf(utbetaling), Sykdomstidslinje()))
        assertEquals(21, inspektør.dager.size)
        assertTrue(inspektør.dager.values.all { it is Dag.Sykedag || it is Dag.SykHelgedag })
    }

    @Test
    fun `konvertert tidslinje overskriver ikke ny`() {
        val tidslinje = tidslinjeOf(10.NAV)
        beregnUtbetalinger(tidslinje)
        val utbetaling = opprettUtbetaling(tidslinje, sisteDato = 10.januar)
        val sykdomstidslinje = Sykdomstidslinje.arbeidsdager(1.januar til 10.januar, SykdomstidslinjeHendelse.Hendelseskilde.INGEN)
        val inspektør = SykdomstidslinjeInspektør(Utbetaling.sykdomstidslinje(listOf(utbetaling), sykdomstidslinje))
        assertEquals(10, inspektør.dager.size)
        assertTrue(inspektør.dager.values.all { it is Dag.Arbeidsdag || it is Dag.FriskHelgedag })
    }

    @Test
    fun `utbetalinger inkluderer ikke dager etter siste dato`() {
        val tidslinje = tidslinjeOf(16.AP, 3.NAV, 2.HELG, 5.NAV, 2.HELG, 5.NAV)
        beregnUtbetalinger(tidslinje)

        val sisteDato = 21.januar
        val utbetaling = Utbetaling.lagUtbetaling(
            emptyList(),
            UNG_PERSON_FNR_2018,
            UUID.randomUUID(),
            ORGNUMMER,
            tidslinje,
            sisteDato,
            aktivitetslogg,
            LocalDate.MAX,
            100,
            148
        )
        lateinit var utbetalingstidslinje: Utbetalingstidslinje
        utbetaling.accept(object : UtbetalingVisitor {
            override fun preVisit(tidslinje: Utbetalingstidslinje) {
                utbetalingstidslinje = tidslinje
            }
        })
        assertEquals(1.januar til sisteDato, utbetalingstidslinje.periode())
        assertEquals(17.januar til sisteDato, utbetaling.periode)
    }

    @Test
    fun `periode for annullering`() {
        val tidslinje = tidslinjeOf(16.AP, 3.NAV, 2.HELG, 1.FRI, 4.NAV, 2.HELG, 1.FRI, 4.NAV)
        beregnUtbetalinger(tidslinje)
        val første = opprettUtbetaling(tidslinje, sisteDato = 21.januar)
        val andre = opprettUtbetaling(tidslinje, første, 28.januar)
        val tredje = opprettUtbetaling(tidslinje, andre, 2.februar)
        val annullering =
            tredje.annuller(AnnullerUtbetaling(UUID.randomUUID(), "", "", "", tredje.arbeidsgiverOppdrag().fagsystemId(), "", "", LocalDateTime.now()))
                ?: fail {
                    "Klarte ikke lage annullering"
                }
        assertEquals(17.januar til 2.februar, annullering.periode)
        assertEquals(17.januar, annullering.arbeidsgiverOppdrag().førstedato)
    }

    @Test
    fun `forlenger seg ikke på en annullering`() {
        val tidslinje = tidslinjeOf(16.AP, 3.NAV, 2.HELG, 5.NAV)
        beregnUtbetalinger(tidslinje)
        val første = opprettUtbetaling(tidslinje)
        val annullering = første.annuller(AnnullerUtbetaling(UUID.randomUUID(), "", "", "", første.arbeidsgiverOppdrag().fagsystemId(), "", "", LocalDateTime.now())) ?: fail {
            "Klarte ikke lage annullering"
        }
        val andre = opprettUtbetaling(tidslinje, annullering)
        assertNotEquals(første.arbeidsgiverOppdrag().fagsystemId(), andre.arbeidsgiverOppdrag().fagsystemId())
    }

    @Test
    fun `omgjøre en revurdert periode med opphør`() {
        val tidslinje = tidslinjeOf(16.AP, 3.NAV, 2.HELG, 5.NAV)
        val ferietidslinje = tidslinjeOf(16.AP, 10.FRI)
        beregnUtbetalinger(tidslinje)
        val første = opprettUtbetaling(tidslinje)
        val andre = opprettUtbetaling(ferietidslinje, første)
        val tredje = opprettUtbetaling(tidslinje, andre)
        assertEquals(første.arbeidsgiverOppdrag().fagsystemId(), andre.arbeidsgiverOppdrag().fagsystemId())
        assertTrue(andre.arbeidsgiverOppdrag()[0].erOpphør())
        assertEquals(første.arbeidsgiverOppdrag().fagsystemId(), tredje.arbeidsgiverOppdrag().fagsystemId())
        assertEquals(17.januar, tredje.arbeidsgiverOppdrag().førstedato)
    }

    @Test
    fun `forlenger seg på en revurdert periode med opphør`() {
        val tidslinje = tidslinjeOf(16.AP, 3.NAV, 2.HELG, 5.NAV)
        val ferietidslinje = tidslinjeOf(16.AP, 10.FRI)
        beregnUtbetalinger(tidslinje)
        val første = opprettUtbetaling(tidslinje)
        val andre = opprettUtbetaling(ferietidslinje, første)
        val tredje = opprettUtbetaling(beregnUtbetalinger(ferietidslinje + tidslinjeOf(10.NAV, startDato = 27.januar)), andre)
        assertEquals(første.arbeidsgiverOppdrag().fagsystemId(), andre.arbeidsgiverOppdrag().fagsystemId())
        assertTrue(andre.arbeidsgiverOppdrag()[0].erOpphør())
        assertEquals(første.arbeidsgiverOppdrag().fagsystemId(), tredje.arbeidsgiverOppdrag().fagsystemId())
        assertEquals(27.januar, tredje.arbeidsgiverOppdrag().førstedato)
    }

    @Test
    fun nettoBeløp() {
        val tidslinje = tidslinjeOf(5.NAV, 2.HELG, 4.NAV)
        beregnUtbetalinger(tidslinje)
        val første = opprettUtbetaling(tidslinje, sisteDato = 7.januar)
        val andre = opprettUtbetaling(tidslinje, første)

        OppdragInspektør(første.arbeidsgiverOppdrag()).also {
            assertEquals(6000, it.nettoBeløp(0))
        }
        OppdragInspektør(andre.arbeidsgiverOppdrag()).also {
            assertEquals(4800, it.nettoBeløp(0))
        }
    }

    @Test
    fun `sorterer etter når fagsystemIDen ble oppretta`() {
        val tidslinje = tidslinjeOf(
            16.AP, 1.NAV, 2.HELG, 5.NAV(1200, 50.0), 7.FRI, 16.AP, 1.NAV,
            startDato = 1.januar(2020)
        )

        beregnUtbetalinger(tidslinje)

        val første = opprettUtbetaling(tidslinje.kutt(19.januar(2020)))
        val andre = opprettUtbetaling(tidslinje.kutt(24.januar(2020)), tidligere = første)
        val tredje = opprettUtbetaling(tidslinje.kutt(18.februar(2020)), tidligere = andre)
        val andreAnnullert =
            andre.annuller(AnnullerUtbetaling(UUID.randomUUID(), "", "", "", andre.arbeidsgiverOppdrag().fagsystemId(), "", "", LocalDateTime.now())) ?: fail {
                "Klarte ikke lage annullering"
            }
        godkjenn(andreAnnullert)

        assertEquals(listOf(andre, tredje), listOf(tredje, andre, første).aktive())
        assertEquals(listOf(tredje), listOf(andreAnnullert, tredje, andre, første).aktive())
    }

    @Test
    fun `setter refFagsystemId og refDelytelseId når en linje peker på en annen`() {
        val tidslinje = tidslinjeOf(
            16.AP, 1.NAV, 2.HELG, 5.NAV(1200, 50.0),
            startDato = 1.januar(2020)
        )

        beregnUtbetalinger(tidslinje)

        val tidligere = opprettUtbetaling(tidslinje.kutt(19.januar(2020)))
        val utbetaling = opprettUtbetaling(tidslinje.kutt(24.januar(2020)), tidligere = tidligere)

        OppdragInspektør(utbetaling.arbeidsgiverOppdrag()).also {
            assertEquals(2, it.antallLinjer())
            assertNull(it.refDelytelseId(0))
            assertNull(it.refFagsystemId(0))
            assertNotNull(it.refDelytelseId(1))
            assertNotNull(it.refFagsystemId(1))
        }
    }

    @Test
    fun `separate utbetalinger`() {
        val tidslinje = tidslinjeOf(
            16.AP,
            1.NAV,
            2.HELG,
            5.NAV,
            2.HELG,
            5.AP,
            2.HELG,
            5.NAV,
            2.HELG,
            5.NAV,
            2.HELG,
            5.NAV,
            2.HELG,
            5.NAV,
            2.HELG,
            startDato = 1.januar(2020)
        )

        beregnUtbetalinger(tidslinje)

        val første = opprettUtbetaling(tidslinje.kutt(19.januar(2020)))
        val andre = opprettUtbetaling(tidslinje, tidligere = første)

        val inspektør1 = OppdragInspektør(første.arbeidsgiverOppdrag())
        val inspektør2 = OppdragInspektør(andre.arbeidsgiverOppdrag())
        assertEquals(1, inspektør1.antallLinjer())
        assertEquals(1, inspektør2.antallLinjer())
        assertNull(inspektør1.refDelytelseId(0))
        assertNull(inspektør1.refFagsystemId(0))
        assertNull(inspektør2.refDelytelseId(0))
        assertNull(inspektør2.refFagsystemId(0))

        assertNotEquals(inspektør1.fagSystemId(0), inspektør2.fagSystemId(0))
    }

    @Test
    fun `går ikke videre når ett av to oppdrag er overført`() {
        val tidslinje = tidslinjeOf(16.AP, 15.NAV(dekningsgrunnlag = 1000, refusjonsbeløp = 600))
        beregnUtbetalinger(tidslinje)
        val (utbetaling, utbetalingId) = opprettGodkjentUtbetaling(tidslinje)
        val personOppdrag = utbetaling.inspektør.personOppdrag
        utbetaling.håndter(
            UtbetalingOverført(
                meldingsreferanseId = UUID.randomUUID(),
                aktørId = "aktørId",
                fødselsnummer = "fnr",
                orgnummer = "orgnr",
                fagsystemId = utbetaling.inspektør.arbeidsgiverOppdrag.fagsystemId(),
                utbetalingId = utbetalingId.toString(),
                avstemmingsnøkkel = 1234L,
                overføringstidspunkt = LocalDateTime.now()
            )
        )
        assertEquals(Utbetaling.Sendt, utbetaling.inspektør.tilstand)
    }

    @Test
    fun `går videre når begge oppdragene er overført`() {
        val tidslinje = tidslinjeOf(16.AP, 15.NAV(dekningsgrunnlag = 1000, refusjonsbeløp = 600))
        beregnUtbetalinger(tidslinje)
        val (utbetaling, utbetalingId) = opprettGodkjentUtbetaling(tidslinje)
        utbetaling.håndter(
            UtbetalingOverført(
                meldingsreferanseId = UUID.randomUUID(),
                aktørId = "aktørId",
                fødselsnummer = "fnr",
                orgnummer = "orgnr",
                fagsystemId = utbetaling.inspektør.arbeidsgiverOppdrag.fagsystemId(),
                utbetalingId = utbetalingId.toString(),
                avstemmingsnøkkel = 1234L,
                overføringstidspunkt = LocalDateTime.now()
            )
        )
        utbetaling.håndter(
            UtbetalingOverført(
                meldingsreferanseId = UUID.randomUUID(),
                aktørId = "aktørId",
                fødselsnummer = "fnr",
                orgnummer = "orgnr",
                fagsystemId = utbetaling.inspektør.personOppdrag.fagsystemId(),
                utbetalingId = utbetalingId.toString(),
                avstemmingsnøkkel = 1234L,
                overføringstidspunkt = LocalDateTime.now()
            )
        )
        assertEquals(Utbetaling.Overført, utbetaling.inspektør.tilstand)
    }

    @Test
    fun `går ikke videre når ett av to oppdrag er akseptert`() {
        val tidslinje = tidslinjeOf(16.AP, 15.NAV(dekningsgrunnlag = 1000, refusjonsbeløp = 600))
        beregnUtbetalinger(tidslinje)
        val (utbetaling, utbetalingId) = opprettGodkjentUtbetaling(tidslinje)
        val personOppdrag = utbetaling.inspektør.personOppdrag
        utbetaling.håndter(
            UtbetalingHendelse(
                meldingsreferanseId = UUID.randomUUID(),
                aktørId = "aktørId",
                fødselsnummer = "fnr",
                orgnummer = "orgnr",
                fagsystemId = utbetaling.inspektør.arbeidsgiverOppdrag.fagsystemId(),
                utbetalingId = utbetalingId.toString(),
                status = AKSEPTERT,
                melding = "",
                avstemmingsnøkkel = 1234L,
                overføringstidspunkt = LocalDateTime.now()
            )
        )
        assertEquals(Utbetaling.Sendt, utbetaling.inspektør.tilstand)
    }

    @Test
    fun `går videre når begge oppdragene er akseptert`() {
        val tidslinje = tidslinjeOf(16.AP, 15.NAV(dekningsgrunnlag = 1000, refusjonsbeløp = 600))
        beregnUtbetalinger(tidslinje)
        val (utbetaling, utbetalingId) = opprettGodkjentUtbetaling(tidslinje)
        utbetaling.håndter(
            UtbetalingHendelse(
                meldingsreferanseId = UUID.randomUUID(),
                aktørId = "aktørId",
                fødselsnummer = "fnr",
                orgnummer = "orgnr",
                fagsystemId = utbetaling.inspektør.arbeidsgiverOppdrag.fagsystemId(),
                utbetalingId = utbetalingId.toString(),
                status = AKSEPTERT,
                melding = "",
                avstemmingsnøkkel = 1234L,
                overføringstidspunkt = LocalDateTime.now()
            )
        )
        utbetaling.håndter(
            UtbetalingHendelse(
                meldingsreferanseId = UUID.randomUUID(),
                aktørId = "aktørId",
                fødselsnummer = "fnr",
                orgnummer = "orgnr",
                fagsystemId = utbetaling.inspektør.personOppdrag.fagsystemId(),
                utbetalingId = utbetalingId.toString(),
                status = AKSEPTERT,
                melding = "",
                avstemmingsnøkkel = 1234L,
                overføringstidspunkt = LocalDateTime.now()
            )
        )
        assertEquals(Utbetaling.Utbetalt, utbetaling.inspektør.tilstand)
    }

    private val Utbetaling.inspektør get() = UtbetalingsInspektør(this)

    @Test
    fun `delvis refusjon`() {
        val tidslinje = tidslinjeOf(16.AP, 15.NAV(dekningsgrunnlag = 1000, refusjonsbeløp = 600))
        beregnUtbetalinger(tidslinje)
        Toggle.LageBrukerutbetaling.enable {
            val utbetaling = opprettUtbetaling(tidslinje)
            assertTrue(utbetaling.harDelvisRefusjon())
        }
    }

    @Test
    fun `null refusjon`() {
        val tidslinje = tidslinjeOf(16.AP, 15.NAV(dekningsgrunnlag = 1000, refusjonsbeløp = 0))
        beregnUtbetalinger(tidslinje)
        Toggle.LageBrukerutbetaling.enable {
            val utbetaling = opprettUtbetaling(tidslinje)
            assertFalse(utbetaling.harDelvisRefusjon())
            assertTrue(utbetaling.harUtbetalinger())
        }
    }

    @Test
    fun `overføre utbetaling med null refusjon`() {
        val tidslinje = tidslinjeOf(16.AP, 15.NAV(dekningsgrunnlag = 1000, refusjonsbeløp = 0))
        beregnUtbetalinger(tidslinje)
        Toggle.LageBrukerutbetaling.enable {
            val utbetaling = opprettUbetaltUtbetaling(tidslinje)
            val hendelselogg = godkjenn(utbetaling)
            val utbetalingsbehov = hendelselogg.behov().filter { it.type == Behovtype.Utbetaling }
            assertEquals(1, utbetalingsbehov.size) { "Forventer bare ett utbetalingsbehov" }
            assertEquals(Fagområde.Sykepenger.verdi, utbetalingsbehov.first().detaljer().getValue("fagområde"))
        }
    }

    @Test
    fun `tre utbetalinger`() {
        val tidslinje = tidslinjeOf(
            16.AP, 1.NAV, 2.HELG, 5.NAV(1200, 50.0), 2.HELG, 5.NAV,
            startDato = 1.januar(2020)
        )

        beregnUtbetalinger(tidslinje)

        val første = opprettUtbetaling(tidslinje.kutt(19.januar(2020)))
        val andre = opprettUtbetaling(tidslinje.kutt(24.januar(2020)), tidligere = første)
        val tredje = opprettUtbetaling(tidslinje.kutt(31.januar(2020)), tidligere = andre)

        val inspektør = OppdragInspektør(tredje.arbeidsgiverOppdrag())
        assertEquals(3, inspektør.antallLinjer())
        assertNull(inspektør.refDelytelseId(0))
        assertNull(inspektør.refFagsystemId(0))

        assertEquals(1, inspektør.delytelseId(0))
        assertEquals(2, inspektør.delytelseId(1))
        assertEquals(3, inspektør.delytelseId(2))

        assertEquals(inspektør.delytelseId(0), inspektør.refDelytelseId(1))
        assertEquals(inspektør.delytelseId(1), inspektør.refDelytelseId(2))

        assertEquals(første.arbeidsgiverOppdrag().fagsystemId(), inspektør.refFagsystemId(1))
        assertEquals(andre.arbeidsgiverOppdrag().fagsystemId(), inspektør.refFagsystemId(2))
    }

    @Test
    fun `overgang fra full til null refusjon`() {
        Toggle.LageBrukerutbetaling.enable {
            val tidslinje = tidslinjeOf(
                16.AP, 1.NAV, 2.HELG, 5.NAV, 2.HELG, 5.NAV, 2.HELG, 5.NAV(1200, refusjonsbeløp = 0.0), 2.HELG, 5.NAV, 2.HELG, 1.ARB, 4.NAV(1200, refusjonsbeløp = 0.0)
            )

            beregnUtbetalinger(tidslinje)

            val første = opprettUtbetaling(tidslinje.kutt(26.januar))
            val andre = opprettUtbetaling(tidslinje.kutt(31.januar), tidligere = første)
            val tredje = opprettUtbetaling(tidslinje.kutt(7.februar), tidligere = andre)
            val fjerde = opprettUtbetaling(tidslinje.kutt(14.februar), tidligere = tredje)
            val femte = opprettUtbetaling(tidslinje.kutt(21.februar), tidligere = fjerde)

            assertEquals(første.arbeidsgiverOppdrag().fagsystemId(), andre.arbeidsgiverOppdrag().fagsystemId())
            assertEquals(andre.arbeidsgiverOppdrag().fagsystemId(), tredje.arbeidsgiverOppdrag().fagsystemId())
            assertEquals(tredje.arbeidsgiverOppdrag().fagsystemId(), fjerde.arbeidsgiverOppdrag().fagsystemId())
            assertEquals(fjerde.arbeidsgiverOppdrag().fagsystemId(), femte.arbeidsgiverOppdrag().fagsystemId())

            assertNotEquals(første.arbeidsgiverOppdrag().fagsystemId(), første.personOppdrag().fagsystemId())

            assertEquals(første.personOppdrag().fagsystemId(), andre.personOppdrag().fagsystemId())
            assertEquals(andre.personOppdrag().fagsystemId(), tredje.personOppdrag().fagsystemId())
            assertEquals(tredje.personOppdrag().fagsystemId(), fjerde.personOppdrag().fagsystemId())
            assertEquals(fjerde.personOppdrag().fagsystemId(), femte.personOppdrag().fagsystemId())

            assertEquals(0, første.personOppdrag().inspektør.antallLinjer())
            assertEquals(0, andre.personOppdrag().inspektør.antallLinjer())
            assertEquals(1, tredje.personOppdrag().inspektør.antallLinjer())
            assertEquals(1, fjerde.personOppdrag().inspektør.antallLinjer())
            assertEquals(2, femte.personOppdrag().inspektør.antallLinjer())
        }
    }

    @Test
    fun `utbetalingOverført som ikke treffer på fagsystemId`() {
        val (utbetaling, utbetalingId) = opprettGodkjentUtbetaling()
        utbetaling.håndter(utbetalingOverført(utbetalingId, "feil fagsystemId"))
        assertEquals(Utbetaling.Sendt, UtbetalingsInspektør(utbetaling).tilstand)
    }

    @Test
    fun `utbetalingOverført som treffer på arbeidsgiverFagsystemId`() {
        val (utbetaling, utbetalingId) = opprettGodkjentUtbetaling()
        val fagsystemId = utbetaling.arbeidsgiverOppdrag().fagsystemId()
        utbetaling.håndter(utbetalingOverført(utbetalingId, fagsystemId))
        assertEquals(Utbetaling.Overført, UtbetalingsInspektør(utbetaling).tilstand)
    }

    @Test
    fun `utbetalingOverført som treffer på brukerFagsystemId`() {
        val (utbetaling, utbetalingId) = opprettGodkjentUtbetaling()
        val fagsystemId = utbetaling.personOppdrag().fagsystemId()
        utbetaling.håndter(utbetalingOverført(utbetalingId, fagsystemId))
        assertEquals(Utbetaling.Overført, UtbetalingsInspektør(utbetaling).tilstand)
    }

    @Test
    fun `utbetalingOverført som bommer på utbetalingId`() {
        val (utbetaling, _) = opprettGodkjentUtbetaling()
        val fagsystemId = utbetaling.personOppdrag().fagsystemId()
        utbetaling.håndter(utbetalingOverført(UUID.randomUUID(), fagsystemId))
        assertEquals(Utbetaling.Sendt, UtbetalingsInspektør(utbetaling).tilstand)
    }

    @Test
    fun `utbetalingHendelse som treffer på brukeroppdraget`() {
        val (utbetaling, utbetalingId) = opprettGodkjentUtbetaling()
        val fagsystemId = utbetaling.personOppdrag().fagsystemId()
        utbetaling.håndter(utbetalingOverført(UUID.randomUUID(), fagsystemId))
        utbetaling.håndter(utbetalingHendelse(utbetalingId, fagsystemId))
        assertEquals(Utbetaling.Utbetalt, UtbetalingsInspektør(utbetaling).tilstand)
    }

    @Test
    fun `g-regulering skal treffe personOppdrag`() {
        val (utbetalingMedPersonOppdragMatch, _) = opprettGodkjentUtbetaling()
        val personFagsystemId = utbetalingMedPersonOppdragMatch.personOppdrag().fagsystemId()
        val arbeidsgiverFagsystemId = utbetalingMedPersonOppdragMatch.arbeidsgiverOppdrag().fagsystemId()
        val (utbetalingUtenMatch, _) = opprettGodkjentUtbetaling()
        val utbetalinger = listOf(utbetalingUtenMatch, utbetalingMedPersonOppdragMatch)

        val funnetArbeidsgiverUtbetaling = Utbetaling.finnUtbetalingForJustering(utbetalinger, arbeidsgiverFagsystemId.gRegulering())
        assertEquals(utbetalingMedPersonOppdragMatch, funnetArbeidsgiverUtbetaling, "Fant ikke arbeidsgiverutbetaling")

        val funnetUtbetaling = Utbetaling.finnUtbetalingForJustering(utbetalinger, personFagsystemId.gRegulering())
        assertEquals(utbetalingMedPersonOppdragMatch, funnetUtbetaling, "Fant ikke personutbetaling")

        assertNull(Utbetaling.finnUtbetalingForJustering(utbetalinger, "somethingrandom".gRegulering()))
    }

    @Test
    fun `serialiserer avstemmingsnøkkel som null når den ikke er satt`() {
        val tidslinje = tidslinjeOf(16.AP, 15.NAV(dekningsgrunnlag = 1000))
        beregnUtbetalinger(tidslinje)
        val utbetaling = opprettUbetaltUtbetaling(tidslinje)
        assertNull(utbetaling.toMap()["avstemmingsnøkkel"])
    }

    private fun String.gRegulering() = Grunnbeløpsregulering(UUID.randomUUID(), "", "", "", LocalDate.now(), this)

    private fun beregnUtbetalinger(tidslinje: Utbetalingstidslinje) = tidslinje.also {
        MaksimumUtbetaling(
            listOf(tidslinje),
            aktivitetslogg,
            1.januar
        ).betal()
    }

    private fun opprettGodkjentUtbetaling(
        tidslinje: Utbetalingstidslinje = tidslinjeOf(16.AP, 5.NAV(3000)),
        sisteDato: LocalDate = tidslinje.periode().endInclusive,
        fødselsnummer: String = UNG_PERSON_FNR_2018,
        orgnummer: String = ORGNUMMER,
        aktivitetslogg: Aktivitetslogg = this.aktivitetslogg
    ): Pair<Utbetaling, UUID> = opprettUbetaltUtbetaling(beregnUtbetalinger(tidslinje), null, sisteDato, fødselsnummer, orgnummer, aktivitetslogg)
        .also { godkjenn(it) }
        .let { it to UtbetalingsInspektør(it).utbetalingId }

    private fun opprettUbetaltUtbetaling(
        tidslinje: Utbetalingstidslinje,
        tidligere: Utbetaling? = null,
        sisteDato: LocalDate = tidslinje.periode().endInclusive,
        fødselsnummer: String = UNG_PERSON_FNR_2018,
        orgnummer: String = ORGNUMMER,
        aktivitetslogg: Aktivitetslogg = this.aktivitetslogg
    ) = Utbetaling.lagUtbetaling(
        tidligere?.let { listOf(tidligere) } ?: emptyList(),
        fødselsnummer,
        UUID.randomUUID(),
        orgnummer,
        tidslinje,
        sisteDato,
        aktivitetslogg,
        LocalDate.MAX,
        100,
        148
    )

    private fun opprettUtbetaling(
        tidslinje: Utbetalingstidslinje,
        tidligere: Utbetaling? = null,
        sisteDato: LocalDate = tidslinje.periode().endInclusive,
        fødselsnummer: String = UNG_PERSON_FNR_2018,
        orgnummer: String = ORGNUMMER,
        aktivitetslogg: Aktivitetslogg = this.aktivitetslogg
    ) = opprettUbetaltUtbetaling(tidslinje, tidligere, sisteDato, fødselsnummer, orgnummer, aktivitetslogg).also { utbetaling ->
        var utbetalingId: String = ""
        godkjenn(utbetaling).also {
            utbetalingId = it.behov().first { it.type == Behovtype.Utbetaling }.kontekst()["utbetalingId"]
                ?: throw IllegalStateException("Finner ikke utbetalingId i: ${it.behov().first { it.type == Behovtype.Utbetaling }.kontekst()}")
        }
        utbetaling.håndter(
            UtbetalingOverført(
                meldingsreferanseId = UUID.randomUUID(),
                aktørId = "ignore",
                fødselsnummer = "ignore",
                orgnummer = "ignore",
                fagsystemId = utbetaling.arbeidsgiverOppdrag().fagsystemId(),
                utbetalingId = utbetalingId,
                avstemmingsnøkkel = 123456L,
                overføringstidspunkt = LocalDateTime.now()
            )
        )
        utbetaling.håndter(
            UtbetalingHendelse(
                meldingsreferanseId = UUID.randomUUID(),
                aktørId = "ignore",
                fødselsnummer = UNG_PERSON_FNR_2018,
                orgnummer = ORGNUMMER,
                fagsystemId = utbetaling.arbeidsgiverOppdrag().fagsystemId(),
                utbetalingId = utbetalingId,
                status = AKSEPTERT,
                melding = "hei",
                avstemmingsnøkkel = 123456L,
                overføringstidspunkt = LocalDateTime.now()
            )
        )
    }

    private fun godkjenn(utbetaling: Utbetaling) =
        Utbetalingsgodkjenning(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = "ignore",
            fødselsnummer = "ignore",
            organisasjonsnummer = "ignore",
            utbetalingId = utbetaling.toMap()["id"] as UUID,
            vedtaksperiodeId = "ignore",
            saksbehandler = "Z999999",
            saksbehandlerEpost = "mille.mellomleder@nav.no",
            utbetalingGodkjent = true,
            godkjenttidspunkt = LocalDateTime.now(),
            automatiskBehandling = false,
        ).also {
            utbetaling.håndter(it)
        }

    private class OppdragInspektør(oppdrag: Oppdrag) : UtbetalingVisitor {
        private var linjeteller = 0
        private val fagsystemIder = mutableListOf<String>()
        private val totalBeløp = mutableListOf<Int>()
        private val nettoBeløp = mutableListOf<Int>()
        private val delytelseIder = mutableListOf<Int>()
        private val refDelytelseIder = mutableListOf<Int?>()
        private val refFagsystemIder = mutableListOf<String?>()

        init {
            oppdrag.accept(this)
        }

        override fun preVisitOppdrag(
            oppdrag: Oppdrag,
            totalBeløp: Int,
            nettoBeløp: Int,
            tidsstempel: LocalDateTime,
            endringskode: Endringskode,
            avstemmingsnøkkel: Long?,
            status: Oppdragstatus?,
            overføringstidspunkt: LocalDateTime?
        ) {
            fagsystemIder.add(oppdrag.fagsystemId())
            this.nettoBeløp.add(nettoBeløp)
        }

        override fun visitUtbetalingslinje(
            linje: Utbetalingslinje,
            fom: LocalDate,
            tom: LocalDate,
            satstype: Satstype,
            beløp: Int?,
            aktuellDagsinntekt: Int?,
            grad: Double?,
            delytelseId: Int,
            refDelytelseId: Int?,
            refFagsystemId: String?,
            endringskode: Endringskode,
            datoStatusFom: LocalDate?,
            klassekode: Klassekode
        ) {
            linjeteller += 1
            delytelseIder.add(delytelseId)
            refDelytelseIder.add(refDelytelseId)
            refFagsystemIder.add(refFagsystemId)
        }

        fun antallLinjer() = linjeteller
        fun fagSystemId(indeks: Int) = fagsystemIder.elementAt(indeks)
        fun delytelseId(indeks: Int) = delytelseIder.elementAt(indeks)
        fun refDelytelseId(indeks: Int) = refDelytelseIder.elementAt(indeks)
        fun refFagsystemId(indeks: Int) = refFagsystemIder.elementAt(indeks)
        fun totalBeløp(indeks: Int) = totalBeløp.elementAt(indeks)
        fun nettoBeløp(indeks: Int) = nettoBeløp.elementAt(indeks)
    }

    class UtbetalingsInspektør(utbetaling: Utbetaling) : UtbetalingVisitor {
        lateinit var utbetalingId: UUID
        lateinit var tilstand: Utbetaling.Tilstand
        lateinit var arbeidsgiverOppdrag: Oppdrag
        lateinit var personOppdrag: Oppdrag

        init {
            utbetaling.accept(this)
        }

        override fun preVisitUtbetaling(
            utbetaling: Utbetaling,
            id: UUID,
            beregningId: UUID,
            type: Utbetaling.Utbetalingtype,
            tilstand: Utbetaling.Tilstand,
            tidsstempel: LocalDateTime,
            oppdatert: LocalDateTime,
            arbeidsgiverNettoBeløp: Int,
            personNettoBeløp: Int,
            maksdato: LocalDate,
            forbrukteSykedager: Int?,
            gjenståendeSykedager: Int?,
            stønadsdager: Int
        ) {
            utbetalingId = id
            this.tilstand = tilstand
        }

        override fun preVisitArbeidsgiverOppdrag(oppdrag: Oppdrag) {
            this.arbeidsgiverOppdrag = oppdrag
        }

        override fun preVisitPersonOppdrag(oppdrag: Oppdrag) {
            this.personOppdrag = oppdrag
        }
    }

    private fun utbetalingOverført(utbetalingId: UUID, fagsystemId: String) = UtbetalingOverført(
        meldingsreferanseId = UUID.randomUUID(),
        aktørId = "ignore",
        fødselsnummer = "ignore",
        orgnummer = "ignore",
        fagsystemId = fagsystemId,
        utbetalingId = utbetalingId.toString(),
        avstemmingsnøkkel = 123456L,
        overføringstidspunkt = LocalDateTime.now()
    )

    private fun utbetalingHendelse(utbetalingId: UUID, fagsystemId: String) = UtbetalingHendelse(
        meldingsreferanseId = UUID.randomUUID(),
        aktørId = "ignore",
        fødselsnummer = "ignore",
        orgnummer = "ignore",
        fagsystemId = fagsystemId,
        utbetalingId = utbetalingId.toString(),
        status = AKSEPTERT,
        melding = "hei",
        avstemmingsnøkkel = 123456L,
        overføringstidspunkt = LocalDateTime.now()
    )
}
