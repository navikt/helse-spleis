package no.nav.helse.hendelser

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import no.nav.helse.etterlevelse.Bokstav
import no.nav.helse.etterlevelse.Ledd
import no.nav.helse.etterlevelse.Paragraf
import no.nav.helse.etterlevelse.Subsumsjonslogg
import no.nav.helse.etterlevelse.`§ 8-28 ledd 3 bokstav b`
import no.nav.helse.etterlevelse.`§ 8-28 ledd 3 bokstav c`
import no.nav.helse.etterlevelse.`§ 8-28 ledd 5`
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.Kilde
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning.Companion.overstyrTilkommendeInntekter
import no.nav.helse.person.inntekt.Inntektsdata
import no.nav.helse.person.inntekt.Inntektsgrunnlag
import no.nav.helse.person.inntekt.NyInntektUnderveis
import no.nav.helse.person.inntekt.Saksbehandler
import no.nav.helse.person.refusjon.Refusjonsservitør

class OverstyrArbeidsgiveropplysninger(
    meldingsreferanseId: UUID,
    internal val skjæringstidspunkt: LocalDate,
    private val arbeidsgiveropplysninger: List<ArbeidsgiverInntektsopplysning>,
    opprettet: LocalDateTime,
    private val refusjonstidslinjer: Map<String, Pair<Beløpstidslinje, Boolean>>,
    private val begrunnelser: List<Overstyringbegrunnelse>
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

    internal fun subsummer(subsumsjonslogg: Subsumsjonslogg, startdatoArbeidsforhold: LocalDate?, organisasjonsnummer: String) {
        val begrunnelseForArbeidsgiver = begrunnelser.singleOrNull { it.organisasjonsnummer == organisasjonsnummer } ?: return
        val inntektForArbeidsgiver = arbeidsgiveropplysninger.single { it.orgnummer == organisasjonsnummer }.inntektsopplysning.inntektsdata
        begrunnelseForArbeidsgiver.subsummer(inntektForArbeidsgiver, subsumsjonslogg, startdatoArbeidsforhold)
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

    data class Overstyringbegrunnelse(
        val organisasjonsnummer: String,
        val forklaring: String,
        val subsumsjon: Subsumsjon?,
    ) {
        enum class Begrunnelse {
            NYOPPSTARTET_ARBEIDSFORHOLD,
            VARIG_LØNNSENDRING,
            MANGELFULL_ELLER_URIKTIG_INNRAPPORTERING
        }

        val begrunnelse = subsumsjon?.let {
            when {
                subsumsjon.paragraf == Paragraf.PARAGRAF_8_28.ref && subsumsjon.ledd == Ledd.LEDD_3.nummer && subsumsjon.bokstav == Bokstav.BOKSTAV_B.ref.toString() -> Begrunnelse.NYOPPSTARTET_ARBEIDSFORHOLD
                subsumsjon.paragraf == Paragraf.PARAGRAF_8_28.ref && subsumsjon.ledd == Ledd.LEDD_3.nummer && subsumsjon.bokstav == Bokstav.BOKSTAV_C.ref.toString() -> Begrunnelse.VARIG_LØNNSENDRING
                subsumsjon.paragraf == Paragraf.PARAGRAF_8_28.ref && subsumsjon.ledd == Ledd.LEDD_5.nummer -> Begrunnelse.MANGELFULL_ELLER_URIKTIG_INNRAPPORTERING
                else -> null
            }
        }

        fun subsummer(inntektsdata: Inntektsdata, subsumsjonslogg: Subsumsjonslogg, startdatoArbeidsforhold: LocalDate?) {
            val overstyrtInntektMap = mapOf("dato" to inntektsdata.dato, "beløp" to inntektsdata.beløp.månedlig)
            when (begrunnelse) {
                Begrunnelse.NYOPPSTARTET_ARBEIDSFORHOLD -> {
                    requireNotNull(startdatoArbeidsforhold) { "Fant ikke aktivt arbeidsforhold for skjæringstidspunktet i arbeidsforholdshistorikken" }
                    subsumsjonslogg.logg(
                        `§ 8-28 ledd 3 bokstav b`(
                            organisasjonsnummer = organisasjonsnummer,
                            startdatoArbeidsforhold = startdatoArbeidsforhold,
                            overstyrtInntektFraSaksbehandler = overstyrtInntektMap,
                            skjæringstidspunkt = inntektsdata.dato,
                            forklaring = forklaring,
                            grunnlagForSykepengegrunnlagÅrlig = inntektsdata.beløp.årlig,
                            grunnlagForSykepengegrunnlagMånedlig = inntektsdata.beløp.månedlig
                        )
                    )
                }
                Begrunnelse.VARIG_LØNNSENDRING -> {
                    subsumsjonslogg.logg(
                        `§ 8-28 ledd 3 bokstav c`(
                            organisasjonsnummer = organisasjonsnummer,
                            overstyrtInntektFraSaksbehandler = overstyrtInntektMap,
                            skjæringstidspunkt = inntektsdata.dato,
                            forklaring = forklaring,
                            grunnlagForSykepengegrunnlagÅrlig = inntektsdata.beløp.årlig,
                            grunnlagForSykepengegrunnlagMånedlig = inntektsdata.beløp.månedlig
                        )
                    )
                }
                Begrunnelse.MANGELFULL_ELLER_URIKTIG_INNRAPPORTERING -> {
                    subsumsjonslogg.logg(
                        `§ 8-28 ledd 5`(
                            organisasjonsnummer = organisasjonsnummer,
                            overstyrtInntektFraSaksbehandler = overstyrtInntektMap,
                            skjæringstidspunkt = inntektsdata.dato,
                            forklaring = forklaring,
                            grunnlagForSykepengegrunnlagÅrlig = inntektsdata.beløp.årlig,
                            grunnlagForSykepengegrunnlagMånedlig = inntektsdata.beløp.månedlig
                        )
                    )
                }
                null -> {}
            }
        }
    }
}
