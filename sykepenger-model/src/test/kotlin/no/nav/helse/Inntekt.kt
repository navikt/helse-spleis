package no.nav.helse

import java.time.LocalDate
import no.nav.helse.dsl.a1
import no.nav.helse.etterlevelse.Subsumsjonslogg
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning
import no.nav.helse.person.inntekt.Inntektsgrunnlag
import no.nav.helse.person.inntekt.arbeidsgiverinntekt
import no.nav.helse.økonomi.Inntekt

internal val Inntekt.sykepengegrunnlag get() = inntektsgrunnlag(a1)

internal fun Inntekt.inntektsgrunnlag(orgnr: String) = inntektsgrunnlag(orgnr, 1.januar)
internal fun Inntekt.inntektsgrunnlag() = inntektsgrunnlag(a1, 1.januar)
internal fun Inntekt.inntektsgrunnlag(skjæringstidspunkt: LocalDate) =
    inntektsgrunnlag(a1, skjæringstidspunkt)

internal fun Inntekt.inntektsgrunnlag(orgnr: String, skjæringstidspunkt: LocalDate, subsumsjonslogg: Subsumsjonslogg = Subsumsjonslogg.EmptyLog) =
    Inntektsgrunnlag(
        arbeidsgiverInntektsopplysninger = listOf(
            ArbeidsgiverInntektsopplysning(
                orgnummer = orgnr,
                faktaavklartInntekt = arbeidsgiverinntekt(skjæringstidspunkt, this),
                korrigertInntekt = null,
                skjønnsmessigFastsatt = null
            )
        ),
        selvstendigInntektsopplysning = null,
        deaktiverteArbeidsforhold = emptyList(),
        skjæringstidspunkt = skjæringstidspunkt,
        subsumsjonslogg = subsumsjonslogg
    )

internal fun Inntekt.inntektsgrunnlag(orgnr: String, skjæringstidspunkt: LocalDate, virkningstidspunkt: LocalDate) =
    Inntektsgrunnlag(
        skjæringstidspunkt = skjæringstidspunkt,
        arbeidsgiverInntektsopplysninger = listOf<ArbeidsgiverInntektsopplysning>(
            ArbeidsgiverInntektsopplysning(
                orgnummer = orgnr,
                faktaavklartInntekt = arbeidsgiverinntekt(skjæringstidspunkt, this),
                korrigertInntekt = null,
                skjønnsmessigFastsatt = null
            )
        ),
        selvstendigInntektsopplysning = null,
        deaktiverteArbeidsforhold = emptyList(),
        vurdertInfotrygd = false,
        `6G` = Grunnbeløp.`6G`.beløp(skjæringstidspunkt, virkningstidspunkt)
    )
