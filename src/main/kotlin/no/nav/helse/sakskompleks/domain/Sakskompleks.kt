package no.nav.helse.sakskompleks.domain

import no.nav.helse.sykmelding.domain.Sykmelding
import no.nav.helse.søknad.domain.Sykepengesøknad
import java.util.*

data class Sakskompleks(val id: UUID,
                        val aktørId: String,
                        val sykmeldinger: List<Sykmelding>,
                        val søknader: List<Sykepengesøknad>)
