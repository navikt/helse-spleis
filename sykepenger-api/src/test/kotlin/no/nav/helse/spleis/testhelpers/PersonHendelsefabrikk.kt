package no.nav.helse.spleis.testhelpers

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import java.util.UUID
import no.nav.helse.hendelser.Avsender.SAKSBEHANDLER
import no.nav.helse.hendelser.Dødsmelding
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.MinimumSykdomsgradsvurderingMelding
import no.nav.helse.hendelser.OverstyrArbeidsforhold
import no.nav.helse.hendelser.OverstyrArbeidsgiveropplysninger
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.PersonPåminnelse
import no.nav.helse.hendelser.SkjønnsmessigFastsettelse
import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger
import no.nav.helse.hendelser.til
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.Kilde
import no.nav.helse.person.inntekt.Inntektsdata
import no.nav.helse.spleis.testhelpers.OverstyrtArbeidsgiveropplysning.Companion.medSaksbehandlerinntekt
import no.nav.helse.spleis.testhelpers.OverstyrtArbeidsgiveropplysning.Companion.medSkjønnsmessigFastsattInntekt
import no.nav.helse.spleis.testhelpers.OverstyrtArbeidsgiveropplysning.Companion.refusjonstidslinjer
import no.nav.helse.økonomi.Inntekt

