package no.nav.helse.serde.mapping

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import no.nav.helse.hendelser.*
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.serde.AktivitetsloggerData
import no.nav.helse.serde.PersonData
import no.nav.helse.serde.PersonData.HendelseWrapperData
import no.nav.helse.serde.PersonData.HendelseWrapperData.Hendelsestype
import no.nav.helse.serde.reflection.ReflectClass
import no.nav.helse.serde.reflection.ReflectInstance.Companion.get

internal fun konverterTilAktivitetslogger(aktivitetsloggerData: AktivitetsloggerData): Aktivitetslogger {
    val aktivitetslogger = Aktivitetslogger(aktivitetsloggerData.originalMessage)

    val aktivitetClass: ReflectClass = ReflectClass.getNestedClass<Aktivitetslogger>("Aktivitet")

    val aktiviteter = aktivitetslogger.get<Aktivitetslogger, MutableList<Any>>("aktiviteter")
    aktivitetsloggerData.aktiviteter.forEach {
        aktiviteter.add(when (it.alvorlighetsgrad) {
            AktivitetsloggerData.Alvorlighetsgrad.INFO -> aktivitetClass.getNestedClass("Info").getInstance(
                it.melding,
                it.tidsstempel
            )
            AktivitetsloggerData.Alvorlighetsgrad.WARN -> aktivitetClass.getNestedClass("Warn").getInstance(
                it.melding,
                it.tidsstempel
            )
            AktivitetsloggerData.Alvorlighetsgrad.NEED -> aktivitetClass.getNestedClass("Need").getInstance(
                Aktivitetslogger.Aktivitet.Need.NeedType.valueOf(requireNotNull(it.needType)),
                it.melding,
                it.tidsstempel
            )
            AktivitetsloggerData.Alvorlighetsgrad.ERROR -> aktivitetClass.getNestedClass("Error").getInstance(
                it.melding,
                it.tidsstempel
            )
            AktivitetsloggerData.Alvorlighetsgrad.SEVERE -> aktivitetClass.getNestedClass("Severe").getInstance(
                it.melding,
                it.tidsstempel
            )
        })
    }

    return aktivitetslogger
}


internal fun konverterTilHendelse(
    objectMapper: ObjectMapper,
    personData: PersonData,
    data: HendelseWrapperData
): ArbeidstakerHendelse {
    return when (data.type) {
        Hendelsestype.Inntektsmelding -> parseInntektsmelding(objectMapper, personData, data.data)
        Hendelsestype.Ytelser -> parseYtelser(objectMapper, personData, data.data)
        Hendelsestype.Vilkårsgrunnlag -> parseVilkårsgrunnlag(objectMapper, personData, data.data)
        Hendelsestype.ManuellSaksbehandling -> parseManuellSaksbehandling(objectMapper, personData, data.data)
        Hendelsestype.NySøknad -> parseNySøknad(objectMapper, personData, data.data)
        Hendelsestype.SendtSøknad -> parseSendtSøknad(objectMapper, personData, data.data)
    }
}

private fun parseSendtSøknad(objectMapper: ObjectMapper, personData: PersonData, jsonNode: Map<String,Any?>): ModelSendtSøknad {
    val data: HendelseWrapperData.SendtSøknadData = objectMapper.convertValue(jsonNode)
    return ModelSendtSøknad(
        hendelseId = data.hendelseId,
        fnr = data.fnr,
        aktørId = data.aktørId,
        orgnummer = data.orgnummer,
        sendtNav = data.sendtNav,
        perioder = data.perioder.map(::parseSykeperiode),
        aktivitetslogger = konverterTilAktivitetslogger(data.aktivitetslogger),
        harAndreInntektskilder = data.harAndreInntektskilder
    )
}

private fun parseSykeperiode(data: HendelseWrapperData.SendtSøknadData.SykeperiodeData) = when(data.type) {
    HendelseWrapperData.SendtSøknadData.SykeperiodeData.TypeData.Ferie -> ModelSendtSøknad.Periode.Ferie(
        fom = data.fom,
        tom = data.tom
    )
    HendelseWrapperData.SendtSøknadData.SykeperiodeData.TypeData.Sykdom -> ModelSendtSøknad.Periode.Sykdom(
        fom = data.fom,
        tom = data.tom,
        grad = requireNotNull(data.grad),
        faktiskGrad = requireNotNull(data.faktiskGrad)
    )
    HendelseWrapperData.SendtSøknadData.SykeperiodeData.TypeData.Utdanning -> ModelSendtSøknad.Periode.Utdanning(
        fom = data.fom,
        tom = data.tom
    )
    HendelseWrapperData.SendtSøknadData.SykeperiodeData.TypeData.Permisjon -> ModelSendtSøknad.Periode.Permisjon(
        fom = data.fom,
        tom = data.tom
    )
    HendelseWrapperData.SendtSøknadData.SykeperiodeData.TypeData.Egenmelding -> ModelSendtSøknad.Periode.Egenmelding(
        fom = data.fom,
        tom = data.tom
    )
    HendelseWrapperData.SendtSøknadData.SykeperiodeData.TypeData.Arbeid -> ModelSendtSøknad.Periode.Arbeid(
        fom = data.fom,
        tom = data.tom
    )
}

