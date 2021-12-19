package no.nav.helse.utbetalingslinjer

import no.nav.helse.hendelser.utbetaling.UtbetalingHendelse
import no.nav.helse.hendelser.utbetaling.UtbetalingOverført
import no.nav.helse.inspectors.inspektør
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.testhelpers.desember
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class OppdragTest {

    @Test
    fun `tomme oppdrag er synkroniserte med hverandre`() {
        val oppdrag1 = Oppdrag("mottaker", Fagområde.SykepengerRefusjon)
        val oppdrag2 = Oppdrag("mottaker", Fagområde.Sykepenger)
        assertTrue(Oppdrag.synkronisert(oppdrag1, oppdrag1))
        assertTrue(Oppdrag.synkronisert(oppdrag1, oppdrag2))
    }

    @Test
    fun `tomme oppdrag er synkroniserte med andre ikke tomme`() {
        val oppdrag1 = Oppdrag("mottaker", Fagområde.SykepengerRefusjon, listOf(
            Utbetalingslinje(
                fom = 1.januar,
                tom = 16.januar,
                endringskode = Endringskode.NY,
                aktuellDagsinntekt = 1000,
                beløp = 1000,
                grad = 100
            )
        ), sisteArbeidsgiverdag = 31.desember(2017))
        val oppdrag2 = Oppdrag("mottaker", Fagområde.Sykepenger)
        assertTrue(Oppdrag.synkronisert(oppdrag1, oppdrag1))
        assertTrue(Oppdrag.synkronisert(oppdrag1, oppdrag2))
    }

    @Test
    fun `oppdrag med status er synkronisert med tomt oppdrag`() {
        val oppdrag1 = Oppdrag("mottaker", Fagområde.SykepengerRefusjon, listOf(
            Utbetalingslinje(
                fom = 1.januar,
                tom = 16.januar,
                endringskode = Endringskode.NY,
                aktuellDagsinntekt = 1000,
                beløp = 1000,
                grad = 100
            )
        ), sisteArbeidsgiverdag = 31.desember(2017))
        val oppdrag2 = Oppdrag("mottaker", Fagområde.Sykepenger)
        oppdrag1.lagreOverføringsinformasjon(UtbetalingOverført(UUID.randomUUID(), "aktør", "fnr", "orgnr", oppdrag1.fagsystemId(), UUID.randomUUID().toString(), 1234L, LocalDateTime.now()))
        assertTrue(Oppdrag.synkronisert(oppdrag1, oppdrag1))
        assertTrue(Oppdrag.synkronisert(oppdrag1, oppdrag2))
    }

    @Test
    fun `oppdrag med ulik status er ikke synkroniserte`() {
        val oppdrag1 = Oppdrag("mottaker", Fagområde.SykepengerRefusjon, listOf(
            Utbetalingslinje(
                fom = 1.januar,
                tom = 16.januar,
                endringskode = Endringskode.NY,
                aktuellDagsinntekt = 1000,
                beløp = 1000,
                grad = 100
            )
        ), sisteArbeidsgiverdag = 31.desember(2017))
        val oppdrag2 = Oppdrag("mottaker", Fagområde.Sykepenger, listOf(
            Utbetalingslinje(
                fom = 1.januar,
                tom = 16.januar,
                endringskode = Endringskode.NY,
                aktuellDagsinntekt = 1000,
                beløp = 1000,
                grad = 100
            )
        ), sisteArbeidsgiverdag = 31.desember(2017))
        oppdrag1.lagreOverføringsinformasjon(UtbetalingOverført(UUID.randomUUID(), "aktør", "fnr", "orgnr", oppdrag1.fagsystemId(), UUID.randomUUID().toString(), 1234L, LocalDateTime.now()))
        assertFalse(Oppdrag.synkronisert(oppdrag1, oppdrag2))
    }

    @Test
    fun `oppdrag med ulik status er ikke synkroniserte 2`() {
        val oppdrag1 = Oppdrag("mottaker", Fagområde.SykepengerRefusjon, listOf(
            Utbetalingslinje(
                fom = 1.januar,
                tom = 16.januar,
                endringskode = Endringskode.NY,
                aktuellDagsinntekt = 1000,
                beløp = 1000,
                grad = 100
            )
        ), sisteArbeidsgiverdag = 31.desember(2017))
        val oppdrag2 = Oppdrag("mottaker", Fagområde.Sykepenger, listOf(
            Utbetalingslinje(
                fom = 1.januar,
                tom = 16.januar,
                endringskode = Endringskode.NY,
                aktuellDagsinntekt = 1000,
                beløp = 1000,
                grad = 100
            )
        ), sisteArbeidsgiverdag = 31.desember(2017))
        oppdrag1.lagreOverføringsinformasjon(UtbetalingOverført(UUID.randomUUID(), "aktør", "fnr", "orgnr", oppdrag1.fagsystemId(), UUID.randomUUID().toString(), 1234L, LocalDateTime.now()))
        oppdrag1.lagreOverføringsinformasjon(UtbetalingHendelse(UUID.randomUUID(), "aktør", "fnr", "orgnr", oppdrag1.fagsystemId(), UUID.randomUUID().toString(), Oppdragstatus.AKSEPTERT, "", 1234L, LocalDateTime.now()))
        oppdrag2.lagreOverføringsinformasjon(UtbetalingOverført(UUID.randomUUID(), "aktør", "fnr", "orgnr", oppdrag2.fagsystemId(), UUID.randomUUID().toString(), 1234L, LocalDateTime.now()))
        assertFalse(Oppdrag.synkronisert(oppdrag1, oppdrag2))
    }

    @Test
    fun `oppdrag med lik status er synkroniserte`() {
        val oppdrag1 = Oppdrag("mottaker", Fagområde.SykepengerRefusjon, listOf(
            Utbetalingslinje(
                fom = 1.januar,
                tom = 16.januar,
                endringskode = Endringskode.NY,
                aktuellDagsinntekt = 1000,
                beløp = 1000,
                grad = 100
            )
        ), sisteArbeidsgiverdag = 31.desember(2017))
        val oppdrag2 = Oppdrag("mottaker", Fagområde.Sykepenger, listOf(
            Utbetalingslinje(
                fom = 1.januar,
                tom = 16.januar,
                endringskode = Endringskode.NY,
                aktuellDagsinntekt = 1000,
                beløp = 1000,
                grad = 100
            )
        ), sisteArbeidsgiverdag = 31.desember(2017))
        oppdrag1.lagreOverføringsinformasjon(UtbetalingOverført(UUID.randomUUID(), "aktør", "fnr", "orgnr", oppdrag1.fagsystemId(), UUID.randomUUID().toString(), 1234L, LocalDateTime.now()))
        oppdrag2.lagreOverføringsinformasjon(UtbetalingOverført(UUID.randomUUID(), "aktør", "fnr", "orgnr", oppdrag2.fagsystemId(), UUID.randomUUID().toString(), 1234L, LocalDateTime.now()))
        assertTrue(Oppdrag.synkronisert(oppdrag1, oppdrag2))
    }

    @Test
    fun `tomt oppdrag ber ikke om simulering (brukerutbetaling)`() {
        val oppdrag = Oppdrag("mottaker", Fagområde.Sykepenger)
        val aktivitetslogg = Aktivitetslogg()
        oppdrag.simuler(aktivitetslogg, 1.januar, "Sara Saksbehandler")
        assertTrue(aktivitetslogg.behov().isEmpty())
    }

    @Test
    fun `nettobeløp for annulleringer`() {
        val oppdrag = Oppdrag(
            "mottaker", Fagområde.Sykepenger, listOf(
                Utbetalingslinje(
                    fom = 1.januar,
                    tom = 16.januar,
                    endringskode = Endringskode.UEND,
                    aktuellDagsinntekt = 1000,
                    beløp = 1000,
                    grad = 100
                )

            ), sisteArbeidsgiverdag = 16.januar
        )
        val annullering = oppdrag.annuller(Aktivitetslogg())
        assertEquals(-1 * oppdrag.totalbeløp(), annullering.nettoBeløp())
    }

    @Test
    fun `uend linjer i oppdrag ber ikke om simulering (brukerutbetaling)`() {
        val oppdrag = Oppdrag(
            "mottaker", Fagområde.Sykepenger, listOf(
                Utbetalingslinje(
                    fom = 1.januar,
                    tom = 16.januar,
                    endringskode = Endringskode.UEND,
                    aktuellDagsinntekt = 1000,
                    beløp = 1000,
                    grad = 100
                )

            ), sisteArbeidsgiverdag = 16.januar
        )
        val aktivitetslogg = Aktivitetslogg()
        oppdrag.simuler(aktivitetslogg, 1.januar, "Sara Saksbehandler")
        assertTrue(aktivitetslogg.behov().isEmpty())
    }

    @Test
    fun `tomt oppdrag ber ikke om overføring (brukerutbetaling)`() {
        val oppdrag = Oppdrag("mottaker", Fagområde.Sykepenger)
        val aktivitetslogg = Aktivitetslogg()
        oppdrag.overfør(aktivitetslogg, 1.januar, "Sara Saksbehandler")
        assertTrue(aktivitetslogg.behov().isEmpty())
    }

    @Test
    fun `tomt oppdrag ber ikke om simulering (arbeidsgiver)`() {
        val oppdrag = Oppdrag("mottaker", Fagområde.SykepengerRefusjon)
        val aktivitetslogg = Aktivitetslogg()
        oppdrag.simuler(aktivitetslogg, 1.januar, "Sara Saksbehandler")
        assertTrue(aktivitetslogg.behov().isEmpty())
    }

    @Test
    fun `tomt oppdrag ber ikke om overføring (arbeidsgiver)`() {
        val oppdrag = Oppdrag("mottaker", Fagområde.SykepengerRefusjon)
        val aktivitetslogg = Aktivitetslogg()
        oppdrag.overfør(aktivitetslogg, 1.januar, "Sara Saksbehandler")
        assertTrue(aktivitetslogg.behov().isEmpty())
    }

    @Test
    fun `er relevant`() {
        val fagsystemId = "a"
        val fagområde = Fagområde.SykepengerRefusjon
        val oppdrag = Oppdrag("mottaker", fagområde, fagsystemId = fagsystemId, sisteArbeidsgiverdag = 1.januar)
        assertTrue(oppdrag.erRelevant(fagsystemId, fagområde))
        assertFalse(oppdrag.erRelevant(fagsystemId, Fagområde.Sykepenger))
        assertFalse(oppdrag.erRelevant("b", fagområde))
    }

    @Test
    fun `simulerer oppdrag med linjer`(){
        val oppdrag = Oppdrag(
            "mottaker", Fagområde.Sykepenger, listOf(
                Utbetalingslinje(
                    fom = 17.januar,
                    tom = 31.januar,
                    endringskode = Endringskode.NY,
                    aktuellDagsinntekt = 1000,
                    beløp = 1000,
                    grad = 100
                )

            ), sisteArbeidsgiverdag = 16.januar
        )
        val aktivitetslogg = Aktivitetslogg()
        oppdrag.simuler(aktivitetslogg, 1.januar, "Sara Saksbehandler")
        assertTrue(aktivitetslogg.behov().isNotEmpty())
    }

    @Test
    fun `overfører oppdrag med linjer`(){
        val oppdrag = Oppdrag(
            "mottaker", Fagområde.Sykepenger, listOf(
                Utbetalingslinje(
                    fom = 17.januar,
                    tom = 31.januar,
                    endringskode = Endringskode.NY,
                    aktuellDagsinntekt = 1000,
                    beløp = 1000,
                    grad = 100
                )

            ), sisteArbeidsgiverdag = 16.januar
        )
        val aktivitetslogg = Aktivitetslogg()
        oppdrag.overfør(aktivitetslogg, 1.januar, "Sara Saksbehandler")
        assertTrue(aktivitetslogg.behov().isNotEmpty())
    }

    @Test
    fun `lagrer avstemmingsnøkkel, overføringstidspunkt og status på oppdraget ved overføring`() {
        val oppdrag = Oppdrag(
            "mottaker", Fagområde.Sykepenger, listOf(
                Utbetalingslinje(
                    fom = 17.januar,
                    tom = 31.januar,
                    endringskode = Endringskode.NY,
                    aktuellDagsinntekt = 1000,
                    beløp = 1000,
                    grad = 100
                )

            ), sisteArbeidsgiverdag = 16.januar
        )
        val avstemmingsnøkkel: Long = 1235
        val overføringstidspunkt = LocalDateTime.now()

        oppdrag.lagreOverføringsinformasjon(UtbetalingOverført(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = "1234",
            fødselsnummer = "1234",
            orgnummer = "5678",
            fagsystemId = oppdrag.fagsystemId(),
            utbetalingId = "7894",
            avstemmingsnøkkel = avstemmingsnøkkel,
            overføringstidspunkt = overføringstidspunkt
        ))

        assertEquals(avstemmingsnøkkel, oppdrag.inspektør.avstemmingsnøkkel)
        assertEquals(overføringstidspunkt, oppdrag.inspektør.overføringstidspunkt)
        assertEquals(Oppdragstatus.OVERFØRT, oppdrag.inspektør.status())
    }

    @Test
    fun `lagrer avstemmingsnøkkel, overføringstidspunkt og status på oppdraget ved akseptert ubetaling`() {
        val oppdrag = Oppdrag(
            "mottaker", Fagområde.Sykepenger, listOf(
                Utbetalingslinje(
                    fom = 17.januar,
                    tom = 31.januar,
                    endringskode = Endringskode.NY,
                    aktuellDagsinntekt = 1000,
                    beløp = 1000,
                    grad = 100
                )

            ), sisteArbeidsgiverdag = 16.januar
        )
        val avstemmingsnøkkel: Long = 1235
        val overføringstidspunkt = LocalDateTime.now()

        oppdrag.lagreOverføringsinformasjon(UtbetalingHendelse(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = "1234",
            fødselsnummer = "1234",
            orgnummer = "5678",
            fagsystemId = oppdrag.fagsystemId(),
            utbetalingId = "7894",
            avstemmingsnøkkel = avstemmingsnøkkel,
            overføringstidspunkt = overføringstidspunkt,
            melding = "foo",
            status = Oppdragstatus.AKSEPTERT
        ))

        assertEquals(avstemmingsnøkkel, oppdrag.inspektør.avstemmingsnøkkel)
        assertEquals(overføringstidspunkt, oppdrag.inspektør.overføringstidspunkt)
        assertEquals(Oppdragstatus.AKSEPTERT, oppdrag.inspektør.status())
    }

    @Test
    fun `overskriver ikke overføringstidspunkt`() {
        val oppdrag = Oppdrag(
            "mottaker", Fagområde.Sykepenger, listOf(
                Utbetalingslinje(
                    fom = 17.januar,
                    tom = 31.januar,
                    endringskode = Endringskode.NY,
                    aktuellDagsinntekt = 1000,
                    beløp = 1000,
                    grad = 100
                )

            ), sisteArbeidsgiverdag = 16.januar
        )
        val avstemmingsnøkkel: Long = 1235
        val overføringstidspunkt = LocalDateTime.now()

        oppdrag.lagreOverføringsinformasjon(UtbetalingOverført(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = "1234",
            fødselsnummer = "1234",
            orgnummer = "5678",
            fagsystemId = oppdrag.fagsystemId(),
            utbetalingId = "7894",
            avstemmingsnøkkel = avstemmingsnøkkel,
            overføringstidspunkt = overføringstidspunkt
        ))

        val aksepteringstidspunkt = overføringstidspunkt.plusSeconds(3)
        oppdrag.lagreOverføringsinformasjon(UtbetalingHendelse(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = "1234",
            fødselsnummer = "1234",
            orgnummer = "5678",
            fagsystemId = oppdrag.fagsystemId(),
            utbetalingId = "7894",
            avstemmingsnøkkel = avstemmingsnøkkel,
            overføringstidspunkt = aksepteringstidspunkt,
            melding = "foo",
            status = Oppdragstatus.AKSEPTERT
        ))

        assertEquals(avstemmingsnøkkel, oppdrag.inspektør.avstemmingsnøkkel)
        assertEquals(overføringstidspunkt, oppdrag.inspektør.overføringstidspunkt)
        assertEquals(Oppdragstatus.AKSEPTERT, oppdrag.inspektør.status())
    }

    @Test
    fun `lagrer ikke avstemmingsnøkkel, overførsingstidspunkt og status på annen fagsystemId`() {
        val oppdrag = Oppdrag(
            "mottaker", Fagområde.Sykepenger, listOf(
                Utbetalingslinje(
                    fom = 17.januar,
                    tom = 31.januar,
                    endringskode = Endringskode.NY,
                    aktuellDagsinntekt = 1000,
                    beløp = 1000,
                    grad = 100
                )

            ), sisteArbeidsgiverdag = 16.januar
        )

        oppdrag.lagreOverføringsinformasjon(UtbetalingOverført(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = "1234",
            fødselsnummer = "1234",
            orgnummer = "5678",
            fagsystemId = "${UUID.randomUUID()}",
            utbetalingId = "7894",
            avstemmingsnøkkel = 6666,
            overføringstidspunkt = LocalDateTime.now()
        ))

        oppdrag.lagreOverføringsinformasjon(UtbetalingHendelse(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = "1234",
            fødselsnummer = "1234",
            orgnummer = "5678",
            fagsystemId = "${UUID.randomUUID()}",
            utbetalingId = "7894",
            avstemmingsnøkkel = 6666,
            overføringstidspunkt = LocalDateTime.now(),
            melding = "foo",
            status = Oppdragstatus.AKSEPTERT
        ))

        assertNull(oppdrag.inspektør.avstemmingsnøkkel)
        assertNull(oppdrag.inspektør.overføringstidspunkt)
        assertNull(oppdrag.inspektør.status())
    }
}
