package no.nav.helse.spleis

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
    internal val hendelseRecorder: HendelseRecorder = HendelseRecorder(dataSource)

    internal val personRestInterface: PersonRestInterface
    private val personRepository = PersonPostgresRepository(dataSource)
    private val lagrePersonDao = LagrePersonDao(dataSource)
    private val utbetalingsreferanseRepository = UtbetalingsreferansePostgresRepository(dataSource)
    private val lagreUtbetalingDao = LagreUtbetalingDao(dataSource)

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
            hendelseRecorder = hendelseRecorder
        )
    }
}
