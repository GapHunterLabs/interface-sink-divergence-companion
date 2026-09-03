package dev.gaphunter.interfacesinkdivergencecompanion.inspection

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiFile
import dev.gaphunter.interfacesinkdivergencecompanion.detect.InterfaceSinkDivergenceFinder
import dev.gaphunter.interfacesinkdivergencecompanion.model.SinkDivergenceHit
import dev.gaphunter.interfacesinkdivergencecompanion.review.ReviewPrompt

/** Flags a call through an interface-typed reference where real implementations diverge on whether they log the tainted argument unsanitized -- see [InterfaceSinkDivergenceFinder]. */
class InterfaceSinkDivergenceInspection : LocalInspectionTool() {

    companion object {
        const val MAX_FILE_LENGTH = 500_000
    }

    override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
        if (file.text.length > MAX_FILE_LENGTH) return null

        val hits = InterfaceSinkDivergenceFinder.findAll(file)
        if (hits.isEmpty()) return null

        val problems = hits.map { hit ->
            manager.createProblemDescriptor(
                hit.anchor,
                messageFor(hit),
                isOnTheFly,
                emptyArray(),
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            )
        }

        val path = file.virtualFile?.path
        if (path != null) {
            for (hit in hits) {
                val lineNumber = file.viewProvider.document?.getLineNumber(hit.anchor.textRange.startOffset) ?: -1
                ReviewPrompt.recordHit(file.project, "$path:$lineNumber:${hit.methodName}")
            }
        }

        return problems.toTypedArray()
    }

    private fun messageFor(hit: SinkDivergenceHit): String =
        "This call to ${hit.interfaceName}.${hit.methodName}() passes tainted input that SOME real implementations log unsanitized " +
            "(CWE-117/CWE-532) while at least one other real implementation never does -- the interface type alone never guarantees " +
            "which behavior you get once dependency injection swaps implementations"
}