internal class PersonHendelsefabrikk {
    internal fun lagMinimumSykdomsgradsvurderingMelding(perioderMedMinimumSykdomsgradVurdertOK: Set<Periode> = emptySet(), perioderMedMinimumSykdomsgradVurdertIkkeOK: Set<Periode> = emptySet()) =
        MinimumSykdomsgradsvurderingMelding(
            perioderMedMinimumSykdomsgradVurdertOK = perioderMedMinimumSykdomsgradVurdertOK,
            perioderMedMinimumSykdomsgradVurdertIkkeOK = perioderMedMinimumSykdomsgradVurdertIkkeOK,
            meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID())
        )

    internal fun lagDødsmelding(dødsdato: LocalDate) =
        Dødsmelding(
            meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
            dødsdato = dødsdato
        )

    internal fun lagOverstyrArbeidsforhold(skjæringstidspunkt: LocalDate, vararg overstyrteArbeidsforhold: OverstyrArbeidsforhold.ArbeidsforholdOverstyrt) =
        OverstyrArbeidsforhold(
            meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
            skjæringstidspunkt = skjæringstidspunkt,
            overstyrteArbeidsforhold = overstyrteArbeidsforhold.toList(),
            opprettet = LocalDateTime.now()
        )

    internal fun lagPåminnelse() =
        PersonPåminnelse(
            meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID())
        )

    internal fun lagSkjønnsmessigFastsettelse(skjæringstidspunkt: LocalDate, arbeidsgiveropplysninger: List<OverstyrtArbeidsgiveropplysning>, meldingsreferanseId: UUID) =
        SkjønnsmessigFastsettelse(
            meldingsreferanseId = MeldingsreferanseId(meldingsreferanseId),
            skjæringstidspunkt = skjæringstidspunkt,
            arbeidsgiveropplysninger = arbeidsgiveropplysninger.medSkjønnsmessigFastsattInntekt(meldingsreferanseId, skjæringstidspunkt),
            opprettet = LocalDateTime.now()
        )

    internal fun lagOverstyrArbeidsgiveropplysninger(skjæringstidspunkt: LocalDate, arbeidsgiveropplysninger: List<OverstyrtArbeidsgiveropplysning>, meldingsreferanseId: UUID): OverstyrArbeidsgiveropplysninger {
        val opprettet = LocalDateTime.now()
        return OverstyrArbeidsgiveropplysninger(
            meldingsreferanseId = MeldingsreferanseId(meldingsreferanseId),
            skjæringstidspunkt = skjæringstidspunkt,
            arbeidsgiveropplysninger = arbeidsgiveropplysninger.medSaksbehandlerinntekt(meldingsreferanseId, skjæringstidspunkt),
            refusjonstidslinjer = arbeidsgiveropplysninger.refusjonstidslinjer(skjæringstidspunkt, meldingsreferanseId, opprettet),
            opprettet = opprettet
        )
    }

    internal fun lagUtbetalingshistorikkForFeriepenger(opptjeningsår: Year) =
        UtbetalingshistorikkForFeriepenger(
            meldingsreferanseId = MeldingsreferanseId(UUID.randomUUID()),
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
    private val refusjonsopplysninger: List<Triple<LocalDate, LocalDate?, Inntekt>>? = null,
    private val gjelder: Periode? = null
) {
    private fun refusjonsopplysninger(skjæringstidspunkt: LocalDate) = refusjonsopplysninger ?: listOf(Triple(skjæringstidspunkt, null, inntekt))

    internal companion object {
        private fun List<OverstyrtArbeidsgiveropplysning>.tilArbeidsgiverInntektsopplysning(meldingsreferanseId: UUID, skjæringstidspunkt: LocalDate) =
            map {
                OverstyrArbeidsgiveropplysninger.KorrigertArbeidsgiverInntektsopplysning(
                    organisasjonsnummer = it.orgnummer,
                    gjelder = it.gjelder ?: (skjæringstidspunkt til LocalDate.MAX),
                    inntektsdata = Inntektsdata(
                        hendelseId = MeldingsreferanseId(meldingsreferanseId),
                        dato = skjæringstidspunkt,
                        beløp = it.inntekt,
                        tidsstempel = LocalDateTime.now()
                    ),
                    begrunnelse = OverstyrArbeidsgiveropplysninger.Overstyringbegrunnelse(
                        forklaring = "forklaring",
                        begrunnelse = OverstyrArbeidsgiveropplysninger.Overstyringbegrunnelse.Begrunnelse.NYOPPSTARTET_ARBEIDSFORHOLD
                    )
                )
            }

        internal fun List<OverstyrtArbeidsgiveropplysning>.medSaksbehandlerinntekt(meldingsreferanseId: UUID, skjæringstidspunkt: LocalDate) = tilArbeidsgiverInntektsopplysning(meldingsreferanseId, skjæringstidspunkt)

        internal fun List<OverstyrtArbeidsgiveropplysning>.medSkjønnsmessigFastsattInntekt(meldingsreferanseId: UUID, skjæringstidspunkt: LocalDate): List<SkjønnsmessigFastsettelse.SkjønnsfastsattInntekt> {
            return map {
                SkjønnsmessigFastsettelse.SkjønnsfastsattInntekt(
                    orgnummer = it.orgnummer,
                    inntektsdata = Inntektsdata(
                        hendelseId = MeldingsreferanseId(meldingsreferanseId),
                        dato = skjæringstidspunkt,
                        beløp = it.inntekt,
                        tidsstempel = LocalDateTime.now()
                    )
                )
            }
        }

        internal fun List<OverstyrtArbeidsgiveropplysning>.refusjonstidslinjer(skjæringstidspunkt: LocalDate, meldingsreferanseId: UUID, opprettet: LocalDateTime) = this.associateBy { it.orgnummer }.mapValues { (_, opplysning) ->
            val defaultRefusjonFom = opplysning.gjelder?.start ?: skjæringstidspunkt
            val strekkbar = opplysning.refusjonsopplysninger(defaultRefusjonFom).any { (_, tom) -> tom == null }
            opplysning.refusjonsopplysninger(defaultRefusjonFom).fold(Beløpstidslinje()) { acc, (fom, tom, beløp) ->
                acc + Beløpstidslinje.fra(fom til (tom ?: fom), beløp, Kilde(MeldingsreferanseId(meldingsreferanseId), SAKSBEHANDLER, opprettet))
            } to strekkbar
        }
    }
}
