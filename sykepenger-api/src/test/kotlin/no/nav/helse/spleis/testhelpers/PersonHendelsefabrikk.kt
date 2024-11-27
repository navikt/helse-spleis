package no.nav.helse.spleis.testhelpers

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import java.util.UUID
import no.nav.helse.hendelser.Avsender.SAKSBEHANDLER
import no.nav.helse.hendelser.Dødsmelding
import no.nav.helse.hendelser.MinimumSykdomsgradsvurderingMelding
import no.nav.helse.hendelser.OverstyrArbeidsforhold
import no.nav.helse.hendelser.OverstyrArbeidsgiveropplysninger
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.PersonPåminnelse
import no.nav.helse.hendelser.SkjønnsmessigFastsettelse
import no.nav.helse.hendelser.Subsumsjon
import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger
import no.nav.helse.hendelser.til
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.Kilde
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning
import no.nav.helse.person.inntekt.Inntektsopplysning
import no.nav.helse.person.inntekt.Refusjonsopplysning
import no.nav.helse.person.inntekt.Refusjonsopplysning.Refusjonsopplysninger.RefusjonsopplysningerBuilder
import no.nav.helse.person.inntekt.Saksbehandler
import no.nav.helse.person.inntekt.SkjønnsmessigFastsatt
import no.nav.helse.spleis.testhelpers.OverstyrtArbeidsgiveropplysning.Companion.medSaksbehandlerinntekt
import no.nav.helse.spleis.testhelpers.OverstyrtArbeidsgiveropplysning.Companion.medSkjønnsmessigFastsattInntekt
import no.nav.helse.spleis.testhelpers.OverstyrtArbeidsgiveropplysning.Companion.refusjonstidslinjer
import no.nav.helse.økonomi.Inntekt

internal class PersonHendelsefabrikk() {
    internal fun lagMinimumSykdomsgradsvurderingMelding(perioderMedMinimumSykdomsgradVurdertOK: Set<Periode> = emptySet(), perioderMedMinimumSykdomsgradVurdertIkkeOK: Set<Periode> = emptySet()) =
        MinimumSykdomsgradsvurderingMelding(
            perioderMedMinimumSykdomsgradVurdertOK = perioderMedMinimumSykdomsgradVurdertOK,
            perioderMedMinimumSykdomsgradVurdertIkkeOK = perioderMedMinimumSykdomsgradVurdertIkkeOK,
            meldingsreferanseId = UUID.randomUUID()
        )

    internal fun lagDødsmelding(dødsdato: LocalDate) =
        Dødsmelding(
            meldingsreferanseId = UUID.randomUUID(),
            dødsdato = dødsdato
        )

    internal fun lagOverstyrArbeidsforhold(skjæringstidspunkt: LocalDate, vararg overstyrteArbeidsforhold: OverstyrArbeidsforhold.ArbeidsforholdOverstyrt) =
        OverstyrArbeidsforhold(
            meldingsreferanseId = UUID.randomUUID(),
            skjæringstidspunkt = skjæringstidspunkt,
            overstyrteArbeidsforhold = overstyrteArbeidsforhold.toList(),
            opprettet = LocalDateTime.now()
        )

    internal fun lagPåminnelse() =
        PersonPåminnelse(
            meldingsreferanseId = UUID.randomUUID()
        )

    internal fun lagSkjønnsmessigFastsettelse(skjæringstidspunkt: LocalDate, arbeidsgiveropplysninger: List<OverstyrtArbeidsgiveropplysning>, meldingsreferanseId: UUID) =
        SkjønnsmessigFastsettelse(
            meldingsreferanseId = meldingsreferanseId,
            skjæringstidspunkt = skjæringstidspunkt,
            arbeidsgiveropplysninger = arbeidsgiveropplysninger.medSkjønnsmessigFastsattInntekt(meldingsreferanseId, skjæringstidspunkt),
            opprettet = LocalDateTime.now()
        )

    internal fun lagOverstyrArbeidsgiveropplysninger(skjæringstidspunkt: LocalDate, arbeidsgiveropplysninger: List<OverstyrtArbeidsgiveropplysning>, meldingsreferanseId: UUID): OverstyrArbeidsgiveropplysninger {
        val opprettet = LocalDateTime.now()
        return OverstyrArbeidsgiveropplysninger(
            meldingsreferanseId = meldingsreferanseId,
            skjæringstidspunkt = skjæringstidspunkt,
            arbeidsgiveropplysninger = arbeidsgiveropplysninger.medSaksbehandlerinntekt(meldingsreferanseId, skjæringstidspunkt),
            refusjonstidslinjer = arbeidsgiveropplysninger.refusjonstidslinjer(skjæringstidspunkt, meldingsreferanseId, opprettet),
            opprettet = opprettet
        )
    }

