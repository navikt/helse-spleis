package no.nav.helse.person

import no.nav.helse.hendelser.*
import no.nav.helse.juli
import no.nav.helse.oktober
import no.nav.helse.september
import no.nav.helse.testhelpers.april
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
    fun `eksisterende vedtaksperiode godtar ikke søknader som ikke overlapper tidslinje i sendt søknad`() {
        val vedtaksperiode = periodeFor(
            nySøknad(perioder = listOf(Triple(1.juli, 20.juli, 100)))
        )

        assertFalse(
            vedtaksperiode.håndter(
                sendtSøknad(
                    perioder = listOf(
                        SendtSøknad.Periode.Sykdom(
                            fom = 21.juli,
                            tom = 25.juli,
                            grad = 100
                        )
                    )
                )
            )
        )

    }

    @Test
    fun `påminnelse returnerer true basert på om påminnelsen ble håndtert eller ikke`() {
        val id = UUID.randomUUID()
        val vedtaksperiode = periodeFor(nySøknad = nySøknad(), id = id)

        assertFalse(vedtaksperiode.håndter(påminnelse(UUID.randomUUID(), TilstandType.MOTTATT_NY_SØKNAD)))
        assertTrue(vedtaksperiode.håndter(påminnelse(id, TilstandType.MOTTATT_NY_SØKNAD)))
    }

    @Test
    fun `første fraversdag skal returnere første fraversdag fra inntektsmelding`() {
        val førsteFraværsdag = 20.april
        val vedtaksperiode = periodeFor(nySøknad())
        vedtaksperiode.håndter(inntektsmelding(førsteFraværsdag = førsteFraværsdag))

        assertEquals(førsteFraværsdag, førsteFraværsdag(vedtaksperiode))
    }

    @Test
    fun `om en inntektsmelding ikke er mottat skal første fraværsdag returnere null`() {
        val vedtaksperiode = periodeFor(
            nySøknad(perioder = listOf(Triple(1.juli, 20.juli, 100)))
        )

        assertNull(førsteFraværsdag(vedtaksperiode))
    }

    private fun førsteFraværsdag(vedtaksperiode: Vedtaksperiode): LocalDate? {
        var _førsteFraværsdag: LocalDate? = null
        vedtaksperiode.accept(object : VedtaksperiodeVisitor {
            override fun visitFørsteFraværsdag(førsteFraværsdag: LocalDate?) {
                _førsteFraværsdag = førsteFraværsdag
            }
        })
        return _førsteFraværsdag
    }

    private fun inntektsmelding(førsteFraværsdag: LocalDate = LocalDate.now()) =
        Inntektsmelding(
            hendelseId = UUID.randomUUID(),
            refusjon = Inntektsmelding.Refusjon(
                opphørsdato = LocalDate.now(),
                beløpPrMåned = 1000.0
            ),
            orgnummer = organisasjonsnummer,
            fødselsnummer = fødselsnummer,
            aktørId = aktør,
            mottattDato = LocalDateTime.now(),
            førsteFraværsdag = førsteFraværsdag,
            beregnetInntekt = 1000.0,
            aktivitetslogger = Aktivitetslogger(),
            aktivitetslogg = Aktivitetslogg(),
            arbeidsgiverperioder = listOf(Periode(10.september, 10.september.plusDays(16))),
            ferieperioder = emptyList()
        )

    private fun nySøknad(
        fnr: String = fødselsnummer,
        aktørId: String = aktør,
        orgnummer: String = organisasjonsnummer,
        perioder: List<Triple<LocalDate, LocalDate, Int>> = listOf(Triple(16.september, 5.oktober, 100))
    ) = NySøknad(
        hendelseId = UUID.randomUUID(),
        fnr = fnr,
        aktørId = aktørId,
        orgnummer = orgnummer,
        rapportertdato = LocalDateTime.now(),
        sykeperioder = perioder,
        aktivitetslogger = Aktivitetslogger(),
        aktivitetslogg = Aktivitetslogg()
    )

    private fun sendtSøknad(
        perioder: List<SendtSøknad.Periode> = listOf(
            SendtSøknad.Periode.Sykdom(
                16.september,
                5.oktober,
                100
            )
        ), rapportertDato: LocalDateTime = LocalDateTime.now()
    ) =
        SendtSøknad(
            hendelseId = UUID.randomUUID(),
            fnr = fødselsnummer,
            aktørId = aktør,
            orgnummer = organisasjonsnummer,
            sendtNav = rapportertDato,
            perioder = perioder,
            aktivitetslogger = Aktivitetslogger(),
            aktivitetslogg = Aktivitetslogg(),
            harAndreInntektskilder = false
        )

    private fun påminnelse(vedtaksperiodeId: UUID, tilstandType: TilstandType) = Påminnelse(
        hendelseId = UUID.randomUUID(),
        aktørId = "",
        fødselsnummer = "",
        organisasjonsnummer = "",
        vedtaksperiodeId = vedtaksperiodeId.toString(),
        tilstand = tilstandType,
        antallGangerPåminnet = 1,
        tilstandsendringstidspunkt = LocalDateTime.now(),
        påminnelsestidspunkt = LocalDateTime.now(),
        nestePåminnelsestidspunkt = LocalDateTime.now(),
        aktivitetslogger = Aktivitetslogger(),
        aktivitetslogg = Aktivitetslogg()
    )

    private fun periodeFor(nySøknad: NySøknad, id: UUID = UUID.randomUUID()) = Vedtaksperiode(
        person = person,
        arbeidsgiver = arbeidsgiver,
        id = id,
        aktørId = nySøknad.aktørId(),
        fødselsnummer = nySøknad.fødselsnummer(),
        organisasjonsnummer = nySøknad.organisasjonsnummer()
    ).also {
        it.håndter(nySøknad)
    }
}
