package no.nav.helse.person

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.UtbetalingHendelse
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.mars
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

class ReplayHendelserTest : HendelseTestHelper() {

    @BeforeEach
    internal fun opprettPerson() {
        førsteSykedag = 1.januar
        sisteSykedag = 31.januar
        replayEvents.clear()
        person = Person("12345", UNG_PERSON_FNR_2018)
        person.addObserver(replayObserver)
    }


    private val replayEvents = mutableListOf<PersonObserver.VedtaksperiodeReplayEvent>()
    private val hendelseIderMap = mutableMapOf<Int, List<UUID>>()
    private val vedtaksperiodeTeller = 0

    private val replayObserver = object : PersonObserver {
        override fun vedtaksperiodeReplay(event: PersonObserver.VedtaksperiodeReplayEvent) {
            replayEvents.add(event)
        }
    }

    private val personVisitor = object : PersonVisitor {
        override fun preVisitVedtaksperiode(
            vedtaksperiode: Vedtaksperiode,
            id: UUID,
            arbeidsgiverNettoBeløp: Int,
            personNettoBeløp: Int,
            periode: Periode,
            opprinneligPeriode: Periode,
            hendelseIder: List<UUID>
        ) {
            hendelseIderMap[vedtaksperiodeTeller] = hendelseIder
            vedtaksperiodeTeller.inc()
        }
    }

    @Test
    fun `ny, tidligere sykmelding medfører umiddelbar replay av etterfølgende perioder som ikke er avsluttet eller til utbetaling`() {
        person.håndter(sykmelding(1.mars, 31.mars))
        person.håndter(søknad(1.mars, 31.mars))

        håndterGodkjenning(0)
        person.accept(personVisitor)

        assertEquals(1, replayEvents.size)
        assertEquals(replayEvents[0].hendelseIder[0], hendelseIderMap[0]?.get(0))
        assertEquals(replayEvents[0].hendelseIder[1], hendelseIderMap[0]?.get(1))
    }

    @Test
    fun `ny, tidligere sykmelding medfører replay av etterfølgende perioder som er avsluttet eller til utbetaling først når ny periode er utbetalt`() {
        håndterGodkjenning(0, 2.februar, 28.februar)
        person.håndter(utbetaling(UtbetalingHendelse.Oppdragstatus.AKSEPTERT, 0))

        håndterGodkjenning(0)
        person.accept(personVisitor)

        assertEquals(0, replayEvents.size)

        person.håndter(utbetaling(UtbetalingHendelse.Oppdragstatus.AKSEPTERT, 0))
        person.accept(personVisitor)

        assertEquals(1, replayEvents.size)
        assertEquals(replayEvents[0].hendelseIder[0], hendelseIderMap[0]?.get(0))
        assertEquals(replayEvents[0].hendelseIder[1], hendelseIderMap[0]?.get(1))
    }
}
