package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID
import no.nav.helse.dto.deserialisering.ArbeidstakerFaktaavklartInntektInnDto
import no.nav.helse.dto.deserialisering.ArbeidstakerRenameMeInnDto
import no.nav.helse.dto.serialisering.ArbeidstakerFaktaavklartInntektUtDto
import no.nav.helse.dto.serialisering.ArbeidstakerRenameMeUtDto
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IV_7

internal data class ArbeidstakerFaktaavklartInntekt(
    val id: UUID,
    val inntektsdata: Inntektsdata,
    val inntektsopplysning: ArbeidstakerRenameMe
) {
    internal fun funksjoneltLik(other: ArbeidstakerFaktaavklartInntekt): Boolean {
        if (!this.inntektsdata.funksjoneltLik(other.inntektsdata)) return false
        return this.inntektsopplysning.kilde::class == other.inntektsopplysning.kilde::class
    }

    internal fun kopierTidsnærOpplysning(
        nyDato: LocalDate,
        aktivitetslogg: IAktivitetslogg,
        nyArbeidsgiverperiode: Boolean,
        inntektshistorikk: Inntektshistorikk
    ) {
        if (inntektsopplysning.kilde !is Arbeidstakerinntektskilde.Arbeidsgiver) return
        if (nyDato == this.inntektsdata.dato) return
        val dagerMellom = ChronoUnit.DAYS.between(this.inntektsdata.dato, nyDato)
        if (dagerMellom >= 60) {
            aktivitetslogg.info("Det er $dagerMellom dager mellom forrige inntektdato (${this.inntektsdata.dato}) og ny inntektdato ($nyDato), dette utløser varsel om gjenbruk.")
            aktivitetslogg.varsel(RV_IV_7)
        } else if (nyArbeidsgiverperiode) {
            aktivitetslogg.info("Det er ny arbeidsgiverperiode, og dette utløser varsel om gjenbruk. Forrige inntektdato var ${this.inntektsdata.dato} og ny inntektdato er $nyDato")
            aktivitetslogg.varsel(RV_IV_7)
        }

        inntektshistorikk.leggTil(Inntektsmeldinginntekt(UUID.randomUUID(), this.inntektsdata.copy(dato = nyDato), Inntektsmeldinginntekt.Kilde.Arbeidsgiver))
        aktivitetslogg.info("Kopierte inntekt som lå lagret på ${this.inntektsdata.dato} til $nyDato")
    }

    internal fun dto() = ArbeidstakerFaktaavklartInntektUtDto(
        id = this.id,
        inntektsdata = this.inntektsdata.dto(),
        inntektsopplysning = this.inntektsopplysning.dto()
    )

    internal companion object {
        internal fun gjenopprett(dto: ArbeidstakerFaktaavklartInntektInnDto) = ArbeidstakerFaktaavklartInntekt(
            id = dto.id,
            inntektsdata = Inntektsdata.gjenopprett(dto.inntektsdata),
            inntektsopplysning = ArbeidstakerRenameMe.gjenopprett(dto.inntektsopplysning)
        )

    }
}

internal data class ArbeidstakerRenameMe(val kilde: Arbeidstakerinntektskilde) {
    fun dto() = ArbeidstakerRenameMeUtDto(
        kilde = kilde.dto()
    )

    companion object {
        fun gjenopprett(dto: ArbeidstakerRenameMeInnDto) = ArbeidstakerRenameMe(
            kilde = Arbeidstakerinntektskilde.gjenopprett(dto.kilde)
        )
    }
}

