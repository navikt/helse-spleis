package no.nav.helse

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.person.AbstractPersonTest
import no.nav.helse.person.ArbeidsgiverInntektsopplysning
import no.nav.helse.person.Inntektshistorikk
import no.nav.helse.person.Sykepengegrunnlag
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.økonomi.Inntekt

internal val Inntekt.sykepengegrunnlag get() = sykepengegrunnlag(AbstractPersonTest.ORGNUMMER)

internal fun Inntekt.sykepengegrunnlag(orgnr: String) = sykepengegrunnlag(orgnr, 1.januar)
internal fun Inntekt.sykepengegrunnlag(skjæringstidspunkt: LocalDate) =
    sykepengegrunnlag(AbstractPersonTest.ORGNUMMER, skjæringstidspunkt)

internal fun Inntekt.sykepengegrunnlag(orgnr: String, skjæringstidspunkt: LocalDate) =
    Sykepengegrunnlag(
        arbeidsgiverInntektsopplysninger = listOf(
            ArbeidsgiverInntektsopplysning(
                orgnr,
                Inntektshistorikk.Inntektsmelding(UUID.randomUUID(), skjæringstidspunkt, UUID.randomUUID(), this)
            )
        ),
        deaktiverteArbeidsforhold = emptyList(),
        skjæringstidspunkt = skjæringstidspunkt,
        subsumsjonObserver = SubsumsjonObserver.NullObserver
    )