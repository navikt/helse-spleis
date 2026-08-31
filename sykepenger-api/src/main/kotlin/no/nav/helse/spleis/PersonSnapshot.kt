package no.nav.helse.spleis

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import no.nav.helse.etterlevelse.Regelverkslogg.Companion.EmptyLog
import no.nav.helse.person.Person
import no.nav.helse.serde.SerialisertPerson
import no.nav.helse.spleis.dao.HendelseDao
import no.nav.helse.spleis.dao.PersonDao
import no.nav.helse.spleis.dto.HendelseDTO
import no.nav.helse.spleis.speil.dto.PersonDTO
import no.nav.helse.spleis.speil.serializePersonForSpeil

internal object ApiMetrikker {
    fun målDatabase(meterRegistry: MeterRegistry, block: () -> SerialisertPerson?): SerialisertPerson? = mål(meterRegistry, "hent_person", block)

    fun målDeserialisering(meterRegistry: MeterRegistry, block: () -> Person): Person = mål(meterRegistry, "deserialiser_person", block)

    fun målByggSnapshot(meterRegistry: MeterRegistry, block: () -> PersonDTO): PersonDTO = mål(meterRegistry, "bygg_snapshot", block)

    private fun <R> mål(meterRegistry: MeterRegistry, operasjon: String, block: () -> R): R {
        val timer = Timer.start(meterRegistry)
        return block().also {
            timer.stop(
                Timer.builder("person_snapshot_api")
                    .description("Metrikker for henting av speil-snapshot")
                    .tag("operasjon", operasjon)
                    .register(meterRegistry)
            )
        }
    }
}

internal data class PersonSnapshot(
    val person: PersonDTO,
    val hendelser: List<HendelseDTO>
)

/**
 * Henter og bygger speil-snapshotet for en person.
 */
internal fun hentPersonSnapshot(
    spekematClient: SpekematClient,
    personDao: PersonDao,
    hendelseDao: HendelseDao,
    fnr: String,
    callId: String,
    meterRegistry: MeterRegistry
): PersonSnapshot? {
    val serialisertPerson = ApiMetrikker.målDatabase(meterRegistry) { personDao.hentPersonFraFnr(fnr.toLong()) } ?: return null
    val spekemat = spekematClient.hentSpekemat(fnr, callId)
    val person = ApiMetrikker.målDeserialisering(meterRegistry) {
        Person.gjenopprett(EmptyLog, serialisertPerson.tilPersonDto())
    }
    val personDto = ApiMetrikker.målByggSnapshot(meterRegistry) { serializePersonForSpeil(person, spekemat) }
    return PersonSnapshot(personDto, hendelseDao.hentHendelser(fnr.toLong()))
}
