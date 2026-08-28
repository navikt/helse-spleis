package no.nav.helse.spleis.rest.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.LocalDate
import java.time.LocalDateTime

enum class ApiHendelsetype {
    Inntektsmelding,
    InntektFraAOrdningen,
    SendtSoknadNav,
    SendtSoknadArbeidsgiver,
    SendtSoknadFrilans,
    SendtSoknadSelvstendig,
    SendtSoknadArbeidsledig,
    NySoknad,
}

/**
 * Hendelser har allerede et [type]-felt som entydig identifiserer subtypen, så det brukes som
 * diskriminator i stedet for å legge på et eget felt.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes(
    JsonSubTypes.Type(value = ApiInntektsmelding::class, name = "Inntektsmelding"),
    JsonSubTypes.Type(value = ApiInntektFraAOrdningen::class, name = "InntektFraAOrdningen"),
    JsonSubTypes.Type(value = ApiSoknadNav::class, name = "SendtSoknadNav"),
    JsonSubTypes.Type(value = ApiSoknadArbeidsgiver::class, name = "SendtSoknadArbeidsgiver"),
    JsonSubTypes.Type(value = ApiSoknadFrilans::class, name = "SendtSoknadFrilans"),
    JsonSubTypes.Type(value = ApiSoknadSelvstendig::class, name = "SendtSoknadSelvstendig"),
    JsonSubTypes.Type(value = ApiSoknadArbeidsledig::class, name = "SendtSoknadArbeidsledig"),
    JsonSubTypes.Type(value = ApiSykmelding::class, name = "NySoknad")
)
sealed interface ApiHendelse {
    val id: String
    val eksternDokumentId: String
    val type: ApiHendelsetype
}

data class ApiInntektsmelding(
    override val id: String,
    override val eksternDokumentId: String,
    val mottattDato: LocalDateTime,
    val beregnetInntekt: Double
) : ApiHendelse {
    override val type = ApiHendelsetype.Inntektsmelding
}

data class ApiInntektFraAOrdningen(
    override val id: String,
    override val eksternDokumentId: String,
    val mottattDato: LocalDateTime
) : ApiHendelse {
    override val type = ApiHendelsetype.InntektFraAOrdningen
}

data class ApiSoknadNav(
    override val id: String,
    override val eksternDokumentId: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertDato: LocalDateTime,
    val sendtNav: LocalDateTime
) : ApiHendelse {
    override val type = ApiHendelsetype.SendtSoknadNav
}

data class ApiSoknadFrilans(
    override val id: String,
    override val eksternDokumentId: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertDato: LocalDateTime,
    val sendtNav: LocalDateTime
) : ApiHendelse {
    override val type = ApiHendelsetype.SendtSoknadFrilans
}

data class ApiSoknadSelvstendig(
    override val id: String,
    override val eksternDokumentId: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertDato: LocalDateTime,
    val sendtNav: LocalDateTime
) : ApiHendelse {
    override val type = ApiHendelsetype.SendtSoknadSelvstendig
}

data class ApiSoknadArbeidsledig(
    override val id: String,
    override val eksternDokumentId: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertDato: LocalDateTime,
    val sendtNav: LocalDateTime
) : ApiHendelse {
    override val type = ApiHendelsetype.SendtSoknadArbeidsledig
}

data class ApiSoknadArbeidsgiver(
    override val id: String,
    override val eksternDokumentId: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertDato: LocalDateTime,
    val sendtArbeidsgiver: LocalDateTime
) : ApiHendelse {
    override val type = ApiHendelsetype.SendtSoknadArbeidsgiver
}

data class ApiSykmelding(
    override val id: String,
    override val eksternDokumentId: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertDato: LocalDateTime
) : ApiHendelse {
    override val type = ApiHendelsetype.NySoknad
}
