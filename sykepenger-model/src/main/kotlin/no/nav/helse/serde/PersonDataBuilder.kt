package no.nav.helse.serde

import no.nav.helse.hendelser.SimuleringResultat
import no.nav.helse.memento.ArbeidsgiverInntektsopplysningForSammenligningsgrunnlagMemento
import no.nav.helse.memento.ArbeidsgiverInntektsopplysningMemento
import no.nav.helse.memento.ArbeidsgiverMemento
import no.nav.helse.memento.AvsenderMemento
import no.nav.helse.memento.BegrunnelseMemento
import no.nav.helse.memento.DagMemento
import no.nav.helse.memento.DokumentsporingMemento
import no.nav.helse.memento.DokumenttypeMemento
import no.nav.helse.memento.EndringIRefusjonMemento
import no.nav.helse.memento.EndringskodeMemento
import no.nav.helse.memento.FagområdeMemento
import no.nav.helse.memento.FeriepengeMemento
import no.nav.helse.memento.ForkastetVedtaksperiodeMemento
import no.nav.helse.memento.GenerasjonEndringMemento
import no.nav.helse.memento.GenerasjonMemento
import no.nav.helse.memento.GenerasjonTilstandMemento
import no.nav.helse.memento.GenerasjonkildeMemento
import no.nav.helse.memento.HendelseskildeMemento
import no.nav.helse.memento.InfotrygdArbeidsgiverutbetalingsperiodeMemento
import no.nav.helse.memento.InfotrygdFerieperiodeMemento
import no.nav.helse.memento.InfotrygdInntektsopplysningMemento
import no.nav.helse.memento.InfotrygdPersonutbetalingsperiodeMemento
import no.nav.helse.memento.InfotrygdhistorikkelementMemento
import no.nav.helse.memento.InntektsopplysningMemento
import no.nav.helse.memento.InntekttypeMemento
import no.nav.helse.memento.KlassekodeMemento
import no.nav.helse.memento.MedlemskapsvurderingMemento
import no.nav.helse.memento.OppdragMemento
import no.nav.helse.memento.OppdragstatusMemento
import no.nav.helse.memento.OpptjeningMemento
import no.nav.helse.memento.PersonMemento
import no.nav.helse.memento.RefusjonMemento
import no.nav.helse.memento.RefusjonsopplysningMemento
import no.nav.helse.memento.SammenligningsgrunnlagMemento
import no.nav.helse.memento.SatstypeMemento
import no.nav.helse.memento.SkatteopplysningMemento
import no.nav.helse.memento.SykdomshistorikkElementMemento
import no.nav.helse.memento.SykdomstidslinjeMemento
import no.nav.helse.memento.SykepengegrunnlagMemento
import no.nav.helse.memento.SykmeldingsperioderMemento
import no.nav.helse.memento.UtbetalingMemento
import no.nav.helse.memento.UtbetalingTilstandMemento
import no.nav.helse.memento.UtbetalingVurderingMemento
import no.nav.helse.memento.UtbetalingsdagMemento
import no.nav.helse.memento.UtbetalingslinjeMemento
import no.nav.helse.memento.UtbetalingstidslinjeMemento
import no.nav.helse.memento.UtbetalingtypeMemento
import no.nav.helse.memento.UtbetaltDagMemento
import no.nav.helse.memento.VedtaksperiodeMemento
import no.nav.helse.memento.VedtaksperiodetilstandMemento
import no.nav.helse.memento.VilkårsgrunnlagInnslagMemento
import no.nav.helse.memento.VilkårsgrunnlagMemento
import no.nav.helse.person.Person
import no.nav.helse.person.TilstandType
import no.nav.helse.serde.mapping.JsonMedlemskapstatus
import no.nav.helse.utbetalingslinjer.Oppdragstatus

fun Person.tilSerialisertPerson(pretty: Boolean = false): SerialisertPerson {
    return tilPersonData().tilSerialisertPerson(pretty)
}

private fun Person.tilPersonData() = memento().tilPersonData()
internal fun PersonData.tilSerialisertPerson(pretty: Boolean = false): SerialisertPerson {
    val node = SerialisertPerson.medSkjemaversjon(serdeObjectMapper.valueToTree(this))
    return SerialisertPerson(if (pretty) node.toPrettyString() else node.toString())
}
internal fun PersonMemento.tilPersonData() = PersonData(
    aktørId = this.aktørId,
    fødselsdato = this.alder.fødselsdato,
    fødselsnummer = this.fødselsnummer,
    opprettet = this.opprettet,
    arbeidsgivere = this.arbeidsgivere.map { it.tilPersonData() },
    infotrygdhistorikk = this.infotrygdhistorikk.elementer.map { it.tilPersonData() },
    vilkårsgrunnlagHistorikk = vilkårsgrunnlagHistorikk.historikk.map { it.tilPersonData() },
    dødsdato = this.alder.dødsdato
)

