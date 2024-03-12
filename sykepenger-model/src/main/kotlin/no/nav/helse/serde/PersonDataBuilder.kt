package no.nav.helse.serde

import java.time.LocalDate
import no.nav.helse.hendelser.SimuleringResultat
import no.nav.helse.dto.ArbeidsgiverInntektsopplysningForSammenligningsgrunnlagDto
import no.nav.helse.dto.ArbeidsgiverInntektsopplysningDto
import no.nav.helse.dto.ArbeidsgiverDto
import no.nav.helse.dto.AvsenderDto
import no.nav.helse.dto.BegrunnelseDto
import no.nav.helse.dto.SykdomstidslinjeDagDto
import no.nav.helse.dto.DokumentsporingDto
import no.nav.helse.dto.DokumenttypeDto
import no.nav.helse.dto.EndringIRefusjonDto
import no.nav.helse.dto.EndringskodeDto
import no.nav.helse.dto.FagområdeDto
import no.nav.helse.dto.FeriepengeDto
import no.nav.helse.dto.ForkastetVedtaksperiodeDto
import no.nav.helse.dto.GenerasjonEndringDto
import no.nav.helse.dto.GenerasjonDto
import no.nav.helse.dto.GenerasjonTilstandDto
import no.nav.helse.dto.GenerasjonkildeDto
import no.nav.helse.dto.HendelseskildeDto
import no.nav.helse.dto.InfotrygdArbeidsgiverutbetalingsperiodeDto
import no.nav.helse.dto.InfotrygdFerieperiodeDto
import no.nav.helse.dto.InfotrygdInntektsopplysningDto
import no.nav.helse.dto.InfotrygdPersonutbetalingsperiodeDto
import no.nav.helse.dto.InfotrygdhistorikkelementDto
import no.nav.helse.dto.InntektsopplysningDto
import no.nav.helse.dto.InntekttypeDto
import no.nav.helse.dto.KlassekodeDto
import no.nav.helse.dto.MedlemskapsvurderingDto
import no.nav.helse.dto.OppdragDto
import no.nav.helse.dto.OppdragstatusDto
import no.nav.helse.dto.OpptjeningDto
import no.nav.helse.dto.PersonDto
import no.nav.helse.dto.RefusjonDto
import no.nav.helse.dto.RefusjonsopplysningDto
import no.nav.helse.dto.SammenligningsgrunnlagDto
import no.nav.helse.dto.SatstypeDto
import no.nav.helse.dto.SkatteopplysningDto
import no.nav.helse.dto.SykdomshistorikkElementDto
import no.nav.helse.dto.SykdomstidslinjeDto
import no.nav.helse.dto.SykepengegrunnlagDto
import no.nav.helse.dto.SykmeldingsperioderDto
import no.nav.helse.dto.UtbetalingDto
import no.nav.helse.dto.UtbetalingTilstandDto
import no.nav.helse.dto.UtbetalingVurderingDto
import no.nav.helse.dto.UtbetalingsdagDto
import no.nav.helse.dto.UtbetalingslinjeDto
import no.nav.helse.dto.UtbetalingstidslinjeDto
import no.nav.helse.dto.UtbetalingtypeDto
import no.nav.helse.dto.UtbetaltDagDto
import no.nav.helse.dto.VedtaksperiodeDto
import no.nav.helse.dto.VedtaksperiodetilstandDto
import no.nav.helse.dto.VilkårsgrunnlagInnslagDto
import no.nav.helse.dto.VilkårsgrunnlagDto
import no.nav.helse.nesteDag
import no.nav.helse.person.Person
import no.nav.helse.person.TilstandType
import no.nav.helse.serde.PersonData.ArbeidsgiverData.SykdomstidslinjeData.DagData
import no.nav.helse.serde.PersonData.UtbetalingstidslinjeData.UtbetalingsdagData
import no.nav.helse.serde.mapping.JsonMedlemskapstatus
import no.nav.helse.utbetalingslinjer.Oppdragstatus

fun Person.tilSerialisertPerson(pretty: Boolean = false): SerialisertPerson {
    return tilPersonData().tilSerialisertPerson(pretty)
}

private fun Person.tilPersonData() = dto().tilPersonData()
internal fun PersonData.tilSerialisertPerson(pretty: Boolean = false): SerialisertPerson {
    val node = SerialisertPerson.medSkjemaversjon(serdeObjectMapper.valueToTree(this))
    return SerialisertPerson(if (pretty) node.toPrettyString() else node.toString())
}
internal fun PersonDto.tilPersonData() = PersonData(
    aktørId = this.aktørId,
    fødselsdato = this.alder.fødselsdato,
    fødselsnummer = this.fødselsnummer,
    opprettet = this.opprettet,
    arbeidsgivere = this.arbeidsgivere.map { it.tilPersonData() },
    infotrygdhistorikk = this.infotrygdhistorikk.elementer.map { it.tilPersonData() },
    vilkårsgrunnlagHistorikk = vilkårsgrunnlagHistorikk.historikk.map { it.tilPersonData() },
    dødsdato = this.alder.dødsdato
)

