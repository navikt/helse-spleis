package no.nav.helse.spleis.graphql.dto

import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.LocalDate
import java.time.LocalDateTime

enum class GraphQLHendelsetype {
    Inntektsmelding,
    SendtSoknadNav,
    SendtSoknadArbeidsgiver,
    NySoknad,
    Ukjent
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "__typename")
interface GraphQLHendelse {
    val id: String
    val type: GraphQLHendelsetype
}

data class GraphQLInntektsmelding(
    override val id: String,
    val mottattDato: LocalDateTime,
    val beregnetInntekt: Double
) : GraphQLHendelse {
    override val type = GraphQLHendelsetype.Inntektsmelding
}

data class GraphQLSoknadNav(
    override val id: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertDato: LocalDateTime,
    val sendtNav: LocalDateTime
) : GraphQLHendelse {
    override val type = GraphQLHendelsetype.SendtSoknadNav
}

data class GraphQLSoknadArbeidsgiver(
    override val id: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertDato: LocalDateTime,
    val sendtArbeidsgiver: LocalDateTime
) : GraphQLHendelse {
    override val type = GraphQLHendelsetype.SendtSoknadArbeidsgiver
}

data class GraphQLSykmelding(
    override val id: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertDato: LocalDateTime
) : GraphQLHendelse {
    override val type = GraphQLHendelsetype.NySoknad
}
