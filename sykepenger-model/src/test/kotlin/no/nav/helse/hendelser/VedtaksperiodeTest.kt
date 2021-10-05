package no.nav.helse.hendelser

import no.nav.helse.person.*
import no.nav.helse.person.infotrygdhistorikk.Infotrygdhistorikk
import no.nav.helse.person.infotrygdhistorikk.InfotrygdhistorikkElement
import no.nav.helse.somFødselsnummer
import no.nav.helse.utbetalingstidslinje.Alder
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler.Companion.NormalArbeidstaker
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverUtbetalinger
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class VedtaksperiodeTest {
    private val today: LocalDate = LocalDate.now()

    @Test
    fun `det skal sendes en melding til aktivitetsloggen når validering av perioder i AvventerHistorikk feiler`() {
        val tilstand = Vedtaksperiode.AvventerHistorikk
        val person = Person("1234567891011", "12101078910".somFødselsnummer())
        val arbeidsgiver = Arbeidsgiver(person, "1234567891011")
        val aktivitetslogg = Aktivitetslogg()
        val vedtskaperiode = vedtaksperiode(person, arbeidsgiver)
        val ytelser = ytelser(aktivitetslogg)
        val infotrygdHistorikk = infotrygdHistorikkMedUgyldigePerioder() // <--- valideringsfeil
        val arbeidsgiverUtbetalinger = arbeidsgiverUtbetalinger()

        tilstand.håndter(person, arbeidsgiver, vedtskaperiode, ytelser, infotrygdHistorikk, arbeidsgiverUtbetalinger)

        assertTrue(
            aktivitetslogg.aktiviteter.any {
                it.inOrder().contains("Kaster vedtaksperiode i tilstand 'AvventerHistorikk' ut til Infotrygd pga valideringsfeil")
            }
        )
    }

    private fun vedtaksperiode(person: Person, arbeidsgiver: Arbeidsgiver) = Vedtaksperiode(
        person,
        arbeidsgiver,
        Inntektsmelding(
            UUID.randomUUID(),
            Inntektsmelding.Refusjon(null, null),
            "12345678",
            "12345678910",
            "1234567891011",
            today.plusDays(50),
            Inntekt.INGEN,
            listOf(Periode(today.minusDays(200), today.minusDays(200))),
            null,
            null,
            false,
            LocalDateTime.now()
        )
    )

    private fun ytelser(aktivitetslogg: Aktivitetslogg) = Ytelser(
        UUID.randomUUID(),
        "1234567891011",
        "12345678910",
        "12345678",
        "vedtaksperiodeId",
        null,
        Foreldrepermisjon(null, null, aktivitetslogg),
        Pleiepenger(emptyList(), aktivitetslogg),
        Omsorgspenger(emptyList(), aktivitetslogg),
        Opplæringspenger(emptyList(), aktivitetslogg),
        Institusjonsopphold(emptyList(), aktivitetslogg),
        Dødsinfo(null),
        Arbeidsavklaringspenger(emptyList()),
        Dagpenger(emptyList()),
        aktivitetslogg
    )

    private fun arbeidsgiverUtbetalinger() = ArbeidsgiverUtbetalinger(
        NormalArbeidstaker,
        emptyMap(),
        Utbetalingstidslinje(),
        "04102112345".somFødselsnummer().alder(),
        null,
        VilkårsgrunnlagHistorikk()
    )

    private fun infotrygdHistorikkMedUgyldigePerioder() = Infotrygdhistorikk().apply {
        oppdaterHistorikk(
            InfotrygdhistorikkElement.opprett(
                LocalDateTime.now(),
                UUID.randomUUID(),
                emptyList(),
                emptyList(),
                emptyMap(),
                listOf(Pair(today, today)),
                false
            )
        )
    }

}
