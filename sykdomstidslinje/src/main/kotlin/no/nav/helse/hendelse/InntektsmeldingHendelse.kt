package no.nav.helse.hendelse

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import java.util.*

class InntektsmeldingHendelse private constructor(hendelseId: String, private val inntektsmelding: Inntektsmelding): PersonHendelse, SykdomstidslinjeHendelse(hendelseId) {

    constructor(inntektsmelding: Inntektsmelding): this(UUID.randomUUID().toString(), inntektsmelding)

    companion object {
        fun fromJson(jsonNode: JsonNode): InntektsmeldingHendelse{
            return InntektsmeldingHendelse(jsonNode["hendelseId"].textValue(), Inntektsmelding(jsonNode["inntektsmelding"]))
        }
    }

    override fun hendelsetype() =
        Type.InntektsmeldingMottatt

    override fun aktørId() =
        inntektsmelding.arbeidstakerAktorId

    override fun rapportertdato() =
        inntektsmelding.rapportertDato

    override fun organisasjonsnummer() =
        inntektsmelding.virksomhetsnummer

    override fun sykdomstidslinje(): Sykdomstidslinje {
        val arbeidsgiverperiodetidslinjer = inntektsmelding.arbeidsgiverperioder
            .map { Sykdomstidslinje.sykedager(it.fom, it.tom, this) }
        val ferietidslinjer = inntektsmelding.ferie
            .map { Sykdomstidslinje.ferie(it.fom, it.tom, this) }

        // TODO: førsteFraværsdag er ikke med i kontrakten enda
        // val førsteFraværsdagTidslinje = listOf(Sykdomstidslinje.sykedag(gjelder = førsteFraværsdag, hendelse = this))

        return (/*førsteFraværsdagTidslinje + */arbeidsgiverperiodetidslinjer + ferietidslinjer)
            .reduce { resultatTidslinje, delTidslinje -> resultatTidslinje + delTidslinje }
    }

    override fun toJson(): JsonNode {
        return (super.toJson() as ObjectNode).set("inntektsmelding", inntektsmelding.jsonNode)
    }

}

