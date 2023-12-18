package no.nav.helse

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.Alder.Companion.alder
import no.nav.helse.etterlevelse.SubsumsjonObserver
import no.nav.helse.hendelser.til
import no.nav.helse.person.AbstractPersonTest
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysningForSammenligningsgrunnlag
import no.nav.helse.person.inntekt.Inntektsmelding
import no.nav.helse.person.inntekt.Refusjonsopplysning
import no.nav.helse.person.inntekt.Refusjonsopplysning.Refusjonsopplysninger
import no.nav.helse.person.inntekt.Refusjonsopplysning.Refusjonsopplysninger.Companion.refusjonsopplysninger
import no.nav.helse.person.inntekt.Sammenligningsgrunnlag
import no.nav.helse.person.inntekt.Skatteopplysning
import no.nav.helse.person.inntekt.Skatteopplysning.Inntekttype.LØNNSINNTEKT
import no.nav.helse.person.inntekt.Sykepengegrunnlag
import no.nav.helse.økonomi.Inntekt

internal val Inntekt.sykepengegrunnlag get() = sykepengegrunnlag(AbstractPersonTest.ORGNUMMER)

internal fun Inntekt.sykepengegrunnlag(orgnr: String) = sykepengegrunnlag(AbstractPersonTest.UNG_PERSON_FØDSELSDATO.alder, orgnr, 1.januar)
internal fun Inntekt.sykepengegrunnlag(alder: Alder) = sykepengegrunnlag(alder, AbstractPersonTest.ORGNUMMER, 1.januar)
internal fun Inntekt.sykepengegrunnlag(skjæringstidspunkt: LocalDate) =
    sykepengegrunnlag(AbstractPersonTest.UNG_PERSON_FØDSELSDATO.alder, AbstractPersonTest.ORGNUMMER, skjæringstidspunkt)

internal fun Inntekt.sykepengegrunnlag(alder: Alder, orgnr: String, skjæringstidspunkt: LocalDate, subsumsjonObserver: SubsumsjonObserver = SubsumsjonObserver.NullObserver, skattInntekt: Inntekt? = null, refusjonsopplysninger: Refusjonsopplysninger = Refusjonsopplysninger()) =
    Sykepengegrunnlag(
        alder = alder,
        arbeidsgiverInntektsopplysninger = listOf(
            ArbeidsgiverInntektsopplysning(
                orgnr,
                skjæringstidspunkt til LocalDate.MAX,
                Inntektsmelding(skjæringstidspunkt, UUID.randomUUID(), this, LocalDateTime.now()),
                refusjonsopplysninger
            )
        ),
        sammenligningsgrunnlag = Sammenligningsgrunnlag(skattInntekt?.let {
            val meldingsreferanseId = UUID.randomUUID()
            val innteker = (1L..12L).map { Skatteopplysning(meldingsreferanseId, skattInntekt, skjæringstidspunkt.yearMonth.minusMonths(it), LØNNSINNTEKT, "", "") }
            listOf(ArbeidsgiverInntektsopplysningForSammenligningsgrunnlag(orgnr, innteker))
        } ?: emptyList()),
        skjæringstidspunkt = skjæringstidspunkt,
        subsumsjonObserver = subsumsjonObserver,
        personObservers = emptyList()
    )
internal fun Inntekt.sykepengegrunnlag(orgnr: String, skjæringstidspunkt: LocalDate, virkningstidspunkt: LocalDate) =
    Sykepengegrunnlag.ferdigSykepengegrunnlag(
        alder = AbstractPersonTest.UNG_PERSON_FØDSELSDATO.alder,
        skjæringstidspunkt = skjæringstidspunkt,
        arbeidsgiverInntektsopplysninger = listOf(
            ArbeidsgiverInntektsopplysning(
                orgnr,
                skjæringstidspunkt til LocalDate.MAX,
                Inntektsmelding(skjæringstidspunkt, UUID.randomUUID(), this, LocalDateTime.now()),
                Refusjonsopplysning(UUID.randomUUID(), skjæringstidspunkt, null, this).refusjonsopplysninger
            )
        ),
        sammenligningsgrunnlag = Sammenligningsgrunnlag(emptyList()),
        deaktiverteArbeidsforhold = emptyList(),
        vurdertInfotrygd = false,
        `6G` = Grunnbeløp.`6G`.beløp(skjæringstidspunkt, virkningstidspunkt),
        tilstand = Sykepengegrunnlag.FastsattEtterHovedregel
    )