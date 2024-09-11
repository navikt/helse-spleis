
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource

class AftereEachTestOpenSpannerExtension : AfterEachCallback, CloseableResource {

    @Throws(Exception::class)
    override fun afterEach(context: ExtensionContext) {
        // lock the access so only one Thread has access to it
        var started = false
        val v = SpannerEtterTestInterceptor()
        try {
            if (!started) {
                started = true
                // Your "after each test" startup logic goes here
                v.openTheSpanner(context)
                // The following line registers a callback hook when the root test context is
                // shut down
                context.root.getStore(GLOBAL).put("any unique name", this)

                // do your work - which might take some time -
                // or just uses more time than the simple check of a boolean
            }
        } catch (e: Exception) {
            v.openTheSpanner(context, e.cause?.message)
        }
    }


    override fun close() {

    }
}