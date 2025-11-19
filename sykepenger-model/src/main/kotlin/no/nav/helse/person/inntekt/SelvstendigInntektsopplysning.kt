package no.nav.helse.person.inntekt

import no.nav.helse.dto.deserialisering.SelvstendigInntektsopplysningInnDto
import no.nav.helse.dto.serialisering.SelvstendigInntektsopplysningUtDto
import no.nav.helse.person.EventSubscription.UtkastTilVedtakEvent.Inntektskilde
import no.nav.helse.person.builders.UtkastTilVedtakBuilder

internal data class SelvstendigInntektsopplysning(
    val faktaavklartInntekt: SelvstendigFaktaavklartInntekt,
    val skjønnsmessigFastsatt: SkjønnsmessigFastsatt?
) {
    val fastsattÅrsinntekt = faktaavklartInntekt.normalinntekt
    val beregningsgrunnlag = faktaavklartInntekt.beregningsgrunnlag

    internal companion object {
        internal fun SelvstendigInntektsopplysning.berik(builder: UtkastTilVedtakBuilder) =
            builder.arbeidsgiverinntekt(
                arbeidsgiver = "SELVSTENDIG",
                omregnedeÅrsinntekt = this.fastsattÅrsinntekt,
                skjønnsfastsatt = this.skjønnsmessigFastsatt?.inntektsdata?.beløp,
                inntektskilde = if (this.skjønnsmessigFastsatt != null) Inntektskilde.Saksbehandler else Inntektskilde.Sigrun
            )

        internal fun gjenopprett(dto: SelvstendigInntektsopplysningInnDto): SelvstendigInntektsopplysning {
            return SelvstendigInntektsopplysning(
                faktaavklartInntekt = SelvstendigFaktaavklartInntekt.gjenopprett(dto.faktaavklartInntekt),
                skjønnsmessigFastsatt = dto.skjønnsmessigFastsatt?.let { SkjønnsmessigFastsatt.gjenopprett(it) }
            )
        }
    }

    internal fun dto() = SelvstendigInntektsopplysningUtDto(
        faktaavklartInntekt = this.faktaavklartInntekt.dto(),
        skjønnsmessigFastsatt = skjønnsmessigFastsatt?.dto()
    )
}
