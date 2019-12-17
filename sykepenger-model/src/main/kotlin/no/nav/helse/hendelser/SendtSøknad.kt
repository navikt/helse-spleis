package no.nav.helse.hendelser

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.serde.safelyUnwrapDate
import no.nav.helse.sykdomstidslinje.ConcreteSykdomstidslinje
import no.nav.helse.sykdomstidslinje.dag.Dag.NøkkelHendelseType.Søknad
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class SendtSøknad(hendelseId: UUID, søknad: JsonNode) : SøknadHendelse(hendelseId, Hendelsetype.SendtSøknad, søknad) {

    constructor(søknad: JsonNode) : this(UUID.randomUUID(), søknad)

    private val fom get() = søknad["fom"].asText().let { LocalDate.parse(it) }
    private val tom get() = søknad["tom"].asText().let { LocalDate.parse(it) }
    private val sendtNav = søknad["sendtNav"]?.takeUnless { it.isNull }?.let { LocalDateTime.parse(it.asText()) }

    private val egenmeldinger get() = søknad["egenmeldinger"]?.map {
        Periode(
            it
        )
    } ?: emptyList()
    private val fraværsperioder
        get() = søknad["fravar"]?.filterNot {
            Fraværstype.valueOf(it["type"].textValue()) in listOf(
                Fraværstype.UTDANNING_FULLTID,
                Fraværstype.UTDANNING_DELTID
            )
        }?.map { FraværsPeriode(it) } ?: emptyList()

    private val utdanningsperioder
        get() = søknad["fravar"]?.filter {
            Fraværstype.valueOf(it["type"].textValue()) in listOf(
                Fraværstype.UTDANNING_FULLTID,
                Fraværstype.UTDANNING_DELTID
            )
        }?.map { Utdanningsfraværsperiode(it) } ?: emptyList()

    private val arbeidGjenopptatt get() = søknad["arbeidGjenopptatt"]?.safelyUnwrapDate()

    override fun opprettet() = requireNotNull(sendtNav)

    override fun rapportertdato(): LocalDateTime = requireNotNull(sendtNav)

    override fun nøkkelHendelseType() = Søknad

    override fun kanBehandles(): Boolean {
        return super.kanBehandles()
                && sykeperioder.all { (it.faktiskGrad ?: it.sykmeldingsgrad) == 100 }
                && sendtNav != null
                && fom >= sendtNav.toLocalDate().minusMonths(3).withDayOfMonth(1)
    }

    private val sykeperiodeTidslinje
        get(): List<ConcreteSykdomstidslinje> = sykeperioder
                .map { ConcreteSykdomstidslinje.sykedager(it.fom, it.tom, this) }

    private val egenmeldingsTidslinje
        get(): List<ConcreteSykdomstidslinje> = egenmeldinger
                .map { ConcreteSykdomstidslinje.egenmeldingsdager(it.fom, it.tom, this) }

    private val ferieTidslinje
        get(): List<ConcreteSykdomstidslinje> = fraværsperioder
                .filter { it.type == Fraværstype.FERIE }
                .map { ConcreteSykdomstidslinje.ferie(it.fom, it.tom, this) }

    private val permisjonTidslinje
        get(): List<ConcreteSykdomstidslinje> = fraværsperioder
                .filter { it.type == Fraværstype.PERMISJON }
                .map { ConcreteSykdomstidslinje.permisjonsdager(it.fom, it.tom, this) }

    private val arbeidGjenopptattTidslinje
        get(): List<ConcreteSykdomstidslinje> = arbeidGjenopptatt
                ?.let { listOf(ConcreteSykdomstidslinje.ikkeSykedager(it, tom, this)) }
                ?: emptyList()

    private val studiedagertidslinje = utdanningsperioder.map {
        ConcreteSykdomstidslinje.studiedager(it.fom, tom, this)
    }

    override fun sykdomstidslinje() =
            (sykeperiodeTidslinje + egenmeldingsTidslinje + ferieTidslinje + arbeidGjenopptattTidslinje + studiedagertidslinje + permisjonTidslinje)
                    .reduce { resultatTidslinje, delTidslinje ->
                        resultatTidslinje + delTidslinje
                    }

    private class Periode(val jsonNode: JsonNode) {
        val fom: LocalDate = LocalDate.parse(jsonNode["fom"].textValue())
        val tom: LocalDate = LocalDate.parse(jsonNode["tom"].textValue())
    }

    private class FraværsPeriode(val jsonNode: JsonNode) {
        val fom: LocalDate = LocalDate.parse(jsonNode["fom"].textValue())
        val tom: LocalDate = LocalDate.parse(jsonNode["tom"].textValue())
        val type: Fraværstype = enumValueOf(jsonNode["type"].textValue())
    }

    private class Utdanningsfraværsperiode(val jsonNode: JsonNode) {
        val fom: LocalDate = LocalDate.parse(jsonNode["fom"].textValue())
        val type: Fraværstype = enumValueOf(jsonNode["type"].textValue())
    }

    private enum class Fraværstype {
        FERIE,
        PERMISJON,
        UTLANDSOPPHOLD,
        UTDANNING_FULLTID,
        UTDANNING_DELTID
    }
}
