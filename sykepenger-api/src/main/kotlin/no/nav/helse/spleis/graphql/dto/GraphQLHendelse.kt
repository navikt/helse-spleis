package no.nav.helse.spleis.graphql.dto

import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.LocalDate
import java.time.LocalDateTime

enum class GraphQLHendelsetype {
    Inntektsmelding,
    SendtSoknadNav,
    SendtSoknadArbeidsgiver,
    SendtSoknadFrilans,
    SendtSoknadSelvstendig,
    SendtSoknadArbeidsledig,
    NySoknad,
    NySoknadFrilans,
    NySoknadSelvstendig,
    NySoknadArbeidsledig,
    Ukjent
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "__typename")
interface GraphQLHendelse {
    val id: String
    val eksternDokumentId: String
    val type: GraphQLHendelsetype
}

data class GraphQLInntektsmelding(
    override val id: String,
    override val eksternDokumentId: String,
    val mottattDato: LocalDateTime,
    val beregnetInntekt: Double
) : GraphQLHendelse {
    override val type = GraphQLHendelsetype.Inntektsmelding
}

data class GraphQLSoknadNav(
    override val id: String,
    override val eksternDokumentId: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertDato: LocalDateTime,
    val sendtNav: LocalDateTime
) : GraphQLHendelse {
    override val type = GraphQLHendelsetype.SendtSoknadNav
}

data class GraphQLSoknadFrilans(
    override val id: String,
    override val eksternDokumentId: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertDato: LocalDateTime,
    val sendtNav: LocalDateTime
) : GraphQLHendelse {
    override val type = GraphQLHendelsetype.SendtSoknadFrilans
}
data class GraphQLSoknadSelvstendig(
    override val id: String,
    override val eksternDokumentId: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertDato: LocalDateTime,
    val sendtNav: LocalDateTime
) : GraphQLHendelse {
    override val type = GraphQLHendelsetype.SendtSoknadSelvstendig
}
data class GraphQLSoknadArbeidsledig(
    override val id: String,
    override val eksternDokumentId: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertDato: LocalDateTime,
    val sendtNav: LocalDateTime
) : GraphQLHendelse {
    override val type = GraphQLHendelsetype.SendtSoknadArbeidsledig
}

data class GraphQLSoknadArbeidsgiver(
    override val id: String,
    override val eksternDokumentId: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertDato: LocalDateTime,
    val sendtArbeidsgiver: LocalDateTime
) : GraphQLHendelse {
    override val type = GraphQLHendelsetype.SendtSoknadArbeidsgiver
}

data class GraphQLSykmelding(
    override val id: String,
    override val eksternDokumentId: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertDato: LocalDateTime
) : GraphQLHendelse {
    override val type = GraphQLHendelsetype.NySoknad
}

data class GraphQLNySoknadFrilans(
    override val id: String,
    override val eksternDokumentId: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertDato: LocalDateTime
) : GraphQLHendelse {
    override val type = GraphQLHendelsetype.NySoknadFrilans
}
data class GraphQLNySoknadSelvstendig(
    override val id: String,
    override val eksternDokumentId: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertDato: LocalDateTime
) : GraphQLHendelse {
    override val type = GraphQLHendelsetype.NySoknadSelvstendig
}
data class GraphQLNySoknadArbeidsledig(
    override val id: String,
    override val eksternDokumentId: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertDato: LocalDateTime
) : GraphQLHendelse {
    override val type = GraphQLHendelsetype.NySoknadArbeidsledig
}