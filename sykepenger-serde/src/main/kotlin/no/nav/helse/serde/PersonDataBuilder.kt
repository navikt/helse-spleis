package no.nav.helse.serde

import java.time.LocalDate
import no.nav.helse.dto.ArbeidsgiverperiodeavklaringDto
import no.nav.helse.dto.ArbeidssituasjonDto
import no.nav.helse.dto.AvsenderDto
import no.nav.helse.dto.BegrunnelseDto
import no.nav.helse.dto.BehandlingkildeDto
import no.nav.helse.dto.BehandlingtilstandDto
import no.nav.helse.dto.BeløpstidslinjeDto
import no.nav.helse.dto.DokumentsporingDto
import no.nav.helse.dto.DokumenttypeDto
import no.nav.helse.dto.EndringskodeDto
import no.nav.helse.dto.FagområdeDto
import no.nav.helse.dto.FeriepengerendringskodeDto
import no.nav.helse.dto.FeriepengerfagområdeDto
import no.nav.helse.dto.FeriepengerklassekodeDto
import no.nav.helse.dto.HendelseskildeDto
import no.nav.helse.dto.InfotrygdFerieperiodeDto
import no.nav.helse.dto.InntekttypeDto
import no.nav.helse.dto.KlassekodeDto
import no.nav.helse.dto.MaksdatobestemmelseDto
import no.nav.helse.dto.MedlemskapsvurderingDto
import no.nav.helse.dto.OppdragstatusDto
import no.nav.helse.dto.RefusjonsservitørDto
import no.nav.helse.dto.SelvstendigOpptjeningDto
import no.nav.helse.dto.SimuleringResultatDto
import no.nav.helse.dto.SkatteopplysningDto
import no.nav.helse.dto.SykdomshistorikkElementDto
import no.nav.helse.dto.SykdomstidslinjeDagDto
import no.nav.helse.dto.SykdomstidslinjeDto
import no.nav.helse.dto.SykmeldingsperioderDto
import no.nav.helse.dto.UtbetalingTilstandDto
import no.nav.helse.dto.UtbetalingVurderingDto
import no.nav.helse.dto.UtbetalingtypeDto
import no.nav.helse.dto.VedtaksperiodetilstandDto
import no.nav.helse.dto.deserialisering.ForberedendeVilkårsgrunnlagDto
import no.nav.helse.dto.deserialisering.YrkesaktivitetstypeDto
import no.nav.helse.dto.serialisering.ArbeidsgiverInntektsopplysningUtDto
import no.nav.helse.dto.serialisering.ArbeidsgiverUtDto
import no.nav.helse.dto.serialisering.ArbeidstakerFaktaavklartInntektUtDto
import no.nav.helse.dto.serialisering.ArbeidstakerinntektskildeUtDto
import no.nav.helse.dto.serialisering.BehandlingUtDto
import no.nav.helse.dto.serialisering.BehandlingendringUtDto
import no.nav.helse.dto.serialisering.FeriepengeUtDto
import no.nav.helse.dto.serialisering.FeriepengeoppdragUtDto
import no.nav.helse.dto.serialisering.FeriepengeutbetalingslinjeUtDto
import no.nav.helse.dto.serialisering.ForkastetVedtaksperiodeUtDto
import no.nav.helse.dto.serialisering.InfotrygdArbeidsgiverutbetalingsperiodeUtDto
import no.nav.helse.dto.serialisering.InfotrygdPersonutbetalingsperiodeUtDto
import no.nav.helse.dto.serialisering.InfotrygdhistorikkelementUtDto
import no.nav.helse.dto.serialisering.InntektsgrunnlagUtDto
import no.nav.helse.dto.serialisering.InntektsmeldingUtDto
import no.nav.helse.dto.serialisering.MaksdatoresultatUtDto
import no.nav.helse.dto.serialisering.OppdragUtDto
import no.nav.helse.dto.serialisering.OpptjeningUtDto
import no.nav.helse.dto.serialisering.PersonUtDto
import no.nav.helse.dto.serialisering.SaksbehandlerUtDto
import no.nav.helse.dto.serialisering.SelvstendigFaktaavklartInntektUtDto
import no.nav.helse.dto.serialisering.SelvstendigInntektsopplysningUtDto
import no.nav.helse.dto.serialisering.SkjønnsmessigFastsattUtDto
import no.nav.helse.dto.serialisering.UtbetalingUtDto
import no.nav.helse.dto.serialisering.UtbetalingsdagUtDto
import no.nav.helse.dto.serialisering.UtbetalingslinjeUtDto
import no.nav.helse.dto.serialisering.UtbetalingstidslinjeUtDto
import no.nav.helse.dto.serialisering.UtbetaltDagUtDto
import no.nav.helse.dto.serialisering.VedtaksperiodeUtDto
import no.nav.helse.dto.serialisering.VilkårsgrunnlagInnslagUtDto
import no.nav.helse.dto.serialisering.VilkårsgrunnlagUtDto
import no.nav.helse.serde.PersonData.ArbeidsgiverData.InntektsmeldingData.InntektsmeldingKildeDto
import no.nav.helse.serde.PersonData.ArbeidsgiverData.SykdomstidslinjeData.DagData
import no.nav.helse.serde.PersonData.ArbeidsgiverData.VedtaksperiodeData.BehandlingData.EndringData.ArbeidsgiverperiodeData
import no.nav.helse.serde.PersonData.ArbeidsgiverData.VedtaksperiodeData.BehandlingData.EndringData.ArbeidssituasjonData
import no.nav.helse.serde.PersonData.ArbeidsgiverData.VedtaksperiodeData.TilstandTypeData.AVSLUTTET
import no.nav.helse.serde.PersonData.ArbeidsgiverData.VedtaksperiodeData.TilstandTypeData.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.serde.PersonData.ArbeidsgiverData.VedtaksperiodeData.TilstandTypeData.AVVENTER_ANNULLERING
import no.nav.helse.serde.PersonData.ArbeidsgiverData.VedtaksperiodeData.TilstandTypeData.AVVENTER_A_ORDNINGEN
import no.nav.helse.serde.PersonData.ArbeidsgiverData.VedtaksperiodeData.TilstandTypeData.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.serde.PersonData.ArbeidsgiverData.VedtaksperiodeData.TilstandTypeData.AVVENTER_GODKJENNING
import no.nav.helse.serde.PersonData.ArbeidsgiverData.VedtaksperiodeData.TilstandTypeData.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.serde.PersonData.ArbeidsgiverData.VedtaksperiodeData.TilstandTypeData.AVVENTER_HISTORIKK
import no.nav.helse.serde.PersonData.ArbeidsgiverData.VedtaksperiodeData.TilstandTypeData.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.serde.PersonData.ArbeidsgiverData.VedtaksperiodeData.TilstandTypeData.AVVENTER_INFOTRYGDHISTORIKK
import no.nav.helse.serde.PersonData.ArbeidsgiverData.VedtaksperiodeData.TilstandTypeData.AVVENTER_INNTEKTSMELDING
import no.nav.helse.serde.PersonData.ArbeidsgiverData.VedtaksperiodeData.TilstandTypeData.AVVENTER_REVURDERING
import no.nav.helse.serde.PersonData.ArbeidsgiverData.VedtaksperiodeData.TilstandTypeData.AVVENTER_SIMULERING
import no.nav.helse.serde.PersonData.ArbeidsgiverData.VedtaksperiodeData.TilstandTypeData.AVVENTER_SIMULERING_REVURDERING
import no.nav.helse.serde.PersonData.ArbeidsgiverData.VedtaksperiodeData.TilstandTypeData.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.serde.PersonData.ArbeidsgiverData.VedtaksperiodeData.TilstandTypeData.AVVENTER_VILKÅRSPRØVING_REVURDERING
import no.nav.helse.serde.PersonData.ArbeidsgiverData.VedtaksperiodeData.TilstandTypeData.REVURDERING_FEILET
import no.nav.helse.serde.PersonData.ArbeidsgiverData.VedtaksperiodeData.TilstandTypeData.SELVSTENDIG_AVSLUTTET
import no.nav.helse.serde.PersonData.ArbeidsgiverData.VedtaksperiodeData.TilstandTypeData.SELVSTENDIG_AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.serde.PersonData.ArbeidsgiverData.VedtaksperiodeData.TilstandTypeData.SELVSTENDIG_AVVENTER_GODKJENNING
import no.nav.helse.serde.PersonData.ArbeidsgiverData.VedtaksperiodeData.TilstandTypeData.SELVSTENDIG_AVVENTER_HISTORIKK
import no.nav.helse.serde.PersonData.ArbeidsgiverData.VedtaksperiodeData.TilstandTypeData.SELVSTENDIG_AVVENTER_INFOTRYGDHISTORIKK
import no.nav.helse.serde.PersonData.ArbeidsgiverData.VedtaksperiodeData.TilstandTypeData.SELVSTENDIG_AVVENTER_SIMULERING
import no.nav.helse.serde.PersonData.ArbeidsgiverData.VedtaksperiodeData.TilstandTypeData.SELVSTENDIG_AVVENTER_VILKÅRSPRØVING
import no.nav.helse.serde.PersonData.ArbeidsgiverData.VedtaksperiodeData.TilstandTypeData.SELVSTENDIG_START
import no.nav.helse.serde.PersonData.ArbeidsgiverData.VedtaksperiodeData.TilstandTypeData.SELVSTENDIG_TIL_UTBETALING
import no.nav.helse.serde.PersonData.ArbeidsgiverData.VedtaksperiodeData.TilstandTypeData.START
import no.nav.helse.serde.PersonData.ArbeidsgiverData.VedtaksperiodeData.TilstandTypeData.TIL_ANNULLERING
import no.nav.helse.serde.PersonData.ArbeidsgiverData.VedtaksperiodeData.TilstandTypeData.TIL_INFOTRYGD
import no.nav.helse.serde.PersonData.ArbeidsgiverData.VedtaksperiodeData.TilstandTypeData.TIL_UTBETALING
import no.nav.helse.serde.PersonData.ArbeidsgiverData.YrkesaktivitetTypeData
import no.nav.helse.serde.PersonData.UtbetalingstidslinjeData.UtbetalingsdagData
import no.nav.helse.serde.PersonData.VilkårsgrunnlagElementData.ArbeidsgiverInntektsopplysningData.InntektsopplysningData.InntektsopplysningskildeData
import no.nav.helse.serde.PersonData.VilkårsgrunnlagElementData.ArbeidsgiverInntektsopplysningData.InntektsopplysningData.InntektsopplysningstypeData
import no.nav.helse.serde.PersonData.VilkårsgrunnlagElementData.ArbeidsgiverInntektsopplysningData.SkatteopplysningData
import no.nav.helse.serde.SerialisertPerson.Companion.gjeldendeVersjon
import no.nav.helse.serde.mapping.JsonMedlemskapstatus

