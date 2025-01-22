package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID
import no.nav.helse.dto.deserialisering.FaktaavklartInntektInnDto
import no.nav.helse.dto.serialisering.FaktaavklartInntektUtDto
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IV_7
import no.nav.helse.person.inntekt.Arbeidsgiverinntekt.Kilde

internal data class FaktaavklartInntekt(
    val id: UUID,
    val inntektsdata: Inntektsdata,
    val inntektsopplysning: Inntektsopplysning
) {
    internal fun funksjoneltLik(other: FaktaavklartInntekt) =
        this.inntektsopplysning::class == other.inntektsopplysning::class && this.inntektsdata.funksjoneltLik(other.inntektsdata)

    internal fun kopierTidsnærOpplysning(
        nyDato: LocalDate,
        aktivitetslogg: IAktivitetslogg,
        nyArbeidsgiverperiode: Boolean,
        inntektshistorikk: Inntektshistorikk
    ) {
        if (inntektsopplysning !is Arbeidsgiverinntekt) return
        if (nyDato == this.inntektsdata.dato) return
        val dagerMellom = ChronoUnit.DAYS.between(this.inntektsdata.dato, nyDato)
        if (dagerMellom >= 60) {
            aktivitetslogg.info("Det er $dagerMellom dager mellom forrige inntektdato (${this.inntektsdata.dato}) og ny inntektdato ($nyDato), dette utløser varsel om gjenbruk.")
            aktivitetslogg.varsel(RV_IV_7)
        } else if (nyArbeidsgiverperiode) {
            aktivitetslogg.info("Det er ny arbeidsgiverperiode, og dette utløser varsel om gjenbruk. Forrige inntektdato var ${this.inntektsdata.dato} og ny inntektdato er $nyDato")
            aktivitetslogg.varsel(RV_IV_7)
        }

        inntektshistorikk.leggTil(Inntektsmeldinginntekt(UUID.randomUUID(), this.inntektsdata.copy(dato = nyDato), when (inntektsopplysning.kilde) {
            Kilde.Arbeidsgiver -> Inntektsmeldinginntekt.Kilde.Arbeidsgiver
            Kilde.AOrdningen -> Inntektsmeldinginntekt.Kilde.AOrdningen
        }))
        aktivitetslogg.info("Kopierte inntekt som lå lagret på ${this.inntektsdata.dato} til $nyDato")
    }

    internal fun dto() = FaktaavklartInntektUtDto(
        id = this.id,
        inntektsdata = this.inntektsdata.dto(),
        inntektsopplysning = this.inntektsopplysning.dto()
    )

    internal companion object {
        internal fun List<FaktaavklartInntekt>.markerFlereArbeidsgivere(aktivitetslogg: IAktivitetslogg) {
            if (distinctBy { it.inntektsdata.dato }.size <= 1 && none { it.inntektsopplysning is SkattSykepengegrunnlag }) return
            aktivitetslogg.varsel(Varselkode.RV_VV_2)
        }

        internal fun gjenopprett(dto: FaktaavklartInntektInnDto) = FaktaavklartInntekt(
            id = dto.id,
            inntektsdata = Inntektsdata.gjenopprett(dto.inntektsdata),
            inntektsopplysning = Inntektsopplysning.gjenopprett(dto.inntektsopplysning)
        )

    }

}
