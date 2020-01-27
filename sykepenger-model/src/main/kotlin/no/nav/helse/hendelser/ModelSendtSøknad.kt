package no.nav.helse.hendelser

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.person.PersonVisitor
import no.nav.helse.sykdomstidslinje.ConcreteSykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.dag.Dag
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class ModelSendtSøknad(
    hendelseId: UUID,
    private val fnr: String,
    private val aktørId: String,
    private val orgnummer: String,
    private val rapportertdato: LocalDateTime,
    private val perioder: List<Periode>,
    private val originalJson: String,
    aktivitetslogger: Aktivitetslogger
) : SykdomstidslinjeHendelse(hendelseId, Hendelsestype.SendtSøknad, aktivitetslogger) {

    private val fom: LocalDate
    private val tom: LocalDate

    companion object {
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        fun fromJson(json: String): ModelSendtSøknad {
            return objectMapper.readTree(json).let {
                val søknadTom = it["søknad"].path("tom").asLocalDate()
                val aktivitetslogger = Aktivitetslogger(json)
                ModelSendtSøknad(
                    hendelseId = UUID.fromString(it["hendelseId"].textValue()),
                    fnr = it.path("søknad").path("fnr").asText(),
                    aktørId = it.path("søknad").path("aktorId").asText(),
                    orgnummer = it.path("søknad").path("arbeidsgiver").path("orgnummer").asText(),
                    rapportertdato = it.path("søknad").path("opprettet").asText().let { LocalDateTime.parse(it) },
                    perioder = it.path("søknad").path("soknadsperioder").map { periode: JsonNode ->
                        Periode.Sykdom(
                            fom = periode["fom"].asLocalDate(),
                            tom = periode["tom"].asLocalDate(),
                            grad = periode["sykmeldingsgrad"].asInt(),
                            faktiskGrad = periode["faktiskGrad"]?.asDouble() ?: periode["sykmeldingsgrad"].asDouble()
                        )
                    } + it.path("søknad").path("egenmeldinger").map {
                        Periode.Egenmelding(
                            fom = it.path("fom").asLocalDate(),
                            tom = it.path("tom").asLocalDate()
                        )
                    } + it.path("søknad").path("fravar").mapNotNull {
                        val fraværstype = it["type"].asText()
                        val fom = it.path("fom").asLocalDate()
                        when (fraværstype) {
                            in listOf("UTDANNING_FULLTID", "UTDANNING_DELTID") -> Periode.Utdanning(fom, søknadTom)
                            "PERMISJON" -> Periode.Permisjon(fom, it.path("tom").asLocalDate())
                            "FERIE" -> Periode.Ferie(fom, it.path("tom").asLocalDate())
                            else -> {
                                aktivitetslogger.warn("Ukjent fraværstype $fraværstype")
                                null
                            }
                        }
                    } + (it.path("søknad").path("arbeidGjenopptatt").asOptionalLocalDate()?.let {
                        listOf(
                            Periode.Arbeid(
                                it,
                                søknadTom
                            )
                        )
                    }
                        ?: emptyList()),
                    aktivitetslogger = aktivitetslogger,
                    originalJson = objectMapper.writeValueAsString(it.path("søknad"))
                )
            }
        }

        private fun JsonNode.asOptionalLocalDate() =
            takeIf(JsonNode::isTextual)?.asText()?.takeIf(String::isNotEmpty)?.let { LocalDate.parse(it) }


        private fun JsonNode.asLocalDate() =
            asText().let { LocalDate.parse(it) }

    }

    init {
        if (perioder.isEmpty()) aktivitetslogger.severe("Søknad må inneholde perioder")
        perioder.filterIsInstance<Periode.Sykdom>()
            .also { fom = it.minBy { it.fom }?.fom ?: aktivitetslogger.severe("Søknad mangler fradato") }
            .also { tom = it.maxBy { it.tom }?.tom ?: aktivitetslogger.severe("Søknad mangler tildato") }
    }

    override fun kopierAktiviteterTil(aktivitetslogger: Aktivitetslogger) {
        aktivitetslogger.addAll(this.aktivitetslogger, "Sendt søknad")
    }

    override fun sykdomstidslinje() = perioder
        .map { it.sykdomstidslinje(this) }
        .reduce(ConcreteSykdomstidslinje::plus)

    override fun nøkkelHendelseType() = Dag.NøkkelHendelseType.Søknad

    override fun fødselsnummer() = fnr

    override fun organisasjonsnummer() = orgnummer

    override fun rapportertdato() = rapportertdato

    override fun aktørId() = aktørId

    // TODO: Should not be part of Model events
    override fun toJson(): String = objectMapper.writeValueAsString(
        mapOf(
            "hendelseId" to hendelseId(),
            "type" to hendelsetype(),
            "søknad" to objectMapper.readTree(originalJson)
        )
    )

    override fun kanBehandles() = !valider().hasErrors()

    internal fun valider(): Aktivitetslogger {
        perioder.forEach { it.valider(this, aktivitetslogger) }
        return aktivitetslogger
    }

    override fun accept(visitor: PersonVisitor) {
        visitor.visitSendtSøknadHendelse(this)
    }

    sealed class Periode(internal val fom: LocalDate, internal val tom: LocalDate) {

        internal abstract fun sykdomstidslinje(sendtSøknad: ModelSendtSøknad): ConcreteSykdomstidslinje

        internal open fun valider(sendtSøknad: ModelSendtSøknad, aktivitetslogger: Aktivitetslogger) {}

        internal fun valider(sendtSøknad: ModelSendtSøknad, aktivitetslogger: Aktivitetslogger, beskjed: String) {
            if (fom < sendtSøknad.fom || tom > sendtSøknad.tom) aktivitetslogger.error(beskjed)
        }

        class Ferie(fom: LocalDate, tom: LocalDate) : Periode(fom, tom) {
            override fun sykdomstidslinje(sendtSøknad: ModelSendtSøknad) =
                ConcreteSykdomstidslinje.ferie(fom, tom, sendtSøknad)

            override fun valider(sendtSøknad: ModelSendtSøknad, aktivitetslogger: Aktivitetslogger) =
                valider(sendtSøknad, aktivitetslogger, "Ferie ligger utenfor sykdomsvindu")
        }

        class Sykdom(
            fom: LocalDate,
            tom: LocalDate,
            private val grad: Int,
            private val faktiskGrad: Double = grad.toDouble()
        ) : Periode(fom, tom) {
            override fun sykdomstidslinje(sendtSøknad: ModelSendtSøknad) =
                ConcreteSykdomstidslinje.sykedager(fom, tom, sendtSøknad)

            override fun valider(sendtSøknad: ModelSendtSøknad, aktivitetslogger: Aktivitetslogger) {
                if (grad != 100) aktivitetslogger.error("grad i søknaden er ikke 100%%")
                if (faktiskGrad != 100.0) aktivitetslogger.error("faktisk grad i søknaden er ikke 100%%")
            }
        }

        class Utdanning(fom: LocalDate, private val _tom: LocalDate? = null) : Periode(fom, LocalDate.MAX) {
            override fun sykdomstidslinje(sendtSøknad: ModelSendtSøknad) =
                ConcreteSykdomstidslinje.utenlandsdager(fom, _tom ?: sendtSøknad.tom, sendtSøknad)

            override fun valider(sendtSøknad: ModelSendtSøknad, aktivitetslogger: Aktivitetslogger) =
                aktivitetslogger.error("Utdanning foreløpig ikke understøttet")
        }

        class Permisjon(fom: LocalDate, tom: LocalDate) : Periode(fom, tom) {
            override fun sykdomstidslinje(sendtSøknad: ModelSendtSøknad) =
                ConcreteSykdomstidslinje.permisjonsdager(fom, tom, sendtSøknad)

            override fun valider(sendtSøknad: ModelSendtSøknad, aktivitetslogger: Aktivitetslogger) =
                aktivitetslogger.error("Permisjon foreløpig ikke understøttet")
        }

        class Egenmelding(fom: LocalDate, tom: LocalDate) : Periode(fom, tom) {
            override fun sykdomstidslinje(sendtSøknad: ModelSendtSøknad) =
                ConcreteSykdomstidslinje.egenmeldingsdager(fom, tom, sendtSøknad)
        }

        class Arbeid(fom: LocalDate, tom: LocalDate) : Periode(fom, tom) {
            override fun sykdomstidslinje(sendtSøknad: ModelSendtSøknad) =
                ConcreteSykdomstidslinje.ikkeSykedager(fom, tom, sendtSøknad)

            override fun valider(sendtSøknad: ModelSendtSøknad, aktivitetslogger: Aktivitetslogger) =
                valider(sendtSøknad, aktivitetslogger, "Arbeidsdag ligger utenfor sykdomsvindu")
        }
    }
}
