package no.nav.helse.person

import no.nav.helse.hendelser.*
import no.nav.helse.spleis.e2e.TestArbeidsgiverInspektør
import no.nav.helse.testhelpers.januar
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

abstract class HendelseTestHelper {

    internal open val aktørId = "aktørId"
    internal open val UNG_PERSON_FNR_2018 = "12020052345"
    internal open val ORGNUMMER = "12345"
    internal lateinit var førsteSykedag: LocalDate
    internal lateinit var sisteSykedag: LocalDate

    internal lateinit var person: Person
    internal val inspektør get() = TestArbeidsgiverInspektør(person)
    internal lateinit var hendelse: ArbeidstakerHendelse

    internal fun håndterGodkjenning(index: Int, fom: LocalDate = førsteSykedag, tom: LocalDate = sisteSykedag) {
        person.håndter(sykmelding(fom, tom))
        person.håndter(søknad(fom, tom))
        person.håndter(inntektsmelding(fom))
        person.håndter(vilkårsgrunnlag(index))
        person.håndter(ytelser(index = index))
        person.håndter(simulering(index))
        person.håndter(utbetalingsgodkjenning(true, index))
    }

    internal fun utbetaling(status: UtbetalingHendelse.Oppdragstatus, index: Int) =
        UtbetalingHendelse(
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

    internal fun utbetalingsgodkjenning(godkjent: Boolean, index: Int) = Utbetalingsgodkjenning(
        aktørId = aktørId,
        fødselsnummer = UNG_PERSON_FNR_2018,
        organisasjonsnummer = ORGNUMMER,
        vedtaksperiodeId = inspektør.vedtaksperiodeId(index).toString(),
        saksbehandler = "Ola Nordmann",
        utbetalingGodkjent = godkjent,
        godkjenttidspunkt = LocalDateTime.now()
    ).apply {
        hendelse = this
    }

    internal fun ytelser(
        index: Int,
        utbetalinger: List<Utbetalingshistorikk.Periode> = emptyList(),
        foreldrepengeYtelse: Periode? = null,
        svangerskapYtelse: Periode? = null
    ) = Aktivitetslogg().let {
        Ytelser(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = aktørId,
            fødselsnummer = UNG_PERSON_FNR_2018,
            organisasjonsnummer = ORGNUMMER,
            vedtaksperiodeId = inspektør.vedtaksperiodeId(index).toString(),
            utbetalingshistorikk = Utbetalingshistorikk(
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
            aktivitetslogg = it
        ).apply {
            hendelse = this
        }
    }

    internal fun sykmelding(fom: LocalDate = førsteSykedag, tom: LocalDate = sisteSykedag) =
        Sykmelding(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = aktørId,
            orgnummer = ORGNUMMER,
            sykeperioder = listOf(Sykmeldingsperiode(fom, tom, 100)),
            mottatt = fom.plusMonths(3).atStartOfDay()
        ).apply {
            hendelse = this
        }

    internal fun søknad(fom: LocalDate = førsteSykedag, tom: LocalDate = sisteSykedag) =
        Søknad(
            meldingsreferanseId = UUID.randomUUID(),
            fnr = UNG_PERSON_FNR_2018,
            aktørId = aktørId,
            orgnummer = ORGNUMMER,
            perioder = listOf(Søknad.Søknadsperiode.Sykdom(fom, tom, 100)),
            harAndreInntektskilder = false,
            sendtTilNAV = sisteSykedag.atStartOfDay(),
            permittert = false
        ).apply {
            hendelse = this
        }

    internal fun inntektsmelding(fom: LocalDate = førsteSykedag, tom: LocalDate = fom.plusDays(16), førsteFraværsdag: LocalDate = fom) =
        Inntektsmelding(
            meldingsreferanseId = UUID.randomUUID(),
            refusjon = Inntektsmelding.Refusjon(null, 31000.månedlig, emptyList()),
            orgnummer = ORGNUMMER,
            fødselsnummer = UNG_PERSON_FNR_2018,
            aktørId = aktørId,
            førsteFraværsdag = førsteFraværsdag,
            beregnetInntekt = 31000.månedlig,
            arbeidsgiverperioder = listOf(Periode(fom, tom)),
            ferieperioder = emptyList(),
            arbeidsforholdId = null,
            begrunnelseForReduksjonEllerIkkeUtbetalt = null
        ).apply {
            hendelse = this
        }

    internal fun vilkårsgrunnlag(index: Int) =
        Vilkårsgrunnlag(
            vedtaksperiodeId = inspektør.vedtaksperiodeId(index).toString(),
            aktørId = aktørId,
            fødselsnummer = UNG_PERSON_FNR_2018,
            orgnummer = ORGNUMMER,
            inntektsvurdering = Inntektsvurdering((1..12)
                .map { YearMonth.of(2018, it) to (ORGNUMMER to 31000.månedlig) }
                .groupBy({ it.first }) { it.second }),
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

    internal fun simulering(index: Int) =
        Simulering(
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