private fun ArbeidsgiverDto.tilPersonData() = PersonData.ArbeidsgiverData(
    id = this.id,
    organisasjonsnummer = this.organisasjonsnummer,
    inntektshistorikk = this.inntektshistorikk.historikk.map { it.tilPersonData() },
    sykdomshistorikk = this.sykdomshistorikk.elementer.map { it.tilPersonData() },
    sykmeldingsperioder = this.sykmeldingsperioder.tilPersonData(),
    vedtaksperioder = this.vedtaksperioder.map { it.tilPersonData() },
    forkastede = this.forkastede.map { it.tilPersonData() },
    utbetalinger = this.utbetalinger.map { it.tilPersonData() },
    feriepengeutbetalinger = this.feriepengeutbetalinger.map { it.tilPersonData() },
    refusjonshistorikk = this.refusjonshistorikk.refusjoner.map { it.tilPersonData() }
)

private fun InntektsopplysningDto.InntektsmeldingDto.tilPersonData() = PersonData.ArbeidsgiverData.InntektsmeldingData(
    id = this.id,
    dato = this.dato,
    hendelseId = this.hendelseId,
    beløp = this.beløp.beløp,
    tidsstempel = this.tidsstempel
)

private fun SykdomshistorikkElementDto.tilPersonData() = PersonData.SykdomshistorikkData(
    id = this.id,
    tidsstempel = this.tidsstempel,
    hendelseId = this.hendelseId,
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
    return utenDatoer(this) == utenDatoer(other) && (dato ?: tom!!).nesteDag == other.dato
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
        grad = this.økonomi.grad.prosent,
        other = null,
        melding = null,
        dato = dato,
        fom = null,
        tom = null
    )
    is SykdomstidslinjeDagDto.ArbeidsgiverdagDto -> DagData(
        type = PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.ARBEIDSGIVERDAG,
        kilde = this.kilde.tilPersonData(),
        grad = this.økonomi.grad.prosent,
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
        grad = this.økonomi.grad.prosent,
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
        grad = this.økonomi.grad.prosent,
        other = null,
        melding = null,
        dato = dato,
        fom = null,
        tom = null
    )
    is SykdomstidslinjeDagDto.SykedagDto -> DagData(
        type = PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.SYKEDAG,
        kilde = this.kilde.tilPersonData(),
        grad = this.økonomi.grad.prosent,
        other = null,
        melding = null,
        dato = dato,
        fom = null,
        tom = null
    )
    is SykdomstidslinjeDagDto.SykedagNavDto -> DagData(
        type = PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.SYKEDAG_NAV,
        kilde = this.kilde.tilPersonData(),
        grad = this.økonomi.grad.prosent,
        other = null,
        melding = null,
        dato = dato,
        fom = null,
        tom = null
    )
}
private fun HendelseskildeDto.tilPersonData() = PersonData.ArbeidsgiverData.SykdomstidslinjeData.KildeData(
    type = this.type,
    id = this.meldingsreferanseId,
    tidsstempel = this.tidsstempel
)
private fun RefusjonDto.tilPersonData() = PersonData.ArbeidsgiverData.RefusjonData(
    meldingsreferanseId = this.meldingsreferanseId,
    førsteFraværsdag = this.førsteFraværsdag,
    arbeidsgiverperioder = this.arbeidsgiverperioder.map { PersonData.ArbeidsgiverData.PeriodeData(it.fom, it.tom) },
    beløp = this.beløp?.beløp,
    sisteRefusjonsdag = this.sisteRefusjonsdag,
    endringerIRefusjon = this.endringerIRefusjon.map { it.tilPersonData() },
    tidsstempel = this.tidsstempel
)
private fun EndringIRefusjonDto.tilPersonData() = PersonData.ArbeidsgiverData.RefusjonData.EndringIRefusjonData(
    beløp = this.beløp.beløp,
    endringsdato = this.endringsdato
)

