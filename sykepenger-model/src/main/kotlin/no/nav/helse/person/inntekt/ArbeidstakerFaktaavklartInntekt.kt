package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID
import no.nav.helse.dto.deserialisering.ArbeidstakerFaktaavklartInntektInnDto
import no.nav.helse.dto.serialisering.ArbeidstakerFaktaavklartInntektUtDto
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IV_7
import no.nav.helse.økonomi.Inntekt

internal data class ArbeidstakerFaktaavklartInntekt(
    override val id: UUID,
    override val inntektsdata: Inntektsdata,
    val inntektsopplysningskilde: Arbeidstakerinntektskilde
) : FaktaavklartInntekt {
    internal fun funksjoneltLik(other: ArbeidstakerFaktaavklartInntekt): Boolean {
        if (!this.inntektsdata.funksjoneltLik(other.inntektsdata)) return false
        return this.inntektsopplysningskilde::class == other.inntektsopplysningskilde::class
    }

    private fun lagreTidsnærOpplysning(
        forrigeDato: LocalDate,
        aktivitetslogg: IAktivitetslogg,
        nyArbeidsgiverperiode: Boolean,
        inntektshistorikk: Inntektshistorikk
    ) {
        vurderVarselForGjenbrukAvInntekt(forrigeDato, nyArbeidsgiverperiode, aktivitetslogg)
        inntektshistorikk.leggTil(Inntektsmeldinginntekt(UUID.randomUUID(), this.inntektsdata))
        aktivitetslogg.info("Kopierte inntekt som lå lagret på $forrigeDato til ${this.inntektsdata.dato}")
    }

    internal fun kopierTidsnærOpplysning(
        nyDato: LocalDate,
        aktivitetslogg: IAktivitetslogg,
        nyArbeidsgiverperiode: Boolean,
        inntektshistorikk: Inntektshistorikk
    ) {
        if (inntektsopplysningskilde !is Arbeidstakerinntektskilde.Arbeidsgiver) return
        return medInnteksdato(nyDato).lagreTidsnærOpplysning(inntektsdata.dato, aktivitetslogg, nyArbeidsgiverperiode, inntektshistorikk)
    }

    internal fun medInnteksdato(dato: LocalDate) = copy(inntektsdata = inntektsdata.copy(dato = dato))

    internal fun vurderVarselForGjenbrukAvInntekt(forrigeDato: LocalDate, harNyArbeidsgiverperiode: Boolean, aktivitetslogg: IAktivitetslogg) {
        if (inntektsdata.dato == forrigeDato) return
        val dagerMellom = ChronoUnit.DAYS.between(forrigeDato, inntektsdata.dato)
        if (dagerMellom >= 60) {
            aktivitetslogg.info("Det er $dagerMellom dager mellom forrige inntektdato ($forrigeDato) og ny inntektdato (${inntektsdata.dato}), dette utløser varsel om gjenbruk.")
            aktivitetslogg.varsel(RV_IV_7)
        } else if (harNyArbeidsgiverperiode) {
            aktivitetslogg.info("Det er ny arbeidsgiverperiode, og dette utløser varsel om gjenbruk. Forrige inntektdato var $forrigeDato og ny inntektdato er ${inntektsdata.dato}")
            aktivitetslogg.varsel(RV_IV_7)
        }
    }

    internal fun dto() = ArbeidstakerFaktaavklartInntektUtDto(
        id = this.id,
        inntektsdata = this.inntektsdata.dto(),
        inntektsopplysningskilde = this.inntektsopplysningskilde.dto()
    )

    internal companion object {
        internal fun gjenopprett(dto: ArbeidstakerFaktaavklartInntektInnDto) = ArbeidstakerFaktaavklartInntekt(
            id = dto.id,
            inntektsdata = Inntektsdata.gjenopprett(dto.inntektsdata),
            inntektsopplysningskilde = Arbeidstakerinntektskilde.gjenopprett(dto.inntektsopplysningskilde)
        )
    }

    internal fun view() = ArbeistakerFaktaavklartInntektView(inntektsdata.hendelseId.id, inntektsdata.beløp)

    internal class ArbeistakerFaktaavklartInntektView(override val hendelseId: UUID, override val beløp: Inntekt) : FaktaavklartInntektView
}