fun PersonData.tilSerialisertPerson(): SerialisertPerson {
    val json = serdeObjectMapper.writeValueAsString(this)
    return SerialisertPerson(
        json = json,
        skjemaVersjon = gjeldendeVersjon()
    )
}

fun PersonUtDto.tilPersonData() = PersonData(
    fødselsdato = this.alder.fødselsdato,
    fødselsnummer = this.fødselsnummer,
    opprettet = this.opprettet,
    arbeidsgivere = this.arbeidsgivere.map { it.tilPersonData() },
    infotrygdhistorikk = this.infotrygdhistorikk.elementer.map { it.tilPersonData() },
    vilkårsgrunnlagHistorikk = vilkårsgrunnlagHistorikk.historikk.map { it.tilPersonData() },
    minimumSykdomsgradVurdering = minimumSykdomsgradVurdering.perioder.map { PersonData.MinimumSykdomsgradVurderingPeriodeData(it.fom, it.tom) },
    dødsdato = this.alder.dødsdato
)

private fun ArbeidsgiverUtDto.tilPersonData() = PersonData.ArbeidsgiverData(
    id = this.id,
    organisasjonsnummer = this.organisasjonsnummer,
    yrkesaktivitetstype = when (this.yrkesaktivitetstype) {
        YrkesaktivitetstypeDto.ARBEIDSTAKER -> YrkesaktivitetTypeData.ARBEIDSTAKER
        YrkesaktivitetstypeDto.ARBEIDSLEDIG -> YrkesaktivitetTypeData.ARBEIDSLEDIG
        YrkesaktivitetstypeDto.FRILANS -> YrkesaktivitetTypeData.FRILANS
        YrkesaktivitetstypeDto.SELVSTENDIG -> YrkesaktivitetTypeData.SELVSTENDIG
    },
    inntektshistorikk = this.inntektshistorikk.historikk.map { it.tilPersonData() },
    sykdomshistorikk = this.sykdomshistorikk.elementer.map { it.tilPersonData() },
    sykmeldingsperioder = this.sykmeldingsperioder.tilPersonData(),
    vedtaksperioder = this.vedtaksperioder.map { it.tilPersonData() },
    forkastede = this.forkastede.map { it.tilPersonData() },
    utbetalinger = this.utbetalinger.map { it.tilPersonData() },
    feriepengeutbetalinger = this.feriepengeutbetalinger.map { it.tilPersonData() },
    ubrukteRefusjonsopplysninger = this.ubrukteRefusjonsopplysninger.ubrukteRefusjonsopplysninger.tilPersonData(),
)

private fun InntektsmeldingUtDto.tilPersonData() = PersonData.ArbeidsgiverData.InntektsmeldingData(
    id = this.id,
    dato = this.inntektsdata.dato,
    hendelseId = this.inntektsdata.hendelseId.id,
    beløp = this.inntektsdata.beløp.månedligDouble.beløp,
    kilde = when (this.kilde) {
        InntektsmeldingUtDto.KildeDto.Arbeidsgiver -> InntektsmeldingKildeDto.Arbeidsgiver
        InntektsmeldingUtDto.KildeDto.AOrdningen -> InntektsmeldingKildeDto.AOrdningen
    },
    tidsstempel = this.inntektsdata.tidsstempel
)

private fun SykdomshistorikkElementDto.tilPersonData() = PersonData.SykdomshistorikkData(
    id = this.id,
    tidsstempel = this.tidsstempel,
    hendelseId = this.hendelseId?.id,
    hendelseSykdomstidslinje = this.hendelseSykdomstidslinje.tilPersonData(),
    beregnetSykdomstidslinje = this.beregnetSykdomstidslinje.tilPersonData(),
)

private fun SykdomstidslinjeDto.tilPersonData() = PersonData.ArbeidsgiverData.SykdomstidslinjeData(
    dager = dager.map { it.tilPersonData() }.forkortSykdomstidslinje(),
    låstePerioder = this.låstePerioder.map { PersonData.ArbeidsgiverData.PeriodeData(it.fom, it.tom) },
    periode = this.periode?.let { PersonData.ArbeidsgiverData.PeriodeData(it.fom, it.tom) }
)

private fun List<DagData>.forkortSykdomstidslinje(): List<DagData> {
    return this.fold(emptyList()) { result, neste ->
        val slåttSammen = result.lastOrNull()?.utvideMed(neste) ?: return@fold result + neste
        result.dropLast(1) + slåttSammen
    }
}

private fun DagData.utvideMed(other: DagData): DagData? {
    if (!kanUtvidesMed(other)) return null
    val otherDato = checkNotNull(other.dato) { "dato må være satt" }
    if (this.dato != null) {
        return this.copy(dato = null, fom = dato, tom = otherDato)
    }
    return this.copy(tom = other.dato)
}

private fun DagData.kanUtvidesMed(other: DagData): Boolean {
    // alle verdier må være like (untatt datoene)
    val utenDatoer = { dag: DagData -> dag.copy(fom = null, tom = null, dato = LocalDate.EPOCH) }
    return utenDatoer(this) == utenDatoer(other) && (dato ?: tom!!).plusDays(1) == other.dato
}

