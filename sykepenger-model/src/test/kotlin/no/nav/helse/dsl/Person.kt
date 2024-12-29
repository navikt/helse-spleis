package no.nav.helse.dsl

import java.time.format.DateTimeFormatter
import no.nav.helse.Personidentifikator
import no.nav.helse.februar
import no.nav.helse.økonomi.Inntekt.Companion.månedlig

val UNG_PERSON_FØDSELSDATO = 12.februar(1992)

private val fnrformatter = DateTimeFormatter.ofPattern("ddMMyy")
internal val UNG_PERSON_FNR_2018: Personidentifikator = Personidentifikator("${UNG_PERSON_FØDSELSDATO.format(fnrformatter)}40045")

internal val INNTEKT = 31000.00.månedlig
