package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import no.nav.helse.dto.deserialisering.InntektsopplysningInnDto
import no.nav.helse.dto.deserialisering.InntektsopplysningInnDto.InntektsmeldingDto.KildeDto
import no.nav.helse.dto.serialisering.InntektsopplysningUtDto
import no.nav.helse.hendelser.OverstyrArbeidsgiveropplysninger
import no.nav.helse.person.Person
import no.nav.helse.person.PersonObserver.UtkastTilVedtakEvent.Inntektskilde
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IV_7
import no.nav.helse.yearMonth
import no.nav.helse.økonomi.Inntekt

class Inntektsmeldinginntekt internal constructor(
    id: UUID,
    dato: LocalDate,
    hendelseId: UUID,
    beløp: Inntekt,
    tidsstempel: LocalDateTime,
    private val kilde: Kilde
) : Inntektsopplysning(id, hendelseId, dato, beløp, tidsstempel) {
    internal constructor(
        dato: LocalDate,
        hendelseId: UUID,
        beløp: Inntekt,
        kilde: Kilde = Kilde.Arbeidsgiver,
        tidsstempel: LocalDateTime = LocalDateTime.now()
    ) : this(UUID.randomUUID(), dato, hendelseId, beløp, tidsstempel, kilde)

    override fun gjenbrukbarInntekt(beløp: Inntekt?) =
        beløp?.let { Inntektsmeldinginntekt(dato, hendelseId, it, kilde, tidsstempel) } ?: this

    internal fun inntektskilde(): Inntektskilde = when (kilde) {
        Kilde.Arbeidsgiver -> Inntektskilde.Arbeidsgiver
        Kilde.AOrdningen -> Inntektskilde.AOrdningen
    }

    internal fun view() = InntektsmeldingView(
        id = id,
        dato = dato,
        hendelseId = hendelseId,
        beløp = beløp,
        tidsstempel = tidsstempel
    )

    override fun blirOverstyrtAv(ny: Inntektsopplysning): Inntektsopplysning {
        return ny.overstyrer(this)
    }

    override fun overstyrer(gammel: Saksbehandler) = this
    override fun overstyrer(gammel: SkjønnsmessigFastsatt) =
        if (erOmregnetÅrsinntektEndret(this, gammel)) this
        else gammel.overstyrer(this)

    internal fun avklarSykepengegrunnlag(skatt: SkatteopplysningSykepengegrunnlag): Inntektsopplysning {
        if (skatt.dato.yearMonth < this.dato.yearMonth) return skatt
        return this
    }

    internal fun kanLagres(other: Inntektsmeldinginntekt) = this.hendelseId != other.hendelseId || this.dato != other.dato
    override fun erSamme(other: Inntektsopplysning): Boolean {
        return other is Inntektsmeldinginntekt && this.dato == other.dato && other.beløp == this.beløp
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
        inntektshistorikk.leggTil(Inntektsmeldinginntekt(nyDato, hendelseId, beløp, kilde, tidsstempel))
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

        fun dto() = when (this) {
            Arbeidsgiver -> InntektsopplysningUtDto.InntektsmeldingDto.KildeDto.Arbeidsgiver
            AOrdningen -> InntektsopplysningUtDto.InntektsmeldingDto.KildeDto.AOrdningen
        }

        companion object {
            fun gjenopprett(dto: KildeDto) = when (dto) {
                KildeDto.Arbeidsgiver -> Arbeidsgiver
                KildeDto.AOrdningen -> AOrdningen
            }
        }
    }

    internal companion object {
        internal fun gjenopprett(dto: InntektsopplysningInnDto.InntektsmeldingDto): Inntektsmeldinginntekt {
            return Inntektsmeldinginntekt(
                id = dto.id,
                dato = dto.dato,
                hendelseId = dto.hendelseId,
                beløp = Inntekt.gjenopprett(dto.beløp),
                tidsstempel = dto.tidsstempel,
                kilde = Kilde.gjenopprett(dto.kilde),
            )
        }

        internal fun List<Inntektsmeldinginntekt>.finnInntektsmeldingForSkjæringstidspunkt(
            skjæringstidspunkt: LocalDate,
            førsteFraværsdag: LocalDate?
        ): Inntektsmeldinginntekt? {
            val inntektsmeldinger = this.filter { it.dato == skjæringstidspunkt || it.dato == førsteFraværsdag }
            return inntektsmeldinger.maxByOrNull { inntektsmelding -> inntektsmelding.tidsstempel }
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
