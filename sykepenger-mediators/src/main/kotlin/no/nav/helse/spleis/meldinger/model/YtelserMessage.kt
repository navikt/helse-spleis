package no.nav.helse.spleis.meldinger.model

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.asOptionalLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import java.time.LocalDate
import no.nav.helse.Toggle.Companion.ArbeidsavklaringspengerV2
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
    private val arbeidsavklaringspenger: List<Pair<LocalDate, LocalDate>>
    private val ugyldigeArbeidsavklaringspengeperioder: List<Pair<LocalDate, LocalDate>>
    private val dagpenger: List<Pair<LocalDate, LocalDate>>
    private val ugyldigeDagpengeperioder: List<Pair<LocalDate, LocalDate>>

    private val foreldrepengerytelse = packet["@løsning.${Behovtype.Foreldrepenger.name}.Foreldrepengeytelse"]
        .takeIf(JsonNode::isObject)?.path("perioder")?.map(::asGradertPeriode) ?: emptyList()
    private val svangerskapsytelse = packet["@løsning.${Behovtype.Foreldrepenger.name}.Svangerskapsytelse"]
        .takeIf(JsonNode::isObject)?.path("perioder")?.map(::asGradertPeriode)
        ?: emptyList()

    private val foreldrepenger = Foreldrepenger(foreldrepengeytelse = foreldrepengerytelse)
    private val svangerskapspenger = Svangerskapspenger(svangerskapsytelse = svangerskapsytelse)

    private val pleiepenger =
        Pleiepenger(packet["@løsning.${Behovtype.Pleiepenger.name}"].map(::asGradertPeriode))

    private val omsorgspenger =
        Omsorgspenger(packet["@løsning.${Behovtype.Omsorgspenger.name}"].map(::asGradertPeriode))

    private val opplæringspenger =
        Opplæringspenger(packet["@løsning.${Behovtype.Opplæringspenger.name}"].map(::asGradertPeriode))

    private val institusjonsopphold = Institusjonsopphold(packet["@løsning.${Behovtype.Institusjonsopphold.name}"].map {
        Institusjonsoppholdsperiode(
            it.path("startdato").asLocalDate(),
            it.path("faktiskSluttdato").asOptionalLocalDate()
        )

    })

    private val inntekterForBeregning = InntekterForBeregning(packet["@løsning.${Behovtype.InntekterForBeregning.name}.inntekter"].map {
        InntekterForBeregning.Inntektsperiode.Beløp(
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

    private val arbeidsavklaringspengerV2 = if (ArbeidsavklaringspengerV2.enabled) Arbeidsavklaringspenger(
        packet["@løsning.${Behovtype.ArbeidsavklaringspengerV2.name}.utbetalingsperioder"]
            .map { Periode(it.path("fom").asLocalDate(), it.path("tom").asLocalDate()) })
    else Arbeidsavklaringspenger(emptyList())

    private val selvstendigForsikring = when (yrkesaktivitetssporing) {
        Behandlingsporing.Yrkesaktivitet.Selvstendig -> packet["@løsning.${Behovtype.SelvstendigForsikring.name}"].firstOrNull()?.let {
            SelvstendigForsikring(
                virkningsdato = it.path("startdato").asLocalDate(),
                opphørsdato = it.path("sluttdato").asOptionalLocalDate(),
                type = SelvstendigForsikring.Forsikringstype.valueOf(it.path("forsikringstype").asText())
            )
        }.also {
            if (packet["@løsning.${Behovtype.SelvstendigForsikring.name}"].size() > 1) {
                sikkerlogg.warn("Mottok mer enn én selvstendig forsikring i melding ${meldingsporing.id}")
            }
        }

        Behandlingsporing.Yrkesaktivitet.Arbeidsledig,
        is Behandlingsporing.Yrkesaktivitet.Arbeidstaker,
        Behandlingsporing.Yrkesaktivitet.Frilans -> null
    }

    init {
        packet["@løsning.${Behovtype.Arbeidsavklaringspenger.name}.meldekortperioder"].map(::asDatePair)
            .partition { it.first <= it.second }
            .also {
                arbeidsavklaringspenger = it.first
                ugyldigeArbeidsavklaringspengeperioder = it.second
            }
        packet["@løsning.${Behovtype.Dagpenger.name}.meldekortperioder"]
            .map(::asDatePair)
            .partition { it.first <= it.second }
            .also {
                dagpenger = it.first
                ugyldigeDagpengeperioder = it.second
            }
    }

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
            arbeidsavklaringspenger = if (ArbeidsavklaringspengerV2.enabled) arbeidsavklaringspengerV2
            else
                Arbeidsavklaringspenger(arbeidsavklaringspenger.map { Periode(it.first, it.second) }),
            dagpenger = Dagpenger(dagpenger.map { Periode(it.first, it.second) }),
            inntekterForBeregning = inntekterForBeregning,
            selvstendigForsikring = selvstendigForsikring
        ).also {
            if (ugyldigeArbeidsavklaringspengeperioder.isNotEmpty()) sikkerlogg.warn("Arena inneholdt en eller flere AAP-perioder med ugyldig fom/tom for")
            if (ugyldigeDagpengeperioder.isNotEmpty()) sikkerlogg.warn("Arena inneholdt en eller flere Dagpengeperioder med ugyldig fom/tom for")
        }

    override fun behandle(mediator: IHendelseMediator, context: MessageContext) {
        mediator.behandle(this, ytelser, context)
    }

    private fun asDatePair(jsonNode: JsonNode) =
        jsonNode.path("fom").asLocalDate() to jsonNode.path("tom").asLocalDate()

    private fun asGradertPeriode(jsonNode: JsonNode) =
        GradertPeriode(
            Periode(jsonNode.path("fom").asLocalDate(), jsonNode.path("tom").asLocalDate()),
            jsonNode.path("grad").asInt()
        )
}
