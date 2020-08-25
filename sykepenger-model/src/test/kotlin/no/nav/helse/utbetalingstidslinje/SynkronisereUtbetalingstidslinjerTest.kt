package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Sykmeldingsperiode
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Person
import no.nav.helse.person.inntektsdatoer
import no.nav.helse.testhelpers.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class SynkronisereUtbetalingstidslinjerTest {
    private lateinit var arb1: Arbeidsgiver
    private lateinit var arb2: Arbeidsgiver
    private lateinit var arb3: Arbeidsgiver
    private lateinit var arb4: Arbeidsgiver

    companion object {
        internal const val UNG_PERSON_FNR_2018 = "12020052345"
        const val AKTØRID = "42"
    }

    @BeforeEach
    internal fun setup() {
        val person = Person(AKTØRID, UNG_PERSON_FNR_2018)
        arb1 = Arbeidsgiver(person, "A1")
        arb2 = Arbeidsgiver(person, "A2")
        arb3 = Arbeidsgiver(person, "A3")
        arb4 = Arbeidsgiver(person, "A4")
        arb1.håndter(sykmelding(1.januar to 31.januar, "A1"))
        arb1.håndter(sykmelding(8.april to 31.mai, "A1"))
        arb2.håndter(sykmelding(8.januar to 28.februar, "A2"))
        arb3.håndter(sykmelding(15.januar to 7.februar, "A3"))
        arb4.håndter(sykmelding(1.april to 30.april, "A4"))
        arb4.håndter(sykmelding(1.mai to 8.mai, "A4"))
    }

    @Test
    fun `Inntektsdatoer for flere perioder`() {
        assertEquals(listOf(31.desember(2017), 31.mars), listOf(arb1, arb2, arb3, arb4).inntektsdatoer())
    }

    @Test
    fun ` `(){

    }

//    @Test
//    fun `Inntektsdatoer for flere perioder 2`() {
//        val tidslinjer = ArbeidsgiverUtbetalinger(
//            mapOf(
//                arb1 to tidslinjeOf(31.NAV),
//                arb2 to tidslinjeOf(24.NAV, 28.NAV, startDato = 8.januar),
//                arb3 to tidslinjeOf(17.NAV, 7.NAV, startDato = 15.januar),
//                arb4 to tidslinjeOf(30.NAV, startDato = 1.april)
//            ),
//            tidslinjeOf(),
//            1.januar to 31.januar,
//            Alder(UNG_PERSON_FNR_2018),
//            NormalArbeidstaker,
//            Aktivitetslogg(),
//            arb1.organisasjonsnummer(),
//            UNG_PERSON_FNR_2018
//        )
//        assertEquals(listOf(31.desember(2017), 31.mars), tidslinjer.inntektsdatoer())
//    }

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
