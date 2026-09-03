package dev.gaphunter.interfacesinkdivergencecompanion.detect

import com.intellij.psi.JavaRecursiveElementWalkingVisitor
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiCodeBlock
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiModifier
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ClassInheritorsSearch
import dev.gaphunter.interfacesinkdivergencecompanion.model.SinkDivergenceHit

/**
 * Combines TWO techniques this catalog already proved separately: the
 * real Class Hierarchy Analysis from `interface-exception-divergence-
 * companion` (resolve EVERY concrete implementation of an interface
 * method, never discard for "ambiguity" the way a whole-project CALL
 * GRAPH mechanism would) with the taint-to-sink pattern from
 * `log-injection-companion`/the SpEL/XPath/LDAP sink finders.
 *
 * For a call through an interface-typed reference passing a tainted
 * argument (an HTTP endpoint parameter, same method, direct reference
 * or one-hop concatenation), resolves every real implementation and
 * checks -- INSIDE each one's own override body -- whether the
 * corresponding parameter reaches an unsanitized logging call. If SOME
 * implementations do and at least one other real implementation never
 * does, the call site is flagged: the interface type alone never
 * guarantees which behavior a caller actually gets once dependency
 * injection swaps implementations.
 *
 * **v0.1 scope, stated honestly:** only interfaces with 2-10 real
 * implementations; taint only through a bare reference or one-hop
 * concatenation, both at the call site AND inside each implementation
 * (a wrapping call anywhere in the chain breaks it, treated as
 * sanitized); only `Logger.info/warn/error/debug/trace`-shaped sinks
 * (same recognition as `log-injection-companion`).
 */
object InterfaceSinkDivergenceFinder {

    private const val MIN_IMPLEMENTATIONS = 2
    private const val MAX_IMPLEMENTATIONS = 10

    fun findAll(file: PsiFile): List<SinkDivergenceHit> {
        val hits = mutableListOf<SinkDivergenceHit>()
        file.accept(object : JavaRecursiveElementWalkingVisitor() {
            override fun visitMethod(method: PsiMethod) {
                super.visitMethod(method)
                if (!ControllerEndpointSignals.isEndpointMethod(method)) return
                val body = method.body ?: return
                val taintedNames = method.parameterList.parameters.map { it.name }.toSet()
                if (taintedNames.isEmpty()) return
                hits += hitsInBody(body, taintedNames)
            }
        })
        return hits
    }

    private fun hitsInBody(body: PsiCodeBlock, taintedNames: Set<String>): List<SinkDivergenceHit> {
        val hits = mutableListOf<SinkDivergenceHit>()
        body.accept(object : JavaRecursiveElementWalkingVisitor() {
            override fun visitMethodCallExpression(call: PsiMethodCallExpression) {
                super.visitMethodCallExpression(call)
                hitForCall(call, taintedNames)?.let { hits += it }
            }
        })
        return hits
    }

    private fun hitForCall(call: PsiMethodCallExpression, taintedNames: Set<String>): SinkDivergenceHit? {
        val qualifier = call.methodExpression.qualifierExpression ?: return null
        val interfaceClass = (qualifier.type as? PsiClassType)?.resolve() ?: return null
        if (!interfaceClass.isInterface) return null

        val interfaceMethod = call.resolveMethod() ?: return null
        if (interfaceMethod.containingClass != interfaceClass) return null

        val arguments = call.argumentList.expressions
        val taintedArgIndex = arguments.indexOfFirst { TaintReferenceMatcher.isTaintedReference(it, taintedNames) }
        if (taintedArgIndex < 0) return null

        val implementations = ClassInheritorsSearch.search(interfaceClass, GlobalSearchScope.projectScope(interfaceClass.project), true)
            .findAll()
            .filter { !it.isInterface && !it.hasModifierProperty(PsiModifier.ABSTRACT) }
        if (implementations.size !in MIN_IMPLEMENTATIONS..MAX_IMPLEMENTATIONS) return null

        var realOverrideCount = 0
        var loggingCount = 0
        for (implementation in implementations) {
            val overriding = implementation.findMethodBySignature(interfaceMethod, true) ?: continue
            if (overriding.containingClass != implementation) continue
            realOverrideCount++
            val implParamName = overriding.parameterList.parameters.getOrNull(taintedArgIndex)?.name ?: continue
            if (bodyLogsParameter(overriding.body, implParamName)) loggingCount++
        }
        if (realOverrideCount < MIN_IMPLEMENTATIONS) return null
        if (loggingCount !in 1 until realOverrideCount) return null

        val anchor = call.methodExpression.referenceNameElement ?: call.methodExpression
        return SinkDivergenceHit(anchor, interfaceClass.name ?: "<interface>", interfaceMethod.name)
    }

    private fun bodyLogsParameter(body: PsiCodeBlock?, paramName: String): Boolean {
        if (body == null) return false
        var found = false
        body.accept(object : JavaRecursiveElementWalkingVisitor() {
            override fun visitMethodCallExpression(call: PsiMethodCallExpression) {
                if (found) return
                super.visitMethodCallExpression(call)
                if (!LoggingCallSignals.isLoggingCall(call)) return
                if (call.argumentList.expressions.any { TaintReferenceMatcher.isTaintedReference(it, paramName) }) found = true
            }
        })
        return found
    }
}