private fun SykdomstidslinjeDagDto.tilPersonData() = when (this) {
    is SykdomstidslinjeDagDto.UkjentDagDto -> DagData(
        type = PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.UKJENT_DAG,
        kilde = this.kilde.tilPersonData(),
        grad = 0.0,
        other = null,
        melding = null,
        dato = dato,
        fom = null,
        tom = null
    )

    is SykdomstidslinjeDagDto.AndreYtelserDto -> DagData(
        type = when (this.ytelse) {
            SykdomstidslinjeDagDto.AndreYtelserDto.YtelseDto.AAP -> PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.ANDRE_YTELSER_AAP
            SykdomstidslinjeDagDto.AndreYtelserDto.YtelseDto.Foreldrepenger -> PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.ANDRE_YTELSER_FORELDREPENGER
            SykdomstidslinjeDagDto.AndreYtelserDto.YtelseDto.Omsorgspenger -> PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.ANDRE_YTELSER_OMSORGSPENGER
            SykdomstidslinjeDagDto.AndreYtelserDto.YtelseDto.Pleiepenger -> PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.ANDRE_YTELSER_PLEIEPENGER
            SykdomstidslinjeDagDto.AndreYtelserDto.YtelseDto.Svangerskapspenger -> PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.ANDRE_YTELSER_SVANGERSKAPSPENGER
            SykdomstidslinjeDagDto.AndreYtelserDto.YtelseDto.Opplæringspenger -> PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.ANDRE_YTELSER_OPPLÆRINGSPENGER
            SykdomstidslinjeDagDto.AndreYtelserDto.YtelseDto.Dagpenger -> PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.ANDRE_YTELSER_DAGPENGER
        },
        kilde = this.kilde.tilPersonData(),
        grad = 0.0,
        other = null,
        melding = null,
        dato = dato,
        fom = null,
        tom = null
    )

    is SykdomstidslinjeDagDto.ArbeidIkkeGjenopptattDagDto -> DagData(
        type = PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.ARBEID_IKKE_GJENOPPTATT_DAG,
        kilde = this.kilde.tilPersonData(),
        grad = 0.0,
        other = null,
        melding = null,
        dato = dato,
        fom = null,
        tom = null
    )

    is SykdomstidslinjeDagDto.ArbeidsdagDto -> DagData(
        type = PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.ARBEIDSDAG,
        kilde = this.kilde.tilPersonData(),
        grad = 0.0,
        other = null,
        melding = null,
        dato = dato,
        fom = null,
        tom = null
    )

    is SykdomstidslinjeDagDto.ArbeidsgiverHelgedagDto -> DagData(
        type = PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.ARBEIDSGIVERDAG,
        kilde = this.kilde.tilPersonData(),
        grad = this.grad.prosentDesimal,
        other = null,
        melding = null,
        dato = dato,
        fom = null,
        tom = null
    )

    is SykdomstidslinjeDagDto.ArbeidsgiverdagDto -> DagData(
        type = PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.ARBEIDSGIVERDAG,
        kilde = this.kilde.tilPersonData(),
        grad = this.grad.prosentDesimal,
        other = null,
        melding = null,
        dato = dato,
        fom = null,
        tom = null
    )

    is SykdomstidslinjeDagDto.FeriedagDto -> DagData(
        type = PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.FERIEDAG,
        kilde = this.kilde.tilPersonData(),
        grad = 0.0,
        other = null,
        melding = null,
        dato = dato,
        fom = null,
        tom = null
    )

    is SykdomstidslinjeDagDto.ForeldetSykedagDto -> DagData(
        type = PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.FORELDET_SYKEDAG,
        kilde = this.kilde.tilPersonData(),
        grad = this.grad.prosentDesimal,
        other = null,
        melding = null,
        dato = dato,
        fom = null,
        tom = null
    )

    is SykdomstidslinjeDagDto.FriskHelgedagDto -> DagData(
        type = PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.FRISK_HELGEDAG,
        kilde = this.kilde.tilPersonData(),
        grad = 0.0,
        other = null,
        melding = null,
        dato = dato,
        fom = null,
        tom = null
    )

    is SykdomstidslinjeDagDto.PermisjonsdagDto -> DagData(
        type = PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.PERMISJONSDAG,
        kilde = this.kilde.tilPersonData(),
        grad = 0.0,
        other = null,
        melding = null,
        dato = dato,
        fom = null,
        tom = null
    )

    is SykdomstidslinjeDagDto.ProblemDagDto -> DagData(
        type = PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.PROBLEMDAG,
        kilde = this.kilde.tilPersonData(),
        grad = 0.0,
        other = this.other.tilPersonData(),
        melding = this.melding,
        dato = dato,
        fom = null,
        tom = null
    )

    is SykdomstidslinjeDagDto.SykHelgedagDto -> DagData(
        type = PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.SYKEDAG,
        kilde = this.kilde.tilPersonData(),
        grad = this.grad.prosentDesimal,
        other = null,
        melding = null,
        dato = dato,
        fom = null,
        tom = null
    )

    is SykdomstidslinjeDagDto.SykedagDto -> DagData(
        type = PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.SYKEDAG,
        kilde = this.kilde.tilPersonData(),
        grad = this.grad.prosentDesimal,
        other = null,
        melding = null,
        dato = dato,
        fom = null,
        tom = null
    )
}

private fun HendelseskildeDto.tilPersonData() = PersonData.ArbeidsgiverData.SykdomstidslinjeData.KildeData(
    type = this.type,
    id = this.meldingsreferanseId.id,
    tidsstempel = this.tidsstempel
)

private fun SykmeldingsperioderDto.tilPersonData() = perioder.map {
    PersonData.ArbeidsgiverData.SykmeldingsperiodeData(it.fom, it.tom)
}

private fun ForkastetVedtaksperiodeUtDto.tilPersonData() = PersonData.ArbeidsgiverData.ForkastetVedtaksperiodeData(
    vedtaksperiode = this.vedtaksperiode.tilPersonData()
)

private fun VedtaksperiodeUtDto.tilPersonData() = PersonData.ArbeidsgiverData.VedtaksperiodeData(
    id = id,
    tilstand = when (tilstand) {
        VedtaksperiodetilstandDto.AVSLUTTET -> AVSLUTTET
        VedtaksperiodetilstandDto.AVSLUTTET_UTEN_UTBETALING -> AVSLUTTET_UTEN_UTBETALING
        VedtaksperiodetilstandDto.AVVENTER_BLOKKERENDE_PERIODE -> AVVENTER_BLOKKERENDE_PERIODE
        VedtaksperiodetilstandDto.AVVENTER_A_ORDNINGEN -> AVVENTER_A_ORDNINGEN
        VedtaksperiodetilstandDto.AVVENTER_GODKJENNING -> AVVENTER_GODKJENNING
        VedtaksperiodetilstandDto.AVVENTER_GODKJENNING_REVURDERING -> AVVENTER_GODKJENNING_REVURDERING
        VedtaksperiodetilstandDto.AVVENTER_HISTORIKK -> AVVENTER_HISTORIKK
        VedtaksperiodetilstandDto.AVVENTER_HISTORIKK_REVURDERING -> AVVENTER_HISTORIKK_REVURDERING
        VedtaksperiodetilstandDto.AVVENTER_INFOTRYGDHISTORIKK -> AVVENTER_INFOTRYGDHISTORIKK
        VedtaksperiodetilstandDto.AVVENTER_INNTEKTSMELDING -> AVVENTER_INNTEKTSMELDING
        VedtaksperiodetilstandDto.AVVENTER_REVURDERING -> AVVENTER_REVURDERING
        VedtaksperiodetilstandDto.AVVENTER_SIMULERING -> AVVENTER_SIMULERING
        VedtaksperiodetilstandDto.AVVENTER_SIMULERING_REVURDERING -> AVVENTER_SIMULERING_REVURDERING
        VedtaksperiodetilstandDto.AVVENTER_VILKÅRSPRØVING -> AVVENTER_VILKÅRSPRØVING
        VedtaksperiodetilstandDto.AVVENTER_VILKÅRSPRØVING_REVURDERING -> AVVENTER_VILKÅRSPRØVING_REVURDERING
        VedtaksperiodetilstandDto.REVURDERING_FEILET -> REVURDERING_FEILET
        VedtaksperiodetilstandDto.START -> START
        VedtaksperiodetilstandDto.TIL_INFOTRYGD -> TIL_INFOTRYGD
        VedtaksperiodetilstandDto.TIL_UTBETALING -> TIL_UTBETALING

        VedtaksperiodetilstandDto.AVVENTER_ANNULLERING -> AVVENTER_ANNULLERING
        VedtaksperiodetilstandDto.TIL_ANNULLERING -> TIL_ANNULLERING

        VedtaksperiodetilstandDto.SELVSTENDIG_START -> SELVSTENDIG_START
        VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_INFOTRYGDHISTORIKK -> SELVSTENDIG_AVVENTER_INFOTRYGDHISTORIKK
        VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_BLOKKERENDE_PERIODE -> SELVSTENDIG_AVVENTER_BLOKKERENDE_PERIODE
        VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_VILKÅRSPRØVING -> SELVSTENDIG_AVVENTER_VILKÅRSPRØVING
        VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_HISTORIKK -> SELVSTENDIG_AVVENTER_HISTORIKK
        VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_SIMULERING -> SELVSTENDIG_AVVENTER_SIMULERING
        VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_GODKJENNING -> SELVSTENDIG_AVVENTER_GODKJENNING

        VedtaksperiodetilstandDto.SELVSTENDIG_TIL_UTBETALING -> SELVSTENDIG_TIL_UTBETALING
        VedtaksperiodetilstandDto.SELVSTENDIG_AVSLUTTET -> SELVSTENDIG_AVSLUTTET

    },
    skjæringstidspunkt = skjæringstidspunkt,
    behandlinger = behandlinger.behandlinger.map { it.tilPersonData() },
    opprettet = opprettet,
    oppdatert = oppdatert
)

