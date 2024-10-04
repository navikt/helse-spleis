package no.nav.helse.person.infotrygdhistorikk

import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.helse.dto.deserialisering.InfotrygdInntektsopplysningInnDto
import no.nav.helse.dto.serialisering.InfotrygdInntektsopplysningUtDto
import no.nav.helse.økonomi.Inntekt

class Inntektsopplysning private constructor(
    private val orgnummer: String,
    private val sykepengerFom: LocalDate,
    private val inntekt: Inntekt,
    private val refusjonTilArbeidsgiver: Boolean,
    private val refusjonTom: LocalDate?,
    private var lagret: LocalDateTime?
) {
    constructor(
        orgnummer: String,
        sykepengerFom: LocalDate,
        inntekt: Inntekt,
        refusjonTilArbeidsgiver: Boolean,
        refusjonTom: LocalDate? = null
    ) : this(orgnummer, sykepengerFom, inntekt, refusjonTilArbeidsgiver, refusjonTom, null)

    internal fun funksjoneltLik(other: Inntektsopplysning): Boolean {
        if (this.orgnummer != other.orgnummer) return false
        if (this.sykepengerFom != other.sykepengerFom) return false
        if (this.inntekt != other.inntekt) return false
        if (this.refusjonTom != other.refusjonTom) return false
        return this.refusjonTilArbeidsgiver == other.refusjonTilArbeidsgiver
    }

    internal companion object {
        internal fun sorter(inntekter: List<Inntektsopplysning>) =
            inntekter.sortedWith(compareBy({ it.sykepengerFom }, { it.inntekt }, { it.orgnummer }, { it.hashCode() }))

        internal fun gjenopprett(dto: InfotrygdInntektsopplysningInnDto): Inntektsopplysning {
            return Inntektsopplysning(
                orgnummer = dto.orgnummer,
                sykepengerFom = dto.sykepengerFom,
                inntekt = Inntekt.gjenopprett(dto.inntekt),
                refusjonTom = dto.refusjonTom,
                refusjonTilArbeidsgiver = dto.refusjonTilArbeidsgiver,
                lagret = dto.lagret
            )
        }
    }

    internal fun dto() = InfotrygdInntektsopplysningUtDto(
        orgnummer = orgnummer,
        sykepengerFom = sykepengerFom,
        inntekt = inntekt.dto(),
        refusjonTilArbeidsgiver = refusjonTilArbeidsgiver,
        refusjonTom = refusjonTom,
        lagret = lagret
    )
}
