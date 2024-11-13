package no.nav.helse.hendelser

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning
import no.nav.helse.person.inntekt.Inntektsmelding
import no.nav.helse.person.inntekt.Refusjonsopplysning
import no.nav.helse.person.inntekt.Refusjonsopplysning.Refusjonsopplysninger.Companion.refusjonsopplysninger
import no.nav.helse.økonomi.Inntekt

class GjenopplivVilkårsgrunnlag(
    meldingsreferanseId: UUID,
    private val vilkårsgrunnlagId: UUID,
    private val nyttSkjæringstidspunkt: LocalDate?,
    private val arbeidsgiveropplysninger: Map<String, Inntekt>
): Hendelse {
    override val behandlingsporing = Behandlingsporing.IngenArbeidsgiver

    override val metadata = LocalDateTime.now().let { nå ->
        HendelseMetadata(
            meldingsreferanseId = meldingsreferanseId,
            avsender = Avsender.SAKSBEHANDLER,
            innsendt = nå,
            registrert = nå,
            automatiskBehandling = false
        )
    }

    internal fun gjenoppliv(aktivitetslogg: IAktivitetslogg, vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk) {
        vilkårsgrunnlagHistorikk.gjenoppliv(this, aktivitetslogg, vilkårsgrunnlagId, nyttSkjæringstidspunkt)
    }

    internal fun arbeidsgiverinntektsopplysninger(skjæringstidspunkt: LocalDate) = arbeidsgiveropplysninger.map { (organisasjonsnummer, inntekt) ->
        val inntektsmeldingInntekt = Inntektsmelding(skjæringstidspunkt, metadata.meldingsreferanseId, inntekt)
        ArbeidsgiverInntektsopplysning(
            orgnummer = organisasjonsnummer,
            gjelder = skjæringstidspunkt til LocalDate.MAX,
            inntektsopplysning = inntektsmeldingInntekt,
            refusjonsopplysninger = Refusjonsopplysning(
                meldingsreferanseId = metadata.meldingsreferanseId,
                fom = skjæringstidspunkt,
                tom = null,
                beløp = inntekt,
                avsender = metadata.avsender,
                tidsstempel = metadata.registrert
            ).refusjonsopplysninger
        )
    }

    internal fun valider(organisasjonsnummere: List<String>) {
        if (arbeidsgiveropplysninger.isEmpty()) return
        check(organisasjonsnummere.containsAll(arbeidsgiveropplysninger.keys)) {
            "Det er forsøkt å legge til inntektsopplysnigner for arbeidsgiver som ikke finnes på personen."
        }
    }
}