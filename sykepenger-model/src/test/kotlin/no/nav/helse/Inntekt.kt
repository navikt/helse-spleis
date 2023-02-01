package no.nav.helse

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.Alder.Companion.alder
import no.nav.helse.person.AbstractPersonTest
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning
import no.nav.helse.person.inntekt.Inntektsmelding
import no.nav.helse.person.inntekt.Refusjonsopplysning
import no.nav.helse.person.inntekt.Refusjonsopplysning.Refusjonsopplysninger
import no.nav.helse.person.inntekt.Refusjonsopplysning.Refusjonsopplysninger.Companion.refusjonsopplysninger
import no.nav.helse.person.inntekt.Sykepengegrunnlag
import no.nav.helse.økonomi.Inntekt

internal val Inntekt.sykepengegrunnlag get() = sykepengegrunnlag(AbstractPersonTest.ORGNUMMER)

internal fun Inntekt.sykepengegrunnlag(orgnr: String) = sykepengegrunnlag(AbstractPersonTest.UNG_PERSON_FØDSELSDATO.alder, orgnr, 1.januar)
internal fun Inntekt.sykepengegrunnlag(alder: Alder) = sykepengegrunnlag(alder, AbstractPersonTest.ORGNUMMER, 1.januar)
internal fun Inntekt.sykepengegrunnlag(skjæringstidspunkt: LocalDate) =
    sykepengegrunnlag(AbstractPersonTest.UNG_PERSON_FØDSELSDATO.alder, AbstractPersonTest.ORGNUMMER, skjæringstidspunkt)

internal fun Inntekt.sykepengegrunnlag(alder: Alder, orgnr: String, skjæringstidspunkt: LocalDate, subsumsjonObserver: SubsumsjonObserver = SubsumsjonObserver.NullObserver) =
    Sykepengegrunnlag(
        alder = alder,
        arbeidsgiverInntektsopplysninger = listOf(
            ArbeidsgiverInntektsopplysning(
                orgnr,
                Inntektsmelding(UUID.randomUUID(), skjæringstidspunkt, UUID.randomUUID(), this, LocalDateTime.now()),
                Refusjonsopplysninger()
            )
        ),
        skjæringstidspunkt = skjæringstidspunkt,
        subsumsjonObserver = subsumsjonObserver
    )
internal fun Inntekt.sykepengegrunnlag(orgnr: String, skjæringstidspunkt: LocalDate, virkningstidspunkt: LocalDate) =
    Sykepengegrunnlag(
        alder = AbstractPersonTest.UNG_PERSON_FØDSELSDATO.alder,
        skjæringstidspunkt = skjæringstidspunkt,
        arbeidsgiverInntektsopplysninger = listOf(
            ArbeidsgiverInntektsopplysning(
                orgnr,
                Inntektsmelding(UUID.randomUUID(), skjæringstidspunkt, UUID.randomUUID(), this, LocalDateTime.now()),
                Refusjonsopplysning(UUID.randomUUID(), skjæringstidspunkt, null, this).refusjonsopplysninger
            )
        ),
        deaktiverteArbeidsforhold = emptyList(),
        vurdertInfotrygd = false,
        `6G` = Grunnbeløp.`6G`.beløp(skjæringstidspunkt, virkningstidspunkt)
    )