private fun BehandlingUtDto.tilPersonData() = PersonData.ArbeidsgiverData.VedtaksperiodeData.BehandlingData(
    id = this.id,
    tilstand = when (this.tilstand) {
        BehandlingtilstandDto.ANNULLERT_PERIODE -> PersonData.ArbeidsgiverData.VedtaksperiodeData.BehandlingData.TilstandData.ANNULLERT_PERIODE
        BehandlingtilstandDto.AVSLUTTET_UTEN_VEDTAK -> PersonData.ArbeidsgiverData.VedtaksperiodeData.BehandlingData.TilstandData.AVSLUTTET_UTEN_VEDTAK
        BehandlingtilstandDto.BEREGNET -> PersonData.ArbeidsgiverData.VedtaksperiodeData.BehandlingData.TilstandData.BEREGNET
        BehandlingtilstandDto.BEREGNET_OMGJØRING -> PersonData.ArbeidsgiverData.VedtaksperiodeData.BehandlingData.TilstandData.BEREGNET_OMGJØRING
        BehandlingtilstandDto.BEREGNET_REVURDERING -> PersonData.ArbeidsgiverData.VedtaksperiodeData.BehandlingData.TilstandData.BEREGNET_REVURDERING
        BehandlingtilstandDto.REVURDERT_VEDTAK_AVVIST -> PersonData.ArbeidsgiverData.VedtaksperiodeData.BehandlingData.TilstandData.REVURDERT_VEDTAK_AVVIST
        BehandlingtilstandDto.TIL_INFOTRYGD -> PersonData.ArbeidsgiverData.VedtaksperiodeData.BehandlingData.TilstandData.TIL_INFOTRYGD
        BehandlingtilstandDto.UBEREGNET -> PersonData.ArbeidsgiverData.VedtaksperiodeData.BehandlingData.TilstandData.UBEREGNET
        BehandlingtilstandDto.UBEREGNET_OMGJØRING -> PersonData.ArbeidsgiverData.VedtaksperiodeData.BehandlingData.TilstandData.UBEREGNET_OMGJØRING
        BehandlingtilstandDto.UBEREGNET_REVURDERING -> PersonData.ArbeidsgiverData.VedtaksperiodeData.BehandlingData.TilstandData.UBEREGNET_REVURDERING
        BehandlingtilstandDto.VEDTAK_FATTET -> PersonData.ArbeidsgiverData.VedtaksperiodeData.BehandlingData.TilstandData.VEDTAK_FATTET
        BehandlingtilstandDto.VEDTAK_IVERKSATT -> PersonData.ArbeidsgiverData.VedtaksperiodeData.BehandlingData.TilstandData.VEDTAK_IVERKSATT
        BehandlingtilstandDto.UBEREGNET_ANNULLERING -> PersonData.ArbeidsgiverData.VedtaksperiodeData.BehandlingData.TilstandData.UBEREGNET_ANNULLERING
        BehandlingtilstandDto.BEREGNET_ANNULLERING -> PersonData.ArbeidsgiverData.VedtaksperiodeData.BehandlingData.TilstandData.BEREGNET_ANNULLERING
        BehandlingtilstandDto.OVERFØRT_ANNULLERING -> PersonData.ArbeidsgiverData.VedtaksperiodeData.BehandlingData.TilstandData.OVERFØRT_ANNULLERING
    },
    vedtakFattet = this.vedtakFattet,
    avsluttet = this.avsluttet,
    kilde = this.kilde.tilPersonData(),
    endringer = this.endringer.map { it.tilPersonData() },
)

private fun BehandlingkildeDto.tilPersonData() = PersonData.ArbeidsgiverData.VedtaksperiodeData.BehandlingData.KildeData(
    meldingsreferanseId = this.meldingsreferanseId.id,
    innsendt = this.innsendt,
    registrert = this.registert,
    avsender = this.avsender.tilPersonData()
)

private fun AvsenderDto.tilPersonData() = when (this) {
    AvsenderDto.ARBEIDSGIVER -> PersonData.ArbeidsgiverData.VedtaksperiodeData.BehandlingData.AvsenderData.ARBEIDSGIVER
    AvsenderDto.SAKSBEHANDLER -> PersonData.ArbeidsgiverData.VedtaksperiodeData.BehandlingData.AvsenderData.SAKSBEHANDLER
    AvsenderDto.SYKMELDT -> PersonData.ArbeidsgiverData.VedtaksperiodeData.BehandlingData.AvsenderData.SYKMELDT
    AvsenderDto.SYSTEM -> PersonData.ArbeidsgiverData.VedtaksperiodeData.BehandlingData.AvsenderData.SYSTEM
}

private fun BehandlingendringUtDto.tilPersonData() = PersonData.ArbeidsgiverData.VedtaksperiodeData.BehandlingData.EndringData(
    id = id,
    tidsstempel = tidsstempel,
    sykmeldingsperiodeFom = sykmeldingsperiode.fom,
    sykmeldingsperiodeTom = sykmeldingsperiode.tom,
    fom = periode.fom,
    tom = periode.tom,
    arbeidssituasjon = when (arbeidssituasjon) {
        ArbeidssituasjonDto.ARBEIDSTAKER -> ArbeidssituasjonData.ARBEIDSTAKER
        ArbeidssituasjonDto.ARBEIDSLEDIG -> ArbeidssituasjonData.ARBEIDSLEDIG
        ArbeidssituasjonDto.SELVSTENDIG_NÆRINGSDRIVENDE -> ArbeidssituasjonData.SELVSTENDIG_NÆRINGSDRIVENDE
        ArbeidssituasjonDto.BARNEPASSER -> ArbeidssituasjonData.BARNEPASSER
        ArbeidssituasjonDto.FRILANSER -> ArbeidssituasjonData.FRILANSER
        ArbeidssituasjonDto.JORDBRUKER -> ArbeidssituasjonData.JORDBRUKER
        ArbeidssituasjonDto.FISKER -> ArbeidssituasjonData.FISKER
        ArbeidssituasjonDto.ANNET -> ArbeidssituasjonData.ANNET
    },
    skjæringstidspunkt = skjæringstidspunkt,
    skjæringstidspunkter = skjæringstidspunkter,
    utbetalingId = utbetalingId,
    vilkårsgrunnlagId = vilkårsgrunnlagId,
    sykdomstidslinje = sykdomstidslinje.tilPersonData(),
    utbetalingstidslinje = utbetalingstidslinje.tilPersonData(),
    refusjonstidslinje = refusjonstidslinje.tilPersonData(),
    inntektsendringer = inntektsendringer.tilPersonData(),
    dokumentsporing = dokumentsporing.tilPersonData(),
    arbeidsgiverperiode = arbeidsgiverperiode.tilPersonData(),
    dagerNavOvertarAnsvar = dagerNavOvertarAnsvar.map { PersonData.ArbeidsgiverData.PeriodeData(it.fom, it.tom) },
    egenmeldingsdager = egenmeldingsdager.map { PersonData.ArbeidsgiverData.PeriodeData(it.fom, it.tom) },
    maksdatoresultat = maksdatoresultat.tilPersonData(),
    inntektjusteringer = inntektjusteringer.map { (inntektskilde, beløpstidslinje) ->
        inntektskilde.id to beløpstidslinje.tilPersonData()
    }.toMap(),
    faktaavklartInntekt = faktaavklartInntekt?.tilPersonData(),
    ventetid = ventetid?.let { PersonData.ArbeidsgiverData.PeriodeData(it.fom, it.tom) },
    forberedendeVilkårsgrunnlag = forberedendeVilkårsgrunnlag?.tilPersonData()
)

private fun ArbeidsgiverperiodeavklaringDto.tilPersonData() = ArbeidsgiverperiodeData(
    ferdigAvklart = this.ferdigAvklart,
    dager = this.dager.map { PersonData.ArbeidsgiverData.PeriodeData(it.fom, it.tom) }
)

