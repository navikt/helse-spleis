package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import no.nav.helse.dto.deserialisering.InntektsopplysningInnDto
import no.nav.helse.dto.serialisering.InntektsopplysningUtDto
import no.nav.helse.hendelser.OverstyrArbeidsgiveropplysninger
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Person
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.PersonObserver.Inntektsopplysningstype.INNTEKTSMELDING
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IV_7
import no.nav.helse.økonomi.Inntekt

class Inntektsmelding internal constructor(
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

    override fun gjenbrukbarInntekt(beløp: Inntekt?) = beløp?.let { Inntektsmelding(dato, hendelseId, it, tidsstempel) }?: this

    internal fun accept(visitor: InntektsmeldingVisitor) {
        visitor.visitInntektsmelding(this, id, dato, hendelseId, beløp, tidsstempel)
    }

    override fun blirOverstyrtAv(ny: Inntektsopplysning): Inntektsopplysning {
        return ny.overstyrer(this)
    }

    override fun overstyrer(gammel: Saksbehandler) = this

    override fun overstyrer(gammel: SkjønnsmessigFastsatt) =
        if (erOmregnetÅrsinntektEndret(this, gammel)) this
        else gammel.overstyrer(this)

    override fun avklarSykepengegrunnlag(skjæringstidspunkt: LocalDate, førsteFraværsdag: LocalDate?): AvklarbarSykepengegrunnlag? {
        if (dato == skjæringstidspunkt) return this
        if (førsteFraværsdag == null || dato != førsteFraværsdag) return null
        return this
    }

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

    internal fun kopierTidsnærOpplysning(
        nyDato: LocalDate,
        hendelse: IAktivitetslogg,
        nyArbeidsgiverperiode: Boolean,
        inntektshistorikk: Inntektshistorikk
    ) {
        if (nyDato == this.dato) return
        val dagerMellom = ChronoUnit.DAYS.between(this.dato, nyDato)
        if (dagerMellom >= 60) {
            hendelse.info("Det er $dagerMellom dager mellom forrige inntektdato (${this.dato}) og ny inntektdato ($nyDato), dette utløser varsel om gjenbruk.")
            hendelse.varsel(RV_IV_7)
        } else if (nyArbeidsgiverperiode) {
            hendelse.info("Det er ny arbeidsgiverperiode, og dette utløser varsel om gjenbruk. Forrige inntektdato var ${this.dato} og ny inntektdato er $nyDato")
            hendelse.varsel(RV_IV_7)
        }
        inntektshistorikk.leggTil(Inntektsmelding(nyDato, hendelseId, beløp, tidsstempel))
        hendelse.info("Kopierte inntekt som lå lagret på ${this.dato} til $nyDato")
    }

    override fun dto() =
        InntektsopplysningUtDto.InntektsmeldingDto(
            id = id,
            hendelseId = hendelseId,
            dato = dato,
            beløp = beløp.dto(),
            tidsstempel = tidsstempel
        )

    internal companion object {
        internal fun gjenopprett(dto: InntektsopplysningInnDto.InntektsmeldingDto): Inntektsmelding {
            return Inntektsmelding(
                id = dto.id,
                dato = dto.dato,
                hendelseId = dto.hendelseId,
                beløp = Inntekt.gjenopprett(dto.beløp),
                tidsstempel = dto.tidsstempel
            )
        }
    }
}