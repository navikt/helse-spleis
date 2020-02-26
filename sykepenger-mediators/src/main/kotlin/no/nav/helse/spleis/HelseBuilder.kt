package no.nav.helse.spleis

import no.nav.helse.person.PersonObserver
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.spleis.db.*
import no.nav.helse.spleis.rest.PersonRestInterface
import javax.sql.DataSource

// Understands how to create the mediators and create the objects they need
internal class HelseBuilder(
    dataSource: DataSource,
    rapidsConnection: RapidsConnection
) {

    private val hendelseDirector: HendelseMediator
    private val hendelseProbe: HendelseProbe = HendelseProbe()
    internal val hendelseRecorder: HendelseRecorder = HendelseRecorder(dataSource)

    internal val personRestInterface: PersonRestInterface
    private val personRepository: PersonRepository = PersonPostgresRepository(dataSource)
    private val lagrePersonDao: PersonObserver = LagrePersonDao(dataSource)
    private val utbetalingsreferanseRepository: UtbetalingsreferanseRepository =
        UtbetalingsreferansePostgresRepository(dataSource)
    private val lagreUtbetalingDao: PersonObserver = LagreUtbetalingDao(dataSource)

    init {
        personRestInterface = PersonRestInterface(
            personRepository = personRepository,
            utbetalingsreferanseRepository = utbetalingsreferanseRepository
        )

        hendelseDirector = HendelseMediator(
            rapidsConnection = rapidsConnection,
            personRepository = personRepository,
            lagrePersonDao = lagrePersonDao,
            lagreUtbetalingDao = lagreUtbetalingDao,
            hendelseProbe = hendelseProbe,
            hendelseRecorder = hendelseRecorder
        )
    }
}