private fun MaksdatoresultatUtDto.tilPersonData() = PersonData.ArbeidsgiverData.VedtaksperiodeData.MaksdatoresultatData(
    vurdertTilOgMed = vurdertTilOgMed,
    bestemmelse = when (bestemmelse) {
        MaksdatobestemmelseDto.IKKE_VURDERT -> PersonData.ArbeidsgiverData.VedtaksperiodeData.MaksdatobestemmelseData.IKKE_VURDERT
        MaksdatobestemmelseDto.ORDINÆR_RETT -> PersonData.ArbeidsgiverData.VedtaksperiodeData.MaksdatobestemmelseData.ORDINÆR_RETT
        MaksdatobestemmelseDto.BEGRENSET_RETT -> PersonData.ArbeidsgiverData.VedtaksperiodeData.MaksdatobestemmelseData.BEGRENSET_RETT
        MaksdatobestemmelseDto.SYTTI_ÅR -> PersonData.ArbeidsgiverData.VedtaksperiodeData.MaksdatobestemmelseData.SYTTI_ÅR
    },
    startdatoTreårsvindu = startdatoTreårsvindu,
    startdatoSykepengerettighet = startdatoSykepengerettighet,
    forbrukteDager = forbrukteDager.map { PersonData.ArbeidsgiverData.PeriodeData(it.fom, it.tom) },
    oppholdsdager = oppholdsdager.map { PersonData.ArbeidsgiverData.PeriodeData(it.fom, it.tom) },
    avslåtteDager = avslåtteDager.map { PersonData.ArbeidsgiverData.PeriodeData(it.fom, it.tom) },
    maksdato = maksdato,
    gjenståendeDager = gjenståendeDager
)

private fun DokumentsporingDto.tilPersonData() = PersonData.ArbeidsgiverData.VedtaksperiodeData.DokumentsporingData(
    dokumentId = this.id.id,
    dokumenttype = when (type) {
        DokumenttypeDto.InntektsmeldingDager -> PersonData.ArbeidsgiverData.VedtaksperiodeData.DokumentTypeData.InntektsmeldingDager
        DokumenttypeDto.InntektsmeldingInntekt -> PersonData.ArbeidsgiverData.VedtaksperiodeData.DokumentTypeData.InntektsmeldingInntekt
        DokumenttypeDto.InntektsmeldingRefusjon -> PersonData.ArbeidsgiverData.VedtaksperiodeData.DokumentTypeData.InntektsmeldingRefusjon
        DokumenttypeDto.InntektFraAOrdningen -> PersonData.ArbeidsgiverData.VedtaksperiodeData.DokumentTypeData.InntektFraAOrdningen
        DokumenttypeDto.OverstyrArbeidsforhold -> PersonData.ArbeidsgiverData.VedtaksperiodeData.DokumentTypeData.OverstyrArbeidsforhold
        DokumenttypeDto.OverstyrArbeidsgiveropplysninger -> PersonData.ArbeidsgiverData.VedtaksperiodeData.DokumentTypeData.OverstyrArbeidsgiveropplysninger
        DokumenttypeDto.OverstyrInntekt -> PersonData.ArbeidsgiverData.VedtaksperiodeData.DokumentTypeData.OverstyrInntekt
        DokumenttypeDto.OverstyrRefusjon -> PersonData.ArbeidsgiverData.VedtaksperiodeData.DokumentTypeData.OverstyrRefusjon
        DokumenttypeDto.OverstyrTidslinje -> PersonData.ArbeidsgiverData.VedtaksperiodeData.DokumentTypeData.OverstyrTidslinje
        DokumenttypeDto.SkjønnsmessigFastsettelse -> PersonData.ArbeidsgiverData.VedtaksperiodeData.DokumentTypeData.SkjønnsmessigFastsettelse
        DokumenttypeDto.Sykmelding -> PersonData.ArbeidsgiverData.VedtaksperiodeData.DokumentTypeData.Sykmelding
        DokumenttypeDto.Søknad -> PersonData.ArbeidsgiverData.VedtaksperiodeData.DokumentTypeData.Søknad
        DokumenttypeDto.AndreYtelser -> PersonData.ArbeidsgiverData.VedtaksperiodeData.DokumentTypeData.AndreYtelser
    }
)

private fun UtbetalingUtDto.tilPersonData() = PersonData.UtbetalingData(
    id = this.id,
    korrelasjonsId = this.korrelasjonsId,
    fom = this.periode.fom,
    tom = this.periode.tom,
    annulleringer = this.annulleringer,
    utbetalingstidslinje = this.utbetalingstidslinje.tilPersonData(),
    arbeidsgiverOppdrag = this.arbeidsgiverOppdrag.tilPersonData(),
    personOppdrag = this.personOppdrag.tilPersonData(),
    tidsstempel = this.tidsstempel,
    type = when (this.type) {
        UtbetalingtypeDto.ANNULLERING -> PersonData.UtbetalingData.UtbetalingtypeData.ANNULLERING
        UtbetalingtypeDto.ETTERUTBETALING -> PersonData.UtbetalingData.UtbetalingtypeData.ETTERUTBETALING
        UtbetalingtypeDto.REVURDERING -> PersonData.UtbetalingData.UtbetalingtypeData.REVURDERING
        UtbetalingtypeDto.UTBETALING -> PersonData.UtbetalingData.UtbetalingtypeData.UTBETALING
    },
    status = when (this.tilstand) {
        UtbetalingTilstandDto.ANNULLERT -> PersonData.UtbetalingData.UtbetalingstatusData.ANNULLERT
        UtbetalingTilstandDto.FORKASTET -> PersonData.UtbetalingData.UtbetalingstatusData.FORKASTET
        UtbetalingTilstandDto.GODKJENT -> PersonData.UtbetalingData.UtbetalingstatusData.GODKJENT
        UtbetalingTilstandDto.GODKJENT_UTEN_UTBETALING -> PersonData.UtbetalingData.UtbetalingstatusData.GODKJENT_UTEN_UTBETALING
        UtbetalingTilstandDto.IKKE_GODKJENT -> PersonData.UtbetalingData.UtbetalingstatusData.IKKE_GODKJENT
        UtbetalingTilstandDto.IKKE_UTBETALT -> PersonData.UtbetalingData.UtbetalingstatusData.IKKE_UTBETALT
        UtbetalingTilstandDto.NY -> PersonData.UtbetalingData.UtbetalingstatusData.NY
        UtbetalingTilstandDto.OVERFØRT -> PersonData.UtbetalingData.UtbetalingstatusData.OVERFØRT
        UtbetalingTilstandDto.UTBETALT -> PersonData.UtbetalingData.UtbetalingstatusData.UTBETALT
    },
    maksdato = this.maksdato,
    forbrukteSykedager = this.forbrukteSykedager,
    gjenståendeSykedager = this.gjenståendeSykedager,
    vurdering = this.vurdering?.tilPersonData(),
    overføringstidspunkt = overføringstidspunkt,
    avstemmingsnøkkel = avstemmingsnøkkel,
    avsluttet = avsluttet,
    oppdatert = oppdatert
)

private fun UtbetalingstidslinjeUtDto.tilPersonData() = PersonData.UtbetalingstidslinjeData(
    dager = this.dager.map { it.tilPersonData() }.forkortUtbetalingstidslinje()
)

private fun List<UtbetalingsdagData>.forkortUtbetalingstidslinje(): List<UtbetalingsdagData> {
    return this.fold(emptyList()) { result, neste ->
        val slåttSammen = result.lastOrNull()?.utvideMed(neste) ?: return@fold result + neste
        result.dropLast(1) + slåttSammen
    }
}

private fun UtbetalingsdagData.utvideMed(other: UtbetalingsdagData): UtbetalingsdagData? {
    if (!kanUtvidesMed(other)) return null
    val otherDato = checkNotNull(other.dato) { "dato må være satt" }
    if (this.dato != null) {
        return this.copy(dato = null, fom = dato, tom = otherDato)
    }
    return this.copy(tom = other.dato)
}

private fun UtbetalingsdagData.kanUtvidesMed(other: UtbetalingsdagData): Boolean {
    // alle verdier må være like (untatt datoene)
    val utenDatoer = { dag: UtbetalingsdagData -> dag.copy(fom = null, tom = null, dato = LocalDate.EPOCH) }
    return utenDatoer(this) == utenDatoer(other) && (dato ?: tom!!).plusDays(1) == other.dato
}

