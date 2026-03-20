package no.nav.helse.spleis

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import no.nav.helse.hendelser.MeldingsreferanseId

data class Behov(
    val type: Behovstype,
    val input: Map<String, Any>
) {
    enum class Behovstype(val utgåendeNavn: String) {
        Sykepengehistorikk("Sykepengehistorikk"),
        // Denne (SykepengehistorikkForFeriepenger) er litt special. Behovet sendes ut fra
        // feriepengejobben og vi håndterer bare løsningen i Spleis
        SykepengehistorikkForFeriepenger("SykepengehistorikkForFeriepenger"),

        Foreldrepenger("Foreldrepenger"),
        Pleiepenger("Pleiepenger"),
        Omsorgspenger("Omsorgspenger"),
        Opplæringspenger("Opplæringspenger"),
        Institusjonsopphold("Institusjonsopphold"),

        Godkjenning("Godkjenning"),
        Simulering("Simulering"),
        Utbetaling("Utbetaling"),
        Feriepengeutbetaling("Feriepengeutbetaling"),

        InntekterForSykepengegrunnlag("InntekterForSykepengegrunnlag"),
        InntekterForOpptjeningsvurdering("InntekterForOpptjeningsvurdering"),
        InntekterForBeregning("InntekterForBeregning"),

        Dagpenger("DagpengerV2"),
        Arbeidsavklaringspenger("ArbeidsavklaringspengerV2"),
        Medlemskap("Medlemskap"),
        Arbeidsforhold("ArbeidsforholdV2"),

        SelvstendigForsikring("SelvstendigForsikring")
    }

    companion object {
        fun List<Behov>.somJsonMessage(
            meldingsreferanseId: MeldingsreferanseId, // TODO: Dette feltet er sendt på alle behov, men tror ingen sparkel-apper bruker det, må sjekkes opp i!
            extra: Map<String, Any> = emptyMap()) = JsonMessage.newNeed(
                behov = this.map { it.type.utgåendeNavn },
                map = this.associate { it.type.utgåendeNavn to it.input }.plus(extra).plus("meldingsreferanseId" to meldingsreferanseId.id)
            )
    }
}

