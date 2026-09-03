package dev.gaphunter.interfacesinkdivergencecompanion.model

import com.intellij.psi.PsiElement

/** A confirmed sink-behavior divergence: a call through [interfaceName].[methodName] (the interface type) passes a tainted argument that SOME real implementations log unsanitized and at least one other real implementation never does. */
data class SinkDivergenceHit(val anchor: PsiElement, val interfaceName: String, val methodName: String)
