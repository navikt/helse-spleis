package no.nav.helse.person

import no.nav.helse.hendelser.*
import no.nav.helse.testhelpers.fangeSkjæringstidspunkt
import no.nav.helse.testhelpers.juli
import no.nav.helse.testhelpers.oktober
import no.nav.helse.testhelpers.september
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class VedtaksperiodeTest {

    private val aktør = "1234"
    private val fødselsnummer = "5678"
    private val organisasjonsnummer = "123456789"
    private val person = Person(aktør, fødselsnummer)
    private val arbeidsgiver = Arbeidsgiver(person, organisasjonsnummer)

    @Test
    fun `eksisterende vedtaksperiode godtar ikke søknader som ikke overlapper tidslinje i sykmelding`() {
        val vedtaksperiode = periodeFor(
            sykmelding(perioder = listOf(Sykmeldingsperiode(1.juli, 20.juli, 100.prosent)))
        )

        assertFalse(
            vedtaksperiode.håndter(
                søknad(
                    perioder = listOf(
                        Søknad.Søknadsperiode.Sykdom(21.juli, 25.juli, 100.prosent)
                    )
                )
            )
        )

    }

    @Test
    fun `påminnelse returnerer true basert på om påminnelsen ble håndtert eller ikke`() {
        val id = UUID.randomUUID()
        val vedtaksperiode = periodeFor(sykmelding = sykmelding(), id = id)

        assertFalse(vedtaksperiode.håndter(påminnelse(UUID.randomUUID(), TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP)))
        assertTrue(vedtaksperiode.håndter(påminnelse(id, TilstandType.MOTTATT_SYKMELDING_FERDIG_GAP)))
    }

    @Test
    fun `skjæringstidspunkt stemmer ikke med førsteFraværsdag fra inntektsmelding når den er oppgitt feil`() {
        val førsteFraværsdag = 12.september
        val vedtaksperiode = periodeFor(sykmelding())
        vedtaksperiode.håndter(inntektsmelding(førsteFraværsdag = førsteFraværsdag))

        assertEquals(10.september, fangeSkjæringstidspunkt(vedtaksperiode))
    }

    @Test
    fun `om en inntektsmelding ikke er mottat skal skjæringstidspunkt beregnes`() {
        val vedtaksperiode = periodeFor(
            sykmelding(perioder = listOf(Sykmeldingsperiode(1.juli, 20.juli, 100.prosent)))
        )

        assertEquals(1.juli, fangeSkjæringstidspunkt(vedtaksperiode))
    }

    private fun inntektsmelding(førsteFraværsdag: LocalDate = LocalDate.now()) =
        Inntektsmelding(
            meldingsreferanseId = UUID.randomUUID(),
            refusjon = Inntektsmelding.Refusjon(
                opphørsdato = null,
                inntekt = 1000.månedlig
            ),
            orgnummer = organisasjonsnummer,
            fødselsnummer = fødselsnummer,
            aktørId = aktør,
            førsteFraværsdag = førsteFraværsdag,
            beregnetInntekt = 1000.månedlig,
            arbeidsgiverperioder = listOf(Periode(10.september, 10.september.plusDays(16))),
            ferieperioder = emptyList(),
            arbeidsforholdId = null,
            begrunnelseForReduksjonEllerIkkeUtbetalt = null
        )

    private fun sykmelding(
        fnr: String = fødselsnummer,
        aktørId: String = aktør,
        orgnummer: String = organisasjonsnummer,
        perioder: List<Sykmeldingsperiode> = listOf(Sykmeldingsperiode(16.september, 5.oktober, 100.prosent))
    ) = Sykmelding(
        meldingsreferanseId = UUID.randomUUID(),
        fnr = fnr,
        aktørId = aktørId,
        orgnummer = orgnummer,
        sykeperioder = perioder,
        mottatt = Sykmeldingsperiode.periode(perioder)?.start?.atStartOfDay() ?: LocalDateTime.now()
    )

    private fun søknad(
        perioder: List<Søknad.Søknadsperiode> = listOf(
            Søknad.Søknadsperiode.Sykdom(16.september, 5.oktober, 100.prosent)
        )
    ) =
        Søknad(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = fødselsnummer,
            aktørId = aktør,
            orgnummer = organisasjonsnummer,
            perioder = perioder,
            andreInntektskilder = emptyList(),
            sendtTilNAV = Søknad.Søknadsperiode.søknadsperiode(perioder.toList())!!.endInclusive.atStartOfDay(),
            permittert = false,
            merknaderFraSykmelding = emptyList()
        )

    private fun påminnelse(vedtaksperiodeId: UUID, tilstandType: TilstandType) = Påminnelse(
        meldingsreferanseId = UUID.randomUUID(),
        aktørId = "",
        fødselsnummer = "",
        organisasjonsnummer = "",
        vedtaksperiodeId = vedtaksperiodeId.toString(),
        tilstand = tilstandType,
        antallGangerPåminnet = 1,
        tilstandsendringstidspunkt = LocalDateTime.now(),
        påminnelsestidspunkt = LocalDateTime.now(),
        nestePåminnelsestidspunkt = LocalDateTime.now()
    )

    private fun periodeFor(sykmelding: Sykmelding, id: UUID = UUID.randomUUID()) = Vedtaksperiode(
        person = person,
        arbeidsgiver = arbeidsgiver,
        id = id,
        aktørId = sykmelding.aktørId(),
        fødselsnummer = sykmelding.fødselsnummer(),
        organisasjonsnummer = sykmelding.organisasjonsnummer()
    ).also {
        it.håndter(sykmelding)
    }
}
