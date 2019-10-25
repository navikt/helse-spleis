package no.nav.helse.inntektsmelding

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.SykdomshendelseType
import no.nav.helse.hendelse.PersonHendelse
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.dag.Dag
import java.util.*

class InntektsmeldingHendelse private constructor(hendelseId: String, private val inntektsmelding: Inntektsmelding) : PersonHendelse, SykdomstidslinjeHendelse(hendelseId) {
    constructor(inntektsmelding: Inntektsmelding) : this(UUID.randomUUID().toString(), inntektsmelding)

    companion object {

        fun fromJson(jsonNode: JsonNode): InntektsmeldingHendelse {
            return InntektsmeldingHendelse(jsonNode["hendelseId"].textValue(), Inntektsmelding(jsonNode["inntektsmelding"]))
        }
    }

    override fun nøkkelHendelseType() = Dag.NøkkelHendelseType.Inntektsmelding

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
        return (super.toJson() as ObjectNode).apply {
            put("type", SykdomshendelseType.InntektsmeldingMottatt.name)
            set("inntektsmelding", inntektsmelding.toJson())
        }
    }


}

