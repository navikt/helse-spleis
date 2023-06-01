package no.nav.helse.dsl

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import java.util.UUID
import no.nav.helse.Personidentifikator
import no.nav.helse.dsl.OverstyrtArbeidsgiveropplysning.Companion.medSaksbehandlerinntekt
import no.nav.helse.dsl.OverstyrtArbeidsgiveropplysning.Companion.medSkjønnsmessigFastsattInntekt
import no.nav.helse.hendelser.Dødsmelding
import no.nav.helse.hendelser.OverstyrArbeidsforhold
import no.nav.helse.hendelser.OverstyrArbeidsgiveropplysninger
import no.nav.helse.hendelser.PersonPåminnelse
import no.nav.helse.hendelser.SkjønnsmessigFastsettelse
import no.nav.helse.hendelser.Subsumsjon
import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning
import no.nav.helse.person.inntekt.Inntektsopplysning
import no.nav.helse.person.inntekt.Refusjonsopplysning
import no.nav.helse.person.inntekt.Refusjonsopplysning.Refusjonsopplysninger.RefusjonsopplysningerBuilder
import no.nav.helse.person.inntekt.Saksbehandler
import no.nav.helse.person.inntekt.SkjønnsmessigFastsatt
import no.nav.helse.økonomi.Inntekt

internal class PersonHendelsefabrikk(
    private val aktørId: String,
    private val personidentifikator: Personidentifikator
) {
    internal fun lagDødsmelding(dødsdato: LocalDate) =
        Dødsmelding(
            meldingsreferanseId = UUID.randomUUID(),
            fødselsnummer = personidentifikator.toString(),
            aktørId = aktørId,
            dødsdato = dødsdato
        )
    internal fun lagOverstyrArbeidsforhold(skjæringstidspunkt: LocalDate, vararg overstyrteArbeidsforhold: OverstyrArbeidsforhold.ArbeidsforholdOverstyrt) =
        OverstyrArbeidsforhold(
            meldingsreferanseId = UUID.randomUUID(),
            fødselsnummer = personidentifikator.toString(),
            aktørId = aktørId,
            skjæringstidspunkt = skjæringstidspunkt,
            overstyrteArbeidsforhold = overstyrteArbeidsforhold.toList()
        )
    internal fun lagPåminnelse() =
        PersonPåminnelse(
            meldingsreferanseId = UUID.randomUUID(),
            fødselsnummer = personidentifikator.toString(),
            aktørId = aktørId
        )

    internal fun lagSkjønnsmessigFastsettelse(skjæringstidspunkt: LocalDate, arbeidsgiveropplysninger: List<OverstyrtArbeidsgiveropplysning>, meldingsreferanseId: UUID) =
        SkjønnsmessigFastsettelse(
            meldingsreferanseId = meldingsreferanseId,
            fødselsnummer = personidentifikator.toString(),
            aktørId = aktørId,
            skjæringstidspunkt = skjæringstidspunkt,
            arbeidsgiveropplysninger = arbeidsgiveropplysninger.medSkjønnsmessigFastsattInntekt(meldingsreferanseId, skjæringstidspunkt)
        )

    internal fun lagOverstyrArbeidsgiveropplysninger(skjæringstidspunkt: LocalDate, arbeidsgiveropplysninger: List<OverstyrtArbeidsgiveropplysning>, meldingsreferanseId: UUID) =
        OverstyrArbeidsgiveropplysninger(
            meldingsreferanseId = meldingsreferanseId,
            fødselsnummer = personidentifikator.toString(),
            aktørId = aktørId,
            skjæringstidspunkt = skjæringstidspunkt,
            arbeidsgiveropplysninger = arbeidsgiveropplysninger.medSaksbehandlerinntekt(meldingsreferanseId, skjæringstidspunkt)
        )

    internal fun lagUtbetalingshistorikkForFeriepenger(opptjeningsår: Year) =
        UtbetalingshistorikkForFeriepenger(
            meldingsreferanseId = UUID.randomUUID(),
            aktørId = aktørId,
            fødselsnummer = personidentifikator.toString(),
            utbetalinger = emptyList(),
            feriepengehistorikk = emptyList(),
            arbeidskategorikoder = UtbetalingshistorikkForFeriepenger.Arbeidskategorikoder(emptyList()),
            opptjeningsår = opptjeningsår,
            skalBeregnesManuelt = false
        )
}

internal class OverstyrtArbeidsgiveropplysning(
    private val orgnummer: String,
    private val inntekt: Inntekt,
    private val forklaring: String? = null,
    private val subsumsjon: Subsumsjon? = null,
    private val refusjonsopplysninger: List<Triple<LocalDate, LocalDate?, Inntekt>>? = null
) {
    private fun refusjonsopplysninger(skjæringstidspunkt: LocalDate) =  refusjonsopplysninger ?: listOf(Triple(skjæringstidspunkt, null, inntekt))
    internal companion object {
        private fun List<OverstyrtArbeidsgiveropplysning>.tilArbeidsgiverInntektsopplysning(meldingsreferanseId: UUID, skjæringstidspunkt: LocalDate, inntektsopplysning: (overstyrtArbeidsgiveropplysning: OverstyrtArbeidsgiveropplysning) -> Inntektsopplysning) =
            map {
                ArbeidsgiverInntektsopplysning(
                    orgnummer = it.orgnummer,
                    inntektsopplysning = inntektsopplysning(it),
                    refusjonsopplysninger = RefusjonsopplysningerBuilder().apply { it.refusjonsopplysninger(skjæringstidspunkt).forEach { (fom, tom, refusjonsbeløp) -> leggTil(Refusjonsopplysning(meldingsreferanseId, fom, tom, refusjonsbeløp), LocalDateTime.now()) } }.build()
                )
            }
        internal fun List<OverstyrtArbeidsgiveropplysning>.medSaksbehandlerinntekt(meldingsreferanseId: UUID, skjæringstidspunkt: LocalDate) = tilArbeidsgiverInntektsopplysning(meldingsreferanseId, skjæringstidspunkt) {
            checkNotNull(it.forklaring) { "Forklaring må settes på Saksbehandlerinntekt"}
            Saksbehandler(skjæringstidspunkt, meldingsreferanseId, it.inntekt, it.forklaring, it.subsumsjon, LocalDateTime.now())
        }
        internal fun List<OverstyrtArbeidsgiveropplysning>.medSkjønnsmessigFastsattInntekt(meldingsreferanseId: UUID, skjæringstidspunkt: LocalDate) = tilArbeidsgiverInntektsopplysning(meldingsreferanseId, skjæringstidspunkt) {
            check(it.forklaring == null) { "Skal ikke sette forklaring på Skjønnsmessig fastsatt inntekt" }
            check(it.subsumsjon == null) { "Skal ikke sette subsumsjon på Skjønssmessig fastsatt inntekt" }
            val FJERN_MEG: Subsumsjon? = null
            SkjønnsmessigFastsatt(skjæringstidspunkt, meldingsreferanseId, it.inntekt, "FJERN MEG", FJERN_MEG, LocalDateTime.now())
        }
    }
}