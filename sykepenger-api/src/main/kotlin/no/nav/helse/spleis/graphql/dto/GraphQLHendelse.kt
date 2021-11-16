package no.nav.helse.spleis.graphql.dto

import com.apurebase.kgraphql.schema.dsl.SchemaBuilder
import java.time.LocalDate
import java.time.LocalDateTime

internal fun SchemaBuilder.hendelseTypes() {
    enum<GraphQLHendelsetype>()

    type<GraphQLHendelse>()
    type<GraphQLInntektsmelding>()
    type<GraphQLSoknadNav>()
    type<GraphQLSoknadArbeidsgiver>()
    type<GraphQLSykmelding>()
}

enum class GraphQLHendelsetype {
    Inntektsmelding,
    SendtSoknadNav,
    SendtSoknadArbeidsgiver,
    NySoknad,
    Ukjent
}

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
