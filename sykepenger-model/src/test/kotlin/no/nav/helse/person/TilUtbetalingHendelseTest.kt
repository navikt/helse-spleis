package no.nav.helse.person

import no.nav.helse.hendelser.*
import no.nav.helse.spleis.e2e.TestArbeidsgiverInspektør
import no.nav.helse.testhelpers.desember
import no.nav.helse.testhelpers.inntektperioder
import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class TilUtbetalingHendelseTest {
    companion object {
        private const val aktørId = "aktørId"
        private const val UNG_PERSON_FNR_2018 = "12020052345"
        private const val ORGNUMMER = "12345"
        private lateinit var førsteSykedag: LocalDate
        private lateinit var sisteSykedag: LocalDate
        private lateinit var sykmeldingHendelseId: UUID
        private lateinit var søknadHendelseId: UUID
        private lateinit var inntektsmeldingHendelseId: UUID
    }

    private lateinit var person: Person
    private val inspektør get() = TestArbeidsgiverInspektør(person)
    private lateinit var hendelse: ArbeidstakerHendelse

    private val utbetaltEvents = mutableListOf<PersonObserver.UtbetaltEvent>()

    private val utbetalingObserver = object : PersonObserver {
        override fun vedtaksperiodeUtbetalt(event: PersonObserver.UtbetaltEvent) {
            utbetaltEvents.add(event)
        }
    }

    @BeforeEach
    internal fun opprettPerson() {
        førsteSykedag = 1.januar
        sisteSykedag = 31.januar
        sykmeldingHendelseId = UUID.randomUUID()
        søknadHendelseId = UUID.randomUUID()
        inntektsmeldingHendelseId = UUID.randomUUID()
        utbetaltEvents.clear()
        person = Person("12345", UNG_PERSON_FNR_2018)
        person.addObserver(utbetalingObserver)
    }

    @Test
    fun `utbetaling er godkjent av saksbehandler`() {
        håndterGodkjenning(0)
        person.håndter(utbetaling(UtbetalingHendelse.Oppdragstatus.AKSEPTERT, 0))
        assertTilstand(TilstandType.AVSLUTTET, 0)

        assertEquals(2, utbetaltEvents.first().oppdrag.size)

        PersonObserver.UtbetaltEvent(
            aktørId = aktørId,
            fødselsnummer = UNG_PERSON_FNR_2018,
            organisasjonsnummer = ORGNUMMER,
            hendelser = setOf(sykmeldingHendelseId, søknadHendelseId, inntektsmeldingHendelseId),
            oppdrag = listOf(
                PersonObserver.UtbetaltEvent.Utbetalt(
                    mottaker = ORGNUMMER,
                    fagområde = "SPREF",
                    fagsystemId = utbetaltEvents.first().oppdrag[0].fagsystemId,
                    totalbeløp = 11 * 1431,
                    utbetalingslinjer = listOf(
                        PersonObserver.UtbetaltEvent.Utbetalt.Utbetalingslinje(
                            fom = 17.januar,
                            tom = 31.januar,
                            dagsats = 1431,
                            beløp = 1431,
                            grad = 100.0,
                            sykedager = 11
                        )
                    )
                ),
                PersonObserver.UtbetaltEvent.Utbetalt(
                    mottaker = UNG_PERSON_FNR_2018,
                    fagområde = "SP",
                    fagsystemId = utbetaltEvents.first().oppdrag[1].fagsystemId,
                    totalbeløp = 0,
                    utbetalingslinjer = emptyList()
                )
            ),
            ikkeUtbetalteDager = emptyList(),
            fom = førsteSykedag,
            tom = sisteSykedag,
            forbrukteSykedager = 11,
            gjenståendeSykedager = 237,
            automatiskBehandling = false,
            opprettet = utbetaltEvents.first().opprettet,
            sykepengegrunnlag = 372000.0,
            maksdato = 28.desember
        ).also {
            assertEquals(it, utbetaltEvents.first())
        }
    }

    @Test
    fun `utbetaling er godkjent automatisk`() {
        håndterGodkjenning(0, true)
        person.håndter(utbetaling(UtbetalingHendelse.Oppdragstatus.AKSEPTERT, 0))
        assertTilstand(TilstandType.AVSLUTTET, 0)

        assertEquals(2, utbetaltEvents.first().oppdrag.size)

        PersonObserver.UtbetaltEvent(
            aktørId = aktørId,
            fødselsnummer = UNG_PERSON_FNR_2018,
            organisasjonsnummer = ORGNUMMER,
            hendelser = setOf(sykmeldingHendelseId, søknadHendelseId, inntektsmeldingHendelseId),
            oppdrag = listOf(
                PersonObserver.UtbetaltEvent.Utbetalt(
                    mottaker = ORGNUMMER,
                    fagområde = "SPREF",
                    fagsystemId = utbetaltEvents.first().oppdrag[0].fagsystemId,
                    totalbeløp = 11 * 1431,
                    utbetalingslinjer = listOf(
                        PersonObserver.UtbetaltEvent.Utbetalt.Utbetalingslinje(
                            fom = 17.januar,
                            tom = 31.januar,
                            dagsats = 1431,
                            beløp = 1431,
                            grad = 100.0,
                            sykedager = 11
                        )
                    )
                ),
                PersonObserver.UtbetaltEvent.Utbetalt(
                    mottaker = UNG_PERSON_FNR_2018,
                    fagområde = "SP",
                    fagsystemId = utbetaltEvents.first().oppdrag[1].fagsystemId,
                    totalbeløp = 0,
                    utbetalingslinjer = emptyList()
                )
            ),
            ikkeUtbetalteDager = emptyList(),
            fom = førsteSykedag,
            tom = sisteSykedag,
            forbrukteSykedager = 11,
            gjenståendeSykedager = 237,
            automatiskBehandling = true,
            opprettet = utbetaltEvents.first().opprettet,
            sykepengegrunnlag = 372000.0,
            maksdato = 28.desember
        ).also {
            assertEquals(it, utbetaltEvents.first())
        }
    }

    @Test
    fun `utbetaling ikke godkjent`() {
        håndterGodkjenning(0)
        person.håndter(utbetaling(UtbetalingHendelse.Oppdragstatus.AVVIST, 0))
        assertTilstand(TilstandType.UTBETALING_FEILET, 0)
        assertTrue(utbetaltEvents.isEmpty())
    }

    private fun assertTilstand(expectedTilstand: TilstandType, index: Int) {
        assertEquals(
            expectedTilstand,
            inspektør.sisteTilstand(index)
        )
    }

    private fun håndterGodkjenning(index: Int, automatiskBehandling: Boolean = false) {
        person.håndter(sykmelding())
        person.håndter(søknad())
        person.håndter(inntektsmelding())
        person.håndter(vilkårsgrunnlag(index))
        person.håndter(ytelser(index = index))
        person.håndter(simulering(index))
        person.håndter(utbetalingsgodkjenning(index, true, automatiskBehandling))
    }

    private fun utbetaling(status: UtbetalingHendelse.Oppdragstatus, index: Int) =
        UtbetalingHendelse(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = inspektør.vedtaksperiodeId(index).toString(),
            aktørId = aktørId,
            fødselsnummer = UNG_PERSON_FNR_2018,
            orgnummer = ORGNUMMER,
            utbetalingsreferanse = "ref",
            status = status,
            melding = "hei"
        ).apply {
            hendelse = this
        }

    private fun utbetalingsgodkjenning(index: Int, godkjent: Boolean, automatiskBehandling: Boolean) = Utbetalingsgodkjenning(
        meldingsreferanseId = UUID.randomUUID(),
        aktørId = aktørId,
        fødselsnummer = UNG_PERSON_FNR_2018,
        organisasjonsnummer = ORGNUMMER,
        vedtaksperiodeId = inspektør.vedtaksperiodeId(index).toString(),
        saksbehandler = "Ola Nordmann",
        utbetalingGodkjent = godkjent,
        godkjenttidspunkt = LocalDateTime.now(),
        automatiskBehandling = automatiskBehandling
    ).apply {
        hendelse = this
    }

    private fun ytelser(
        index: Int,
        utbetalinger: List<Utbetalingshistorikk.Periode> = emptyList(),
        foreldrepengeYtelse: Periode? = null,
        svangerskapYtelse: Periode? = null
    ) = Aktivitetslogg().let {
        val meldingsreferanseId = UUID.randomUUID()
        Ytelser(
            meldingsreferanseId = meldingsreferanseId,
            aktørId = aktørId,
            fødselsnummer = UNG_PERSON_FNR_2018,
            organisasjonsnummer = ORGNUMMER,
            vedtaksperiodeId = inspektør.vedtaksperiodeId(index).toString(),
            utbetalingshistorikk = Utbetalingshistorikk(
                meldingsreferanseId = meldingsreferanseId,
                aktørId = aktørId,
                fødselsnummer = UNG_PERSON_FNR_2018,
                organisasjonsnummer = ORGNUMMER,
                vedtaksperiodeId = inspektør.vedtaksperiodeId(index).toString(),
                utbetalinger = utbetalinger,
                inntektshistorikk = emptyList(),
                aktivitetslogg = it
            ),
            foreldrepermisjon = Foreldrepermisjon(
                foreldrepengeytelse = foreldrepengeYtelse,
                svangerskapsytelse = svangerskapYtelse,
                aktivitetslogg = it
            ),
            pleiepenger = Pleiepenger(
                perioder = emptyList(),
                aktivitetslogg = it
            ),
            institusjonsopphold = Institusjonsopphold(
                perioder = emptyList(),
                aktivitetslogg = it
            ),
            aktivitetslogg = it
        ).apply {
            hendelse = this
        }
    }

    private fun sykmelding() =
        Sykmelding(
            meldingsreferanseId = sykmeldingHendelseId,
            fnr = UNG_PERSON_FNR_2018,
            aktørId = aktørId,
            orgnummer = ORGNUMMER,
            sykeperioder = listOf(Sykmeldingsperiode(førsteSykedag, sisteSykedag, 100)),
            mottatt = førsteSykedag.plusMonths(3).atStartOfDay()
        ).apply {
            hendelse = this
        }

    private fun søknad() =
        Søknad(
            meldingsreferanseId = søknadHendelseId,
            fnr = UNG_PERSON_FNR_2018,
            aktørId = aktørId,
            orgnummer = ORGNUMMER,
            perioder = listOf(Søknad.Søknadsperiode.Sykdom(førsteSykedag, sisteSykedag, 100)),
            harAndreInntektskilder = false,
            sendtTilNAV = sisteSykedag.atStartOfDay(),
            permittert = false
        ).apply {
            hendelse = this
        }

    private fun inntektsmelding() =
        Inntektsmelding(
            meldingsreferanseId = inntektsmeldingHendelseId,
            refusjon = Inntektsmelding.Refusjon(null, 31000.månedlig, emptyList()),
            orgnummer = ORGNUMMER,
            fødselsnummer = UNG_PERSON_FNR_2018,
            aktørId = aktørId,
            førsteFraværsdag = førsteSykedag,
            beregnetInntekt = 31000.månedlig,
            arbeidsgiverperioder = listOf(Periode(førsteSykedag, førsteSykedag.plusDays(16))),
            ferieperioder = emptyList(),
            arbeidsforholdId = null,
            begrunnelseForReduksjonEllerIkkeUtbetalt = null
        ).apply {
            hendelse = this
        }

    private fun vilkårsgrunnlag(index: Int) =
        Vilkårsgrunnlag(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = inspektør.vedtaksperiodeId(index).toString(),
            aktørId = aktørId,
            fødselsnummer = UNG_PERSON_FNR_2018,
            orgnummer = ORGNUMMER,
            inntektsvurdering = Inntektsvurdering(inntektperioder {
                1.januar(2018) til 1.desember(2018) inntekter {
                    ORGNUMMER inntekt 31000.månedlig
                }
            }),
            erEgenAnsatt = false,
            medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Ja),
            opptjeningvurdering = Opptjeningvurdering(
                listOf(
                    Opptjeningvurdering.Arbeidsforhold(
                        ORGNUMMER,
                        1.januar(2017)
                    )
                )
            ),
            dagpenger = Dagpenger(emptyList()),
            arbeidsavklaringspenger = Arbeidsavklaringspenger(emptyList())
        ).apply {
            hendelse = this
        }

    private fun simulering(index: Int) =
        Simulering(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = inspektør.vedtaksperiodeId(index).toString(),
            aktørId = aktørId,
            fødselsnummer = UNG_PERSON_FNR_2018,
            orgnummer = ORGNUMMER,
            simuleringOK = true,
            melding = "",
            simuleringResultat = null
        ).apply {
            hendelse = this
        }
}
