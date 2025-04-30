package no.nav.helse.hendelser

import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.helse.etterlevelse.Subsumsjonslogg
import no.nav.helse.etterlevelse.`§ 8-28 ledd 3 bokstav b`
import no.nav.helse.etterlevelse.`§ 8-28 ledd 3 bokstav c`
import no.nav.helse.etterlevelse.`§ 8-28 ledd 5`
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.inntekt.Inntektsdata
import no.nav.helse.person.refusjon.Refusjonsservitør
import no.nav.helse.økonomi.Inntekt

class OverstyrArbeidsgiveropplysninger(
    meldingsreferanseId: MeldingsreferanseId,
    internal val skjæringstidspunkt: LocalDate,
    val arbeidsgiveropplysninger: List<KorrigertArbeidsgiverInntektsopplysning>,
    opprettet: LocalDateTime,
    private val refusjonstidslinjer: Map<String, Pair<Beløpstidslinje, Boolean>>
) : Hendelse, OverstyrInntektsgrunnlag {
    override val behandlingsporing = Behandlingsporing.IngenYrkesaktivitet
    override val metadata = HendelseMetadata(
        meldingsreferanseId = meldingsreferanseId,
        avsender = Avsender.SAKSBEHANDLER,
        innsendt = opprettet,
        registrert = LocalDateTime.now(),
        automatiskBehandling = false
    )

    override fun erRelevant(skjæringstidspunkt: LocalDate) = this.skjæringstidspunkt == skjæringstidspunkt

    internal fun subsummer(subsumsjonslogg: Subsumsjonslogg, startdatoArbeidsforhold: LocalDate?, organisasjonsnummer: String) {
        arbeidsgiveropplysninger.singleOrNull { it.organisasjonsnummer == organisasjonsnummer }
            ?.subsummer(subsumsjonslogg, startdatoArbeidsforhold)
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

    data class KorrigertArbeidsgiverInntektsopplysning(
        val organisasjonsnummer: String,
        val inntektsdata: Inntektsdata,
        val begrunnelse: Overstyringbegrunnelse
    ) {
        fun subsummer(subsumsjonslogg: Subsumsjonslogg, startdatoArbeidsforhold: LocalDate?) {
            begrunnelse.subsummer(inntektsdata.dato, inntektsdata.beløp, organisasjonsnummer, subsumsjonslogg, startdatoArbeidsforhold)
        }
    }

    data class Overstyringbegrunnelse(
        val forklaring: String,
        val begrunnelse: Begrunnelse?
    ) {
        enum class Begrunnelse {
            NYOPPSTARTET_ARBEIDSFORHOLD,
            VARIG_LØNNSENDRING,
            MANGELFULL_ELLER_URIKTIG_INNRAPPORTERING
        }

        fun subsummer(skjæringstidspunkt: LocalDate, beløp: Inntekt, organisasjonsnummer: String, subsumsjonslogg: Subsumsjonslogg, startdatoArbeidsforhold: LocalDate?) {
            val overstyrtInntektMap = mapOf("dato" to skjæringstidspunkt, "beløp" to beløp.månedlig)
            when (begrunnelse) {
                Begrunnelse.NYOPPSTARTET_ARBEIDSFORHOLD -> {
                    requireNotNull(startdatoArbeidsforhold) { "Fant ikke aktivt arbeidsforhold for skjæringstidspunktet i arbeidsforholdshistorikken" }
                    subsumsjonslogg.logg(
                        `§ 8-28 ledd 3 bokstav b`(
                            organisasjonsnummer = organisasjonsnummer,
                            startdatoArbeidsforhold = startdatoArbeidsforhold,
                            overstyrtInntektFraSaksbehandler = overstyrtInntektMap,
                            skjæringstidspunkt = skjæringstidspunkt,
                            forklaring = forklaring,
                            grunnlagForSykepengegrunnlagÅrlig = beløp.årlig,
                            grunnlagForSykepengegrunnlagMånedlig = beløp.månedlig
                        )
                    )
                }
                Begrunnelse.VARIG_LØNNSENDRING -> {
                    subsumsjonslogg.logg(
                        `§ 8-28 ledd 3 bokstav c`(
                            organisasjonsnummer = organisasjonsnummer,
                            overstyrtInntektFraSaksbehandler = overstyrtInntektMap,
                            skjæringstidspunkt = skjæringstidspunkt,
                            forklaring = forklaring,
                            grunnlagForSykepengegrunnlagÅrlig = beløp.årlig,
                            grunnlagForSykepengegrunnlagMånedlig = beløp.månedlig
                        )
                    )
                }
                Begrunnelse.MANGELFULL_ELLER_URIKTIG_INNRAPPORTERING -> {
                    subsumsjonslogg.logg(
                        `§ 8-28 ledd 5`(
                            organisasjonsnummer = organisasjonsnummer,
                            overstyrtInntektFraSaksbehandler = overstyrtInntektMap,
                            skjæringstidspunkt = skjæringstidspunkt,
                            forklaring = forklaring,
                            grunnlagForSykepengegrunnlagÅrlig = beløp.årlig,
                            grunnlagForSykepengegrunnlagMånedlig = beløp.månedlig
                        )
                    )
                }
                null -> {}
            }
        }
    }
}
