package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.*
import no.nav.helse.dto.deserialisering.InntektsopplysningInnDto
import no.nav.helse.dto.deserialisering.InntektsopplysningInnDto.ArbeidsgiverinntektDto.KildeDto
import no.nav.helse.dto.serialisering.InntektsopplysningUtDto
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IV_7

internal class Arbeidsgiverinntekt internal constructor(
    id: UUID,
    inntektsdata: Inntektsdata,
    internal val kilde: Kilde
) : Inntektsopplysning(id, inntektsdata) {

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
        inntektshistorikk.leggTil(Inntektsmeldinginntekt(UUID.randomUUID(), this.inntektsdata.copy(dato = nyDato), when (this.kilde) {
            Kilde.Arbeidsgiver -> Inntektsmeldinginntekt.Kilde.Arbeidsgiver
            Kilde.AOrdningen -> Inntektsmeldinginntekt.Kilde.AOrdningen
        }))
        aktivitetslogg.info("Kopierte inntekt som lå lagret på ${this.inntektsdata.dato} til $nyDato")
    }

    override fun dto() =
        InntektsopplysningUtDto.ArbeidsgiverinntektDto(
            id = id,
            inntektsdata = inntektsdata.dto(),
            kilde = kilde.dto()
        )

    internal enum class Kilde {
        Arbeidsgiver,
        AOrdningen;

        fun dto() = when (this) {
            Arbeidsgiver -> InntektsopplysningUtDto.ArbeidsgiverinntektDto.KildeDto.Arbeidsgiver
            AOrdningen -> InntektsopplysningUtDto.ArbeidsgiverinntektDto.KildeDto.AOrdningen
        }

        companion object {
            fun gjenopprett(dto: KildeDto) = when (dto) {
                KildeDto.Arbeidsgiver -> Arbeidsgiver
                KildeDto.AOrdningen -> AOrdningen
            }
        }
    }

    internal companion object {
        internal fun fraInntektsmelding(inntektsmeldinginntekt: Inntektsmeldinginntekt) =
            Arbeidsgiverinntekt(
                id = UUID.randomUUID(),
                inntektsdata = inntektsmeldinginntekt.inntektsdata,
                kilde = when (inntektsmeldinginntekt.kilde) {
                    Inntektsmeldinginntekt.Kilde.Arbeidsgiver -> Kilde.Arbeidsgiver
                    Inntektsmeldinginntekt.Kilde.AOrdningen -> Kilde.AOrdningen
                }
            )

        internal fun gjenopprett(dto: InntektsopplysningInnDto.ArbeidsgiverinntektDto): Arbeidsgiverinntekt {
            return Arbeidsgiverinntekt(
                id = dto.id,
                inntektsdata = Inntektsdata.gjenopprett(dto.inntektsdata),
                kilde = Kilde.gjenopprett(dto.kilde),
            )
        }
    }
}
