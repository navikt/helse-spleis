package no.nav.helse.spleis

import no.nav.helse.person.PersonObserver
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.streams.KafkaStreams
import java.util.*
import javax.sql.DataSource

internal class HelseBuilder(dataSource: DataSource, hendelseProducer: KafkaProducer<String, String>) {

    private val hendelseBuilder: HendelseBuilder
    private val hendelseProbe: HendelseListener = HendelseProbe()
    private val hendelseRecorder: HendelseListener = HendelseRecorder(dataSource)

    internal val personMediator: PersonMediator
    private val personRepository: PersonRepository = PersonPostgresRepository(dataSource)
    private val lagrePersonDao: PersonObserver = LagrePersonDao(dataSource)
    private val utbetalingsreferanseRepository: UtbetalingsreferanseRepository = UtbetalingsreferansePostgresRepository(dataSource)
    private val lagreUtbetalingDao: PersonObserver = LagreUtbetalingDao(dataSource)

    init {
        personMediator = PersonMediator(
            personRepository = personRepository,
            lagrePersonDao = lagrePersonDao,
            utbetalingsreferanseRepository = utbetalingsreferanseRepository,
            lagreUtbetalingDao = lagreUtbetalingDao,
            producer = hendelseProducer
        )

        hendelseBuilder = HendelseBuilder().apply {
            addListener(hendelseProbe)
            addListener(hendelseRecorder)
            addListener(personMediator)
        }
    }

    fun addStateListener(stateListener: KafkaStreams.StateListener) {
        hendelseBuilder.addStateListener(stateListener)
    }

    fun start(props: Properties) {
        hendelseBuilder.start(props)
    }


    fun stop() {
        hendelseBuilder.stop()
    }
}
