package dev.gaphunter.interfacesinkdivergencecompanion.detect

import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.PsiVariable

/** Recognizes a real logging call (`log.info(...)`/etc.) by the qualifier's own reference name (`log`/`logger`) or declared type text mentioning `Logger` -- text only, never resolved against the real logging framework's classpath. Same convention as `log-injection-companion`'s own copy. */
object LoggingCallSignals {

    private val LOG_METHOD_NAMES = setOf("info", "warn", "error", "debug", "trace")

    fun isLoggingCall(call: PsiMethodCallExpression): Boolean {
        if (call.methodExpression.referenceName !in LOG_METHOD_NAMES) return false
        val qualifier = call.methodExpression.qualifierExpression as? PsiReferenceExpression ?: return false

        val qualifierName = qualifier.referenceName?.lowercase()
        if (qualifierName == "log" || qualifierName == "logger") return true

        val resolvedVariable = qualifier.resolve() as? PsiVariable ?: return false
        return resolvedVariable.type.presentableText.contains("Logger")
    }
}
