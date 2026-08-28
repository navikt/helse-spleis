package no.nav.helse.spleis.rest.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.LocalDate
import java.time.LocalDateTime

enum class Hendelsetype {
    Inntektsmelding,
    InntektFraAOrdningen,
    SendtSoknadNav,
    SendtSoknadArbeidsgiver,
    SendtSoknadFrilans,
    SendtSoknadSelvstendig,
    SendtSoknadArbeidsledig,
    NySoknad,
    Ukjent
}

/**
 * Hendelser har allerede et [type]-felt som entydig identifiserer subtypen, så det brukes som
 * diskriminator i stedet for å legge på et eget felt.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes(
    JsonSubTypes.Type(value = Inntektsmelding::class, name = "Inntektsmelding"),
    JsonSubTypes.Type(value = InntektFraAOrdningen::class, name = "InntektFraAOrdningen"),
    JsonSubTypes.Type(value = SoknadNav::class, name = "SendtSoknadNav"),
    JsonSubTypes.Type(value = SoknadArbeidsgiver::class, name = "SendtSoknadArbeidsgiver"),
    JsonSubTypes.Type(value = SoknadFrilans::class, name = "SendtSoknadFrilans"),
    JsonSubTypes.Type(value = SoknadSelvstendig::class, name = "SendtSoknadSelvstendig"),
    JsonSubTypes.Type(value = SoknadArbeidsledig::class, name = "SendtSoknadArbeidsledig"),
    JsonSubTypes.Type(value = Sykmelding::class, name = "NySoknad")
)
sealed interface Hendelse {
    val id: String
    val eksternDokumentId: String
    val type: Hendelsetype
}

data class Inntektsmelding(
    override val id: String,
    override val eksternDokumentId: String,
    val mottattDato: LocalDateTime,
    val beregnetInntekt: Double
) : Hendelse {
    override val type = Hendelsetype.Inntektsmelding
}

data class InntektFraAOrdningen(
    override val id: String,
    override val eksternDokumentId: String,
    val mottattDato: LocalDateTime
) : Hendelse {
    override val type = Hendelsetype.InntektFraAOrdningen
}

data class SoknadNav(
    override val id: String,
    override val eksternDokumentId: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertDato: LocalDateTime,
    val sendtNav: LocalDateTime
) : Hendelse {
    override val type = Hendelsetype.SendtSoknadNav
}

data class SoknadFrilans(
    override val id: String,
    override val eksternDokumentId: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertDato: LocalDateTime,
    val sendtNav: LocalDateTime
) : Hendelse {
    override val type = Hendelsetype.SendtSoknadFrilans
}

data class SoknadSelvstendig(
    override val id: String,
    override val eksternDokumentId: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertDato: LocalDateTime,
    val sendtNav: LocalDateTime
) : Hendelse {
    override val type = Hendelsetype.SendtSoknadSelvstendig
}

data class SoknadArbeidsledig(
    override val id: String,
    override val eksternDokumentId: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertDato: LocalDateTime,
    val sendtNav: LocalDateTime
) : Hendelse {
    override val type = Hendelsetype.SendtSoknadArbeidsledig
}

data class SoknadArbeidsgiver(
    override val id: String,
    override val eksternDokumentId: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertDato: LocalDateTime,
    val sendtArbeidsgiver: LocalDateTime
) : Hendelse {
    override val type = Hendelsetype.SendtSoknadArbeidsgiver
}

data class Sykmelding(
    override val id: String,
    override val eksternDokumentId: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val rapportertDato: LocalDateTime
) : Hendelse {
    override val type = Hendelsetype.NySoknad
}
