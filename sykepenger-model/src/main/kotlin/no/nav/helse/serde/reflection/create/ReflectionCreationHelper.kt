package no.nav.helse.serde.reflection.create

import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Inntekthistorikk
import java.util.*

internal class ReflectionCreationHelper {
    fun lagArbeidsgiver(
        organisasjonsnummer: String,
        id: UUID,
        inntekthistorikk: Inntekthistorikk
    ): Arbeidsgiver =
        Arbeidsgiver::class.java.getDeclaredConstructor(
            String::class.java, UUID::class.java, Inntekthistorikk::class.java
        ).let {
            it.isAccessible = true
            it.newInstance(organisasjonsnummer, id, inntekthistorikk)
        }

}
