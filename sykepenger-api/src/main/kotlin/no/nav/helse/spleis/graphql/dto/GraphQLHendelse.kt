package no.nav.helse.spleis.graphql.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate
import java.time.LocalDateTime

enum class GraphQLHendelsetype {
    Inntektsmelding,
    SendtSoknadNav,
    SendtSoknadArbeidsgiver,
    NySoknad,
    Ukjent
}
enum class GraphQLHendelsetypename {
    GraphQLSoknadNav,
    GraphQLInntektsmelding,
    GraphQLSoknadArbeidsgiver,
    GraphQLSykmelding,
    __Unknown;

    internal companion object {
        fun fra(type: GraphQLHendelsetype) = when (type) {
            GraphQLHendelsetype.Inntektsmelding -> GraphQLInntektsmelding
            GraphQLHendelsetype.SendtSoknadNav -> GraphQLSoknadNav
            GraphQLHendelsetype.SendtSoknadArbeidsgiver -> GraphQLSoknadArbeidsgiver
            GraphQLHendelsetype.NySoknad -> GraphQLSykmelding
            GraphQLHendelsetype.Ukjent -> __Unknown
        }.name
    }
}

data class GraphQLHendelse(
    val id: String,
    val eksternDokumentId: String,
    val type: GraphQLHendelsetype,

    // for å være bakoverkompatibel. Kan fjernes når/hvis spesialist ikke forventer denne
    @JsonProperty("__typename")
    val typename: String = GraphQLHendelsetypename.fra(type),

    // Inntektsmelding-spesifikk
    val mottattDato: LocalDateTime? = null,
    val beregnetInntekt: Double? = null,

    // Flex-søknad-spesifikk
    val fom: LocalDate? = null,
    val tom: LocalDate? = null,
    val rapportertDato: LocalDateTime? = null,
    val sendtNav: LocalDateTime? = null,
    val sendtArbeidsgiver: LocalDateTime? = null,
)