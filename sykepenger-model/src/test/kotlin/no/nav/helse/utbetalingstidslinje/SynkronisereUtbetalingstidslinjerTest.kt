package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Person
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.testhelpers.*
import org.junit.jupiter.api.BeforeEach
import java.time.LocalDate
import java.util.*

internal class SynkronisereUtbetalingstidslinjerTest {
    private lateinit var arb1: Arbeidsgiver

    companion object {
        internal const val UNG_PERSON_FNR_2018 = "12020052345"
        const val AKTØRID = "42"
    }

    @BeforeEach
    internal fun setup() {
        val person = Person(AKTØRID, UNG_PERSON_FNR_2018)
        arb1 = Arbeidsgiver(person, "A1")
        arb1.håndter(sykmelding(1.januar to 31.januar, "A1"))
        person.håndter(sykmelding(8.januar to 28.februar, "A2"))
        person.håndter(sykmelding(15.januar to 7.februar, "A3"))
        person.håndter(sykmelding(1.april to 30.april, "A4"))
    }

    private fun sykmelding(periode: Periode, orgnummer: String): Sykmelding {
        return Sykmelding(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = AKTØRID,
            orgnummer = orgnummer,
            sykeperioder = listOf(Sykmeldingsperiode(periode.start, periode.endInclusive, 100)),
            mottatt = periode.endInclusive.atStartOfDay()
        )
    }

    private infix fun LocalDate.to(other: LocalDate) = Periode(this, other)
}