private fun UtbetalingsdagUtDto.tilPersonData() = UtbetalingsdagData(
    type = when (this) {
        is UtbetalingsdagUtDto.ArbeidsdagDto -> PersonData.UtbetalingstidslinjeData.TypeData.Arbeidsdag
        is UtbetalingsdagUtDto.ArbeidsgiverperiodeDagDto -> PersonData.UtbetalingstidslinjeData.TypeData.ArbeidsgiverperiodeDag
        is UtbetalingsdagUtDto.ArbeidsgiverperiodeDagNavDto -> PersonData.UtbetalingstidslinjeData.TypeData.ArbeidsgiverperiodedagNav
        is UtbetalingsdagUtDto.AvvistDagDto -> PersonData.UtbetalingstidslinjeData.TypeData.AvvistDag
        is UtbetalingsdagUtDto.ForeldetDagDto -> PersonData.UtbetalingstidslinjeData.TypeData.ForeldetDag
        is UtbetalingsdagUtDto.FridagDto -> PersonData.UtbetalingstidslinjeData.TypeData.Fridag
        is UtbetalingsdagUtDto.NavDagDto -> PersonData.UtbetalingstidslinjeData.TypeData.NavDag
        is UtbetalingsdagUtDto.NavHelgDagDto -> PersonData.UtbetalingstidslinjeData.TypeData.NavHelgDag
        is UtbetalingsdagUtDto.UkjentDagDto -> PersonData.UtbetalingstidslinjeData.TypeData.UkjentDag
        is UtbetalingsdagUtDto.VentetidsdagDto -> PersonData.UtbetalingstidslinjeData.TypeData.Ventetidsdag
    },
    aktuellDagsinntekt = this.økonomi.aktuellDagsinntekt.dagligDouble.beløp,
    inntektjustering = this.økonomi.inntektjustering.dagligDouble.beløp,
    dekningsgrad = this.økonomi.dekningsgrad.prosentDesimal,
    begrunnelser = when (this) {
        is UtbetalingsdagUtDto.AvvistDagDto -> this.begrunnelser.map { it.tilPersonData() }
        else -> null
    },
    grad = this.økonomi.grad.prosentDesimal,
    totalGrad = this.økonomi.totalGrad.prosentDesimal,
    utbetalingsgrad = this.økonomi.utbetalingsgrad.prosentDesimal,
    arbeidsgiverRefusjonsbeløp = økonomi.arbeidsgiverRefusjonsbeløp.dagligDouble.beløp,
    arbeidsgiverbeløp = this.økonomi.arbeidsgiverbeløp?.dagligDouble?.beløp,
    personbeløp = this.økonomi.personbeløp?.dagligDouble?.beløp,
    reservertArbeidsgiverbeløp = this.økonomi.reservertArbeidsgiverbeløp?.dagligDouble?.beløp,
    reservertPersonbeløp = this.økonomi.reservertPersonbeløp?.dagligDouble?.beløp,
    dato = this.dato,
    fom = null,
    tom = null
)

private fun BegrunnelseDto.tilPersonData() = when (this) {
    BegrunnelseDto.AndreYtelserAap -> PersonData.UtbetalingstidslinjeData.BegrunnelseData.AndreYtelserAap
    BegrunnelseDto.AndreYtelserDagpenger -> PersonData.UtbetalingstidslinjeData.BegrunnelseData.AndreYtelserDagpenger
    BegrunnelseDto.AndreYtelserForeldrepenger -> PersonData.UtbetalingstidslinjeData.BegrunnelseData.AndreYtelserForeldrepenger
    BegrunnelseDto.AndreYtelserOmsorgspenger -> PersonData.UtbetalingstidslinjeData.BegrunnelseData.AndreYtelserOmsorgspenger
    BegrunnelseDto.AndreYtelserOpplaringspenger -> PersonData.UtbetalingstidslinjeData.BegrunnelseData.AndreYtelserOpplaringspenger
    BegrunnelseDto.AndreYtelserPleiepenger -> PersonData.UtbetalingstidslinjeData.BegrunnelseData.AndreYtelserPleiepenger
    BegrunnelseDto.AndreYtelserSvangerskapspenger -> PersonData.UtbetalingstidslinjeData.BegrunnelseData.AndreYtelserSvangerskapspenger
    BegrunnelseDto.EgenmeldingUtenforArbeidsgiverperiode -> PersonData.UtbetalingstidslinjeData.BegrunnelseData.EgenmeldingUtenforArbeidsgiverperiode
    BegrunnelseDto.EtterDødsdato -> PersonData.UtbetalingstidslinjeData.BegrunnelseData.EtterDødsdato
    BegrunnelseDto.ManglerMedlemskap -> PersonData.UtbetalingstidslinjeData.BegrunnelseData.ManglerMedlemskap
    BegrunnelseDto.ManglerOpptjening -> PersonData.UtbetalingstidslinjeData.BegrunnelseData.ManglerOpptjening
    BegrunnelseDto.MinimumInntekt -> PersonData.UtbetalingstidslinjeData.BegrunnelseData.MinimumInntekt
    BegrunnelseDto.MinimumInntektOver67 -> PersonData.UtbetalingstidslinjeData.BegrunnelseData.MinimumInntektOver67
    BegrunnelseDto.MinimumSykdomsgrad -> PersonData.UtbetalingstidslinjeData.BegrunnelseData.MinimumSykdomsgrad
    BegrunnelseDto.NyVilkårsprøvingNødvendig -> PersonData.UtbetalingstidslinjeData.BegrunnelseData.NyVilkårsprøvingNødvendig
    BegrunnelseDto.Over70 -> PersonData.UtbetalingstidslinjeData.BegrunnelseData.Over70
    BegrunnelseDto.SykepengedagerOppbrukt -> PersonData.UtbetalingstidslinjeData.BegrunnelseData.SykepengedagerOppbrukt
    BegrunnelseDto.SykepengedagerOppbruktOver67 -> PersonData.UtbetalingstidslinjeData.BegrunnelseData.SykepengedagerOppbruktOver67
}

private fun UtbetalingVurderingDto.tilPersonData() = PersonData.UtbetalingData.VurderingData(
    godkjent = godkjent,
    ident = ident,
    epost = epost,
    tidspunkt = tidspunkt,
    automatiskBehandling = automatiskBehandling
)

private fun OppdragUtDto.tilPersonData() = PersonData.OppdragData(
    mottaker = this.mottaker,
    fagområde = when (this.fagområde) {
        FagområdeDto.SP -> "SP"
        FagområdeDto.SPREF -> "SPREF"
    },
    linjer = this.linjer.map { it.tilPersonData() },
    fagsystemId = this.fagsystemId,
    endringskode = this.endringskode.tilPersonData(),
    tidsstempel = this.tidsstempel,
    nettoBeløp = this.nettoBeløp,
    avstemmingsnøkkel = this.avstemmingsnøkkel,
    status = when (this.status) {
        OppdragstatusDto.AKSEPTERT -> PersonData.OppdragData.OppdragstatusData.AKSEPTERT
        OppdragstatusDto.AKSEPTERT_MED_FEIL -> PersonData.OppdragData.OppdragstatusData.AKSEPTERT_MED_FEIL
        OppdragstatusDto.AVVIST -> PersonData.OppdragData.OppdragstatusData.AVVIST
        OppdragstatusDto.FEIL -> PersonData.OppdragData.OppdragstatusData.FEIL
        OppdragstatusDto.OVERFØRT -> PersonData.OppdragData.OppdragstatusData.OVERFØRT
        null -> null
    },
    overføringstidspunkt = this.overføringstidspunkt,
    erSimulert = this.erSimulert,
    simuleringsResultat = this.simuleringsResultat?.tilPersonData()
)

private fun EndringskodeDto.tilPersonData() = when (this) {
    EndringskodeDto.ENDR -> "ENDR"
    EndringskodeDto.NY -> "NY"
    EndringskodeDto.UEND -> "UEND"
}

private fun UtbetalingslinjeUtDto.tilPersonData() = PersonData.UtbetalingslinjeData(
    fom = this.fom,
    tom = this.tom,
    sats = this.beløp,
    grad = this.grad,
    refFagsystemId = this.refFagsystemId,
    delytelseId = this.delytelseId,
    refDelytelseId = this.refDelytelseId,
    endringskode = this.endringskode.tilPersonData(),
    klassekode = this.klassekode.tilPersonData(),
    datoStatusFom = this.datoStatusFom
)

private fun KlassekodeDto.tilPersonData() = when (this) {
    KlassekodeDto.RefusjonIkkeOpplysningspliktig -> "SPREFAG-IOP"
    KlassekodeDto.SykepengerArbeidstakerOrdinær -> "SPATORD"
    KlassekodeDto.SelvstendigNæringsdrivendeOppgavepliktig -> "SPSND-OP"
    KlassekodeDto.SelvstendigNæringsdrivendeBarnepasserOppgavepliktig -> "SPSNDDM-OP"
    KlassekodeDto.SelvstendigNæringsdrivendeFisker -> "SPSNDFISK"
    KlassekodeDto.SelvstendigNæringsdrivendeJordbrukOgSkogbruk -> "SPSNDJORD"
}

