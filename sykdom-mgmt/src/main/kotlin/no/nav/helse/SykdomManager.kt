package no.nav.helse

class SykdomManager(private val makskapasitet: Int) {
    private val personer = mutableListOf<String>()

    fun registrer(fødselsnummer: String): Boolean {
        if (fødselsnummer in personer) return true
        if (personer.size >= makskapasitet) return false
        personer.add(fødselsnummer)
        return true
    }

    companion object {
        fun gjennopprett(personer: List<String>, makskapasitet: Int): SykdomManager {
            return SykdomManager(makskapasitet).apply { this.personer.addAll(personer) }
        }
    }

}
