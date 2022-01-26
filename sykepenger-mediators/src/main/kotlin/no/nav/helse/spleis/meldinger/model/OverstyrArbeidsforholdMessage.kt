package no.nav.helse.spleis.meldinger.model

import no.nav.helse.hendelser.OverstyrArbeidsforhold
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.spleis.IHendelseMediator
import java.time.LocalDate

internal class OverstyrArbeidsforholdMessage(val packet: JsonMessage): HendelseMessage(packet) {

    override val fødselsnummer: String = packet["fødselsnummer"].asText()

    private val aktørId: String = packet["aktørId"].asText()
    private val skjæringstidspunkt: LocalDate = packet["skjæringstidspunkt"].asLocalDate()
    private val overstyrteArbeidsforhold = packet["overstyrteArbeidsforhold"]
        .map {
            OverstyrArbeidsforhold.ArbeidsforholdOverstyrt(
                orgnummer = it["orgnummer"].asText(),
                erAktivt = it["erAktivt"].asBoolean()
            )
        }

    override fun behandle(mediator: IHendelseMediator) {
        TODO("Not yet implemented")
    }

}