    internal fun lagUtbetalingshistorikkForFeriepenger(opptjeningsår: Year) =
        UtbetalingshistorikkForFeriepenger(
            meldingsreferanseId = UUID.randomUUID(),
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
    private val refusjonsopplysninger: List<Triple<LocalDate, LocalDate?, Inntekt>>? = null,
    private val gjelder: Periode? = null
) {
    private fun refusjonsopplysninger(skjæringstidspunkt: LocalDate) = refusjonsopplysninger ?: listOf(Triple(skjæringstidspunkt, null, inntekt))

    internal companion object {
        private fun List<OverstyrtArbeidsgiveropplysning>.tilArbeidsgiverInntektsopplysning(meldingsreferanseId: UUID, skjæringstidspunkt: LocalDate, inntektsopplysning: (overstyrtArbeidsgiveropplysning: OverstyrtArbeidsgiveropplysning) -> Inntektsopplysning) =
            map {
                ArbeidsgiverInntektsopplysning(
                    orgnummer = it.orgnummer,
                    gjelder = it.gjelder ?: (skjæringstidspunkt til LocalDate.MAX),
                    inntektsopplysning = inntektsopplysning(it),
                    refusjonsopplysninger = RefusjonsopplysningerBuilder().apply { it.refusjonsopplysninger(skjæringstidspunkt).forEach { (fom, tom, refusjonsbeløp) -> leggTil(Refusjonsopplysning(meldingsreferanseId, fom, tom, refusjonsbeløp, SAKSBEHANDLER, LocalDateTime.now()), LocalDateTime.now()) } }.build()
                )
            }

        internal fun List<OverstyrtArbeidsgiveropplysning>.medSaksbehandlerinntekt(meldingsreferanseId: UUID, skjæringstidspunkt: LocalDate) = tilArbeidsgiverInntektsopplysning(meldingsreferanseId, skjæringstidspunkt) {
            checkNotNull(it.forklaring) { "Forklaring må settes på Saksbehandlerinntekt" }
            Saksbehandler(skjæringstidspunkt, meldingsreferanseId, it.inntekt, it.forklaring, it.subsumsjon, LocalDateTime.now())
        }

        internal fun List<OverstyrtArbeidsgiveropplysning>.medSkjønnsmessigFastsattInntekt(meldingsreferanseId: UUID, skjæringstidspunkt: LocalDate): List<ArbeidsgiverInntektsopplysning> {
            forEach {
                check(it.refusjonsopplysninger == null) { "Skal ikke sette refusjonspplysnger på Skjønnsmessig fastsatt inntekt" }
                check(it.forklaring == null) { "Skal ikke sette forklaring på Skjønnsmessig fastsatt inntekt" }
                check(it.subsumsjon == null) { "Skal ikke sette subsumsjon på Skjønssmessig fastsatt inntekt" }
            }
            return map {
                ArbeidsgiverInntektsopplysning(
                    orgnummer = it.orgnummer,
                    gjelder = it.gjelder ?: (skjæringstidspunkt til LocalDate.MAX),
                    inntektsopplysning = SkjønnsmessigFastsatt(
                        skjæringstidspunkt,
                        meldingsreferanseId,
                        it.inntekt,
                        LocalDateTime.now()
                    ),
                    refusjonsopplysninger = Refusjonsopplysning.Refusjonsopplysninger()
                )
            }
        }

        internal fun List<OverstyrtArbeidsgiveropplysning>.refusjonstidslinjer(skjæringstidspunkt: LocalDate, meldingsreferanseId: UUID, opprettet: LocalDateTime) = this.associateBy { it.orgnummer }.mapValues { (_, opplysning) ->
            val defaultRefusjonFom = opplysning.gjelder?.start ?: skjæringstidspunkt
            val strekkbar = opplysning.refusjonsopplysninger(defaultRefusjonFom).any { (_, tom) -> tom == null }
            opplysning.refusjonsopplysninger(defaultRefusjonFom).fold(Beløpstidslinje()) { acc, (fom, tom, beløp) ->
                acc + Beløpstidslinje.fra(fom til (tom ?: fom), beløp, Kilde(meldingsreferanseId, SAKSBEHANDLER, opprettet))
            } to strekkbar
        }
    }
}
