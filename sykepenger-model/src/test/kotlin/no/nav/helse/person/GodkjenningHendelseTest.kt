package no.nav.helse.person

import no.nav.helse.hendelser.*
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.TilstandType.TIL_UTBETALING
import no.nav.helse.person.infotrygdhistorikk.Infotrygdperiode
import no.nav.helse.testhelpers.desember
import no.nav.helse.testhelpers.inntektperioder
import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class GodkjenningHendelseTest : AbstractPersonTest() {
    private companion object {
        private val førsteSykedag = 1.januar
        private val sisteSykedag = 31.januar
    }

    private lateinit var hendelse: ArbeidstakerHendelse

    @Test
    fun `utbetaling er godkjent`() {
        håndterYtelser()
        person.håndter(simulering())
        person.håndter(utbetalingsgodkjenning(true))
        assertEquals(TIL_UTBETALING, inspektør.sisteTilstand(1.vedtaksperiode))
    }

    @Test
    fun `utbetaling ikke godkjent`() {
        håndterYtelser()
        person.håndter(simulering())
        person.håndter(utbetalingsgodkjenning(false))
        assertTrue(inspektør.periodeErForkastet(1.vedtaksperiode))
        assertEquals(TIL_INFOTRYGD, inspektør.sisteTilstand(1.vedtaksperiode))
    }

    @Test
    fun `hendelse etter til utbetaling`() {
        håndterYtelser()
        person.håndter(simulering())
        person.håndter(utbetalingsgodkjenning(true))
        assertEquals(TIL_UTBETALING, inspektør.sisteTilstand(1.vedtaksperiode))
        person.håndter(ytelser())
        assertEquals(TIL_UTBETALING, inspektør.sisteTilstand(1.vedtaksperiode))
        assertEquals(1, inspektør.vedtaksperiodeTeller)
    }

    @Test
    fun `dobbelt svar fra saksbehandler`() {
        håndterYtelser()
        person.håndter(simulering())
        person.håndter(utbetalingsgodkjenning(true))
        person.håndter(utbetalingsgodkjenning(true))
        assertEquals(TIL_UTBETALING, inspektør.sisteTilstand(1.vedtaksperiode))
    }

    @Test
    fun `legger på periodetype på utgående godkjenningsbehov`() {
        håndterYtelser()
        person.håndter(simulering())
        assertEquals(1, hendelse.behov().size)
        assertEquals(Periodetype.FØRSTEGANGSBEHANDLING.name, hendelse.behov().first().detaljer()["periodetype"])
        person.håndter(utbetalingsgodkjenning(true))
    }

    private fun håndterYtelser() {
        person.håndter(sykmelding())
        person.håndter(søknad())
        person.håndter(inntektsmelding())
        person.håndter(ytelser())
        person.håndter(vilkårsgrunnlag())
        person.håndter(ytelser())
    }

    private fun utbetalingsgodkjenning(godkjent: Boolean) = Utbetalingsgodkjenning(
        meldingsreferanseId = UUID.randomUUID(),
        aktørId = "aktørId",
        fødselsnummer = UNG_PERSON_FNR_2018,
        organisasjonsnummer = ORGNUMMER,
        utbetalingId = UUID.fromString(inspektør.sisteBehov(Aktivitetslogg.Aktivitet.Behov.Behovtype.Godkjenning).kontekst()["utbetalingId"] ?: throw IllegalStateException("Finner ikke utbetalingId i: ${inspektør.sisteBehov(
            Aktivitetslogg.Aktivitet.Behov.Behovtype.Godkjenning
        ).kontekst()}")),
        vedtaksperiodeId = "${1.vedtaksperiode}",
        saksbehandler = "Ola Nordmann",
        saksbehandlerEpost = "ola@nordmann.ss",
        utbetalingGodkjent = godkjent,
        godkjenttidspunkt = LocalDateTime.now(),
        automatiskBehandling = false,
    ).apply {
        hendelse = this
    }

    private fun ytelser(
        utbetalinger: List<Infotrygdperiode> = emptyList(),
        foreldrepengeYtelse: Periode? = null,
        svangerskapYtelse: Periode? = null
    ) = Aktivitetslogg().let {
        val meldingsreferanseId = UUID.randomUUID()
        Ytelser(
            meldingsreferanseId = meldingsreferanseId,
            aktørId = "aktørId",
            fødselsnummer = UNG_PERSON_FNR_2018,
            organisasjonsnummer = ORGNUMMER,
            vedtaksperiodeId = "${1.vedtaksperiode}",
            utbetalingshistorikk = Utbetalingshistorikk(
                meldingsreferanseId = meldingsreferanseId,
                aktørId = "aktørId",
                fødselsnummer = UNG_PERSON_FNR_2018,
                organisasjonsnummer = ORGNUMMER,
                vedtaksperiodeId = "${1.vedtaksperiode}",
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
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = "aktørId",
            orgnummer = ORGNUMMER,
            sykeperioder = listOf(Sykmeldingsperiode(førsteSykedag, sisteSykedag, 100.prosent)),
            mottatt = førsteSykedag.plusMonths(3).atStartOfDay()
        ).apply {
            hendelse = this
        }

    private fun søknad() =
        Søknad(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = "aktørId",
            orgnummer = ORGNUMMER,
            perioder = listOf(Søknad.Søknadsperiode.Sykdom(førsteSykedag, sisteSykedag, 100.prosent)),
            andreInntektskilder = emptyList(),
            sendtTilNAV = sisteSykedag.atStartOfDay(),
            permittert = false,
            merknaderFraSykmelding = emptyList()
        ).apply {
            hendelse = this
        }

    private fun inntektsmelding() =
        Inntektsmelding(
            meldingsreferanseId = UUID.randomUUID(),
            refusjon = Inntektsmelding.Refusjon(null, 31000.månedlig, emptyList()),
            orgnummer = ORGNUMMER,
            fødselsnummer = UNG_PERSON_FNR_2018,
            aktørId = "aktørId",
            førsteFraværsdag = førsteSykedag,
            beregnetInntekt = 31000.månedlig,
            arbeidsgiverperioder = listOf(Periode(førsteSykedag, førsteSykedag.plusDays(16))),
            ferieperioder = emptyList(),
            arbeidsforholdId = null,
            begrunnelseForReduksjonEllerIkkeUtbetalt = null
        ).apply {
            hendelse = this
        }

    private fun vilkårsgrunnlag() =
        Vilkårsgrunnlag(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = "${1.vedtaksperiode}",
            aktørId = "aktørId",
            fødselsnummer = UNG_PERSON_FNR_2018,
            orgnummer = ORGNUMMER,
            inntektsvurdering = Inntektsvurdering(
                inntektperioder {
                    inntektsgrunnlag = Inntektsvurdering.Inntektsgrunnlag.SAMMENLIGNINGSGRUNNLAG
                    1.januar(2017) til 1.desember(2017) inntekter {
                        ORGNUMMER inntekt 31000.månedlig
                    }
                }),
            medlemskapsvurdering = Medlemskapsvurdering(Medlemskapsvurdering.Medlemskapstatus.Ja),
            opptjeningvurdering = Opptjeningvurdering(
                listOf(
                    Opptjeningvurdering.Arbeidsforhold(
                        ORGNUMMER,
                        1.januar(2017)
                    )
                )
            )
        ).apply {
            hendelse = this
        }

    private fun simulering() =
        Simulering(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = "${1.vedtaksperiode}",
            aktørId = "aktørId",
            fødselsnummer = UNG_PERSON_FNR_2018,
            orgnummer = ORGNUMMER,
            simuleringOK = true,
            melding = "",
            simuleringResultat = null
        ).apply {
            hendelse = this
        }
}