private fun parseNySøknad(objectMapper: ObjectMapper, personData: PersonData, jsonNode: Map<String,Any?>): ModelNySøknad {
    val data: HendelseWrapperData.NySøknadData = objectMapper.convertValue(jsonNode)
    return ModelNySøknad(
        hendelseId = data.hendelseId,
        fnr = data.fnr,
        aktørId = data.aktørId,
        orgnummer = data.orgnummer,
        rapportertdato = data.rapportertdato,
        sykeperioder = data.sykeperioder.map { Triple(it.fom, it.tom, it.sykdomsgrad) },
        aktivitetslogger = konverterTilAktivitetslogger(data.aktivitetslogger)
    )
}

private fun parseManuellSaksbehandling(
    objectMapper: ObjectMapper,
    personData: PersonData,
    jsonNode: Map<String,Any?>
): ModelManuellSaksbehandling {
    val data: HendelseWrapperData.ManuellSaksbehandlingData = objectMapper.convertValue(jsonNode)
    return ModelManuellSaksbehandling(
        hendelseId = data.hendelseId,
        aktørId = personData.aktørId,
        fødselsnummer = personData.fødselsnummer,
        organisasjonsnummer = data.organisasjonsnummer,
        vedtaksperiodeId = data.vedtaksperiodeId.toString(),
        saksbehandler = data.saksbehandler,
        utbetalingGodkjent = data.utbetalingGodkjent,
        rapportertdato = data.rapportertdato,
        aktivitetslogger = konverterTilAktivitetslogger(data.aktivitetslogger)
    )
}

private fun parseVilkårsgrunnlag(objectMapper: ObjectMapper, personData: PersonData, jsonNode: Map<String,Any?>): ModelVilkårsgrunnlag {
    val data: HendelseWrapperData.VilkårsgrunnlagData = objectMapper.convertValue(jsonNode)
    return ModelVilkårsgrunnlag(
        hendelseId = data.hendelseId,
        vedtaksperiodeId = data.vedtaksperiodeId.toString(),
        aktørId = personData.aktørId,
        fødselsnummer = personData.fødselsnummer,
        orgnummer = data.orgnummer,
        rapportertDato = data.rapportertDato,
        inntektsmåneder = data.inntektsmåneder.map(::parseInntektsmåneder),
        erEgenAnsatt = data.erEgenAnsatt,
        arbeidsforhold = data.arbeidsforhold?.let { ModelVilkårsgrunnlag.ModelArbeidsforhold(it.map(::parseArbeidsforhold)) },
        aktivitetslogger = konverterTilAktivitetslogger(data.aktivitetslogger)
    )
}

private fun parseInntektsmåneder(data: HendelseWrapperData.VilkårsgrunnlagData.Måned) = ModelVilkårsgrunnlag.Måned(
    årMåned = data.årMåned,
    inntektsliste = data.inntektsliste.map { inntekt ->
        ModelVilkårsgrunnlag.Inntekt(
            beløp = inntekt.beløp
        )
    }
)

private fun parseArbeidsforhold(data: HendelseWrapperData.VilkårsgrunnlagData.ArbeidsforholdData) = ModelVilkårsgrunnlag.Arbeidsforhold(
    orgnummer = data.orgnummer,
    fom = data.fom,
    tom = data.tom
)

private fun parseInntektsmelding(
    objectMapper: ObjectMapper,
    personData: PersonData,
    jsonNode: Map<String,Any?>
): ModelInntektsmelding {
    val data: HendelseWrapperData.InntektsmeldingData = objectMapper.convertValue(jsonNode)
    return ModelInntektsmelding(
        hendelseId = data.hendelseId,
        orgnummer = data.orgnummer,
        fødselsnummer = data.fødselsnummer,
        aktørId = data.aktørId,
        mottattDato = data.mottattDato,
        refusjon = ModelInntektsmelding.Refusjon(
            opphørsdato = data.refusjon.opphørsdato,
            beløpPrMåned = data.refusjon.beløpPrMåned,
            endringerIRefusjon = data.refusjon.endringerIRefusjon.map { it.endringsdato }
        ),
        førsteFraværsdag = data.førsteFraværsdag,
        beregnetInntekt = data.beregnetInntekt,
        aktivitetslogger = konverterTilAktivitetslogger(data.aktivitetslogger),
        arbeidsgiverperioder = data.arbeidsgiverperioder.map { Periode(it.fom, it.tom) },
        ferieperioder = data.ferieperioder.map { Periode(it.fom, it.tom) }
    )
}

