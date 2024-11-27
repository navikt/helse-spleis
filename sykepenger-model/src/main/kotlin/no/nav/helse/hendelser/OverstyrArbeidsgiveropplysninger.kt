package no.nav.helse.hendelser

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.nesteDag
import no.nav.helse.person.Person
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.PersonObserver.Inntektsopplysningstype.SAKSBEHANDLER
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.Kilde
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning.Companion.overstyrTilkommendeInntekter
import no.nav.helse.person.inntekt.Inntektsgrunnlag
import no.nav.helse.person.inntekt.NyInntektUnderveis
import no.nav.helse.person.refusjon.Refusjonsservitør

class OverstyrArbeidsgiveropplysninger(
    meldingsreferanseId: UUID,
    internal val skjæringstidspunkt: LocalDate,
    private val arbeidsgiveropplysninger: List<ArbeidsgiverInntektsopplysning>,
    opprettet: LocalDateTime,
    private val refusjonstidslinjer: Map<String, Pair<Beløpstidslinje, Boolean>>,
) : Hendelse, OverstyrInntektsgrunnlag {
    override val behandlingsporing = Behandlingsporing.IngenArbeidsgiver
    override val metadata =
        HendelseMetadata(
            meldingsreferanseId = meldingsreferanseId,
            avsender = Avsender.SAKSBEHANDLER,
            innsendt = opprettet,
            registrert = LocalDateTime.now(),
            automatiskBehandling = false,
        )

    override fun erRelevant(skjæringstidspunkt: LocalDate) =
        this.skjæringstidspunkt == skjæringstidspunkt

    internal fun overstyr(builder: Inntektsgrunnlag.ArbeidsgiverInntektsopplysningerOverstyringer) {
        arbeidsgiveropplysninger.forEach { builder.leggTilInntekt(it) }
    }

    internal fun overstyr(nyInntektUnderveis: List<NyInntektUnderveis>): List<NyInntektUnderveis> {
        val kilde = Kilde(metadata.meldingsreferanseId, Avsender.SAKSBEHANDLER, metadata.registrert)
        return arbeidsgiveropplysninger.overstyrTilkommendeInntekter(
            nyInntektUnderveis,
            skjæringstidspunkt,
            kilde,
        )
    }

    internal fun arbeidsgiveropplysningerKorrigert(
        person: Person,
        orgnummer: String,
        hendelseId: UUID,
    ) {
        if (arbeidsgiveropplysninger.any { it.gjelder(orgnummer) }) {
            person.arbeidsgiveropplysningerKorrigert(
                PersonObserver.ArbeidsgiveropplysningerKorrigertEvent(
                    korrigertInntektsmeldingId = hendelseId,
                    korrigerendeInntektektsopplysningstype = SAKSBEHANDLER,
                    korrigerendeInntektsopplysningId = metadata.meldingsreferanseId,
                )
            )
        }
    }

    internal fun refusjonsservitør(
        startdatoer: Collection<LocalDate>,
        orgnummer: String,
        eksisterendeRefusjonstidslinje: Beløpstidslinje,
    ): Refusjonsservitør? {
        val (refusjonstidslinjeFraOverstyring, strekkbar) =
            refusjonstidslinjer[orgnummer] ?: return null
        if (refusjonstidslinjeFraOverstyring.isEmpty()) return null
        val refusjonstidslinje =
            if (strekkbar) refusjonstidslinjeFraOverstyring
            else
                refusjonstidslinjeFraOverstyring +
                    eksisterendeRefusjonstidslinje.fraOgMed(
                        refusjonstidslinjeFraOverstyring.last().dato.nesteDag
                    )
        return Refusjonsservitør.fra(
            startdatoer = startdatoer,
            refusjonstidslinje = refusjonstidslinje,
        )
    }
}
