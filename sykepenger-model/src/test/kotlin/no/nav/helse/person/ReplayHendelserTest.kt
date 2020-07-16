package no.nav.helse.person

import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.spleis.e2e.TestArbeidsgiverInspektør
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class ReplayHendelserTest {
    companion object {
        private const val aktørId = "aktørId"
        private const val UNG_PERSON_FNR_2018 = "12020052345"
        private const val ORGNUMMER = "12345"
        private lateinit var førsteSykedag: LocalDate
        private lateinit var sisteSykedag: LocalDate
    }

    private lateinit var person: Person
    private val inspektør get() = TestArbeidsgiverInspektør(person)
    private lateinit var hendelse: ArbeidstakerHendelse

    private val replayEvents = mutableListOf<PersonObserver.VedtaksperiodeReplayEvent>()

    private val replayObserver = object : PersonObserver {
        override fun vedtaksperiodeReplay(event: PersonObserver.VedtaksperiodeReplayEvent) {
            replayEvents.add(event)
        }
    }

    @BeforeEach
    internal fun opprettPerson() {
        førsteSykedag = 1.januar
        sisteSykedag = 31.januar
        replayEvents.clear()
        person = Person("12345", UNG_PERSON_FNR_2018)
        person.addObserver(replayObserver)
    }

    @Test
    fun `test`() {
        person.håndter(sykmelding(Sykmeldingsperiode(1.februar, 28.februar, 100)))
        person.håndter(sykmelding(Sykmeldingsperiode(1.januar, 31.januar, 100)))
        assertEquals(1, replayEvents.size)
    }

    private fun sykmelding(vararg sykeperioder: Sykmeldingsperiode, orgnummer: String = "987654321") =
        Sykmelding(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = "12345",
            orgnummer = orgnummer,
            sykeperioder = listOf(*sykeperioder),
            mottatt = sykeperioder.map { it.fom }.min()?.atStartOfDay() ?: LocalDateTime.now()
        )
}
