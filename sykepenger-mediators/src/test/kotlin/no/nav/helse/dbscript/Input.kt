package no.nav.helse.dbscript

import no.nav.helse.dbscript.Input.Beskrivelse.Companion.gyldigBeskrivelse
import no.nav.helse.dbscript.Input.Epost.Companion.gyldigEpost
import no.nav.helse.dbscript.Input.Fødselsnummer.Companion.gyldigFødeselsnummer
import kotlin.text.toLong

internal object Input {
    fun ventPåInput(
        default: String? = null,
        lowercaseInput: Boolean = false,
        valider: (input: String) -> Boolean,
    ): String {
        var svar: String?
        do {
            svar =
                readlnOrNull()?.let { if (lowercaseInput) it.lowercase() else it }?.let { input ->
                    if (input == "exit") error("💀 Avslutter prosessen")
                    if (default != null && input.isEmpty()) return@let default
                    if (!valider(input)) {
                        println("🙅 '$input' er ikke gyldig!")
                        return@let null
                    }
                    input
                }
        } while (svar == null)
        return svar
    }

    fun gåVidereVedJa(
        hva: String,
        default: Boolean,
    ) {
        val (defaultSvar, valg) =
            when (default) {
                true -> "y" to "[Yn]"
                false -> "n" to "[yN]"
            }
        println("## $hva? $valg")
        if (ventPåInput(defaultSvar, lowercaseInput = true) { it in setOf("y", "n") } == "y") return
        error("❌ Avslutter prosessen siden du svarte nei")
    }

    fun ventPåFødselsnummer() = Fødselsnummer(ventPåInput { it.gyldigFødeselsnummer() })

    fun ventPåEpost(default: String?) = Epost(ventPåInput(default, lowercaseInput = true) { it.gyldigEpost() })

    fun ventPåBeskrivelse() = Beskrivelse(ventPåInput { it.gyldigBeskrivelse() })

    data class Fødselsnummer(
        private val verdi: String,
    ) {
        init {
            check(verdi.gyldigFødeselsnummer()) { "Ugyldig fødselsnummer $verdi" }
        }

        override fun toString() = verdi

        internal fun toLong() = verdi.toLong()

        companion object {
            fun String.gyldigFødeselsnummer() = this.length == 11 && runCatching { this.toLong() }.isSuccess
        }
    }

    data class Epost(
        val verdi: String,
    ) {
        init {
            check(verdi.gyldigEpost()) { "Ugyldig epost $verdi" }
        }

        override fun toString() = verdi

        companion object {
            fun String.gyldigEpost() = this.endsWith("@nav.no")
        }
    }

    data class Beskrivelse(
        private val verdi: String,
    ) {
        init {
            check(verdi.gyldigBeskrivelse()) { "Ugyldig beskrivelse $verdi" }
        }

        override fun toString() = verdi

        companion object {
            fun String.gyldigBeskrivelse() = this.trim().length >= 15
        }
    }
}
