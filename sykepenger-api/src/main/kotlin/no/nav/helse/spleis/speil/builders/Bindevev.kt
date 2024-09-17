package no.nav.helse.spleis.speil.builders

import java.time.LocalDate
import java.time.YearMonth
import no.nav.helse.spleis.speil.dto.Arbeidsgiverinntekt
import no.nav.helse.spleis.speil.dto.Arbeidsgiverrefusjon
import no.nav.helse.spleis.speil.dto.Inntekt
import no.nav.helse.spleis.speil.dto.InntekterFraAOrdningen
import no.nav.helse.spleis.speil.dto.Inntektkilde
import no.nav.helse.spleis.speil.dto.Refusjonselement
import no.nav.helse.spleis.speil.dto.SkjønnsmessigFastsattDTO

internal data class IArbeidsgiverinntekt(
    val arbeidsgiver: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val omregnetÅrsinntekt: IOmregnetÅrsinntekt,
    val skjønnsmessigFastsatt: SkjønnsmessigFastsattDTO?,
    val deaktivert: Boolean
) {
    internal fun toDTO(): Arbeidsgiverinntekt {
        return Arbeidsgiverinntekt(
            organisasjonsnummer = arbeidsgiver,
            omregnetÅrsinntekt = omregnetÅrsinntekt.toDTO(),
            skjønnsmessigFastsatt = skjønnsmessigFastsatt,
            fom = fom,
            tom = tom.takeUnless { it == LocalDate.MAX },
            deaktivert = deaktivert
        )
    }

    internal fun erTilkommenInntekt(skjæringstidspunkt: LocalDate) =
        fom > skjæringstidspunkt
}

internal data class IArbeidsgiverrefusjon(
    val arbeidsgiver: String,
    val refusjonsopplysninger: List<Refusjonselement>
) {
    internal fun toDTO(): Arbeidsgiverrefusjon {
        return Arbeidsgiverrefusjon(
            arbeidsgiver = arbeidsgiver,
            refusjonsopplysninger = refusjonsopplysninger
        )
    }
}

internal data class IOmregnetÅrsinntekt(
    val kilde: IInntektkilde,
    val beløp: Double,
    val månedsbeløp: Double,
    val inntekterFraAOrdningen: List<IInntekterFraAOrdningen>? = null //kun gyldig for A-ordningen
) {
    internal fun toDTO(): Inntekt {
        return Inntekt(
            kilde = kilde.toDTO(),
            beløp = beløp,
            månedsbeløp = månedsbeløp,
            inntekterFraAOrdningen = inntekterFraAOrdningen?.sortedBy { it.måned }?.map { it.toDTO() }
        )
    }
}

internal enum class IInntektkilde {
    Saksbehandler, Inntektsmelding, Infotrygd, AOrdningen, IkkeRapportert, Søknad;

    internal fun toDTO() = when (this) {
        Saksbehandler -> Inntektkilde.Saksbehandler
        Inntektsmelding -> Inntektkilde.Inntektsmelding
        Infotrygd -> Inntektkilde.Infotrygd
        AOrdningen -> Inntektkilde.AOrdningen
        IkkeRapportert -> Inntektkilde.IkkeRapportert
        Søknad -> Inntektkilde.Søknad
    }
}

internal data class IInntekterFraAOrdningen(
    val måned: YearMonth,
    val sum: Double
) {
    internal fun toDTO(): InntekterFraAOrdningen {
        return InntekterFraAOrdningen(måned, sum)
    }
}



