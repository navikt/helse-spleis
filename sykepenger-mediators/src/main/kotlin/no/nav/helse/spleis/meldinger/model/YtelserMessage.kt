package no.nav.helse.spleis.meldinger.model

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.asOptionalLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import no.nav.helse.hendelser.Arbeidsavklaringspenger
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.Dagpenger
import no.nav.helse.hendelser.Foreldrepenger
import no.nav.helse.hendelser.GradertPeriode
import no.nav.helse.hendelser.InntekterForBeregning
import no.nav.helse.hendelser.Institusjonsopphold
import no.nav.helse.hendelser.Institusjonsopphold.Institusjonsoppholdsperiode
import no.nav.helse.hendelser.Omsorgspenger
import no.nav.helse.hendelser.Opplæringspenger
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Pleiepenger
import no.nav.helse.hendelser.SelvstendigForsikring
import no.nav.helse.hendelser.Svangerskapspenger
import no.nav.helse.hendelser.Ytelser
import no.nav.helse.hendelser.til
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.helse.spleis.IHendelseMediator
import no.nav.helse.spleis.Meldingsporing
import no.nav.helse.spleis.meldinger.yrkesaktivitetssporing
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import org.slf4j.LoggerFactory

// Understands a JSON message representing an Ytelserbehov
internal class YtelserMessage(packet: JsonMessage, override val meldingsporing: Meldingsporing) : BehovMessage(packet) {

    private companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }

    private val vedtaksperiodeId = packet["vedtaksperiodeId"].asText()
    private val yrkesaktivitetssporing = packet.yrkesaktivitetssporing

    private val foreldrepengerytelse = packet["@løsning.${Behovtype.Foreldrepenger.name}.Foreldrepengeytelse.perioder"]
        .takeIf(JsonNode::isArray)?.map(::asGradertPeriode) ?: emptyList()
    private val svangerskapsytelse = packet["@løsning.${Behovtype.Foreldrepenger.name}.Svangerskapsytelse.perioder"]
        .takeIf(JsonNode::isArray)?.map(::asGradertPeriode) ?: emptyList()

    internal val foreldrepenger = Foreldrepenger(foreldrepengeytelse = foreldrepengerytelse)
    internal val svangerskapspenger = Svangerskapspenger(svangerskapsytelse = svangerskapsytelse)

    internal val pleiepenger =
        Pleiepenger(packet.mapFraArrayEllerObjectMedArray("@løsning.${Behovtype.Pleiepenger.name}", "perioder", ::asGradertPeriode))

    internal val omsorgspenger =
        Omsorgspenger(packet.mapFraArrayEllerObjectMedArray("@løsning.${Behovtype.Omsorgspenger.name}", "perioder", ::asGradertPeriode))

    internal val opplæringspenger =
        Opplæringspenger(packet.mapFraArrayEllerObjectMedArray("@løsning.${Behovtype.Opplæringspenger.name}", "perioder", ::asGradertPeriode))

    internal val institusjonsopphold =
        Institusjonsopphold(packet.mapFraArrayEllerObjectMedArray("@løsning.${Behovtype.Institusjonsopphold.name}", "perioder") {
            Institusjonsoppholdsperiode(
                it.path("startdato").asLocalDate(),
                it.path("faktiskSluttdato").asOptionalLocalDate()
            )
        })

    internal val inntekterForBeregning = InntekterForBeregning(packet["@løsning.${Behovtype.InntekterForBeregning.name}.inntekter"].map {
        InntekterForBeregning.Inntektsperiode(
            inntektskilde = it.path("inntektskilde").asText(),
            periode = it.path("fom").asLocalDate() til it.path("tom").asLocalDate(),
            beløp = when {
                it.path("daglig").isNumber -> it.path("daglig").asDouble().daglig
                it.path("måndelig").isNumber -> it.path("måndelig").asDouble().månedlig
                it.path("årlig").isNumber -> it.path("årlig").asDouble().årlig

                else -> {
                    error("Fant ikke noe beløp")
                }
            }
        )
    })

    internal val arbeidsavklaringspengerV2 = Arbeidsavklaringspenger(
        packet["@løsning.${Behovtype.ArbeidsavklaringspengerV2.name}.utbetalingsperioder"]
            .map { Periode(it.path("fom").asLocalDate(), it.path("tom").asLocalDate()) })

    internal val selvstendigForsikring = when (yrkesaktivitetssporing) {
        Behandlingsporing.Yrkesaktivitet.Selvstendig -> packet.mapFraArrayEllerObjectMedArray("@løsning.${Behovtype.SelvstendigForsikring.name}", "forsikringer") {
            SelvstendigForsikring(
                virkningsdato = it.path("startdato").asLocalDate(),
                opphørsdato = it.path("sluttdato").asOptionalLocalDate(),
                type = SelvstendigForsikring.Forsikringstype.valueOf(it.path("forsikringstype").asText()),
                premiegrunnlag = it.path("premiegrunnlag").asInt().årlig
            )
        }.also { forsikringer ->
            if (forsikringer.size > 1) sikkerlogg.warn("Mottok mer enn én selvstendig forsikring i melding ${meldingsporing.id}")
        }.firstOrNull()

        Behandlingsporing.Yrkesaktivitet.Arbeidsledig,
        is Behandlingsporing.Yrkesaktivitet.Arbeidstaker,
        Behandlingsporing.Yrkesaktivitet.Frilans -> null
    }

    internal val dagpengerV2 = Dagpenger(
        packet["@løsning.${Behovtype.DagpengerV2.name}.meldekortperioder"]
            .map {
                Periode(
                    it.path("fom").asLocalDate(),
                    it.path("tom").asLocalDate()
                )
            }
            .partition { it.start <= it.endInclusive }
            .also {
                if (it.second.isNotEmpty()) sikkerlogg.warn("Arena inneholdt en eller flere Dagpengeperioder med ugyldig fom/tom for")
            }.first
    )

    private val ytelser
        get() = Ytelser(
            meldingsreferanseId = meldingsporing.id,
            behandlingsporing = yrkesaktivitetssporing,
            vedtaksperiodeId = vedtaksperiodeId,
            foreldrepenger = foreldrepenger,
            svangerskapspenger = svangerskapspenger,
            pleiepenger = pleiepenger,
            omsorgspenger = omsorgspenger,
            opplæringspenger = opplæringspenger,
            institusjonsopphold = institusjonsopphold,
            arbeidsavklaringspenger = arbeidsavklaringspengerV2,
            dagpenger = dagpengerV2,
            inntekterForBeregning = inntekterForBeregning,
            selvstendigForsikring = selvstendigForsikring
        )

    override fun behandle(mediator: IHendelseMediator, context: MessageContext) {
        mediator.behandle(this, ytelser, context)
    }

    private fun asGradertPeriode(jsonNode: JsonNode) =
        GradertPeriode(
            Periode(jsonNode.path("fom").asLocalDate(), jsonNode.path("tom").asLocalDate()),
            jsonNode.path("grad").asInt()
        )
}
