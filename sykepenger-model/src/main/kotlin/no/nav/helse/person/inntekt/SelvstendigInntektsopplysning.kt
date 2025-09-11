package no.nav.helse.person.inntekt

import java.util.UUID
import no.nav.helse.dto.deserialisering.SelvstendigInntektsopplysningInnDto
import no.nav.helse.dto.deserialisering.YrkesaktivitetstypeDto
import no.nav.helse.dto.serialisering.SelvstendigInntektsopplysningUtDto
import no.nav.helse.hendelser.Avsender
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.Behandlingsporing.Yrkesaktivitet.Arbeidsledig.somOrganisasjonsnummer
import no.nav.helse.hendelser.SkjønnsmessigFastsettelse
import no.nav.helse.person.PersonObserver.UtkastTilVedtakEvent.Inntektskilde
import no.nav.helse.person.beløp.Kilde
import no.nav.helse.person.builders.UtkastTilVedtakBuilder

internal data class SelvstendigInntektsopplysning(
    val faktaavklartInntekt: SelvstendigFaktaavklartInntekt,
    val skjønnsmessigFastsatt: SkjønnsmessigFastsatt?,
    val yrkesaktivitet: Behandlingsporing.Yrkesaktivitet
) {
    val inntektsgrunnlag = faktaavklartInntekt.inntektsdata
    private val fastsattÅrsinntektInntektsdata = skjønnsmessigFastsatt?.inntektsdata ?: inntektsgrunnlag
    val fastsattÅrsinntekt = fastsattÅrsinntektInntektsdata.beløp

    internal companion object {
        private fun List<SelvstendigInntektsopplysning>.rullTilbakeEventuellSkjønnsmessigFastsettelse() =
            map { it.copy(skjønnsmessigFastsatt = null) }

        private fun SelvstendigInntektsopplysning.kilde() = Kilde(
            avsender = if (skjønnsmessigFastsatt != null) Avsender.SAKSBEHANDLER else Avsender.SYSTEM,
            meldingsreferanseId = fastsattÅrsinntektInntektsdata.hendelseId,
            tidsstempel = fastsattÅrsinntektInntektsdata.tidsstempel
        )

        internal fun SelvstendigInntektsopplysning.beverte(builder: InntekterForBeregning.Builder) {
            builder.fraInntektsgrunnlag(yrkesaktivitet.somOrganisasjonsnummer, fastsattÅrsinntekt, this.kilde())
        }

        internal fun SelvstendigInntektsopplysning.skjønnsfastsett(other: SkjønnsmessigFastsettelse.SkjønnsfastsattInntekt): SelvstendigInntektsopplysning {
            return this.copy(
                skjønnsmessigFastsatt = SkjønnsmessigFastsatt(
                    id = UUID.randomUUID(),
                    inntektsdata = other.inntektsdata
                )
            )
        }

        private fun SelvstendigInntektsopplysning.skalSkjønnsmessigFastsattRullesTilbake(etter: SelvstendigInntektsopplysning) =
            this.inntektsgrunnlag.beløp != etter.inntektsgrunnlag.beløp

        internal fun SelvstendigInntektsopplysning.berik(builder: UtkastTilVedtakBuilder) =
            builder.arbeidsgiverinntekt(
                arbeidsgiver = yrkesaktivitet.somOrganisasjonsnummer,
                omregnedeÅrsinntekt = this.inntektsgrunnlag.beløp,
                skjønnsfastsatt = this.skjønnsmessigFastsatt?.inntektsdata?.beløp,
                inntektskilde = if (this.skjønnsmessigFastsatt != null) Inntektskilde.Saksbehandler else Inntektskilde.Sigrun
            )

        private fun SelvstendigInntektsopplysning.harFunksjonellEndring(other: SelvstendigInntektsopplysning): Boolean {
            if (this.skjønnsmessigFastsatt != other.skjønnsmessigFastsatt) return true
            return !this.faktaavklartInntekt.funksjoneltLik(other.faktaavklartInntekt)
        }

        internal fun gjenopprett(dto: SelvstendigInntektsopplysningInnDto): SelvstendigInntektsopplysning {
            return SelvstendigInntektsopplysning(
                faktaavklartInntekt = SelvstendigFaktaavklartInntekt.gjenopprett(dto.faktaavklartInntekt),
                skjønnsmessigFastsatt = dto.skjønnsmessigFastsatt?.let { SkjønnsmessigFastsatt.gjenopprett(it) },
                yrkesaktivitet = when (dto.yrkesaktivitetstype) {
                    YrkesaktivitetstypeDto.ARBEIDSLEDIG,
                    YrkesaktivitetstypeDto.ARBEIDSTAKER,
                    YrkesaktivitetstypeDto.FRILANS -> error("Kan ikke gjenomrette selvstendig inntektsopplysning med ${dto.yrkesaktivitetstype} som yrkesaktivitetstype")

                    YrkesaktivitetstypeDto.SELVSTENDIG_BARNEPASSER -> Behandlingsporing.Yrkesaktivitet.SelvstendigBarnepasser
                    YrkesaktivitetstypeDto.SELVSTENDIG -> Behandlingsporing.Yrkesaktivitet.Selvstendig

                    YrkesaktivitetstypeDto.SELVSTENDIG_JORDBRUKER,
                    YrkesaktivitetstypeDto.SELVSTENDIG_FISKER -> TODO("Ikke implementert gjenoppretting av ${dto.yrkesaktivitetstype} for selvstendig inntektsopplysning")
                }
            )
        }
    }

    internal fun dto() = SelvstendigInntektsopplysningUtDto(
        faktaavklartInntekt = this.faktaavklartInntekt.dto(),
        skjønnsmessigFastsatt = skjønnsmessigFastsatt?.dto(),
        yrkesaktivitetstype = when (this.yrkesaktivitet) {
            Behandlingsporing.Yrkesaktivitet.Arbeidsledig,
            is Behandlingsporing.Yrkesaktivitet.Arbeidstaker,
            Behandlingsporing.Yrkesaktivitet.Frilans -> error("Kan ikke serialisere selvstendig inntektsopplysning med ${this.yrkesaktivitet} som yrkesaktivitet")

            Behandlingsporing.Yrkesaktivitet.SelvstendigBarnepasser -> YrkesaktivitetstypeDto.SELVSTENDIG_BARNEPASSER
            Behandlingsporing.Yrkesaktivitet.Selvstendig -> YrkesaktivitetstypeDto.SELVSTENDIG

            Behandlingsporing.Yrkesaktivitet.SelvstendigJordbruker,
            Behandlingsporing.Yrkesaktivitet.SelvstendigFisker -> TODO("Ikke implementert serialisering av ${this.yrkesaktivitet} for selvstendig inntektsopplysning")
        }
    )
}
