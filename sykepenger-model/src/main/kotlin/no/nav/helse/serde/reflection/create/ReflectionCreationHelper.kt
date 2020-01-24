package no.nav.helse.serde.reflection.create

import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.InntektHistorie
import java.util.*

internal class ReflectionCreationHelper {
    fun lagArbeidsgiver(
        organisasjonsnummer: String,
        id: UUID,
        inntektHistorie: InntektHistorie
    ): Arbeidsgiver =
        Arbeidsgiver::class.java.getDeclaredConstructor(
            String::class.java, UUID::class.java, InntektHistorie::class.java
        ).let {
            it.isAccessible = true
            it.newInstance(organisasjonsnummer, id, inntektHistorie)
        }

}
