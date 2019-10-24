package no.nav.helse.hendelse

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.sykdomstidslinje.CompositeSykdomstidslinje
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import java.time.LocalDateTime
import java.util.*

class SykepengehistorikkHendelse private constructor(hendelseId: String, private val sykepengehistorikk: Sykepengehistorikk): PersonHendelse,
    SakskompleksHendelse, SykdomstidslinjeHendelse(hendelseId) {

    constructor(sykepengehistorikk: Sykepengehistorikk) : this(UUID.randomUUID().toString(), sykepengehistorikk)

    companion object {
        fun fromJson(jsonNode: JsonNode): SykepengehistorikkHendelse {
            return SykepengehistorikkHendelse(
                jsonNode["hendelseId"].textValue(),
                Sykepengehistorikk(jsonNode["inntektsmelding"])
            )
        }
    }

    // forenkling for å kaste ut flest mulige saker. Vil ikke være relevant når vi faktisk regner maksdato
    private val seksMånederIDager = 6*31

    override fun sakskompleksId() =
        sykepengehistorikk.sakskompleksId

    override fun hendelsetype() =
        Type.SykepengehistorikkMottatt

    override fun aktørId() =
        sykepengehistorikk.aktørId

    override fun rapportertdato(): LocalDateTime {
        TODO("not implemented")
    }

    fun påvirkerSakensMaksdato(sakensTidslinje: Sykdomstidslinje) =
        sykdomstidslinje().antallDagerMellom(sakensTidslinje) <= seksMånederIDager

    override fun organisasjonsnummer(): String? =
        sykepengehistorikk.organisasjonsnummer

    override fun sykdomstidslinje() =
        sykepengehistorikk.perioder.fold(CompositeSykdomstidslinje(emptyList()) as Sykdomstidslinje) { aggregate, periode ->
            aggregate + Sykdomstidslinje.sykedager(
                periode.fom,
                periode.tom,
                this
            )
        }
}
