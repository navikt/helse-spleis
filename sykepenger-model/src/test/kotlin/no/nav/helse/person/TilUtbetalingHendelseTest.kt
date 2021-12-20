package no.nav.helse.person

import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.SendtSøknad.Søknadsperiode.Sykdom
import no.nav.helse.hendelser.utbetaling.UtbetalingHendelse
import no.nav.helse.hendelser.utbetaling.UtbetalingOverført
import no.nav.helse.hendelser.utbetaling.Utbetalingsgodkjenning
import no.nav.helse.inspectors.inspektør
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.UTBETALING_FEILET
import no.nav.helse.person.infotrygdhistorikk.Infotrygdperiode
import no.nav.helse.testhelpers.*
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class TilUtbetalingHendelseTest : AbstractPersonTest() {
    private val SAKSBEHANDLER_IDENT = "O123456"
    private val INNTEKT = 31000.månedlig
    private val førsteSykedag = 1.januar
    private val sisteSykedag = 31.januar
    private val sykmeldingHendelseId = UUID.randomUUID()
    private val søknadHendelseId = UUID.randomUUID()
    private val inntektsmeldingHendelseId = UUID.randomUUID()

    private lateinit var hendelse: ArbeidstakerHendelse

    @Test
    fun `utbetaling er godkjent av saksbehandler`() {
        håndterGodkjenning(SAKSBEHANDLER_IDENT, false)
        person.håndter(utbetaling(Oppdragstatus.AKSEPTERT))
        assertEquals(AVSLUTTET, inspektør.sisteTilstand(1.vedtaksperiode))

        assertEquals(2, observatør.utbetaltEventer.first().oppdrag.size)

        PersonObserver.UtbetaltEvent(
            hendelser = setOf(sykmeldingHendelseId, søknadHendelseId, inntektsmeldingHendelseId),
            utbetalingId = observatør.utbetaltEventer.first().utbetalingId,
            oppdrag = listOf(
                PersonObserver.UtbetaltEvent.Utbetalt(
                    mottaker = ORGNUMMER.toString(),
                    fagområde = "SPREF",
                    fagsystemId = observatør.utbetaltEventer.first().oppdrag[0].fagsystemId,
                    totalbeløp = 11 * 1431,
                    utbetalingslinjer = listOf(
                        PersonObserver.UtbetaltEvent.Utbetalt.Utbetalingslinje(
                            fom = 17.januar,
                            tom = 31.januar,
                            sats = 1431,
                            beløp = 1431,
                            grad = 100,
                            sykedager = 11
                        )
                    )
                ),
                PersonObserver.UtbetaltEvent.Utbetalt(
                    mottaker = UNG_PERSON_FNR_2018.toString(),
                    fagområde = "SP",
                    fagsystemId = observatør.utbetaltEventer.first().oppdrag[1].fagsystemId,
                    totalbeløp = 0,
                    utbetalingslinjer = emptyList()
                )
            ),
            ikkeUtbetalteDager = emptyList(),
            fom = førsteSykedag,
            tom = sisteSykedag,
            forbrukteSykedager = 11,
            gjenståendeSykedager = 237,
            godkjentAv = SAKSBEHANDLER_IDENT,
            automatiskBehandling = false,
            opprettet = observatør.utbetaltEventer.first().opprettet,
            sykepengegrunnlag = INNTEKT.reflection { årlig, _, _, _ -> årlig },
            månedsinntekt = INNTEKT.reflection { _, månedlig, _, _ -> månedlig },
            maksdato = 28.desember
        ).also {
            assertEquals(it, observatør.utbetaltEventer.first())
        }
    }

    @Test
    fun `utbetaling er godkjent automatisk`() {
        håndterGodkjenning("SYSTEM", true)
        person.håndter(utbetaling(Oppdragstatus.AKSEPTERT))
        assertEquals(AVSLUTTET, inspektør.sisteTilstand(1.vedtaksperiode))

        assertEquals(2, observatør.utbetaltEventer.first().oppdrag.size)

        PersonObserver.UtbetaltEvent(
            hendelser = setOf(sykmeldingHendelseId, søknadHendelseId, inntektsmeldingHendelseId),
            utbetalingId = observatør.utbetaltEventer.first().utbetalingId,
            oppdrag = listOf(
                PersonObserver.UtbetaltEvent.Utbetalt(
                    mottaker = ORGNUMMER.toString(),
                    fagområde = "SPREF",
                    fagsystemId = observatør.utbetaltEventer.first().oppdrag[0].fagsystemId,
                    totalbeløp = 11 * 1431,
                    utbetalingslinjer = listOf(
                        PersonObserver.UtbetaltEvent.Utbetalt.Utbetalingslinje(
                            fom = 17.januar,
                            tom = 31.januar,
                            sats = 1431,
                            beløp = 1431,
                            grad = 100,
                            sykedager = 11
                        )
                    )
                ),
                PersonObserver.UtbetaltEvent.Utbetalt(
                    mottaker = UNG_PERSON_FNR_2018.toString(),
                    fagområde = "SP",
                    fagsystemId = observatør.utbetaltEventer.first().oppdrag[1].fagsystemId,
                    totalbeløp = 0,
                    utbetalingslinjer = emptyList()
                )
            ),
            ikkeUtbetalteDager = emptyList(),
            fom = førsteSykedag,
            tom = sisteSykedag,
            forbrukteSykedager = 11,
            gjenståendeSykedager = 237,
            godkjentAv = "SYSTEM",
            automatiskBehandling = true,
            opprettet = observatør.utbetaltEventer.first().opprettet,
            sykepengegrunnlag = INNTEKT.reflection { årlig, _, _, _ -> årlig },
            månedsinntekt = INNTEKT.reflection { _, månedlig, _, _ -> månedlig },
            maksdato = 28.desember
        ).also {
            assertEquals(it, observatør.utbetaltEventer.first())
        }
    }

    @Test
    fun `utbetaling avvist`() {
        håndterGodkjenning()
        person.håndter(utbetaling(Oppdragstatus.AVVIST))
        assertEquals(UTBETALING_FEILET, inspektør.sisteTilstand(1.vedtaksperiode))
        assertTrue(observatør.utbetaltEventer.isEmpty())
    }

    private fun håndterGodkjenning(
        godkjentAv: String = SAKSBEHANDLER_IDENT,
        automatiskBehandling: Boolean = false
    ) {
        person.håndter(sykmelding())
        person.håndter(søknad())
        person.håndter(inntektsmelding())
        person.håndter(ytelser(1.vedtaksperiode))
        person.håndter(vilkårsgrunnlag(1.vedtaksperiode))
        person.håndter(ytelser(1.vedtaksperiode))
        person.håndter(simulering(1.vedtaksperiode))
        person.håndter(utbetalingsgodkjenning(1.vedtaksperiode, true, godkjentAv, automatiskBehandling))
        person.håndter(
            UtbetalingOverført(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR_2018.toString(),
            orgnummer = ORGNUMMER.toString(),
            fagsystemId = inspektør.utbetalinger.last().inspektør.arbeidsgiverOppdrag.fagsystemId(),
            utbetalingId = inspektør.sisteBehov(Behovtype.Utbetaling).kontekst().getValue("utbetalingId").toString(),
            avstemmingsnøkkel = 123456L,
            overføringstidspunkt = LocalDateTime.now()
        )
        )
    }

    private fun utbetaling(status: Oppdragstatus) =
        UtbetalingHendelse(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR_2018.toString(),
            orgnummer = ORGNUMMER.toString(),
            fagsystemId = inspektør.utbetalinger.last().inspektør.arbeidsgiverOppdrag.fagsystemId(),
            utbetalingId = inspektør.sisteBehov(Behovtype.Utbetaling).kontekst().getValue("utbetalingId").toString(),
            status = status,
            melding = "hei",
            avstemmingsnøkkel = 123456L,
            overføringstidspunkt = LocalDateTime.now()
        ).apply {
            hendelse = this
        }

    private fun utbetalingsgodkjenning(
        vedtaksperiodeIdInnhenter: IdInnhenter,
        godkjent: Boolean,
        godkjentAv: String,
        automatiskBehandling: Boolean
    ) = Utbetalingsgodkjenning(
        meldingsreferanseId = UUID.randomUUID(),
        aktørId = AKTØRID,
        fødselsnummer = UNG_PERSON_FNR_2018.toString(),
        organisasjonsnummer = ORGNUMMER.toString(),
        utbetalingId = UUID.fromString(inspektør.sisteBehov(Behovtype.Godkjenning).kontekst()["utbetalingId"] ?: throw IllegalStateException("Finner ikke utbetalingId i: ${inspektør.sisteBehov(
            Behovtype.Godkjenning
        ).kontekst()}")),
        vedtaksperiodeId = "${vedtaksperiodeIdInnhenter(ORGNUMMER)}",
        saksbehandler = godkjentAv,
        saksbehandlerEpost = "mille.mellomleder@nav.no",
        utbetalingGodkjent = godkjent,
        godkjenttidspunkt = LocalDateTime.now(),
        automatiskBehandling = automatiskBehandling,
    ).apply {
        hendelse = this
    }

    private fun ytelser(
        vedtaksperiodeIdInnhenter: IdInnhenter,
        utbetalinger: List<Infotrygdperiode> = emptyList(),
        foreldrepengeYtelse: Periode? = null,
        svangerskapYtelse: Periode? = null
    ) = Aktivitetslogg().let {
        val meldingsreferanseId = UUID.randomUUID()
        Ytelser(
            meldingsreferanseId = meldingsreferanseId,
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR_2018.toString(),
            organisasjonsnummer = ORGNUMMER.toString(),
            vedtaksperiodeId = "${vedtaksperiodeIdInnhenter(ORGNUMMER)}",
            utbetalingshistorikk = Utbetalingshistorikk(
                meldingsreferanseId = meldingsreferanseId,
                aktørId = AKTØRID,
                fødselsnummer = UNG_PERSON_FNR_2018.toString(),
                organisasjonsnummer = ORGNUMMER.toString(),
                vedtaksperiodeId = "${vedtaksperiodeIdInnhenter(ORGNUMMER)}",
                arbeidskategorikoder = emptyMap(),
                harStatslønn = false,
                perioder = utbetalinger,
                inntektshistorikk = emptyList(),
                ugyldigePerioder = emptyList(),
                aktivitetslogg = it,
                besvart = LocalDateTime.now()
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
            omsorgspenger = Omsorgspenger(
                perioder = emptyList(),
                aktivitetslogg = it
            ),
            opplæringspenger = Opplæringspenger(
                perioder = emptyList(),
                aktivitetslogg = it
            ),
            institusjonsopphold = Institusjonsopphold(
                perioder = emptyList(),
                aktivitetslogg = it
            ),
            dødsinfo = Dødsinfo(null),
            arbeidsavklaringspenger = Arbeidsavklaringspenger(emptyList()),
            dagpenger = Dagpenger(emptyList()),
            aktivitetslogg = it
        ).apply {
            hendelse = this
        }
    }

    private fun sykmelding() =
        Sykmelding(
            meldingsreferanseId = sykmeldingHendelseId,
            fnr = UNG_PERSON_FNR_2018.toString(),
            aktørId = AKTØRID,
            orgnummer = ORGNUMMER.toString(),
            sykeperioder = listOf(Sykmeldingsperiode(førsteSykedag, sisteSykedag, 100.prosent)),
            sykmeldingSkrevet = førsteSykedag.atStartOfDay(),
            mottatt = sisteSykedag.atStartOfDay()
        ).apply {
            hendelse = this
        }

    private fun søknad() =
        Søknad(
            meldingsreferanseId = søknadHendelseId,
            fnr = UNG_PERSON_FNR_2018.toString(),
            aktørId = AKTØRID,
            orgnummer = ORGNUMMER.toString(),
            perioder = listOf(Sykdom(førsteSykedag, sisteSykedag, 100.prosent)),
            andreInntektskilder = emptyList(),
            sendtTilNAV = sisteSykedag.atStartOfDay(),
            permittert = false,
            merknaderFraSykmelding = emptyList(),
            sykmeldingSkrevet = LocalDateTime.now()
        ).apply {
            hendelse = this
        }

    private fun inntektsmelding() =
        Inntektsmelding(
            meldingsreferanseId = inntektsmeldingHendelseId,
            refusjon = Inntektsmelding.Refusjon(INNTEKT, null, emptyList()),
            orgnummer = ORGNUMMER.toString(),
            fødselsnummer = UNG_PERSON_FNR_2018.toString(),
            aktørId = AKTØRID,
            førsteFraværsdag = førsteSykedag,
            beregnetInntekt = INNTEKT,
            arbeidsgiverperioder = listOf(Periode(førsteSykedag, førsteSykedag.plusDays(16))),
            arbeidsforholdId = null,
            begrunnelseForReduksjonEllerIkkeUtbetalt = null,
            mottatt = LocalDateTime.now()
        ).apply {
            hendelse = this
        }

    private fun vilkårsgrunnlag(vedtaksperiodeIdInnhenter: IdInnhenter) =
        Vilkårsgrunnlag(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = "${vedtaksperiodeIdInnhenter(ORGNUMMER)}",
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR_2018,
            orgnummer = ORGNUMMER.toString(),
            inntektsvurdering = Inntektsvurdering(inntektperioderForSammenligningsgrunnlag {
                1.januar(2017) til 1.desember(2017) inntekter {
                    ORGNUMMER inntekt INNTEKT
                }
            }),
            medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Ja),
            opptjeningvurdering = Opptjeningvurdering(
                listOf(
                    Arbeidsforhold(
                        ORGNUMMER.toString(),
                        1.januar(2017)
                    )
                )
            ),
            inntektsvurderingForSykepengegrunnlag = InntektForSykepengegrunnlag(
                inntektperioderForSykepengegrunnlag {
                    1.oktober(2017) til 1.desember(2017) inntekter {
                        ORGNUMMER inntekt 31000.månedlig
                    }
                }),
            arbeidsforhold = listOf(
                Arbeidsforhold(
                    ORGNUMMER.toString(),
                    1.januar(2017)
                )
            )
        ).apply {
            hendelse = this
        }

    private fun simulering(vedtaksperiodeIdInnhenter: IdInnhenter) =
        Simulering(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = "${vedtaksperiodeIdInnhenter(ORGNUMMER)}",
            aktørId = AKTØRID,
            fødselsnummer = UNG_PERSON_FNR_2018.toString(),
            orgnummer = ORGNUMMER.toString(),
            fagsystemId = hendelse.behov().first { it.type == Behovtype.Simulering }.detaljer().getValue("fagsystemId") as String,
            fagområde = hendelse.behov().first { it.type == Behovtype.Simulering }.detaljer().getValue("fagområde") as String,
            simuleringOK = true,
            melding = "",
            simuleringResultat = Simulering.SimuleringResultat(
                totalbeløp = 1000,
                perioder = emptyList()
            ),
            utbetalingId = UUID.fromString(hendelse.behov().first { it.type == Behovtype.Simulering }.kontekst().getValue("utbetalingId"))
        ).apply {
            hendelse = this
        }
}
