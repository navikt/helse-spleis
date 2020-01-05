package no.nav.helse.spleis

import no.nav.helse.person.PersonObserver
import org.apache.kafka.clients.producer.KafkaProducer
import javax.sql.DataSource

// Understands how to create the mediators and create the objects they need
internal class HelseBuilder(dataSource: DataSource, hendelseStream: HendelseStream, hendelseProducer: KafkaProducer<String, String>) {

    private val hendelseDirector: HendelseDirector
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

        hendelseDirector = HendelseDirector(hendelseStream).apply {
            addListener(hendelseProbe)
            addListener(hendelseRecorder)
            addListener(personMediator)
        }
    }
}
