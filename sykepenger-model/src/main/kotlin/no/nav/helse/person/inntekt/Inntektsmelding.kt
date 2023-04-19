package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import no.nav.helse.hendelser.OverstyrArbeidsgiveropplysninger
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Person
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.PersonObserver.ArbeidsgiveropplysningerKorrigertEvent.KorrigerendeInntektektsopplysningstype.INNTEKTSMELDING
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IV_7
import no.nav.helse.økonomi.Inntekt

internal class Inntektsmelding(
    id: UUID,
    dato: LocalDate,
    hendelseId: UUID,
    beløp: Inntekt,
    tidsstempel: LocalDateTime
) : AvklarbarSykepengegrunnlag(id, hendelseId, dato, beløp, tidsstempel) {
    internal constructor(dato: LocalDate, hendelseId: UUID, beløp: Inntekt, tidsstempel: LocalDateTime = LocalDateTime.now()) : this(UUID.randomUUID(), dato, hendelseId, beløp, tidsstempel)

    override fun accept(visitor: InntektsopplysningVisitor) {
        accept(visitor as InntektsmeldingVisitor)
    }

    override fun lagreTidsnærInntekt(
        skjæringstidspunkt: LocalDate,
        arbeidsgiver: Arbeidsgiver,
        hendelse: IAktivitetslogg,
        oppholdsperiodeMellom: Periode?,
        refusjonsopplysninger: Refusjonsopplysning.Refusjonsopplysninger,
        orgnummer: String,
        beløp: Inntekt?
    ) {
        arbeidsgiver.lagreTidsnærInntektsmelding(
            skjæringstidspunkt,
            orgnummer,
            beløp?.let {
                Inntektsmelding(dato, hendelseId, it, tidsstempel)
            }?: this,
            refusjonsopplysninger,
            hendelse,
            oppholdsperiodeMellom
        )
    }

    internal fun accept(visitor: InntektsmeldingVisitor) {
        visitor.visitInntektsmelding(this, id, dato, hendelseId, beløp, tidsstempel)
    }

    override fun kanOverstyresAv(ny: Inntektsopplysning): Boolean {
        if (ny !is Saksbehandler && ny !is Inntektsmelding) return false
        val måned = this.dato.withDayOfMonth(1) til this.dato.withDayOfMonth(this.dato.lengthOfMonth())
        if (ny is Inntektsmelding && ny.dato !in måned) return false
        return super.kanOverstyresAv(ny)
    }

    override fun avklarSykepengegrunnlag(skjæringstidspunkt: LocalDate, førsteFraværsdag: LocalDate?): AvklarbarSykepengegrunnlag? {
        if (dato == skjæringstidspunkt) return this
        if (førsteFraværsdag == null || dato != førsteFraværsdag) return null
        return this
    }

    override fun omregnetÅrsinntekt(): Inntekt = beløp

    internal fun kanLagres(other: Inntektsmelding) = this.hendelseId != other.hendelseId || this.dato != other.dato

    override fun erSamme(other: Inntektsopplysning): Boolean {
        return other is Inntektsmelding && this.dato == other.dato && other.beløp == this.beløp
    }
    override fun arbeidsgiveropplysningerKorrigert(
        person: Person,
        inntektsmelding: no.nav.helse.hendelser.Inntektsmelding
    ) {
        person.arbeidsgiveropplysningerKorrigert(
            PersonObserver.ArbeidsgiveropplysningerKorrigertEvent(
                korrigertInntektsmeldingId = hendelseId,
                korrigerendeInntektektsopplysningstype = INNTEKTSMELDING,
                korrigerendeInntektsopplysningId = inntektsmelding.meldingsreferanseId()
            )
        )
    }
    override fun arbeidsgiveropplysningerKorrigert(
        person: Person,
        orgnummer: String,
        saksbehandlerOverstyring: OverstyrArbeidsgiveropplysninger
    ) {
        saksbehandlerOverstyring.arbeidsgiveropplysningerKorrigert(person, orgnummer, hendelseId)
    }

    internal fun kopierTidsnærOpplysning(nyDato: LocalDate, hendelse: IAktivitetslogg, oppholdsperiodeMellom: Periode?): Inntektsmelding {
        val dagerMellom = ChronoUnit.DAYS.between(this.dato, nyDato)
        if (dagerMellom >= 60) {
            hendelse.info("Det er $dagerMellom dager mellom forrige inntektdato (${this.dato}) og ny inntektdato ($nyDato), dette utløser varsel om gjenbruk.")
            hendelse.varsel(RV_IV_7)
        } else if (oppholdsperiodeMellom != null && oppholdsperiodeMellom.count() >= 16 && this.dato < oppholdsperiodeMellom.endInclusive) {
            hendelse.info("Det er ${oppholdsperiodeMellom.count()} dager ($oppholdsperiodeMellom) mellom forrige vedtaksperiodeperiode og det er en antagelse om at det er ny arbeidsgiverperiode, og dette utløser varsel om gjenbruk. " +
                    "Forrige inntektdato var ${this.dato} og ny inntektdato er $nyDato")
            hendelse.varsel(RV_IV_7)
        }
        return Inntektsmelding(nyDato, hendelseId, beløp, tidsstempel)
    }
}