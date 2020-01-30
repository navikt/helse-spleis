package no.nav.helse.person

import no.nav.helse.hendelser.*
import no.nav.helse.person.Vedtaksperiode.Vedtaksperiodetilstand
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeVisitor
import no.nav.helse.utbetalingstidslinje.Utbetalingslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.UtbetalingsdagVisitor
import java.time.LocalDate
import java.util.*

internal interface PersonVisitor : ArbeidsgiverVisitor, HendelseVisitor {
    fun preVisitPerson(person: Person, aktørId: String, fødselsnummer: String) {}
    fun visitPersonAktivitetslogger(aktivitetslogger: Aktivitetslogger) {}
    fun preVisitArbeidsgivere() {}
    fun postVisitArbeidsgivere() {}
    fun postVisitPerson(person: Person, aktørId: String, fødselsnummer: String) {}
}

internal interface HendelseVisitor {
    fun preVisitHendelser() {}
    fun postVisitHendelser() {}
    fun visitInntektsmeldingHendelse(inntektsmelding: ModelInntektsmelding) {}
    fun visitManuellSaksbehandlingHendelse(manuellSaksbehandling: ModelManuellSaksbehandling) {}
    fun visitNySøknadHendelse(nySøknad: ModelNySøknad) {}
    fun visitPåminnelseHendelse(påminnelse: ModelPåminnelse) {}
    fun visitSendtSøknadHendelse(sendtSøknad: ModelSendtSøknad) {}
    fun visitVilkårsgrunnlagHendelse(vilkårsgrunnlag: ModelVilkårsgrunnlag) {}
    fun visitYtelserHendelse(ytelser: ModelYtelser) {}
}

internal interface ArbeidsgiverVisitor : UtbetalingsdagVisitor, VedtaksperiodeVisitor {
    fun preVisitArbeidsgiver(
        arbeidsgiver: Arbeidsgiver,
        id: UUID,
        organisasjonsnummer: String
    ) {}
    fun visitArbeidsgiverAktivitetslogger(aktivitetslogger: Aktivitetslogger) {}
    fun preVisitInntekthistorikk(inntekthistorikk: Inntekthistorikk) {}
    fun postVisitInntekthistorikk(inntekthistorikk: Inntekthistorikk) {}
    fun preVisitTidslinjer() {}
    fun postVisitTidslinjer() {}
    fun preVisitPerioder() {}
    fun postVisitPerioder() {}
    fun postVisitArbeidsgiver(
        arbeidsgiver: Arbeidsgiver,
        id: UUID,
        organisasjonsnummer: String
    ) {}
    fun preVisitInntekter() {}
    fun postVisitInntekter() {}
    fun visitInntekt(inntekt: Inntekthistorikk.Inntekt) {}
}

internal interface VedtaksperiodeVisitor : SykdomstidslinjeVisitor, SykdomshistorikkVisitor {
    fun preVisitVedtaksperiode(vedtaksperiode: Vedtaksperiode, id: UUID) {}
    fun visitMaksdato(maksdato: LocalDate?) {}
    fun visitGodkjentAv(godkjentAv: String?) {}
    fun visitFørsteFraværsdag(førsteFraværsdag: LocalDate?) {}
    fun visitInntektFraInntektsmelding(inntektFraInntektsmelding: Double?) {}
    fun visitDataForVilkårsvurdering(dataForVilkårsvurdering: ModelVilkårsgrunnlag.Grunnlagsdata?) {}
    fun visitVedtaksperiodeAktivitetslogger(aktivitetslogger: Aktivitetslogger) {}
    fun visitUtbetalingslinje(utbetalingslinje: Utbetalingslinje) {}
    fun visitTilstand(tilstand: Vedtaksperiodetilstand) {}
    fun preVisitUtbetalingslinjer() {}
    fun postVisitUtbetalingslinjer() {}
    fun postVisitVedtaksperiode(vedtaksperiode: Vedtaksperiode, id: UUID) {}
    fun preVisitVedtaksperiodeSykdomstidslinje() {}
    fun postVisitVedtaksperiodeSykdomstidslinje() {}
}

internal interface SykdomshistorikkVisitor : SykdomstidslinjeVisitor {
    fun preVisitSykdomshistorikk(sykdomshistorikk: Sykdomshistorikk) {}
    fun preVisitSykdomshistorikkElement(element: Sykdomshistorikk.Element) {}
    fun visitHendelse(hendelse: SykdomstidslinjeHendelse) {}
    fun postVisitSykdomshistorikkElement(element: Sykdomshistorikk.Element) {}
    fun postVisitSykdomshistorikk(sykdomshistorikk: Sykdomshistorikk) {}
    fun preVisitHendelseSykdomstidslinje() {}
    fun postVisitHendelseSykdomstidslinje() {}
    fun preVisitBeregnetSykdomstidslinje() {}
    fun postVisitBeregnetSykdomstidslinje() {}
}
