package no.nav.helse.spleis.e2e.testcontext

import no.nav.helse.hendelser.*
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.spleis.e2e.AbstractEndToEndTest
import no.nav.helse.testhelpers.desember
import no.nav.helse.testhelpers.inntektperioder
import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class Testhendelsefabrikk(
    private val fødselsnummer: String,
    private val aktørId: String,
    private val organisasjonsnummer: String,
    private val inntekt: Inntekt = INNTEKT
) {
    internal companion object {
        val INNTEKT = 31000.00.månedlig
    }

    fun sykmelding(
        vararg sykeperioder: Sykmeldingsperiode,
        id: UUID = UUID.randomUUID(),
        mottatt: LocalDateTime? = null
    ): Sykmelding {
        return Sykmelding(
            meldingsreferanseId = id,
            fnr = fødselsnummer,
            aktørId = aktørId,
            orgnummer = organisasjonsnummer,
            sykeperioder = listOf(*sykeperioder),
            mottatt = mottatt ?: Sykmeldingsperiode.periode(sykeperioder.toList())?.start?.atStartOfDay() ?: LocalDateTime.now()
        )
    }

    fun søknad(
        vararg perioder: Søknad.Søknadsperiode,
        andreInntektskilder: List<Søknad.Inntektskilde> = emptyList(),
        sendtTilNav: LocalDate = Søknad.Søknadsperiode.søknadsperiode(perioder.toList())!!.endInclusive,
        id: UUID = UUID.randomUUID()
    ): Søknad {
        return Søknad(
            meldingsreferanseId = id,
            fnr = fødselsnummer,
            aktørId = aktørId,
            orgnummer = organisasjonsnummer,
            perioder = listOf(*perioder),
            andreInntektskilder = andreInntektskilder,
            sendtTilNAV = sendtTilNav.atStartOfDay(),
            permittert = false,
            merknaderFraSykmelding = emptyList()
        )
    }

    internal fun inntektsmelding(
        id: UUID,
        arbeidsgiverperioder: List<Periode>,
        ferieperioder: List<Periode> = emptyList(),
        beregnetInntekt: Inntekt = AbstractEndToEndTest.INNTEKT,
        førsteFraværsdag: LocalDate = arbeidsgiverperioder.maxOfOrNull { it.start } ?: 1.januar,
        refusjon: Triple<LocalDate?, Inntekt, List<LocalDate>> = Triple(null, beregnetInntekt, emptyList()),
        harOpphørAvNaturalytelser: Boolean = false
    ): Inntektsmelding {
        return Inntektsmelding(
            meldingsreferanseId = id,
            refusjon = Inntektsmelding.Refusjon(refusjon.first, refusjon.second, refusjon.third),
            orgnummer = organisasjonsnummer,
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            førsteFraværsdag = førsteFraværsdag,
            beregnetInntekt = beregnetInntekt,
            arbeidsgiverperioder = arbeidsgiverperioder,
            ferieperioder = ferieperioder,
            arbeidsforholdId = null,
            begrunnelseForReduksjonEllerIkkeUtbetalt = null,
            harOpphørAvNaturalytelser = harOpphørAvNaturalytelser
        )
    }

    fun utbetalingshistorikk(
        vedtaksperiodeId: UUID,
        utbetalinger: List<Utbetalingshistorikk.Infotrygdperiode> = listOf(),
        inntektshistorikk: List<Utbetalingshistorikk.Inntektsopplysning>? = null,
    ): Utbetalingshistorikk {
        return Utbetalingshistorikk(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId.toString(),
            arbeidskategorikoder = emptyMap(),
            utbetalinger = utbetalinger,
            inntektshistorikk = inntektshistorikk ?: standardinntekter()
        )
    }

    fun vilkårsgrunnlag(
        vedtaksperiodeId: UUID,
        arbeidsforhold: List<Opptjeningvurdering.Arbeidsforhold> = emptyList(),
        medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus = Medlemskapsvurdering.Medlemskapstatus.Ja,
        inntektsvurdering: Inntektsvurdering? = null
    ): Vilkårsgrunnlag {
        return Vilkårsgrunnlag(
            meldingsreferanseId = UUID.randomUUID(),
            vedtaksperiodeId = vedtaksperiodeId.toString(),
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            orgnummer = organisasjonsnummer,
            inntektsvurdering = inntektsvurdering ?: standardinntektsvurdering(),
            medlemskapsvurdering = Medlemskapsvurdering(medlemskapstatus),
            opptjeningvurdering = Opptjeningvurdering(
                if (arbeidsforhold.isEmpty()) listOf(
                    Opptjeningvurdering.Arbeidsforhold(organisasjonsnummer, 1.januar(2017))
                )
                else arbeidsforhold
            )
        )
    }

    fun ytelser(
        vedtaksperiodeId: UUID,
        utbetalinger: List<Utbetalingshistorikk.Infotrygdperiode> = listOf(),
        inntektshistorikk: List<Utbetalingshistorikk.Inntektsopplysning>? = null,
        foreldrepenger: Periode? = null,
        svangerskapspenger: Periode? = null,
        pleiepenger: List<Periode> = emptyList(),
        omsorgspenger: List<Periode> = emptyList(),
        opplæringspenger: List<Periode> = emptyList(),
        institusjonsoppholdsperioder: List<Institusjonsopphold.Institusjonsoppholdsperiode> = emptyList(),
        dødsdato: LocalDate? = null,
        statslønn: Boolean = false,
        arbeidskategorikoder: Map<String, LocalDate> = emptyMap(),
        arbeidsavklaringspenger: List<Periode> = emptyList(),
        dagpenger: List<Periode> = emptyList()
    ): Ytelser {
        val aktivitetslogg = Aktivitetslogg()
        val meldingsreferanseId = UUID.randomUUID()
        return Ytelser(
            meldingsreferanseId = meldingsreferanseId,
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId.toString(),
            statslønn = statslønn,
            utbetalingshistorikk = Utbetalingshistorikk(
                meldingsreferanseId = meldingsreferanseId,
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                vedtaksperiodeId = vedtaksperiodeId.toString(),
                arbeidskategorikoder = arbeidskategorikoder,
                utbetalinger = utbetalinger,
                inntektshistorikk = inntektshistorikk ?: standardinntekter(),
                aktivitetslogg = aktivitetslogg
            ),
            foreldrepermisjon = Foreldrepermisjon(
                foreldrepengeytelse = foreldrepenger,
                svangerskapsytelse = svangerskapspenger,
                aktivitetslogg = aktivitetslogg
            ),
            pleiepenger = Pleiepenger(
                perioder = pleiepenger,
                aktivitetslogg = aktivitetslogg
            ),
            omsorgspenger = Omsorgspenger(
                perioder = omsorgspenger,
                aktivitetslogg = aktivitetslogg
            ),
            opplæringspenger = Opplæringspenger(
                perioder = opplæringspenger,
                aktivitetslogg = aktivitetslogg
            ),
            institusjonsopphold = Institusjonsopphold(
                perioder = institusjonsoppholdsperioder,
                aktivitetslogg = aktivitetslogg
            ),
            aktivitetslogg = aktivitetslogg,
            dødsinfo = Dødsinfo(dødsdato),
            arbeidsavklaringspenger = Arbeidsavklaringspenger(arbeidsavklaringspenger),
            dagpenger = Dagpenger(dagpenger)
        )
    }

    internal fun utbetalingsgodkjenning(
        vedtaksperiodeId: UUID,
        utbetalingId: UUID,
        utbetalingGodkjent: Boolean,
        automatiskBehandling: Boolean
    ) = Utbetalingsgodkjenning(
        meldingsreferanseId = UUID.randomUUID(),
        aktørId = aktørId,
        fødselsnummer = fødselsnummer,
        organisasjonsnummer = organisasjonsnummer,
        utbetalingId = utbetalingId,
        vedtaksperiodeId = vedtaksperiodeId.toString(),
        saksbehandler = "Ola Nordmann",
        utbetalingGodkjent = utbetalingGodkjent,
        godkjenttidspunkt = LocalDateTime.now(),
        automatiskBehandling = automatiskBehandling,
        saksbehandlerEpost = "ola.nordmann@nav.no",
        makstidOppnådd = false,
    )

    private fun standardinntekter() =
        listOf(Utbetalingshistorikk.Inntektsopplysning(1.desember(2017), inntekt, organisasjonsnummer, true))

    private fun standardinntektsvurdering() =
        Inntektsvurdering(inntektperioder {
            inntektsgrunnlag = Inntektsvurdering.Inntektsgrunnlag.SAMMENLIGNINGSGRUNNLAG
            1.januar(2017) til 1.desember(2017) inntekter {
                organisasjonsnummer inntekt inntekt
            }
        })
}
