package no.nav.helse.spleis.meldinger.model

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers.asOptionalLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.helse.etterlevelse.Bokstav
import no.nav.helse.etterlevelse.Ledd
import no.nav.helse.etterlevelse.Paragraf
import no.nav.helse.hendelser.Avsender.SAKSBEHANDLER
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.OverstyrArbeidsgiveropplysninger
import no.nav.helse.hendelser.OverstyrArbeidsgiveropplysninger.Overstyringbegrunnelse
import no.nav.helse.hendelser.OverstyrArbeidsgiveropplysninger.Overstyringbegrunnelse.Begrunnelse
import no.nav.helse.hendelser.til
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.Kilde
import no.nav.helse.person.inntekt.Inntektsdata
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.meldinger.meldingsreferanseId
import no.nav.helse.økonomi.Inntekt.Companion.månedlig

internal class OverstyrArbeidsgiveropplysningerMessage(packet: JsonMessage, override val meldingsporing: Meldingsporing) : HendelseMessage(packet) {

    private val skjæringstidspunkt = packet["skjæringstidspunkt"].asLocalDate()
    private val arbeidsgiveropplysninger = packet.arbeidsgiveropplysninger(skjæringstidspunkt)
    private val refusjonstidslinjer = packet.refusjonstidslinjer()

    override fun behandle(mediator: IHendelseMediator, context: MessageContext) =
        mediator.behandle(this, OverstyrArbeidsgiveropplysninger(
            meldingsreferanseId = meldingsporing.id,
            skjæringstidspunkt = skjæringstidspunkt,
            arbeidsgiveropplysninger = arbeidsgiveropplysninger,
            opprettet = opprettet,
            refusjonstidslinjer = refusjonstidslinjer,
        ), context)

    private companion object {

        private fun JsonMessage.arbeidsgiveropplysninger(skjæringstidspunkt: LocalDate): List<OverstyrArbeidsgiveropplysninger.KorrigertArbeidsgiverInntektsopplysning> {
            val arbeidsgivere = get("arbeidsgivere").takeUnless { it.isMissingOrNull() } ?: return emptyList()
            val id = meldingsreferanseId()
            val opprettet = get("@opprettet").asLocalDateTime()
            return arbeidsgivere.map { arbeidsgiveropplysning ->
                val orgnummer = arbeidsgiveropplysning["organisasjonsnummer"].asText()
                val månedligInntekt = arbeidsgiveropplysning["månedligInntekt"].asDouble().månedlig

                val forklaring = arbeidsgiveropplysning.path("forklaring").asText()
                val begrunnelse = arbeidsgiveropplysning.path("subsumsjon").asBegrunnelse()

                OverstyrArbeidsgiveropplysninger.KorrigertArbeidsgiverInntektsopplysning(
                    organisasjonsnummer = orgnummer,
                    inntektsdata = Inntektsdata(
                        hendelseId = id,
                        dato = skjæringstidspunkt,
                        beløp = månedligInntekt,
                        tidsstempel = opprettet
                    ),
                    begrunnelse = Overstyringbegrunnelse(
                        forklaring = forklaring,
                        begrunnelse = begrunnelse
                    )
                )
            }
        }

        private fun JsonNode.asBegrunnelse() = this.takeUnless(JsonNode::isMissingOrNull)?.let {
            val paragraf = it["paragraf"].asText()
            val ledd = it.path("ledd").takeUnless(JsonNode::isMissingOrNull)?.asInt()
            val bokstav = it.path("bokstav").takeUnless(JsonNode::isMissingOrNull)?.asText()
            when {
                paragraf == Paragraf.PARAGRAF_8_28.ref && ledd == Ledd.LEDD_3.nummer && bokstav == Bokstav.BOKSTAV_B.ref.toString() -> Begrunnelse.NYOPPSTARTET_ARBEIDSFORHOLD
                paragraf == Paragraf.PARAGRAF_8_28.ref && ledd == Ledd.LEDD_3.nummer && bokstav == Bokstav.BOKSTAV_C.ref.toString() -> Begrunnelse.VARIG_LØNNSENDRING
                paragraf == Paragraf.PARAGRAF_8_28.ref && ledd == Ledd.LEDD_5.nummer -> Begrunnelse.MANGELFULL_ELLER_URIKTIG_INNRAPPORTERING
                else -> null
            }
        }

        private fun JsonMessage.refusjonstidslinjer(): Map<String, Pair<Beløpstidslinje, Boolean>> {
            val id = meldingsreferanseId()
            val opprettet = get("@opprettet").asLocalDateTime()
            return get("arbeidsgivere").associateBy { it.path("organisasjonsnummer").asText() }.mapValues { (_, arbeidsgiver) ->
                arbeidsgiver.path("refusjonsopplysninger").refusjonstidslinje(id, opprettet)
            }
        }

        private fun JsonNode.refusjonstidslinje(meldingsreferanseId: MeldingsreferanseId, opprettet: LocalDateTime): Pair<Beløpstidslinje, Boolean> {
            var strekkbar = false
            val refusjonstidslinje = this.fold(Beløpstidslinje()) { acc, node ->
                val fom = node.path("fom").asLocalDate()
                val tom = node.path("tom").asOptionalLocalDate()
                if (tom == null) strekkbar = true
                val beløp = node.path("beløp").asDouble().månedlig
                val refusjonstidslinje = Beløpstidslinje.fra(fom til (tom ?: fom), beløp, Kilde(meldingsreferanseId, SAKSBEHANDLER, opprettet))
                refusjonstidslinje + acc
            }
            return refusjonstidslinje to strekkbar
        }
    }
}


