package no.nav.helse.utbetalingslinjer

import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.UtbetalingHendelse.Oppdragstatus.AKSEPTERT
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.helse.person.UtbetalingVisitor
import no.nav.helse.serde.api.SpeilBuilderTest
import no.nav.helse.serde.api.VedtaksperiodeDTO
import no.nav.helse.serde.api.serializePersonForSpeil
import no.nav.helse.serde.reflection.UtbetalingReflect
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingslinjer.Utbetaling.Companion.aktive
import no.nav.helse.utbetalingstidslinje.MaksimumUtbetaling
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class UtbetalingTest {

    private lateinit var aktivitetslogg: Aktivitetslogg

    private companion object {
        private const val UNG_PERSON_FNR_2018 = "12020052345"
        private const val ORGNUMMER = "987654321"
    }

    @BeforeEach
    private fun initEach() {
        aktivitetslogg = Aktivitetslogg()
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
        val annullering = tredje.annuller(AnnullerUtbetaling(UUID.randomUUID(), "", "", "", tredje.arbeidsgiverOppdrag().fagsystemId(), "", "", LocalDateTime.now())) ?: fail {
            "Klarte ikke lage annullering"
        }
        assertEquals(17.januar til 2.februar, annullering.periode)
        assertEquals(17.januar, annullering.arbeidsgiverOppdrag().førstedato)
    }

    @Test
    fun nettoBeløp(){
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
        val andreAnnullert = andre.annuller(AnnullerUtbetaling(UUID.randomUUID(), "", "", "", andre.arbeidsgiverOppdrag().fagsystemId(), "", "", LocalDateTime.now())) ?: fail {
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

    private fun beregnUtbetalinger(vararg tidslinjer: Utbetalingstidslinje) =
        MaksimumUtbetaling(
            listOf(*tidslinjer),
            aktivitetslogg,
            1.januar
        ).betal()

    private fun opprettUtbetaling(
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
    ).also { utbetaling ->
        var utbetalingId: String = ""
        godkjenn(utbetaling).also {
            utbetalingId = it.behov().first { it.type == Behovtype.Utbetaling }.kontekst()["utbetalingId"] ?: throw IllegalStateException("Finner ikke utbetalingId i: ${it.behov().first { it.type == Behovtype.Utbetaling }.kontekst()}")
        }
        utbetaling.håndter(UtbetalingOverført(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = "ignore",
            fødselsnummer = "ignore",
            orgnummer = "ignore",
            fagsystemId = utbetaling.arbeidsgiverOppdrag().fagsystemId(),
            utbetalingId = utbetalingId,
            avstemmingsnøkkel = 123456L,
            overføringstidspunkt = LocalDateTime.now()
        ))
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
            vedtaksperiodeId = "ignore",
            utbetalingId = UtbetalingReflect(utbetaling).toMap()["id"] as UUID,
            saksbehandler = "Z999999",
            saksbehandlerEpost = "mille.mellomleder@nav.no",
            utbetalingGodkjent = true,
            godkjenttidspunkt = LocalDateTime.now(),
            automatiskBehandling = false,
            makstidOppnådd = false,
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
            tidsstempel: LocalDateTime
        ) {
            fagsystemIder.add(oppdrag.fagsystemId())
            this.nettoBeløp.add(nettoBeløp)
        }

        override fun visitUtbetalingslinje(
            linje: Utbetalingslinje,
            fom: LocalDate,
            tom: LocalDate,
            beløp: Int?,
            aktuellDagsinntekt: Int,
            grad: Double,
            delytelseId: Int,
            refDelytelseId: Int?,
            refFagsystemId: String?,
            endringskode: Endringskode,
            datoStatusFom: LocalDate?
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
}
