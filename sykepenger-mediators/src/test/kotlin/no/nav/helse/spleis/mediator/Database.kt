package no.nav.helse.spleis.mediator

import no.nav.helse.testdatabase.DatabaseContainer

// Alle tester bruker unike fødselsnumre (se `nyttFødselsnummer()`), så vi trenger ikke lenger
// en pool av databaser med truncate mellom hver test — én delt database holder, og tester kan
// kjøre parallelt mot den så lenge Hikari-poolen er stor nok til å dekke JUnit-parallellismen.
val databaseContainer = DatabaseContainer(
    appnavn = "spleis-mediators",
    maxHikariPoolSize = 32,
    postgresVersjon = 17,
)
