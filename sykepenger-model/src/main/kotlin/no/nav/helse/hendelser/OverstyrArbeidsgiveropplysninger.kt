package no.nav.helse.hendelser

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import no.nav.helse.etterlevelse.Subsumsjonslogg
import no.nav.helse.etterlevelse.`§ 8-28 ledd 3 bokstav b`
import no.nav.helse.etterlevelse.`§ 8-28 ledd 3 bokstav c`
import no.nav.helse.etterlevelse.`§ 8-28 ledd 5`
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.Kilde
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning.Companion.overstyrTilkommendeInntekter
import no.nav.helse.person.inntekt.Inntektsgrunnlag
import no.nav.helse.person.inntekt.NyInntektUnderveis
import no.nav.helse.person.inntekt.Saksbehandler
import no.nav.helse.person.inntekt.Saksbehandler.Begrunnelse
import no.nav.helse.person.refusjon.Refusjonsservitør

class OverstyrArbeidsgiveropplysninger(
    meldingsreferanseId: UUID,
    internal val skjæringstidspunkt: LocalDate,
    private val arbeidsgiveropplysninger: List<ArbeidsgiverInntektsopplysning>,
    opprettet: LocalDateTime,
    private val refusjonstidslinjer: Map<String, Pair<Beløpstidslinje, Boolean>>
) : Hendelse, OverstyrInntektsgrunnlag {
    override val behandlingsporing = Behandlingsporing.IngenArbeidsgiver
    override val metadata = HendelseMetadata(
        meldingsreferanseId = meldingsreferanseId,
        avsender = Avsender.SAKSBEHANDLER,
        innsendt = opprettet,
        registrert = LocalDateTime.now(),
        automatiskBehandling = false
    )

    init {
        check(arbeidsgiveropplysninger.all { it.inntektsopplysning is Saksbehandler }) {
            "alle inntektene må være saksbehandler"
        }
    }

    override fun erRelevant(skjæringstidspunkt: LocalDate) = this.skjæringstidspunkt == skjæringstidspunkt

    internal fun overstyr(builder: Inntektsgrunnlag.ArbeidsgiverInntektsopplysningerOverstyringer) {
        arbeidsgiveropplysninger.forEach { builder.leggTilInntekt(it) }
    }

    internal fun overstyr(nyInntektUnderveis: List<NyInntektUnderveis>): List<NyInntektUnderveis> {
        val kilde = Kilde(metadata.meldingsreferanseId, Avsender.SAKSBEHANDLER, metadata.registrert)
        return arbeidsgiveropplysninger.overstyrTilkommendeInntekter(nyInntektUnderveis, skjæringstidspunkt, kilde)
    }

    internal fun subsummer(subsumsjonslogg: Subsumsjonslogg, startdatoArbeidsforhold: LocalDate?, organisasjonsnummer: String, saksbehandler: Saksbehandler) {
        saksbehandler.subsummer(subsumsjonslogg, startdatoArbeidsforhold, organisasjonsnummer)
    }

    private fun Saksbehandler.subsummer(subsumsjonslogg: Subsumsjonslogg, startdatoArbeidsforhold: LocalDate?, organisasjonsnummer: String) {
        if (subsumsjon == null) return
        requireNotNull(forklaring) { "Det skal være en forklaring fra saksbehandler ved overstyring av inntekt" }
        when (begrunnelse) {
            Begrunnelse.NYOPPSTARTET_ARBEIDSFORHOLD -> {
                requireNotNull(startdatoArbeidsforhold) { "Fant ikke aktivt arbeidsforhold for skjæringstidspunktet i arbeidsforholdshistorikken" }
                subsumsjonslogg.logg(
                    `§ 8-28 ledd 3 bokstav b`(
                        organisasjonsnummer = organisasjonsnummer,
                        startdatoArbeidsforhold = startdatoArbeidsforhold,
                        overstyrtInntektFraSaksbehandler = mapOf("dato" to inntektsdata.dato, "beløp" to inntektsdata.beløp.månedlig),
                        skjæringstidspunkt = inntektsdata.dato,
                        forklaring = forklaring,
                        grunnlagForSykepengegrunnlagÅrlig = fastsattÅrsinntekt().årlig,
                        grunnlagForSykepengegrunnlagMånedlig = fastsattÅrsinntekt().månedlig
                    )
                )
            }
            Begrunnelse.VARIG_LØNNSENDRING -> {
                subsumsjonslogg.logg(
                    `§ 8-28 ledd 3 bokstav c`(
                        organisasjonsnummer = organisasjonsnummer,
                        overstyrtInntektFraSaksbehandler = mapOf("dato" to inntektsdata.dato, "beløp" to inntektsdata.beløp.månedlig),
                        skjæringstidspunkt = inntektsdata.dato,
                        forklaring = forklaring,
                        grunnlagForSykepengegrunnlagÅrlig = fastsattÅrsinntekt().årlig,
                        grunnlagForSykepengegrunnlagMånedlig = fastsattÅrsinntekt().månedlig
                    )
                )
            }
            Begrunnelse.MANGELFULL_ELLER_URIKTIG_INNRAPPORTERING -> {
                subsumsjonslogg.logg(
                    `§ 8-28 ledd 5`(
                        organisasjonsnummer = organisasjonsnummer,
                        overstyrtInntektFraSaksbehandler = mapOf("dato" to inntektsdata.dato, "beløp" to inntektsdata.beløp.månedlig),
                        skjæringstidspunkt = inntektsdata.dato,
                        forklaring = forklaring,
                        grunnlagForSykepengegrunnlagÅrlig = fastsattÅrsinntekt().årlig,
                        grunnlagForSykepengegrunnlagMånedlig = fastsattÅrsinntekt().månedlig
                    )
                )
            }
            null -> {}
        }
    }

    internal fun refusjonsservitør(
        startdatoer: Collection<LocalDate>,
        orgnummer: String,
        eksisterendeRefusjonstidslinje: Beløpstidslinje
    ): Refusjonsservitør? {
        val (refusjonstidslinjeFraOverstyring, strekkbar) = refusjonstidslinjer[orgnummer] ?: return null
        if (refusjonstidslinjeFraOverstyring.isEmpty()) return null
        if (eksisterendeRefusjonstidslinje.isEmpty()) return Refusjonsservitør.fra(startdatoer = startdatoer, refusjonstidslinje = refusjonstidslinjeFraOverstyring)

        val refusjonstidslinje =
            if (strekkbar) refusjonstidslinjeFraOverstyring.fyll(maxOf(eksisterendeRefusjonstidslinje.last().dato, refusjonstidslinjeFraOverstyring.last().dato))
            else refusjonstidslinjeFraOverstyring

        val nyInformasjon = refusjonstidslinje - eksisterendeRefusjonstidslinje
        if (nyInformasjon.isEmpty()) return null

        return Refusjonsservitør.fra(startdatoer = startdatoer, refusjonstidslinje = eksisterendeRefusjonstidslinje + nyInformasjon)
    }

}