private fun SimuleringResultatDto.tilPersonData() = PersonData.ArbeidsgiverData.VedtaksperiodeData.DataForSimuleringData(
    totalbeløp = this.totalbeløp,
    perioder = this.perioder.map {
        PersonData.ArbeidsgiverData.VedtaksperiodeData.DataForSimuleringData.SimulertPeriode(
            fom = it.fom,
            tom = it.tom,

            utbetalinger = it.utbetalinger.map {
                PersonData.ArbeidsgiverData.VedtaksperiodeData.DataForSimuleringData.SimulertUtbetaling(
                    forfallsdato = it.forfallsdato,
                    utbetalesTil = PersonData.ArbeidsgiverData.VedtaksperiodeData.DataForSimuleringData.Mottaker(
                        id = it.utbetalesTil.id,
                        navn = it.utbetalesTil.navn
                    ),
                    feilkonto = it.feilkonto,
                    detaljer = it.detaljer.map {
                        PersonData.ArbeidsgiverData.VedtaksperiodeData.DataForSimuleringData.Detaljer(
                            fom = it.fom,
                            tom = it.tom,
                            konto = it.konto,
                            beløp = it.beløp,
                            klassekode = PersonData.ArbeidsgiverData.VedtaksperiodeData.DataForSimuleringData.Klassekode(
                                kode = it.klassekode.kode,
                                beskrivelse = it.klassekode.beskrivelse
                            ),
                            uføregrad = it.uføregrad,
                            utbetalingstype = it.utbetalingstype,
                            tilbakeføring = it.tilbakeføring,
                            sats = PersonData.ArbeidsgiverData.VedtaksperiodeData.DataForSimuleringData.Sats(
                                sats = it.sats.sats,
                                antall = it.sats.antall,
                                type = it.sats.type
                            ),
                            refunderesOrgnummer = it.refunderesOrgnummer
                        )
                    }
                )
            }
        )
    }
)

private fun FeriepengeUtDto.tilPersonData() = PersonData.ArbeidsgiverData.FeriepengeutbetalingData(
    infotrygdFeriepengebeløpPerson = this.infotrygdFeriepengebeløpPerson,
    infotrygdFeriepengebeløpArbeidsgiver = this.infotrygdFeriepengebeløpArbeidsgiver,
    spleisFeriepengebeløpArbeidsgiver = this.spleisFeriepengebeløpArbeidsgiver,
    spleisFeriepengebeløpPerson = this.spleisFeriepengebeløpPerson,
    oppdrag = this.oppdrag.tilPersonData(),
    personoppdrag = this.personoppdrag.tilPersonData(),
    opptjeningsår = this.feriepengeberegner.opptjeningsår,
    utbetalteDager = this.feriepengeberegner.utbetalteDager.map { it.tilPersonData() },
    feriepengedager = this.feriepengeberegner.feriepengedager.map { it.tilPersonData() },
    utbetalingId = utbetalingId,
    sendTilOppdrag = sendTilOppdrag,
    sendPersonoppdragTilOS = sendPersonoppdragTilOS
)

private fun FeriepengeoppdragUtDto.tilPersonData() = PersonData.ArbeidsgiverData.FeriepengeutbetalingData.OppdragData(
    mottaker = this.mottaker,
    fagområde = when (this.fagområde) {
        FeriepengerfagområdeDto.SP -> "SP"
        FeriepengerfagområdeDto.SPREF -> "SPREF"
    },
    linjer = this.linjer.map { it.tilPersonData() },
    fagsystemId = this.fagsystemId,
    endringskode = this.endringskode.tilPersonData(),
    tidsstempel = this.tidsstempel
)

private fun FeriepengerendringskodeDto.tilPersonData() = when (this) {
    FeriepengerendringskodeDto.ENDR -> "ENDR"
    FeriepengerendringskodeDto.NY -> "NY"
    FeriepengerendringskodeDto.UEND -> "UEND"
}

private fun FeriepengeutbetalingslinjeUtDto.tilPersonData() = PersonData.ArbeidsgiverData.FeriepengeutbetalingData.OppdragData.UtbetalingslinjeData(
    fom = this.fom,
    tom = this.tom,
    sats = this.beløp,
    refFagsystemId = this.refFagsystemId,
    delytelseId = this.delytelseId,
    refDelytelseId = this.refDelytelseId,
    endringskode = this.endringskode.tilPersonData(),
    klassekode = this.klassekode.tilPersonData(),
    datoStatusFom = this.datoStatusFom
)

private fun FeriepengerklassekodeDto.tilPersonData() = when (this) {
    FeriepengerklassekodeDto.RefusjonFeriepengerIkkeOpplysningspliktig -> "SPREFAGFER-IOP"
    FeriepengerklassekodeDto.SykepengerArbeidstakerFeriepenger -> "SPATFER"
}

private fun UtbetaltDagUtDto.tilPersonData() = PersonData.ArbeidsgiverData.FeriepengeutbetalingData.UtbetaltDagData(
    type = when (this) {
        is UtbetaltDagUtDto.InfotrygdArbeidsgiver -> "InfotrygdArbeidsgiverDag"
        is UtbetaltDagUtDto.InfotrygdPerson -> "InfotrygdPersonDag"
        is UtbetaltDagUtDto.SpleisArbeidsgiver -> "SpleisArbeidsgiverDag"
        is UtbetaltDagUtDto.SpleisPerson -> "SpleisPersonDag"
    },
    orgnummer = orgnummer,
    dato = dato,
    beløp = beløp
)

private fun InfotrygdhistorikkelementUtDto.tilPersonData() = PersonData.InfotrygdhistorikkElementData(
    id = this.id,
    tidsstempel = this.tidsstempel,
    hendelseId = this.hendelseId.id,
    ferieperioder = this.ferieperioder.map { it.tilPersonData() },
    arbeidsgiverutbetalingsperioder = this.arbeidsgiverutbetalingsperioder.map { it.tilPersonData() },
    personutbetalingsperioder = this.personutbetalingsperioder.map { it.tilPersonData() },
    oppdatert = oppdatert
)

private fun InfotrygdFerieperiodeDto.tilPersonData() = PersonData.InfotrygdhistorikkElementData.FerieperiodeData(
    fom = this.periode.fom,
    tom = this.periode.tom
)

private fun InfotrygdArbeidsgiverutbetalingsperiodeUtDto.tilPersonData() = PersonData.InfotrygdhistorikkElementData.ArbeidsgiverutbetalingsperiodeData(
    orgnr = this.orgnr,
    fom = this.periode.fom,
    tom = this.periode.tom
)

private fun InfotrygdPersonutbetalingsperiodeUtDto.tilPersonData() = PersonData.InfotrygdhistorikkElementData.PersonutbetalingsperiodeData(
    orgnr = this.orgnr,
    fom = this.periode.fom,
    tom = this.periode.tom
)

private fun VilkårsgrunnlagInnslagUtDto.tilPersonData() = PersonData.VilkårsgrunnlagInnslagData(
    id = this.id,
    opprettet = this.opprettet,
    vilkårsgrunnlag = this.vilkårsgrunnlag.map { it.tilPersonData() }
)

private fun VilkårsgrunnlagUtDto.tilPersonData() = PersonData.VilkårsgrunnlagElementData(
    skjæringstidspunkt = this.skjæringstidspunkt,
    type = when (this) {
        is VilkårsgrunnlagUtDto.Infotrygd -> PersonData.VilkårsgrunnlagElementData.GrunnlagsdataType.Infotrygd
        is VilkårsgrunnlagUtDto.Spleis -> PersonData.VilkårsgrunnlagElementData.GrunnlagsdataType.Vilkårsprøving
    },
    inntektsgrunnlag = this.inntektsgrunnlag.tilPersonData(),
    opptjening = when (this) {
        is VilkårsgrunnlagUtDto.Spleis -> this.opptjening?.tilPersonData()
        is VilkårsgrunnlagUtDto.Infotrygd -> null
    },
    selvstendigOpptjening = when (this) {
        is VilkårsgrunnlagUtDto.Spleis -> this.selvstendigOpptjening?.tilPersonData()
        is VilkårsgrunnlagUtDto.Infotrygd -> null
    },
    medlemskapstatus = when (this) {
        is VilkårsgrunnlagUtDto.Spleis -> when (this.medlemskapstatus) {
            MedlemskapsvurderingDto.Ja -> JsonMedlemskapstatus.JA
            MedlemskapsvurderingDto.Nei -> JsonMedlemskapstatus.NEI
            MedlemskapsvurderingDto.UavklartMedBrukerspørsmål -> JsonMedlemskapstatus.UAVKLART_MED_BRUKERSPØRSMÅL
            MedlemskapsvurderingDto.VetIkke -> JsonMedlemskapstatus.VET_IKKE
        }

        else -> null
    },
    meldingsreferanseId = when (this) {
        is VilkårsgrunnlagUtDto.Spleis -> this.meldingsreferanseId?.id
        else -> null
    },
    vilkårsgrunnlagId = this.vilkårsgrunnlagId
)