private fun ArbeidsgiverMemento.tilPersonData() = PersonData.ArbeidsgiverData(
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

private fun InntektsopplysningMemento.InntektsmeldingMemento.tilPersonData() = PersonData.ArbeidsgiverData.InntektsmeldingData(
    id = this.id,
    dato = this.dato,
    hendelseId = this.hendelseId,
    beløp = this.beløp.månedligDouble,
    tidsstempel = this.tidsstempel
)

private fun SykdomshistorikkElementMemento.tilPersonData() = PersonData.SykdomshistorikkData(
    id = this.id,
    tidsstempel = this.tidsstempel,
    hendelseId = this.hendelseId,
    hendelseSykdomstidslinje = this.hendelseSykdomstidslinje.tilPersonData(),
    beregnetSykdomstidslinje = this.beregnetSykdomstidslinje.tilPersonData(),
)
private fun SykdomstidslinjeMemento.tilPersonData() = PersonData.ArbeidsgiverData.SykdomstidslinjeData(
    dager = dager.map { it.tilPersonData() },
    låstePerioder = this.låstePerioder.map { PersonData.ArbeidsgiverData.PeriodeData(it.fom, it.tom) },
    periode = this.periode?.let { PersonData.ArbeidsgiverData.PeriodeData(it.fom, it.tom) }
)
private fun DagMemento.tilPersonData() = when (this) {
    is DagMemento.UkjentDagMemento -> PersonData.ArbeidsgiverData.SykdomstidslinjeData.DagData(
        type = PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.UKJENT_DAG,
        kilde = this.kilde.tilPersonData(),
        grad = 0.0,
        other = null,
        melding = null
    ).apply {
        datoer = DateRange.Single(dato)
    }
    is DagMemento.AndreYtelserMemento -> PersonData.ArbeidsgiverData.SykdomstidslinjeData.DagData(
        type = when (this.ytelse) {
            DagMemento.AndreYtelserMemento.YtelseMemento.AAP -> PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.ANDRE_YTELSER_AAP
            DagMemento.AndreYtelserMemento.YtelseMemento.Foreldrepenger -> PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.ANDRE_YTELSER_FORELDREPENGER
            DagMemento.AndreYtelserMemento.YtelseMemento.Omsorgspenger -> PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.ANDRE_YTELSER_OMSORGSPENGER
            DagMemento.AndreYtelserMemento.YtelseMemento.Pleiepenger -> PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.ANDRE_YTELSER_PLEIEPENGER
            DagMemento.AndreYtelserMemento.YtelseMemento.Svangerskapspenger -> PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.ANDRE_YTELSER_SVANGERSKAPSPENGER
            DagMemento.AndreYtelserMemento.YtelseMemento.Opplæringspenger -> PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.ANDRE_YTELSER_OPPLÆRINGSPENGER
            DagMemento.AndreYtelserMemento.YtelseMemento.Dagpenger -> PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.ANDRE_YTELSER_DAGPENGER
        },
        kilde = this.kilde.tilPersonData(),
        grad = 0.0,
        other = null,
        melding = null
    ).apply {
        datoer = DateRange.Single(dato)
    }
    is DagMemento.ArbeidIkkeGjenopptattDagMemento -> PersonData.ArbeidsgiverData.SykdomstidslinjeData.DagData(
        type = PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.ARBEID_IKKE_GJENOPPTATT_DAG,
        kilde = this.kilde.tilPersonData(),
        grad = 0.0,
        other = null,
        melding = null
    ).apply {
        datoer = DateRange.Single(dato)
    }
    is DagMemento.ArbeidsdagMemento -> PersonData.ArbeidsgiverData.SykdomstidslinjeData.DagData(
        type = PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.ARBEIDSDAG,
        kilde = this.kilde.tilPersonData(),
        grad = 0.0,
        other = null,
        melding = null
    ).apply {
        datoer = DateRange.Single(dato)
    }
    is DagMemento.ArbeidsgiverHelgedagMemento -> PersonData.ArbeidsgiverData.SykdomstidslinjeData.DagData(
        type = PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.ARBEIDSGIVERDAG,
        kilde = this.kilde.tilPersonData(),
        grad = this.økonomi.grad.prosent,
        other = null,
        melding = null
    ).apply {
        datoer = DateRange.Single(dato)
    }
    is DagMemento.ArbeidsgiverdagMemento -> PersonData.ArbeidsgiverData.SykdomstidslinjeData.DagData(
        type = PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.ARBEIDSGIVERDAG,
        kilde = this.kilde.tilPersonData(),
        grad = this.økonomi.grad.prosent,
        other = null,
        melding = null
    ).apply {
        datoer = DateRange.Single(dato)
    }
    is DagMemento.FeriedagMemento -> PersonData.ArbeidsgiverData.SykdomstidslinjeData.DagData(
        type = PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.FERIEDAG,
        kilde = this.kilde.tilPersonData(),
        grad = 0.0,
        other = null,
        melding = null
    ).apply {
        datoer = DateRange.Single(dato)
    }
    is DagMemento.ForeldetSykedagMemento -> PersonData.ArbeidsgiverData.SykdomstidslinjeData.DagData(
        type = PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.FORELDET_SYKEDAG,
        kilde = this.kilde.tilPersonData(),
        grad = this.økonomi.grad.prosent,
        other = null,
        melding = null
    ).apply {
        datoer = DateRange.Single(dato)
    }
    is DagMemento.FriskHelgedagMemento -> PersonData.ArbeidsgiverData.SykdomstidslinjeData.DagData(
        type = PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.FRISK_HELGEDAG,
        kilde = this.kilde.tilPersonData(),
        grad = 0.0,
        other = null,
        melding = null
    ).apply {
        datoer = DateRange.Single(dato)
    }
    is DagMemento.PermisjonsdagMemento -> PersonData.ArbeidsgiverData.SykdomstidslinjeData.DagData(
        type = PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.PERMISJONSDAG,
        kilde = this.kilde.tilPersonData(),
        grad = 0.0,
        other = null,
        melding = null
    ).apply {
        datoer = DateRange.Single(dato)
    }
    is DagMemento.ProblemDagMemento -> PersonData.ArbeidsgiverData.SykdomstidslinjeData.DagData(
        type = PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.PROBLEMDAG,
        kilde = this.kilde.tilPersonData(),
        grad = 0.0,
        other = this.other.tilPersonData(),
        melding = this.melding
    ).apply {
        datoer = DateRange.Single(dato)
    }
    is DagMemento.SykHelgedagMemento -> PersonData.ArbeidsgiverData.SykdomstidslinjeData.DagData(
        type = PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.SYKEDAG,
        kilde = this.kilde.tilPersonData(),
        grad = this.økonomi.grad.prosent,
        other = null,
        melding = null
    ).apply {
        datoer = DateRange.Single(dato)
    }
    is DagMemento.SykedagMemento -> PersonData.ArbeidsgiverData.SykdomstidslinjeData.DagData(
        type = PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.SYKEDAG,
        kilde = this.kilde.tilPersonData(),
        grad = this.økonomi.grad.prosent,
        other = null,
        melding = null
    ).apply {
        datoer = DateRange.Single(dato)
    }
    is DagMemento.SykedagNavMemento -> PersonData.ArbeidsgiverData.SykdomstidslinjeData.DagData(
        type = PersonData.ArbeidsgiverData.SykdomstidslinjeData.JsonDagType.SYKEDAG_NAV,
        kilde = this.kilde.tilPersonData(),
        grad = this.økonomi.grad.prosent,
        other = null,
        melding = null
    ).apply {
        datoer = DateRange.Single(dato)
    }
}
private fun HendelseskildeMemento.tilPersonData() = PersonData.ArbeidsgiverData.SykdomstidslinjeData.KildeData(
    type = this.type,
    id = this.meldingsreferanseId,
    tidsstempel = this.tidsstempel
)
private fun RefusjonMemento.tilPersonData() = PersonData.ArbeidsgiverData.RefusjonData(
    meldingsreferanseId = this.meldingsreferanseId,
    førsteFraværsdag = this.førsteFraværsdag,
    arbeidsgiverperioder = this.arbeidsgiverperioder.map { PersonData.ArbeidsgiverData.PeriodeData(it.fom, it.tom) },
    beløp = this.beløp?.månedligDouble,
    sisteRefusjonsdag = this.sisteRefusjonsdag,
    endringerIRefusjon = this.endringerIRefusjon.map { it.tilPersonData() },
    tidsstempel = this.tidsstempel
)
private fun EndringIRefusjonMemento.tilPersonData() = PersonData.ArbeidsgiverData.RefusjonData.EndringIRefusjonData(
    beløp = this.beløp.månedligDouble,
    endringsdato = this.endringsdato
)

private fun SykmeldingsperioderMemento.tilPersonData() = perioder.map {
    PersonData.ArbeidsgiverData.SykmeldingsperiodeData(it.fom, it.tom)
}
private fun ForkastetVedtaksperiodeMemento.tilPersonData() = PersonData.ArbeidsgiverData.ForkastetVedtaksperiodeData(
    vedtaksperiode = this.vedtaksperiode.tilPersonData()
)
private fun VedtaksperiodeMemento.tilPersonData() = PersonData.ArbeidsgiverData.VedtaksperiodeData(
    id = id,
    tilstand = when (tilstand) {
        VedtaksperiodetilstandMemento.AVSLUTTET -> TilstandType.AVSLUTTET
        VedtaksperiodetilstandMemento.AVSLUTTET_UTEN_UTBETALING -> TilstandType.AVSLUTTET_UTEN_UTBETALING
        VedtaksperiodetilstandMemento.AVVENTER_BLOKKERENDE_PERIODE -> TilstandType.AVVENTER_BLOKKERENDE_PERIODE
        VedtaksperiodetilstandMemento.AVVENTER_GODKJENNING -> TilstandType.AVVENTER_GODKJENNING
        VedtaksperiodetilstandMemento.AVVENTER_GODKJENNING_REVURDERING -> TilstandType.AVVENTER_GODKJENNING_REVURDERING
        VedtaksperiodetilstandMemento.AVVENTER_HISTORIKK -> TilstandType.AVVENTER_HISTORIKK
        VedtaksperiodetilstandMemento.AVVENTER_HISTORIKK_REVURDERING -> TilstandType.AVVENTER_HISTORIKK_REVURDERING
        VedtaksperiodetilstandMemento.AVVENTER_INFOTRYGDHISTORIKK -> TilstandType.AVVENTER_INFOTRYGDHISTORIKK
        VedtaksperiodetilstandMemento.AVVENTER_INNTEKTSMELDING -> TilstandType.AVVENTER_INNTEKTSMELDING
        VedtaksperiodetilstandMemento.AVVENTER_REVURDERING -> TilstandType.AVVENTER_REVURDERING
        VedtaksperiodetilstandMemento.AVVENTER_SIMULERING -> TilstandType.AVVENTER_SIMULERING
        VedtaksperiodetilstandMemento.AVVENTER_SIMULERING_REVURDERING -> TilstandType.AVVENTER_SIMULERING_REVURDERING
        VedtaksperiodetilstandMemento.AVVENTER_VILKÅRSPRØVING -> TilstandType.AVVENTER_VILKÅRSPRØVING
        VedtaksperiodetilstandMemento.AVVENTER_VILKÅRSPRØVING_REVURDERING -> TilstandType.AVVENTER_VILKÅRSPRØVING_REVURDERING
        VedtaksperiodetilstandMemento.REVURDERING_FEILET -> TilstandType.REVURDERING_FEILET
        VedtaksperiodetilstandMemento.START -> TilstandType.START
        VedtaksperiodetilstandMemento.TIL_INFOTRYGD -> TilstandType.TIL_INFOTRYGD
        VedtaksperiodetilstandMemento.TIL_UTBETALING -> TilstandType.TIL_UTBETALING
    },
    generasjoner = generasjoner.generasjoner.map { it.tilPersonData() },
    opprettet = opprettet,
    oppdatert = oppdatert
)
private fun GenerasjonMemento.tilPersonData() = PersonData.ArbeidsgiverData.VedtaksperiodeData.GenerasjonData(
    id = this.id,
    tilstand = when (this.tilstand) {
        GenerasjonTilstandMemento.ANNULLERT_PERIODE -> error("Forventer ikke å serialisere ${this.tilstand}")
        GenerasjonTilstandMemento.AVSLUTTET_UTEN_VEDTAK -> PersonData.ArbeidsgiverData.VedtaksperiodeData.GenerasjonData.TilstandData.AVSLUTTET_UTEN_VEDTAK
        GenerasjonTilstandMemento.BEREGNET -> PersonData.ArbeidsgiverData.VedtaksperiodeData.GenerasjonData.TilstandData.BEREGNET
        GenerasjonTilstandMemento.BEREGNET_OMGJØRING -> PersonData.ArbeidsgiverData.VedtaksperiodeData.GenerasjonData.TilstandData.BEREGNET_OMGJØRING
        GenerasjonTilstandMemento.BEREGNET_REVURDERING -> PersonData.ArbeidsgiverData.VedtaksperiodeData.GenerasjonData.TilstandData.BEREGNET_REVURDERING
        GenerasjonTilstandMemento.REVURDERT_VEDTAK_AVVIST -> PersonData.ArbeidsgiverData.VedtaksperiodeData.GenerasjonData.TilstandData.REVURDERT_VEDTAK_AVVIST
        GenerasjonTilstandMemento.TIL_INFOTRYGD -> PersonData.ArbeidsgiverData.VedtaksperiodeData.GenerasjonData.TilstandData.TIL_INFOTRYGD
        GenerasjonTilstandMemento.UBEREGNET -> PersonData.ArbeidsgiverData.VedtaksperiodeData.GenerasjonData.TilstandData.UBEREGNET
        GenerasjonTilstandMemento.UBEREGNET_OMGJØRING -> PersonData.ArbeidsgiverData.VedtaksperiodeData.GenerasjonData.TilstandData.UBEREGNET_OMGJØRING
        GenerasjonTilstandMemento.UBEREGNET_REVURDERING -> PersonData.ArbeidsgiverData.VedtaksperiodeData.GenerasjonData.TilstandData.UBEREGNET_REVURDERING
        GenerasjonTilstandMemento.VEDTAK_FATTET -> PersonData.ArbeidsgiverData.VedtaksperiodeData.GenerasjonData.TilstandData.VEDTAK_FATTET
        GenerasjonTilstandMemento.VEDTAK_IVERKSATT -> PersonData.ArbeidsgiverData.VedtaksperiodeData.GenerasjonData.TilstandData.VEDTAK_IVERKSATT
    },
    vedtakFattet = this.vedtakFattet,
    avsluttet = this.avsluttet,
    kilde = this.kilde.tilPersonData(),
    endringer = this.endringer.map { it.tilPersonData() }
)
private fun GenerasjonkildeMemento.tilPersonData() = PersonData.ArbeidsgiverData.VedtaksperiodeData.GenerasjonData.KildeData(
    meldingsreferanseId = this.meldingsreferanseId,
    innsendt = this.innsendt,
    registrert = this.registert,
    avsender = when (this.avsender) {
        AvsenderMemento.ARBEIDSGIVER -> PersonData.ArbeidsgiverData.VedtaksperiodeData.GenerasjonData.AvsenderData.ARBEIDSGIVER
        AvsenderMemento.SAKSBEHANDLER -> PersonData.ArbeidsgiverData.VedtaksperiodeData.GenerasjonData.AvsenderData.SAKSBEHANDLER
        AvsenderMemento.SYKMELDT -> PersonData.ArbeidsgiverData.VedtaksperiodeData.GenerasjonData.AvsenderData.SYKMELDT
        AvsenderMemento.SYSTEM -> PersonData.ArbeidsgiverData.VedtaksperiodeData.GenerasjonData.AvsenderData.SYSTEM
    }
)
private fun GenerasjonEndringMemento.tilPersonData() = PersonData.ArbeidsgiverData.VedtaksperiodeData.GenerasjonData.EndringData(
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
private fun DokumentsporingMemento.tilPersonData() = PersonData.ArbeidsgiverData.VedtaksperiodeData.DokumentsporingData(
    dokumentId = this.id,
    dokumenttype = when (type) {
        DokumenttypeMemento.InntektsmeldingDager -> PersonData.ArbeidsgiverData.VedtaksperiodeData.DokumentTypeData.InntektsmeldingDager
        DokumenttypeMemento.InntektsmeldingInntekt -> PersonData.ArbeidsgiverData.VedtaksperiodeData.DokumentTypeData.InntektsmeldingInntekt
        DokumenttypeMemento.OverstyrArbeidsforhold -> PersonData.ArbeidsgiverData.VedtaksperiodeData.DokumentTypeData.OverstyrArbeidsforhold
        DokumenttypeMemento.OverstyrArbeidsgiveropplysninger -> PersonData.ArbeidsgiverData.VedtaksperiodeData.DokumentTypeData.OverstyrArbeidsgiveropplysninger
        DokumenttypeMemento.OverstyrInntekt -> PersonData.ArbeidsgiverData.VedtaksperiodeData.DokumentTypeData.OverstyrInntekt
        DokumenttypeMemento.OverstyrRefusjon -> PersonData.ArbeidsgiverData.VedtaksperiodeData.DokumentTypeData.OverstyrRefusjon
        DokumenttypeMemento.OverstyrTidslinje -> PersonData.ArbeidsgiverData.VedtaksperiodeData.DokumentTypeData.OverstyrTidslinje
        DokumenttypeMemento.SkjønnsmessigFastsettelse -> PersonData.ArbeidsgiverData.VedtaksperiodeData.DokumentTypeData.SkjønnsmessigFastsettelse
        DokumenttypeMemento.Sykmelding -> PersonData.ArbeidsgiverData.VedtaksperiodeData.DokumentTypeData.Sykmelding
        DokumenttypeMemento.Søknad -> PersonData.ArbeidsgiverData.VedtaksperiodeData.DokumentTypeData.Søknad
    }
)
private fun UtbetalingMemento.tilPersonData() = PersonData.UtbetalingData(
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
        UtbetalingtypeMemento.ANNULLERING -> "ANNULLERING"
        UtbetalingtypeMemento.ETTERUTBETALING -> "ETTERUTBETALING"
        UtbetalingtypeMemento.FERIEPENGER -> "FERIEPENGER"
        UtbetalingtypeMemento.REVURDERING -> "REVURDERING"
        UtbetalingtypeMemento.UTBETALING -> "UTBETALING"
    },
    status = when (this.tilstand) {
        UtbetalingTilstandMemento.ANNULLERT -> "ANNULLERT"
        UtbetalingTilstandMemento.FORKASTET -> "FORKASTET"
        UtbetalingTilstandMemento.GODKJENT -> "GODKJENT"
        UtbetalingTilstandMemento.GODKJENT_UTEN_UTBETALING -> "GODKJENT_UTEN_UTBETALING"
        UtbetalingTilstandMemento.IKKE_GODKJENT -> "IKKE_GODKJENT"
        UtbetalingTilstandMemento.IKKE_UTBETALT -> "IKKE_UTBETALT"
        UtbetalingTilstandMemento.NY -> "NY"
        UtbetalingTilstandMemento.OVERFØRT -> "OVERFØRT"
        UtbetalingTilstandMemento.UTBETALT -> "UTBETALT"
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
private fun UtbetalingstidslinjeMemento.tilPersonData() = PersonData.UtbetalingstidslinjeData(
    dager = this.dager.map { it.tilPersonData() }
)

private fun UtbetalingsdagMemento.tilPersonData() = PersonData.UtbetalingstidslinjeData.UtbetalingsdagData(
    type = when (this) {
        is UtbetalingsdagMemento.ArbeidsdagMemento -> PersonData.UtbetalingstidslinjeData.TypeData.Arbeidsdag
        is UtbetalingsdagMemento.ArbeidsgiverperiodeDagMemento -> PersonData.UtbetalingstidslinjeData.TypeData.ArbeidsgiverperiodeDag
        is UtbetalingsdagMemento.ArbeidsgiverperiodeDagNavMemento -> PersonData.UtbetalingstidslinjeData.TypeData.ArbeidsgiverperiodedagNav
        is UtbetalingsdagMemento.AvvistDagMemento -> PersonData.UtbetalingstidslinjeData.TypeData.AvvistDag
        is UtbetalingsdagMemento.ForeldetDagMemento -> PersonData.UtbetalingstidslinjeData.TypeData.ForeldetDag
        is UtbetalingsdagMemento.FridagMemento -> PersonData.UtbetalingstidslinjeData.TypeData.Fridag
        is UtbetalingsdagMemento.NavDagMemento -> PersonData.UtbetalingstidslinjeData.TypeData.NavDag
        is UtbetalingsdagMemento.NavHelgDagMemento -> PersonData.UtbetalingstidslinjeData.TypeData.NavHelgDag
        is UtbetalingsdagMemento.UkjentDagMemento -> PersonData.UtbetalingstidslinjeData.TypeData.UkjentDag
    },
    aktuellDagsinntekt = this.økonomi.aktuellDagsinntekt.dagligDouble,
    beregningsgrunnlag = this.økonomi.beregningsgrunnlag.dagligDouble,
    dekningsgrunnlag = this.økonomi.dekningsgrunnlag.dagligDouble,
    grunnbeløpgrense = this.økonomi.grunnbeløpgrense?.årlig,
    begrunnelser = when (this) {
        is UtbetalingsdagMemento.AvvistDagMemento -> this.begrunnelser.map { it.tilPersonData() }
        else -> null
    },
    grad = this.økonomi.grad.prosent,
    totalGrad = this.økonomi.totalGrad.prosent,
    arbeidsgiverRefusjonsbeløp = økonomi.arbeidsgiverRefusjonsbeløp.dagligDouble,
    arbeidsgiverbeløp = this.økonomi.arbeidsgiverbeløp?.dagligDouble,
    personbeløp = this.økonomi.personbeløp?.dagligDouble,
    er6GBegrenset = this.økonomi.er6GBegrenset
).apply {
    datoer = DateRange.Single(dato)
}

private fun BegrunnelseMemento.tilPersonData() = when (this) {
    BegrunnelseMemento.AndreYtelserAap -> PersonData.UtbetalingstidslinjeData.BegrunnelseData.AndreYtelserAap
    BegrunnelseMemento.AndreYtelserDagpenger -> PersonData.UtbetalingstidslinjeData.BegrunnelseData.AndreYtelserDagpenger
    BegrunnelseMemento.AndreYtelserForeldrepenger -> PersonData.UtbetalingstidslinjeData.BegrunnelseData.AndreYtelserForeldrepenger
    BegrunnelseMemento.AndreYtelserOmsorgspenger -> PersonData.UtbetalingstidslinjeData.BegrunnelseData.AndreYtelserOmsorgspenger
    BegrunnelseMemento.AndreYtelserOpplaringspenger -> PersonData.UtbetalingstidslinjeData.BegrunnelseData.AndreYtelserOpplaringspenger
    BegrunnelseMemento.AndreYtelserPleiepenger -> PersonData.UtbetalingstidslinjeData.BegrunnelseData.AndreYtelserPleiepenger
    BegrunnelseMemento.AndreYtelserSvangerskapspenger -> PersonData.UtbetalingstidslinjeData.BegrunnelseData.AndreYtelserSvangerskapspenger
    BegrunnelseMemento.EgenmeldingUtenforArbeidsgiverperiode -> PersonData.UtbetalingstidslinjeData.BegrunnelseData.EgenmeldingUtenforArbeidsgiverperiode
    BegrunnelseMemento.EtterDødsdato -> PersonData.UtbetalingstidslinjeData.BegrunnelseData.EtterDødsdato
    BegrunnelseMemento.ManglerMedlemskap -> PersonData.UtbetalingstidslinjeData.BegrunnelseData.ManglerMedlemskap
    BegrunnelseMemento.ManglerOpptjening -> PersonData.UtbetalingstidslinjeData.BegrunnelseData.ManglerOpptjening
    BegrunnelseMemento.MinimumInntekt -> PersonData.UtbetalingstidslinjeData.BegrunnelseData.MinimumInntekt
    BegrunnelseMemento.MinimumInntektOver67 -> PersonData.UtbetalingstidslinjeData.BegrunnelseData.MinimumInntektOver67
    BegrunnelseMemento.MinimumSykdomsgrad -> PersonData.UtbetalingstidslinjeData.BegrunnelseData.MinimumSykdomsgrad
    BegrunnelseMemento.NyVilkårsprøvingNødvendig -> PersonData.UtbetalingstidslinjeData.BegrunnelseData.NyVilkårsprøvingNødvendig
    BegrunnelseMemento.Over70 -> PersonData.UtbetalingstidslinjeData.BegrunnelseData.Over70
    BegrunnelseMemento.SykepengedagerOppbrukt -> PersonData.UtbetalingstidslinjeData.BegrunnelseData.SykepengedagerOppbrukt
    BegrunnelseMemento.SykepengedagerOppbruktOver67 -> PersonData.UtbetalingstidslinjeData.BegrunnelseData.SykepengedagerOppbruktOver67
}

private fun UtbetalingVurderingMemento.tilPersonData() = PersonData.UtbetalingData.VurderingData(
    godkjent = godkjent,
    ident = ident,
    epost = epost,
    tidspunkt = tidspunkt,
    automatiskBehandling = automatiskBehandling
)
private fun OppdragMemento.tilPersonData() = PersonData.OppdragData(
    mottaker = this.mottaker,
    fagområde = when (this.fagområde) {
        FagområdeMemento.SP -> "SP"
        FagområdeMemento.SPREF -> "SPREF"
    },
    linjer = this.linjer.map { it.tilPersonData() },
    fagsystemId = this.fagsystemId,
    endringskode = this.endringskode.tilPersonData(),
    tidsstempel = this.tidsstempel,
    nettoBeløp = this.nettoBeløp,
    avstemmingsnøkkel = this.avstemmingsnøkkel,
    status = when (this.status) {
        OppdragstatusMemento.AKSEPTERT -> Oppdragstatus.AKSEPTERT
        OppdragstatusMemento.AKSEPTERT_MED_FEIL -> Oppdragstatus.AKSEPTERT_MED_FEIL
        OppdragstatusMemento.AVVIST -> Oppdragstatus.AVVIST
        OppdragstatusMemento.FEIL -> Oppdragstatus.FEIL
        OppdragstatusMemento.OVERFØRT -> Oppdragstatus.OVERFØRT
        null -> null
    },
    overføringstidspunkt = this.overføringstidspunkt,
    erSimulert = this.erSimulert,
    simuleringsResultat = this.simuleringsResultat?.tilPersonData()
)
private fun EndringskodeMemento.tilPersonData() = when (this) {
    EndringskodeMemento.ENDR -> "ENDR"
    EndringskodeMemento.NY -> "NY"
    EndringskodeMemento.UEND -> "UEND"
}
private fun UtbetalingslinjeMemento.tilPersonData() = PersonData.UtbetalingslinjeData(
    fom = this.fom,
    tom = this.tom,
    satstype = when (this.satstype) {
        SatstypeMemento.Daglig -> "dag"
        SatstypeMemento.Engang -> "eng"
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
private fun KlassekodeMemento.tilPersonData() = when (this) {
    KlassekodeMemento.RefusjonFeriepengerIkkeOpplysningspliktig -> "SPREFAGFER-IOP"
    KlassekodeMemento.RefusjonIkkeOpplysningspliktig -> "SPREFAG-IOP"
    KlassekodeMemento.SykepengerArbeidstakerFeriepenger -> "SPATFER"
    KlassekodeMemento.SykepengerArbeidstakerOrdinær -> "SPATORD"
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
private fun FeriepengeMemento.tilPersonData() = PersonData.ArbeidsgiverData.FeriepengeutbetalingData(
    infotrygdFeriepengebeløpPerson = this.infotrygdFeriepengebeløpPerson,
    infotrygdFeriepengebeløpArbeidsgiver = this.infotrygdFeriepengebeløpArbeidsgiver,
    spleisFeriepengebeløpArbeidsgiver = this.spleisFeriepengebeløpArbeidsgiver,
    spleisFeriepengebeløpPerson = this.spleisFeriepengebeløpPerson,
    oppdrag = this.oppdrag.tilPersonData(),
    personoppdrag = this.personoppdrag.tilPersonData(),
    opptjeningsår = this.feriepengeberegner.opptjeningsår,
    utbetalteDager = this.feriepengeberegner.utbetalteDager.map { it.tilPersonData() },
    feriepengedager = emptyList(),
    utbetalingId = utbetalingId,
    sendTilOppdrag = sendTilOppdrag,
    sendPersonoppdragTilOS = sendPersonoppdragTilOS
)
private fun UtbetaltDagMemento.tilPersonData() = PersonData.ArbeidsgiverData.FeriepengeutbetalingData.UtbetaltDagData(
    type = when (this) {
        is UtbetaltDagMemento.InfotrygdArbeidsgiver -> "InfotrygdArbeidsgiverDag"
        is UtbetaltDagMemento.InfotrygdPerson -> "InfotrygdPersonDag"
        is UtbetaltDagMemento.SpleisArbeidsgiver -> "SpleisArbeidsgiverDag"
        is UtbetaltDagMemento.SpleisPerson -> "SpleisPersonDag"
    },
    orgnummer = orgnummer,
    dato = dato,
    beløp = beløp
)
private fun InfotrygdhistorikkelementMemento.tilPersonData() = PersonData.InfotrygdhistorikkElementData(
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
private fun InfotrygdFerieperiodeMemento.tilPersonData() = PersonData.InfotrygdhistorikkElementData.FerieperiodeData(
    fom = this.periode.fom,
    tom = this.periode.tom
)
private fun InfotrygdArbeidsgiverutbetalingsperiodeMemento.tilPersonData() = PersonData.InfotrygdhistorikkElementData.ArbeidsgiverutbetalingsperiodeData(
    orgnr = this.orgnr,
    fom = this.periode.fom,
    tom = this.periode.tom,
    grad = this.grad.prosent,
    inntekt = this.inntekt.dagligInt
)
private fun InfotrygdPersonutbetalingsperiodeMemento.tilPersonData() = PersonData.InfotrygdhistorikkElementData.PersonutbetalingsperiodeData(
    orgnr = this.orgnr,
    fom = this.periode.fom,
    tom = this.periode.tom,
    grad = this.grad.prosent,
    inntekt = this.inntekt.dagligInt
)
private fun InfotrygdInntektsopplysningMemento.tilPersonData() = PersonData.InfotrygdhistorikkElementData.InntektsopplysningData(
    orgnr = this.orgnummer,
    sykepengerFom = this.sykepengerFom,
    inntekt = this.inntekt.månedligDouble,
    refusjonTilArbeidsgiver = refusjonTilArbeidsgiver,
    refusjonTom = refusjonTom,
    lagret = lagret
)
private fun VilkårsgrunnlagInnslagMemento.tilPersonData() = PersonData.VilkårsgrunnlagInnslagData(
    id = this.id,
    opprettet = this.opprettet,
    vilkårsgrunnlag = this.vilkårsgrunnlag.map { it.tilPersonData() }
)
private fun VilkårsgrunnlagMemento.tilPersonData() = PersonData.VilkårsgrunnlagElementData(
    skjæringstidspunkt = this.skjæringstidspunkt,
    type = when (this) {
        is VilkårsgrunnlagMemento.Infotrygd -> PersonData.VilkårsgrunnlagElementData.GrunnlagsdataType.Infotrygd
        is VilkårsgrunnlagMemento.Spleis -> PersonData.VilkårsgrunnlagElementData.GrunnlagsdataType.Vilkårsprøving
    },
    sykepengegrunnlag = this.sykepengegrunnlag.tilPersonData(),
    opptjening = this.opptjening?.tilPersonData(),
    medlemskapstatus = when (this) {
        is VilkårsgrunnlagMemento.Spleis -> when (this.medlemskapstatus) {
            MedlemskapsvurderingMemento.Ja -> JsonMedlemskapstatus.JA
            MedlemskapsvurderingMemento.Nei -> JsonMedlemskapstatus.NEI
            MedlemskapsvurderingMemento.UavklartMedBrukerspørsmål -> JsonMedlemskapstatus.UAVKLART_MED_BRUKERSPØRSMÅL
            MedlemskapsvurderingMemento.VetIkke -> JsonMedlemskapstatus.VET_IKKE
        }
        else -> null
    },
    vurdertOk = when (this) {
        is VilkårsgrunnlagMemento.Spleis -> this.vurdertOk
        else -> null
    },
    meldingsreferanseId = when (this) {
        is VilkårsgrunnlagMemento.Spleis -> this.meldingsreferanseId
        else -> null
    },
    vilkårsgrunnlagId = this.vilkårsgrunnlagId
)

private fun OpptjeningMemento.tilPersonData() = PersonData.VilkårsgrunnlagElementData.OpptjeningData(
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
private fun SykepengegrunnlagMemento.tilPersonData() = PersonData.VilkårsgrunnlagElementData.SykepengegrunnlagData(
    grunnbeløp = this.`6G`?.årlig,
    arbeidsgiverInntektsopplysninger = this.arbeidsgiverInntektsopplysninger.map { it.tilPersonData() },
    sammenligningsgrunnlag = this.sammenligningsgrunnlag.tilPersonData(),
    deaktiverteArbeidsforhold = this.deaktiverteArbeidsforhold.map { it.tilPersonData() },
    vurdertInfotrygd = this.vurdertInfotrygd
)

private fun ArbeidsgiverInntektsopplysningMemento.tilPersonData() = PersonData.VilkårsgrunnlagElementData.ArbeidsgiverInntektsopplysningData(
    orgnummer = this.orgnummer,
    fom = this.gjelder.fom,
    tom = this.gjelder.tom,
    inntektsopplysning = this.inntektsopplysning.tilPersonData(),
    refusjonsopplysninger = this.refusjonsopplysninger.opplysninger.map {
        it.tilPersonData()
    }
)

private fun InntektsopplysningMemento.tilPersonData() = PersonData.VilkårsgrunnlagElementData.ArbeidsgiverInntektsopplysningData.InntektsopplysningData(
    id = this.id,
    dato = this.dato,
    hendelseId = this.hendelseId,
    beløp = when (this) {
        is InntektsopplysningMemento.SkattSykepengegrunnlagMemento -> null
        else -> this.beløp.månedligDouble
    },
    kilde = when (this) {
        is InntektsopplysningMemento.IkkeRapportertMemento -> "IKKE_RAPPORTERT"
        is InntektsopplysningMemento.InfotrygdMemento -> "INFOTRYGD"
        is InntektsopplysningMemento.InntektsmeldingMemento -> "INNTEKTSMELDING"
        is InntektsopplysningMemento.SaksbehandlerMemento -> "SAKSBEHANDLER"
        is InntektsopplysningMemento.SkattSykepengegrunnlagMemento -> "SKATT_SYKEPENGEGRUNNLAG"
        is InntektsopplysningMemento.SkjønnsmessigFastsattMemento -> "SKJØNNSMESSIG_FASTSATT"
    },
    forklaring = when (this) {
        is InntektsopplysningMemento.SaksbehandlerMemento -> this.forklaring
        else -> null
    },
    subsumsjon = when (this) {
        is InntektsopplysningMemento.SaksbehandlerMemento -> this.subsumsjon?.let {
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
        is InntektsopplysningMemento.SaksbehandlerMemento -> this.overstyrtInntekt.id
        is InntektsopplysningMemento.SkjønnsmessigFastsattMemento -> this.overstyrtInntekt.id
        else -> null
    },
    skatteopplysninger = when (this) {
        is InntektsopplysningMemento.SkattSykepengegrunnlagMemento -> this.inntektsopplysninger.map { it.tilPersonDataSkattopplysning() }
        else -> null
    }
)

private fun RefusjonsopplysningMemento.tilPersonData() = PersonData.ArbeidsgiverData.RefusjonsopplysningData(
    meldingsreferanseId = this.meldingsreferanseId,
    fom = this.fom,
    tom = this.tom,
    beløp = this.beløp.månedligDouble
)

private fun SammenligningsgrunnlagMemento.tilPersonData() = PersonData.VilkårsgrunnlagElementData.SammenligningsgrunnlagData(
    sammenligningsgrunnlag = this.sammenligningsgrunnlag.årlig,
    arbeidsgiverInntektsopplysninger = this.arbeidsgiverInntektsopplysninger.map { it.tilPersonData() }
)

private fun ArbeidsgiverInntektsopplysningForSammenligningsgrunnlagMemento.tilPersonData() = PersonData.VilkårsgrunnlagElementData.ArbeidsgiverInntektsopplysningForSammenligningsgrunnlagData(
    orgnummer = this.orgnummer,
    skatteopplysninger = this.inntektsopplysninger.map { it.tilPersonData() }
)

private fun SkatteopplysningMemento.tilPersonData() =
    PersonData.VilkårsgrunnlagElementData.ArbeidsgiverInntektsopplysningForSammenligningsgrunnlagData.SammenligningsgrunnlagInntektsopplysningData(
        hendelseId = this.hendelseId,
        beløp = this.beløp.månedligDouble,
        måned = this.måned,
        type = when (this.type) {
            InntekttypeMemento.LØNNSINNTEKT -> PersonData.VilkårsgrunnlagElementData.ArbeidsgiverInntektsopplysningForSammenligningsgrunnlagData.SammenligningsgrunnlagInntektsopplysningData.InntekttypeData.LØNNSINNTEKT
            InntekttypeMemento.NÆRINGSINNTEKT -> PersonData.VilkårsgrunnlagElementData.ArbeidsgiverInntektsopplysningForSammenligningsgrunnlagData.SammenligningsgrunnlagInntektsopplysningData.InntekttypeData.NÆRINGSINNTEKT
            InntekttypeMemento.PENSJON_ELLER_TRYGD -> PersonData.VilkårsgrunnlagElementData.ArbeidsgiverInntektsopplysningForSammenligningsgrunnlagData.SammenligningsgrunnlagInntektsopplysningData.InntekttypeData.PENSJON_ELLER_TRYGD
            InntekttypeMemento.YTELSE_FRA_OFFENTLIGE -> PersonData.VilkårsgrunnlagElementData.ArbeidsgiverInntektsopplysningForSammenligningsgrunnlagData.SammenligningsgrunnlagInntektsopplysningData.InntekttypeData.YTELSE_FRA_OFFENTLIGE
        },
        fordel = fordel,
        beskrivelse = beskrivelse,
        tidsstempel = tidsstempel
    )
private fun SkatteopplysningMemento.tilPersonDataSkattopplysning() = PersonData.VilkårsgrunnlagElementData.ArbeidsgiverInntektsopplysningData.SkatteopplysningData(
    hendelseId = this.hendelseId,
    beløp = this.beløp.månedligDouble,
    måned = this.måned,
    type = when (this.type) {
        InntekttypeMemento.LØNNSINNTEKT -> "LØNNSINNTEKT"
        InntekttypeMemento.NÆRINGSINNTEKT -> "NÆRINGSINNTEKT"
        InntekttypeMemento.PENSJON_ELLER_TRYGD -> "PENSJON_ELLER_TRYGD"
        InntekttypeMemento.YTELSE_FRA_OFFENTLIGE -> "YTELSE_FRA_OFFENTLIGE"
    },
    fordel = fordel,
    beskrivelse = beskrivelse,
    tidsstempel = tidsstempel
)