private fun SykmeldingsperioderDto.tilPersonData() = perioder.map {
    PersonData.ArbeidsgiverData.SykmeldingsperiodeData(it.fom, it.tom)
}
private fun ForkastetVedtaksperiodeDto.tilPersonData() = PersonData.ArbeidsgiverData.ForkastetVedtaksperiodeData(
    vedtaksperiode = this.vedtaksperiode.tilPersonData()
)
private fun VedtaksperiodeDto.tilPersonData() = PersonData.ArbeidsgiverData.VedtaksperiodeData(
    id = id,
    tilstand = when (tilstand) {
        VedtaksperiodetilstandDto.AVSLUTTET -> TilstandType.AVSLUTTET
        VedtaksperiodetilstandDto.AVSLUTTET_UTEN_UTBETALING -> TilstandType.AVSLUTTET_UTEN_UTBETALING
        VedtaksperiodetilstandDto.AVVENTER_BLOKKERENDE_PERIODE -> TilstandType.AVVENTER_BLOKKERENDE_PERIODE
        VedtaksperiodetilstandDto.AVVENTER_GODKJENNING -> TilstandType.AVVENTER_GODKJENNING
        VedtaksperiodetilstandDto.AVVENTER_GODKJENNING_REVURDERING -> TilstandType.AVVENTER_GODKJENNING_REVURDERING
        VedtaksperiodetilstandDto.AVVENTER_HISTORIKK -> TilstandType.AVVENTER_HISTORIKK
        VedtaksperiodetilstandDto.AVVENTER_HISTORIKK_REVURDERING -> TilstandType.AVVENTER_HISTORIKK_REVURDERING
        VedtaksperiodetilstandDto.AVVENTER_INFOTRYGDHISTORIKK -> TilstandType.AVVENTER_INFOTRYGDHISTORIKK
        VedtaksperiodetilstandDto.AVVENTER_INNTEKTSMELDING -> TilstandType.AVVENTER_INNTEKTSMELDING
        VedtaksperiodetilstandDto.AVVENTER_REVURDERING -> TilstandType.AVVENTER_REVURDERING
        VedtaksperiodetilstandDto.AVVENTER_SIMULERING -> TilstandType.AVVENTER_SIMULERING
        VedtaksperiodetilstandDto.AVVENTER_SIMULERING_REVURDERING -> TilstandType.AVVENTER_SIMULERING_REVURDERING
        VedtaksperiodetilstandDto.AVVENTER_VILKÅRSPRØVING -> TilstandType.AVVENTER_VILKÅRSPRØVING
        VedtaksperiodetilstandDto.AVVENTER_VILKÅRSPRØVING_REVURDERING -> TilstandType.AVVENTER_VILKÅRSPRØVING_REVURDERING
        VedtaksperiodetilstandDto.REVURDERING_FEILET -> TilstandType.REVURDERING_FEILET
        VedtaksperiodetilstandDto.START -> TilstandType.START
        VedtaksperiodetilstandDto.TIL_INFOTRYGD -> TilstandType.TIL_INFOTRYGD
        VedtaksperiodetilstandDto.TIL_UTBETALING -> TilstandType.TIL_UTBETALING
    },
    generasjoner = generasjoner.generasjoner.map { it.tilPersonData() },
    opprettet = opprettet,
    oppdatert = oppdatert
)
private fun GenerasjonDto.tilPersonData() = PersonData.ArbeidsgiverData.VedtaksperiodeData.GenerasjonData(
    id = this.id,
    tilstand = when (this.tilstand) {
        GenerasjonTilstandDto.ANNULLERT_PERIODE -> error("Forventer ikke å serialisere ${this.tilstand}")
        GenerasjonTilstandDto.AVSLUTTET_UTEN_VEDTAK -> PersonData.ArbeidsgiverData.VedtaksperiodeData.GenerasjonData.TilstandData.AVSLUTTET_UTEN_VEDTAK
        GenerasjonTilstandDto.BEREGNET -> PersonData.ArbeidsgiverData.VedtaksperiodeData.GenerasjonData.TilstandData.BEREGNET
        GenerasjonTilstandDto.BEREGNET_OMGJØRING -> PersonData.ArbeidsgiverData.VedtaksperiodeData.GenerasjonData.TilstandData.BEREGNET_OMGJØRING
        GenerasjonTilstandDto.BEREGNET_REVURDERING -> PersonData.ArbeidsgiverData.VedtaksperiodeData.GenerasjonData.TilstandData.BEREGNET_REVURDERING
        GenerasjonTilstandDto.REVURDERT_VEDTAK_AVVIST -> PersonData.ArbeidsgiverData.VedtaksperiodeData.GenerasjonData.TilstandData.REVURDERT_VEDTAK_AVVIST
        GenerasjonTilstandDto.TIL_INFOTRYGD -> PersonData.ArbeidsgiverData.VedtaksperiodeData.GenerasjonData.TilstandData.TIL_INFOTRYGD
        GenerasjonTilstandDto.UBEREGNET -> PersonData.ArbeidsgiverData.VedtaksperiodeData.GenerasjonData.TilstandData.UBEREGNET
        GenerasjonTilstandDto.UBEREGNET_OMGJØRING -> PersonData.ArbeidsgiverData.VedtaksperiodeData.GenerasjonData.TilstandData.UBEREGNET_OMGJØRING
        GenerasjonTilstandDto.UBEREGNET_REVURDERING -> PersonData.ArbeidsgiverData.VedtaksperiodeData.GenerasjonData.TilstandData.UBEREGNET_REVURDERING
        GenerasjonTilstandDto.VEDTAK_FATTET -> PersonData.ArbeidsgiverData.VedtaksperiodeData.GenerasjonData.TilstandData.VEDTAK_FATTET
        GenerasjonTilstandDto.VEDTAK_IVERKSATT -> PersonData.ArbeidsgiverData.VedtaksperiodeData.GenerasjonData.TilstandData.VEDTAK_IVERKSATT
    },
    vedtakFattet = this.vedtakFattet,
    avsluttet = this.avsluttet,
    kilde = this.kilde.tilPersonData(),
    endringer = this.endringer.map { it.tilPersonData() }
)
private fun GenerasjonkildeDto.tilPersonData() = PersonData.ArbeidsgiverData.VedtaksperiodeData.GenerasjonData.KildeData(
    meldingsreferanseId = this.meldingsreferanseId,
    innsendt = this.innsendt,
    registrert = this.registert,
    avsender = when (this.avsender) {
        AvsenderDto.ARBEIDSGIVER -> PersonData.ArbeidsgiverData.VedtaksperiodeData.GenerasjonData.AvsenderData.ARBEIDSGIVER
        AvsenderDto.SAKSBEHANDLER -> PersonData.ArbeidsgiverData.VedtaksperiodeData.GenerasjonData.AvsenderData.SAKSBEHANDLER
        AvsenderDto.SYKMELDT -> PersonData.ArbeidsgiverData.VedtaksperiodeData.GenerasjonData.AvsenderData.SYKMELDT
        AvsenderDto.SYSTEM -> PersonData.ArbeidsgiverData.VedtaksperiodeData.GenerasjonData.AvsenderData.SYSTEM
    }
)
private fun GenerasjonEndringDto.tilPersonData() = PersonData.ArbeidsgiverData.VedtaksperiodeData.GenerasjonData.EndringData(
    id = id,
    tidsstempel = tidsstempel,
    sykmeldingsperiodeFom = sykmeldingsperiode.fom,
    sykmeldingsperiodeTom = sykmeldingsperiode.tom,
    fom = periode.fom,
    tom = periode.tom,
    utbetalingId = utbetalingId,
    vilkårsgrunnlagId = vilkårsgrunnlagId,
    sykdomstidslinje = sykdomstidslinje.tilPersonData(),
    dokumentsporing = dokumentsporing.tilPersonData()
)
private fun DokumentsporingDto.tilPersonData() = PersonData.ArbeidsgiverData.VedtaksperiodeData.DokumentsporingData(
    dokumentId = this.id,
    dokumenttype = when (type) {
        DokumenttypeDto.InntektsmeldingDager -> PersonData.ArbeidsgiverData.VedtaksperiodeData.DokumentTypeData.InntektsmeldingDager
        DokumenttypeDto.InntektsmeldingInntekt -> PersonData.ArbeidsgiverData.VedtaksperiodeData.DokumentTypeData.InntektsmeldingInntekt
        DokumenttypeDto.OverstyrArbeidsforhold -> PersonData.ArbeidsgiverData.VedtaksperiodeData.DokumentTypeData.OverstyrArbeidsforhold
        DokumenttypeDto.OverstyrArbeidsgiveropplysninger -> PersonData.ArbeidsgiverData.VedtaksperiodeData.DokumentTypeData.OverstyrArbeidsgiveropplysninger
        DokumenttypeDto.OverstyrInntekt -> PersonData.ArbeidsgiverData.VedtaksperiodeData.DokumentTypeData.OverstyrInntekt
        DokumenttypeDto.OverstyrRefusjon -> PersonData.ArbeidsgiverData.VedtaksperiodeData.DokumentTypeData.OverstyrRefusjon
        DokumenttypeDto.OverstyrTidslinje -> PersonData.ArbeidsgiverData.VedtaksperiodeData.DokumentTypeData.OverstyrTidslinje
        DokumenttypeDto.SkjønnsmessigFastsettelse -> PersonData.ArbeidsgiverData.VedtaksperiodeData.DokumentTypeData.SkjønnsmessigFastsettelse
        DokumenttypeDto.Sykmelding -> PersonData.ArbeidsgiverData.VedtaksperiodeData.DokumentTypeData.Sykmelding
        DokumenttypeDto.Søknad -> PersonData.ArbeidsgiverData.VedtaksperiodeData.DokumentTypeData.Søknad
    }
)
private fun UtbetalingDto.tilPersonData() = PersonData.UtbetalingData(
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
        UtbetalingtypeDto.ANNULLERING -> "ANNULLERING"
        UtbetalingtypeDto.ETTERUTBETALING -> "ETTERUTBETALING"
        UtbetalingtypeDto.FERIEPENGER -> "FERIEPENGER"
        UtbetalingtypeDto.REVURDERING -> "REVURDERING"
        UtbetalingtypeDto.UTBETALING -> "UTBETALING"
    },
    status = when (this.tilstand) {
        UtbetalingTilstandDto.ANNULLERT -> "ANNULLERT"
        UtbetalingTilstandDto.FORKASTET -> "FORKASTET"
        UtbetalingTilstandDto.GODKJENT -> "GODKJENT"
        UtbetalingTilstandDto.GODKJENT_UTEN_UTBETALING -> "GODKJENT_UTEN_UTBETALING"
        UtbetalingTilstandDto.IKKE_GODKJENT -> "IKKE_GODKJENT"
        UtbetalingTilstandDto.IKKE_UTBETALT -> "IKKE_UTBETALT"
        UtbetalingTilstandDto.NY -> "NY"
        UtbetalingTilstandDto.OVERFØRT -> "OVERFØRT"
        UtbetalingTilstandDto.UTBETALT -> "UTBETALT"
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
private fun UtbetalingstidslinjeDto.tilPersonData() = PersonData.UtbetalingstidslinjeData(
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
    return utenDatoer(this) == utenDatoer(other) && (dato ?: tom!!).nesteDag == other.dato
}

private fun UtbetalingsdagDto.tilPersonData() = UtbetalingsdagData(
    type = when (this) {
        is UtbetalingsdagDto.ArbeidsdagDto -> PersonData.UtbetalingstidslinjeData.TypeData.Arbeidsdag
        is UtbetalingsdagDto.ArbeidsgiverperiodeDagDto -> PersonData.UtbetalingstidslinjeData.TypeData.ArbeidsgiverperiodeDag
        is UtbetalingsdagDto.ArbeidsgiverperiodeDagNavDto -> PersonData.UtbetalingstidslinjeData.TypeData.ArbeidsgiverperiodedagNav
        is UtbetalingsdagDto.AvvistDagDto -> PersonData.UtbetalingstidslinjeData.TypeData.AvvistDag
        is UtbetalingsdagDto.ForeldetDagDto -> PersonData.UtbetalingstidslinjeData.TypeData.ForeldetDag
        is UtbetalingsdagDto.FridagDto -> PersonData.UtbetalingstidslinjeData.TypeData.Fridag
        is UtbetalingsdagDto.NavDagDto -> PersonData.UtbetalingstidslinjeData.TypeData.NavDag
        is UtbetalingsdagDto.NavHelgDagDto -> PersonData.UtbetalingstidslinjeData.TypeData.NavHelgDag
        is UtbetalingsdagDto.UkjentDagDto -> PersonData.UtbetalingstidslinjeData.TypeData.UkjentDag
    },
    aktuellDagsinntekt = this.økonomi.aktuellDagsinntekt.beløp,
    beregningsgrunnlag = this.økonomi.beregningsgrunnlag.beløp,
    dekningsgrunnlag = this.økonomi.dekningsgrunnlag.beløp,
    grunnbeløpgrense = this.økonomi.grunnbeløpgrense?.beløp,
    begrunnelser = when (this) {
        is UtbetalingsdagDto.AvvistDagDto -> this.begrunnelser.map { it.tilPersonData() }
        else -> null
    },
    grad = this.økonomi.grad.prosent,
    totalGrad = this.økonomi.totalGrad.prosent,
    arbeidsgiverRefusjonsbeløp = økonomi.arbeidsgiverRefusjonsbeløp.beløp,
    arbeidsgiverbeløp = this.økonomi.arbeidsgiverbeløp?.beløp,
    personbeløp = this.økonomi.personbeløp?.beløp,
    er6GBegrenset = this.økonomi.er6GBegrenset,
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
private fun OppdragDto.tilPersonData() = PersonData.OppdragData(
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
        OppdragstatusDto.AKSEPTERT -> Oppdragstatus.AKSEPTERT
        OppdragstatusDto.AKSEPTERT_MED_FEIL -> Oppdragstatus.AKSEPTERT_MED_FEIL
        OppdragstatusDto.AVVIST -> Oppdragstatus.AVVIST
        OppdragstatusDto.FEIL -> Oppdragstatus.FEIL
        OppdragstatusDto.OVERFØRT -> Oppdragstatus.OVERFØRT
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
private fun UtbetalingslinjeDto.tilPersonData() = PersonData.UtbetalingslinjeData(
    fom = this.fom,
    tom = this.tom,
    satstype = when (this.satstype) {
        SatstypeDto.Daglig -> "DAG"
        SatstypeDto.Engang -> "ENG"
    },
    sats = this.beløp!!,
    grad = this.grad,
    refFagsystemId = this.refFagsystemId,
    delytelseId = this.delytelseId,
    refDelytelseId = this.refDelytelseId,
    endringskode = this.endringskode.tilPersonData(),
    klassekode = this.klassekode.tilPersonData(),
    datoStatusFom = this.datoStatusFom
)
private fun KlassekodeDto.tilPersonData() = when (this) {
    KlassekodeDto.RefusjonFeriepengerIkkeOpplysningspliktig -> "SPREFAGFER-IOP"
    KlassekodeDto.RefusjonIkkeOpplysningspliktig -> "SPREFAG-IOP"
    KlassekodeDto.SykepengerArbeidstakerFeriepenger -> "SPATFER"
    KlassekodeDto.SykepengerArbeidstakerOrdinær -> "SPATORD"
}
private fun SimuleringResultat.tilPersonData() = PersonData.ArbeidsgiverData.VedtaksperiodeData.DataForSimuleringData(
    totalbeløp = this.totalbeløp,
    perioder = this.perioder.map {
        PersonData.ArbeidsgiverData.VedtaksperiodeData.DataForSimuleringData.SimulertPeriode(
            fom = it.periode.start,
            tom = it.periode.endInclusive,

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
                            fom = it.periode.start,
                            tom = it.periode.endInclusive,
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
private fun FeriepengeDto.tilPersonData() = PersonData.ArbeidsgiverData.FeriepengeutbetalingData(
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
private fun UtbetaltDagDto.tilPersonData() = PersonData.ArbeidsgiverData.FeriepengeutbetalingData.UtbetaltDagData(
    type = when (this) {
        is UtbetaltDagDto.InfotrygdArbeidsgiver -> "InfotrygdArbeidsgiverDag"
        is UtbetaltDagDto.InfotrygdPerson -> "InfotrygdPersonDag"
        is UtbetaltDagDto.SpleisArbeidsgiver -> "SpleisArbeidsgiverDag"
        is UtbetaltDagDto.SpleisPerson -> "SpleisPersonDag"
    },
    orgnummer = orgnummer,
    dato = dato,
    beløp = beløp
)
private fun InfotrygdhistorikkelementDto.tilPersonData() = PersonData.InfotrygdhistorikkElementData(
    id = this.id,
    tidsstempel = this.tidsstempel,
    hendelseId = this.hendelseId,
    ferieperioder = this.ferieperioder.map { it.tilPersonData() },
    arbeidsgiverutbetalingsperioder = this.arbeidsgiverutbetalingsperioder.map { it.tilPersonData() },
    personutbetalingsperioder = this.personutbetalingsperioder.map { it.tilPersonData() },
    inntekter = this.inntekter.map { it.tilPersonData() },
    arbeidskategorikoder = arbeidskategorikoder,
    oppdatert = oppdatert
)
private fun InfotrygdFerieperiodeDto.tilPersonData() = PersonData.InfotrygdhistorikkElementData.FerieperiodeData(
    fom = this.periode.fom,
    tom = this.periode.tom
)
private fun InfotrygdArbeidsgiverutbetalingsperiodeDto.tilPersonData() = PersonData.InfotrygdhistorikkElementData.ArbeidsgiverutbetalingsperiodeData(
    orgnr = this.orgnr,
    fom = this.periode.fom,
    tom = this.periode.tom,
    grad = this.grad.prosent,
    inntekt = this.inntekt.beløp
)
private fun InfotrygdPersonutbetalingsperiodeDto.tilPersonData() = PersonData.InfotrygdhistorikkElementData.PersonutbetalingsperiodeData(
    orgnr = this.orgnr,
    fom = this.periode.fom,
    tom = this.periode.tom,
    grad = this.grad.prosent,
    inntekt = this.inntekt.beløp
)
private fun InfotrygdInntektsopplysningDto.tilPersonData() = PersonData.InfotrygdhistorikkElementData.InntektsopplysningData(
    orgnr = this.orgnummer,
    sykepengerFom = this.sykepengerFom,
    inntekt = this.inntekt.beløp,
    refusjonTilArbeidsgiver = refusjonTilArbeidsgiver,
    refusjonTom = refusjonTom,
    lagret = lagret
)
private fun VilkårsgrunnlagInnslagDto.tilPersonData() = PersonData.VilkårsgrunnlagInnslagData(
    id = this.id,
    opprettet = this.opprettet,
    vilkårsgrunnlag = this.vilkårsgrunnlag.map { it.tilPersonData() }
)
private fun VilkårsgrunnlagDto.tilPersonData() = PersonData.VilkårsgrunnlagElementData(
    skjæringstidspunkt = this.skjæringstidspunkt,
    type = when (this) {
        is VilkårsgrunnlagDto.Infotrygd -> PersonData.VilkårsgrunnlagElementData.GrunnlagsdataType.Infotrygd
        is VilkårsgrunnlagDto.Spleis -> PersonData.VilkårsgrunnlagElementData.GrunnlagsdataType.Vilkårsprøving
    },
    sykepengegrunnlag = this.sykepengegrunnlag.tilPersonData(),
    opptjening = when (this) {
        is VilkårsgrunnlagDto.Spleis -> this.opptjening.tilPersonData()
        is VilkårsgrunnlagDto.Infotrygd -> null
    },
    medlemskapstatus = when (this) {
        is VilkårsgrunnlagDto.Spleis -> when (this.medlemskapstatus) {
            MedlemskapsvurderingDto.Ja -> JsonMedlemskapstatus.JA
            MedlemskapsvurderingDto.Nei -> JsonMedlemskapstatus.NEI
            MedlemskapsvurderingDto.UavklartMedBrukerspørsmål -> JsonMedlemskapstatus.UAVKLART_MED_BRUKERSPØRSMÅL
            MedlemskapsvurderingDto.VetIkke -> JsonMedlemskapstatus.VET_IKKE
        }
        else -> null
    },
    vurdertOk = when (this) {
        is VilkårsgrunnlagDto.Spleis -> this.vurdertOk
        else -> null
    },
    meldingsreferanseId = when (this) {
        is VilkårsgrunnlagDto.Spleis -> this.meldingsreferanseId
        else -> null
    },
    vilkårsgrunnlagId = this.vilkårsgrunnlagId
)

private fun OpptjeningDto.tilPersonData() = PersonData.VilkårsgrunnlagElementData.OpptjeningData(
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
private fun SykepengegrunnlagDto.tilPersonData() = PersonData.VilkårsgrunnlagElementData.SykepengegrunnlagData(
    grunnbeløp = this.`6G`.beløp,
    arbeidsgiverInntektsopplysninger = this.arbeidsgiverInntektsopplysninger.map { it.tilPersonData() },
    sammenligningsgrunnlag = this.sammenligningsgrunnlag.tilPersonData(),
    deaktiverteArbeidsforhold = this.deaktiverteArbeidsforhold.map { it.tilPersonData() },
    vurdertInfotrygd = this.vurdertInfotrygd
)

private fun ArbeidsgiverInntektsopplysningDto.tilPersonData() = PersonData.VilkårsgrunnlagElementData.ArbeidsgiverInntektsopplysningData(
    orgnummer = this.orgnummer,
    fom = this.gjelder.fom,
    tom = this.gjelder.tom,
    inntektsopplysning = this.inntektsopplysning.tilPersonData(),
    refusjonsopplysninger = this.refusjonsopplysninger.opplysninger.map {
        it.tilPersonData()
    }
)

private fun InntektsopplysningDto.tilPersonData() = PersonData.VilkårsgrunnlagElementData.ArbeidsgiverInntektsopplysningData.InntektsopplysningData(
    id = this.id,
    dato = this.dato,
    hendelseId = this.hendelseId,
    beløp = when (this) {
        is InntektsopplysningDto.SkattSykepengegrunnlagDto -> null
        is InntektsopplysningDto.IkkeRapportertDto -> null
        is InntektsopplysningDto.InfotrygdDto -> this.beløp.beløp
        is InntektsopplysningDto.InntektsmeldingDto -> this.beløp.beløp
        is InntektsopplysningDto.SaksbehandlerDto -> this.beløp.beløp
        is InntektsopplysningDto.SkjønnsmessigFastsattDto -> this.beløp.beløp
    },
    kilde = when (this) {
        is InntektsopplysningDto.IkkeRapportertDto -> "IKKE_RAPPORTERT"
        is InntektsopplysningDto.InfotrygdDto -> "INFOTRYGD"
        is InntektsopplysningDto.InntektsmeldingDto -> "INNTEKTSMELDING"
        is InntektsopplysningDto.SaksbehandlerDto -> "SAKSBEHANDLER"
        is InntektsopplysningDto.SkattSykepengegrunnlagDto -> "SKATT_SYKEPENGEGRUNNLAG"
        is InntektsopplysningDto.SkjønnsmessigFastsattDto -> "SKJØNNSMESSIG_FASTSATT"
    },
    forklaring = when (this) {
        is InntektsopplysningDto.SaksbehandlerDto -> this.forklaring
        else -> null
    },
    subsumsjon = when (this) {
        is InntektsopplysningDto.SaksbehandlerDto -> this.subsumsjon?.let {
            PersonData.VilkårsgrunnlagElementData.ArbeidsgiverInntektsopplysningData.InntektsopplysningData.SubsumsjonData(
                paragraf = it.paragraf,
                bokstav = it.bokstav,
                ledd = it.ledd
            )
        }
        else -> null
    },
    tidsstempel = this.tidsstempel,
    overstyrtInntektId = when (this) {
        is InntektsopplysningDto.SaksbehandlerDto -> this.overstyrtInntekt.id
        is InntektsopplysningDto.SkjønnsmessigFastsattDto -> this.overstyrtInntekt.id
        else -> null
    },
    skatteopplysninger = when (this) {
        is InntektsopplysningDto.SkattSykepengegrunnlagDto -> this.inntektsopplysninger.map { it.tilPersonDataSkattopplysning() }
        else -> null
    }
)

private fun RefusjonsopplysningDto.tilPersonData() = PersonData.ArbeidsgiverData.RefusjonsopplysningData(
    meldingsreferanseId = this.meldingsreferanseId,
    fom = this.fom,
    tom = this.tom,
    beløp = this.beløp.beløp
)

private fun SammenligningsgrunnlagDto.tilPersonData() = PersonData.VilkårsgrunnlagElementData.SammenligningsgrunnlagData(
    sammenligningsgrunnlag = this.sammenligningsgrunnlag.beløp,
    arbeidsgiverInntektsopplysninger = this.arbeidsgiverInntektsopplysninger.map { it.tilPersonData() }
)

private fun ArbeidsgiverInntektsopplysningForSammenligningsgrunnlagDto.tilPersonData() = PersonData.VilkårsgrunnlagElementData.ArbeidsgiverInntektsopplysningForSammenligningsgrunnlagData(
    orgnummer = this.orgnummer,
    skatteopplysninger = this.inntektsopplysninger.map { it.tilPersonData() }
)

private fun SkatteopplysningDto.tilPersonData() =
    PersonData.VilkårsgrunnlagElementData.ArbeidsgiverInntektsopplysningForSammenligningsgrunnlagData.SammenligningsgrunnlagInntektsopplysningData(
        hendelseId = this.hendelseId,
        beløp = this.beløp.beløp,
        måned = this.måned,
        type = when (this.type) {
            InntekttypeDto.LØNNSINNTEKT -> PersonData.VilkårsgrunnlagElementData.ArbeidsgiverInntektsopplysningForSammenligningsgrunnlagData.SammenligningsgrunnlagInntektsopplysningData.InntekttypeData.LØNNSINNTEKT
            InntekttypeDto.NÆRINGSINNTEKT -> PersonData.VilkårsgrunnlagElementData.ArbeidsgiverInntektsopplysningForSammenligningsgrunnlagData.SammenligningsgrunnlagInntektsopplysningData.InntekttypeData.NÆRINGSINNTEKT
            InntekttypeDto.PENSJON_ELLER_TRYGD -> PersonData.VilkårsgrunnlagElementData.ArbeidsgiverInntektsopplysningForSammenligningsgrunnlagData.SammenligningsgrunnlagInntektsopplysningData.InntekttypeData.PENSJON_ELLER_TRYGD
            InntekttypeDto.YTELSE_FRA_OFFENTLIGE -> PersonData.VilkårsgrunnlagElementData.ArbeidsgiverInntektsopplysningForSammenligningsgrunnlagData.SammenligningsgrunnlagInntektsopplysningData.InntekttypeData.YTELSE_FRA_OFFENTLIGE
        },
        fordel = fordel,
        beskrivelse = beskrivelse,
        tidsstempel = tidsstempel
    )
private fun SkatteopplysningDto.tilPersonDataSkattopplysning() = PersonData.VilkårsgrunnlagElementData.ArbeidsgiverInntektsopplysningData.SkatteopplysningData(
    hendelseId = this.hendelseId,
    beløp = this.beløp.beløp,
    måned = this.måned,
    type = when (this.type) {
        InntekttypeDto.LØNNSINNTEKT -> "LØNNSINNTEKT"
        InntekttypeDto.NÆRINGSINNTEKT -> "NÆRINGSINNTEKT"
        InntekttypeDto.PENSJON_ELLER_TRYGD -> "PENSJON_ELLER_TRYGD"
        InntekttypeDto.YTELSE_FRA_OFFENTLIGE -> "YTELSE_FRA_OFFENTLIGE"
    },
    fordel = fordel,
    beskrivelse = beskrivelse,
    tidsstempel = tidsstempel
)