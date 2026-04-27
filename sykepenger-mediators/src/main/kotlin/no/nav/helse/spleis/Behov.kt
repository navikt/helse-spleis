package no.nav.helse.spleis

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import no.nav.helse.hendelser.MeldingsreferanseId

data class Behov(
    val type: Behovstype,
    val input: Map<String, Any>
) {
    sealed interface Behovstype {
        val utgåendeNavn: String

        data object Sykepengehistorikk: Behovstype { override val utgåendeNavn = "Sykepengehistorikk" }
        // Denne (SykepengehistorikkForFeriepenger) er litt special. Behovet sendes ut fra
        // feriepengejobben og vi håndterer bare løsningen i Spleis
        data object SykepengehistorikkForFeriepenger: Behovstype { override val utgåendeNavn = "SykepengehistorikkForFeriepenger" }

        data object Foreldrepenger: Behovstype { override val utgåendeNavn = "Foreldrepenger" }
        data object Pleiepenger: Behovstype { override val utgåendeNavn = "Pleiepenger" }
        data object Omsorgspenger: Behovstype { override val utgåendeNavn = "Omsorgspenger" }
        data object Opplæringspenger: Behovstype { override val utgåendeNavn = "Opplæringspenger" }
        data object Institusjonsopphold: Behovstype { override val utgåendeNavn = "Institusjonsopphold" }

        data object Godkjenning: Behovstype { override val utgåendeNavn = "Godkjenning" }
        data object Simulering: Behovstype { override val utgåendeNavn = "Simulering" }
        data object Utbetaling: Behovstype { override val utgåendeNavn = "Utbetaling" }
        data object Feriepengeutbetaling: Behovstype { override val utgåendeNavn = "Feriepengeutbetaling" }

        data object InntekterForSykepengegrunnlag: Behovstype { override val utgåendeNavn = "InntekterForSykepengegrunnlag" }
        data object InntekterForOpptjeningsvurdering: Behovstype { override val utgåendeNavn = "InntekterForOpptjeningsvurdering" }
        data object InntekterForBeregning: Behovstype { override val utgåendeNavn = "InntekterForBeregning" }

        data object Dagpenger: Behovstype { override val utgåendeNavn = "DagpengerV2" }
        data object Arbeidsavklaringspenger: Behovstype { override val utgåendeNavn = "ArbeidsavklaringspengerV2" }
        data object Medlemskap: Behovstype { override val utgåendeNavn = "Medlemskap" }
        data object Arbeidsforhold: Behovstype { override val utgåendeNavn = "ArbeidsforholdV2" }

        data object SelvstendigForsikring: Behovstype { override val utgåendeNavn = "SelvstendigForsikring" }
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