private fun OpptjeningUtDto.tilPersonData() = PersonData.VilkårsgrunnlagElementData.OpptjeningData(
    opptjeningFom = this.opptjeningsperiode.fom,
    opptjeningTom = this.opptjeningsperiode.tom,
    arbeidsforhold = this.arbeidsforhold.map {
        PersonData.VilkårsgrunnlagElementData.OpptjeningData.ArbeidsgiverOpptjeningsgrunnlagData(
            orgnummer = it.orgnummer,
            ansattPerioder = it.ansattPerioder.map {
                PersonData.VilkårsgrunnlagElementData.OpptjeningData.ArbeidsgiverOpptjeningsgrunnlagData.ArbeidsforholdData(
                    ansattFom = it.ansattFom,
                    ansattTom = it.ansattTom,
                    deaktivert = it.deaktivert
                )
            }
        )
    }
)

private fun SelvstendigOpptjeningDto.tilPersonData() = when (this) {
    SelvstendigOpptjeningDto.IkkeOppfylt -> PersonData.VilkårsgrunnlagElementData.SelvstendigOpptjeningData.IKKE_OPPFYLT
    SelvstendigOpptjeningDto.IkkeVurdert -> PersonData.VilkårsgrunnlagElementData.SelvstendigOpptjeningData.IKKE_VURDERT
    SelvstendigOpptjeningDto.Oppfylt -> PersonData.VilkårsgrunnlagElementData.SelvstendigOpptjeningData.OPPFYLT
}

private fun InntektsgrunnlagUtDto.tilPersonData() = PersonData.VilkårsgrunnlagElementData.InntektsgrunnlagData(
    grunnbeløp = this.`6G`.årlig.beløp,
    arbeidsgiverInntektsopplysninger = this.arbeidsgiverInntektsopplysninger.map { it.tilPersonData() },
    selvstendigInntektsopplysninger = this.selvstendigInntektsopplysning?.tilPersonData(),
    deaktiverteArbeidsforhold = this.deaktiverteArbeidsforhold.map { it.tilPersonData() },
    vurdertInfotrygd = this.vurdertInfotrygd
)

private fun ArbeidsgiverInntektsopplysningUtDto.tilPersonData() = PersonData.VilkårsgrunnlagElementData.ArbeidsgiverInntektsopplysningData(
    orgnummer = this.orgnummer,
    inntektsopplysning = this.faktaavklartInntekt.tilPersonData(),
    korrigertInntekt = this.korrigertInntekt?.tilPersonData(),
    skjønnsmessigFastsatt = this.skjønnsmessigFastsatt?.tilPersonData()
)

private fun ArbeidstakerFaktaavklartInntektUtDto.tilPersonData() = PersonData.VilkårsgrunnlagElementData.ArbeidsgiverInntektsopplysningData.InntektsopplysningData(
    id = this.id,
    dato = this.inntektsdata.dato,
    hendelseId = this.inntektsdata.hendelseId.id,
    beløp = this.inntektsdata.beløp.månedligDouble.beløp,
    tidsstempel = this.inntektsdata.tidsstempel,
    type = InntektsopplysningstypeData.ARBEIDSTAKER,
    kilde = when (this.inntektsopplysningskilde) {
        is ArbeidstakerinntektskildeUtDto.InfotrygdDto -> InntektsopplysningskildeData.INFOTRYGD
        is ArbeidstakerinntektskildeUtDto.ArbeidsgiverDto -> InntektsopplysningskildeData.INNTEKTSMELDING
        is ArbeidstakerinntektskildeUtDto.AOrdningenDto -> InntektsopplysningskildeData.SKATT_SYKEPENGEGRUNNLAG
    },
    skatteopplysninger = when (val kilde = this.inntektsopplysningskilde) {
        is ArbeidstakerinntektskildeUtDto.AOrdningenDto -> kilde.inntektsopplysninger.map { it.tilPersonDataSkattopplysning() }
        ArbeidstakerinntektskildeUtDto.ArbeidsgiverDto,
        ArbeidstakerinntektskildeUtDto.InfotrygdDto -> null
    },
    pensjonsgivendeInntekter = null,
    anvendtÅrligGrunnbeløp = null
)

private fun SaksbehandlerUtDto.tilPersonData() = PersonData.VilkårsgrunnlagElementData.ArbeidsgiverInntektsopplysningData.KorrigertInntektsopplysningData(
    id = this.id,
    dato = this.inntektsdata.dato,
    hendelseId = this.inntektsdata.hendelseId.id,
    beløp = this.inntektsdata.beløp.månedligDouble.beløp,
    tidsstempel = this.inntektsdata.tidsstempel
)

private fun SkjønnsmessigFastsattUtDto.tilPersonData() = PersonData.VilkårsgrunnlagElementData.ArbeidsgiverInntektsopplysningData.SkjønnsmessigFastsattData(
    id = this.id,
    dato = this.inntektsdata.dato,
    hendelseId = this.inntektsdata.hendelseId.id,
    beløp = this.inntektsdata.beløp.månedligDouble.beløp,
    tidsstempel = this.inntektsdata.tidsstempel
)

private fun SkatteopplysningDto.tilPersonDataSkattopplysning() = SkatteopplysningData(
    hendelseId = this.hendelseId.id,
    beløp = this.beløp.beløp,
    måned = this.måned,
    type = when (this.type) {
        InntekttypeDto.LØNNSINNTEKT -> SkatteopplysningData.InntekttypeData.LØNNSINNTEKT
        InntekttypeDto.NÆRINGSINNTEKT -> SkatteopplysningData.InntekttypeData.NÆRINGSINNTEKT
        InntekttypeDto.PENSJON_ELLER_TRYGD -> SkatteopplysningData.InntekttypeData.PENSJON_ELLER_TRYGD
        InntekttypeDto.YTELSE_FRA_OFFENTLIGE -> SkatteopplysningData.InntekttypeData.YTELSE_FRA_OFFENTLIGE
    },
    fordel = fordel,
    beskrivelse = beskrivelse,
    tidsstempel = tidsstempel
)

private fun BeløpstidslinjeDto.tilPersonData() = PersonData.BeløpstidslinjeData(
    perioder = this.perioder.map {
        PersonData.BeløpstidslinjeperiodeData(
            fom = it.fom,
            tom = it.tom,
            dagligBeløp = it.dagligBeløp,
            meldingsreferanseId = it.kilde.meldingsreferanseId.id,
            avsender = it.kilde.avsender.tilPersonData(),
            tidsstempel = it.kilde.tidsstempel
        )
    }
)

private fun RefusjonsservitørDto.tilPersonData() = refusjonstidslinjer.mapValues { (_, beløpstidslinje) -> beløpstidslinje.tilPersonData() }

fun SelvstendigInntektsopplysningUtDto.tilPersonData(): PersonData.VilkårsgrunnlagElementData.SelvstendigInntektsopplysningData {
    fun SkjønnsmessigFastsattUtDto.tilPersonData() = PersonData.VilkårsgrunnlagElementData.SelvstendigInntektsopplysningData.SkjønnsmessigFastsattData(
        id = this.id,
        dato = this.inntektsdata.dato,
        hendelseId = this.inntektsdata.hendelseId.id,
        beløp = this.inntektsdata.beløp.månedligDouble.beløp,
        tidsstempel = this.inntektsdata.tidsstempel
    )

    return PersonData.VilkårsgrunnlagElementData.SelvstendigInntektsopplysningData(
        inntektsopplysning = this.faktaavklartInntekt.tilPersonData(),
        skjønnsmessigFastsatt = this.skjønnsmessigFastsatt?.tilPersonData()
    )
}

fun SelvstendigFaktaavklartInntektUtDto.tilPersonData(): PersonData.VilkårsgrunnlagElementData.SelvstendigInntektsopplysningData.InntektsopplysningData = PersonData.VilkårsgrunnlagElementData.SelvstendigInntektsopplysningData.InntektsopplysningData(
    id = this.id,
    dato = this.inntektsdata.dato,
    hendelseId = this.inntektsdata.hendelseId.id,
    beløp = this.inntektsdata.beløp.månedligDouble.beløp,
    tidsstempel = this.inntektsdata.tidsstempel,
    skatteopplysninger = null,
    pensjonsgivendeInntekter = this.pensjonsgivendeInntekter.map {
        PersonData.VilkårsgrunnlagElementData.SelvstendigInntektsopplysningData.InntektsopplysningData.PensjonsgivendeInntektData(
            årstall = it.årstall.value,
            årligBeløp = it.beløp.årlig.beløp
        )
    },
    anvendtÅrligGrunnbeløp = this.anvendtGrunnbeløp.årlig.beløp
)

fun ForberedendeVilkårsgrunnlagDto.tilPersonData() = PersonData.ArbeidsgiverData.VedtaksperiodeData.BehandlingData.EndringData.ForberedendeVilkårsgrunnlagData(this.erOpptjeningVurdertOk)
