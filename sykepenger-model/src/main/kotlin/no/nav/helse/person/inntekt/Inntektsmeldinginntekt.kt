package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import no.nav.helse.dto.deserialisering.InntektsopplysningInnDto
import no.nav.helse.dto.deserialisering.InntektsopplysningInnDto.InntektsmeldingDto.KildeDto
import no.nav.helse.dto.serialisering.InntektsopplysningUtDto
import no.nav.helse.person.PersonObserver.UtkastTilVedtakEvent.Inntektskilde
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IV_7
import no.nav.helse.yearMonth
import no.nav.helse.økonomi.Inntekt

class Inntektsmeldinginntekt internal constructor(
    id: UUID,
    inntektsdata: Inntektsdata,
    internal val kilde: Kilde
) : Inntektsopplysning(id, inntektsdata) {
    internal constructor(
        dato: LocalDate,
        hendelseId: UUID,
        beløp: Inntekt,
        kilde: Kilde = Kilde.Arbeidsgiver,
        tidsstempel: LocalDateTime = LocalDateTime.now()
    ) : this(UUID.randomUUID(), Inntektsdata(hendelseId, dato, beløp, tidsstempel), kilde)

    internal fun inntektskilde(): Inntektskilde = when (kilde) {
        Kilde.Arbeidsgiver -> Inntektskilde.Arbeidsgiver
        Kilde.AOrdningen -> Inntektskilde.AOrdningen
    }

    internal fun view() = InntektsmeldingView(
        id = id,
        inntektsdata = inntektsdata
    )

    internal fun avklarSykepengegrunnlag(skatt: SkatteopplysningSykepengegrunnlag): Inntektsopplysning {
        if (skatt.inntektsdata.dato.yearMonth < this.inntektsdata.dato.yearMonth) return skatt
        return this
    }

    internal fun kanLagres(other: Inntektsmeldinginntekt) = this.inntektsdata.hendelseId != other.inntektsdata.hendelseId || this.inntektsdata.dato != other.inntektsdata.dato

    internal fun kopierTidsnærOpplysning(
        nyDato: LocalDate,
        aktivitetslogg: IAktivitetslogg,
        nyArbeidsgiverperiode: Boolean,
        inntektshistorikk: Inntektshistorikk
    ) {
        if (nyDato == this.inntektsdata.dato) return
        val dagerMellom = ChronoUnit.DAYS.between(this.inntektsdata.dato, nyDato)
        if (dagerMellom >= 60) {
            aktivitetslogg.info("Det er $dagerMellom dager mellom forrige inntektdato (${this.inntektsdata.dato}) og ny inntektdato ($nyDato), dette utløser varsel om gjenbruk.")
            aktivitetslogg.varsel(RV_IV_7)
        } else if (nyArbeidsgiverperiode) {
            aktivitetslogg.info("Det er ny arbeidsgiverperiode, og dette utløser varsel om gjenbruk. Forrige inntektdato var ${this.inntektsdata.dato} og ny inntektdato er $nyDato")
            aktivitetslogg.varsel(RV_IV_7)
        }
        inntektshistorikk.leggTil(Inntektsmeldinginntekt(nyDato, inntektsdata.hendelseId, inntektsdata.beløp, kilde, inntektsdata.tidsstempel))
        aktivitetslogg.info("Kopierte inntekt som lå lagret på ${this.inntektsdata.dato} til $nyDato")
    }

    override fun dto() =
        InntektsopplysningUtDto.InntektsmeldingDto(
            id = id,
            inntektsdata = inntektsdata.dto(),
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
                inntektsdata = Inntektsdata.gjenopprett(dto.inntektsdata),
                kilde = Kilde.gjenopprett(dto.kilde),
            )
        }

        internal fun List<Inntektsmeldinginntekt>.finnInntektsmeldingForSkjæringstidspunkt(
            skjæringstidspunkt: LocalDate,
            førsteFraværsdag: LocalDate?
        ): Inntektsmeldinginntekt? {
            val inntektsmeldinger = this.filter { it.inntektsdata.dato == skjæringstidspunkt || it.inntektsdata.dato == førsteFraværsdag }
            return inntektsmeldinger.maxByOrNull { inntektsmelding -> inntektsmelding.inntektsdata.tidsstempel }
        }
    }
}

internal data class InntektsmeldingView(
    val id: UUID,
    val inntektsdata: Inntektsdata
)
