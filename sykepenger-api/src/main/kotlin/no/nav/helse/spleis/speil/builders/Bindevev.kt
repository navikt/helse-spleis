package no.nav.helse.spleis.speil.builders

import no.nav.helse.spleis.speil.dto.*
import java.time.YearMonth

internal data class IArbeidsgiverinntekt(
    val arbeidsgiver: String,
    val omregnetÅrsinntekt: IOmregnetÅrsinntekt,
    val skjønnsmessigFastsatt: SkjønnsmessigFastsattDTO?,
    val deaktivert: Boolean,
) {
    internal fun toDTO(): Arbeidsgiverinntekt =
        Arbeidsgiverinntekt(
            organisasjonsnummer = arbeidsgiver,
            omregnetÅrsinntekt = omregnetÅrsinntekt.toDTO(),
            skjønnsmessigFastsatt = skjønnsmessigFastsatt,
            deaktivert = deaktivert,
        )
}

internal data class IArbeidsgiverrefusjon(
    val arbeidsgiver: String,
    val refusjonsopplysninger: List<Refusjonselement>,
) {
    internal fun toDTO(): Arbeidsgiverrefusjon =
        Arbeidsgiverrefusjon(
            arbeidsgiver = arbeidsgiver,
            refusjonsopplysninger = refusjonsopplysninger,
        )
}

internal data class IOmregnetÅrsinntekt(
    val kilde: IInntektkilde,
    val beløp: Double,
    val månedsbeløp: Double,
    val inntekterFraAOrdningen: List<IInntekterFraAOrdningen>? = null, // kun gyldig for A-ordningen
) {
    internal fun toDTO(): Inntekt =
        Inntekt(
            kilde = kilde.toDTO(),
            beløp = beløp,
            månedsbeløp = månedsbeløp,
            inntekterFraAOrdningen = inntekterFraAOrdningen?.sortedBy { it.måned }?.map { it.toDTO() },
        )
}

internal enum class IInntektkilde {
    Saksbehandler,
    Inntektsmelding,
    Infotrygd,
    AOrdningen,
    IkkeRapportert,
    ;

    internal fun toDTO() =
        when (this) {
            Saksbehandler -> Inntektkilde.Saksbehandler
            Inntektsmelding -> Inntektkilde.Inntektsmelding
            Infotrygd -> Inntektkilde.Infotrygd
            AOrdningen -> Inntektkilde.AOrdningen
            IkkeRapportert -> Inntektkilde.IkkeRapportert
        }
}

internal data class IInntekterFraAOrdningen(
    val måned: YearMonth,
    val sum: Double,
) {
    internal fun toDTO(): InntekterFraAOrdningen = InntekterFraAOrdningen(måned, sum)
}
