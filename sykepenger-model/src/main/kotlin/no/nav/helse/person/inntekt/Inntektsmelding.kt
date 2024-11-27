package no.nav.helse.person.inntekt

import no.nav.helse.dto.deserialisering.InntektsopplysningInnDto
import no.nav.helse.dto.deserialisering.InntektsopplysningInnDto.InntektsmeldingDto.KildeDto
import no.nav.helse.dto.serialisering.InntektsopplysningUtDto
import no.nav.helse.hendelser.OverstyrArbeidsgiveropplysninger
import no.nav.helse.person.Person
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.PersonObserver.Inntektsopplysningstype.INNTEKTSMELDING
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IV_7
import no.nav.helse.yearMonth
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class Inntektsmelding internal constructor(
    id: UUID,
    dato: LocalDate,
    hendelseId: UUID,
    beløp: Inntekt,
    tidsstempel: LocalDateTime,
    private val kilde: Kilde
) : Inntektsopplysning(id, hendelseId, dato, beløp, tidsstempel) {
    internal constructor(dato: LocalDate, hendelseId: UUID, beløp: Inntekt, kilde: Kilde = Kilde.Arbeidsgiver, tidsstempel: LocalDateTime = LocalDateTime.now()) : this(UUID.randomUUID(), dato, hendelseId, beløp, tidsstempel, kilde)

    override fun gjenbrukbarInntekt(beløp: Inntekt?) = beløp?.let { Inntektsmelding(dato, hendelseId, it, kilde, tidsstempel) } ?: this

    internal fun view() =
        InntektsmeldingView(
            id = id,
            dato = dato,
            hendelseId = hendelseId,
            beløp = beløp,
            tidsstempel = tidsstempel
        )

    override fun blirOverstyrtAv(ny: Inntektsopplysning): Inntektsopplysning = ny.overstyrer(this)

    override fun overstyrer(gammel: Saksbehandler) = this

    override fun overstyrer(gammel: SkjønnsmessigFastsatt) =
        if (erOmregnetÅrsinntektEndret(this, gammel)) {
            this
        } else {
            gammel.overstyrer(this)
        }

    internal fun avklarSykepengegrunnlag(skatt: AvklarbarSykepengegrunnlag): Inntektsopplysning {
        if (skatt.dato.yearMonth < this.dato.yearMonth) return skatt
        return this
    }

    internal fun kanLagres(other: Inntektsmelding) = this.hendelseId != other.hendelseId || this.dato != other.dato

    override fun erSamme(other: Inntektsopplysning): Boolean = other is Inntektsmelding && this.dato == other.dato && other.beløp == this.beløp

    override fun arbeidsgiveropplysningerKorrigert(
        person: Person,
        inntektsmelding: no.nav.helse.hendelser.Inntektsmelding
    ) {
        person.arbeidsgiveropplysningerKorrigert(
            PersonObserver.ArbeidsgiveropplysningerKorrigertEvent(
                korrigertInntektsmeldingId = hendelseId,
                korrigerendeInntektektsopplysningstype = INNTEKTSMELDING,
                korrigerendeInntektsopplysningId = inntektsmelding.metadata.meldingsreferanseId
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

    override fun erSkatteopplysning(): Boolean = kilde == Kilde.AOrdningen

    internal fun kopierTidsnærOpplysning(
        nyDato: LocalDate,
        aktivitetslogg: IAktivitetslogg,
        nyArbeidsgiverperiode: Boolean,
        inntektshistorikk: Inntektshistorikk
    ) {
        if (nyDato == this.dato) return
        val dagerMellom = ChronoUnit.DAYS.between(this.dato, nyDato)
        if (dagerMellom >= 60) {
            aktivitetslogg.info("Det er $dagerMellom dager mellom forrige inntektdato (${this.dato}) og ny inntektdato ($nyDato), dette utløser varsel om gjenbruk.")
            aktivitetslogg.varsel(RV_IV_7)
        } else if (nyArbeidsgiverperiode) {
            aktivitetslogg.info("Det er ny arbeidsgiverperiode, og dette utløser varsel om gjenbruk. Forrige inntektdato var ${this.dato} og ny inntektdato er $nyDato")
            aktivitetslogg.varsel(RV_IV_7)
        }
        inntektshistorikk.leggTil(Inntektsmelding(nyDato, hendelseId, beløp, kilde, tidsstempel))
        aktivitetslogg.info("Kopierte inntekt som lå lagret på ${this.dato} til $nyDato")
    }

    override fun dto() =
        InntektsopplysningUtDto.InntektsmeldingDto(
            id = id,
            hendelseId = hendelseId,
            dato = dato,
            beløp = beløp.dto(),
            tidsstempel = tidsstempel,
            kilde = kilde.dto()
        )

    internal enum class Kilde {
        Arbeidsgiver,
        AOrdningen;

        fun dto() =
            when (this) {
                Arbeidsgiver -> InntektsopplysningUtDto.InntektsmeldingDto.KildeDto.Arbeidsgiver
                AOrdningen -> InntektsopplysningUtDto.InntektsmeldingDto.KildeDto.AOrdningen
            }

        companion object {
            fun gjenopprett(dto: KildeDto) =
                when (dto) {
                    KildeDto.Arbeidsgiver -> Arbeidsgiver
                    KildeDto.AOrdningen -> AOrdningen
                }
        }
    }

    internal companion object {
        internal fun gjenopprett(dto: InntektsopplysningInnDto.InntektsmeldingDto): Inntektsmelding =
            Inntektsmelding(
                id = dto.id,
                dato = dto.dato,
                hendelseId = dto.hendelseId,
                beløp = Inntekt.gjenopprett(dto.beløp),
                tidsstempel = dto.tidsstempel,
                kilde = Kilde.gjenopprett(dto.kilde)
            )

        internal fun List<Inntektsmelding>.finnInntektsmeldingForSkjæringstidspunkt(
            skjæringstidspunkt: LocalDate,
            førsteFraværsdag: LocalDate?
        ): Inntektsmelding? {
            val inntektsmeldinger = this.filter { it.dato == skjæringstidspunkt || it.dato == førsteFraværsdag }
            return inntektsmeldinger.maxByOrNull { inntektsmelding -> inntektsmelding.tidsstempel }
        }

        internal fun List<Inntektsmelding>.avklarSykepengegrunnlag(
            skjæringstidspunkt: LocalDate,
            førsteFraværsdag: LocalDate?,
            skattSykepengegrunnlag: SkattSykepengegrunnlag?
        ): Inntektsopplysning? {
            val inntektsmelding = finnInntektsmeldingForSkjæringstidspunkt(skjæringstidspunkt, førsteFraværsdag)
            val skatt = skattSykepengegrunnlag?.takeIf { it.kanBrukes(skjæringstidspunkt) }?.somSykepengegrunnlag() ?: return inntektsmelding
            return inntektsmelding?.avklarSykepengegrunnlag(skatt) ?: skatt
        }
    }
}

internal data class InntektsmeldingView(
    val id: UUID,
    val dato: LocalDate,
    val hendelseId: UUID,
    val beløp: Inntekt,
    val tidsstempel: LocalDateTime
)