private fun parseYtelser(objectMapper: ObjectMapper, personData: PersonData, jsonNode: Map<String,Any?>): ModelYtelser {
    val data: HendelseWrapperData.YtelserData = objectMapper.convertValue(jsonNode)
    return ModelYtelser(
        hendelseId = data.hendelseId,
        vedtaksperiodeId = data.vedtaksperiodeId.toString(),
        organisasjonsnummer = data.organisasjonsnummer,
        fødselsnummer = personData.fødselsnummer,
        aktørId = personData.aktørId,
        rapportertdato = data.rapportertdato,
        sykepengehistorikk = ModelSykepengehistorikk(
            utbetalinger = data.sykepengehistorikk.utbetalinger.map(::parseUtbetaling),
            inntektshistorikk = data.sykepengehistorikk.inntektshistorikk.map(::parseInntektsopplysning),
            aktivitetslogger = Aktivitetslogger()
        ),
        foreldrepenger = ModelForeldrepenger(
            foreldrepengeytelse = data.foreldrepenger.foreldrepengeytelse?.let { parsePeriode(it) },
            svangerskapsytelse = data.foreldrepenger.svangerskapsytelse?.let { parsePeriode(it) },
            aktivitetslogger = Aktivitetslogger()
        ),
        aktivitetslogger = konverterTilAktivitetslogger(data.aktivitetslogger)
    )
}

private fun parseUtbetaling(
    periode: HendelseWrapperData.YtelserData.SykepengehistorikkData.UtbetalingPeriodeData
): ModelSykepengehistorikk.Periode = when (periode.type) {
    HendelseWrapperData.YtelserData.SykepengehistorikkData.UtbetalingPeriodeData.TypeData.RefusjonTilArbeidsgiver -> {
        ModelSykepengehistorikk.Periode.RefusjonTilArbeidsgiver(
            fom = periode.fom,
            tom = periode.tom,
            dagsats = periode.dagsats
        )
    }
    HendelseWrapperData.YtelserData.SykepengehistorikkData.UtbetalingPeriodeData.TypeData.ReduksjonMedlem -> {
        ModelSykepengehistorikk.Periode.ReduksjonMedlem(
            fom = periode.fom,
            tom = periode.tom,
            dagsats = periode.dagsats
        )
    }
    HendelseWrapperData.YtelserData.SykepengehistorikkData.UtbetalingPeriodeData.TypeData.Etterbetaling -> {
        ModelSykepengehistorikk.Periode.Etterbetaling(
            fom = periode.fom,
            tom = periode.tom,
            dagsats = periode.dagsats
        )
    }
    HendelseWrapperData.YtelserData.SykepengehistorikkData.UtbetalingPeriodeData.TypeData.KontertRegnskap -> {
        ModelSykepengehistorikk.Periode.KontertRegnskap(
            fom = periode.fom,
            tom = periode.tom,
            dagsats = periode.dagsats
        )
    }
    HendelseWrapperData.YtelserData.SykepengehistorikkData.UtbetalingPeriodeData.TypeData.ReduksjonArbeidsgiverRefusjon -> {
        ModelSykepengehistorikk.Periode.ReduksjonArbeidsgiverRefusjon(
            fom = periode.fom,
            tom = periode.tom,
            dagsats = periode.dagsats
        )
    }
    HendelseWrapperData.YtelserData.SykepengehistorikkData.UtbetalingPeriodeData.TypeData.Tilbakeført -> {
        ModelSykepengehistorikk.Periode.Tilbakeført(
            fom = periode.fom,
            tom = periode.tom,
            dagsats = periode.dagsats
        )
    }
    HendelseWrapperData.YtelserData.SykepengehistorikkData.UtbetalingPeriodeData.TypeData.Konvertert -> {
        ModelSykepengehistorikk.Periode.Konvertert(
            fom = periode.fom,
            tom = periode.tom,
            dagsats = periode.dagsats
        )
    }
    HendelseWrapperData.YtelserData.SykepengehistorikkData.UtbetalingPeriodeData.TypeData.Ferie -> {
        ModelSykepengehistorikk.Periode.Ferie(
            fom = periode.fom,
            tom = periode.tom,
            dagsats = periode.dagsats
        )
    }
    HendelseWrapperData.YtelserData.SykepengehistorikkData.UtbetalingPeriodeData.TypeData.Opphold -> {
        ModelSykepengehistorikk.Periode.Opphold(
            fom = periode.fom,
            tom = periode.tom,
            dagsats = periode.dagsats
        )
    }
    HendelseWrapperData.YtelserData.SykepengehistorikkData.UtbetalingPeriodeData.TypeData.Sanksjon -> {
        ModelSykepengehistorikk.Periode.Sanksjon(
            fom = periode.fom,
            tom = periode.tom,
            dagsats = periode.dagsats
        )
    }
    HendelseWrapperData.YtelserData.SykepengehistorikkData.UtbetalingPeriodeData.TypeData.Ukjent -> {
        ModelSykepengehistorikk.Periode.Ukjent(
            fom = periode.fom,
            tom = periode.tom,
            dagsats = periode.dagsats
        )
    }
}

private fun parseInntektsopplysning(
    data: HendelseWrapperData.YtelserData.SykepengehistorikkData.InntektsopplysningData
) = ModelSykepengehistorikk.Inntektsopplysning(
    sykepengerFom = data.sykepengerFom,
    inntektPerMåned = data.inntektPerMåned,
    orgnummer = data.orgnummer
)

private fun parsePeriode(periodeData: PersonData.PeriodeData) =
    Periode(periodeData.fom, periodeData.tom)
