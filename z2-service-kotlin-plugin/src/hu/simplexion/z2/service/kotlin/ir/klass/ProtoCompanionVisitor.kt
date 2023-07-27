/*
 * Copyright © 2022-2023, Simplexion, Hungary and contributors. Use of this source code is governed by the Apache 2.0 license.
 */
package hu.simplexion.z2.service.kotlin.ir.klass

import hu.simplexion.z2.service.kotlin.ir.ServicePluginContext
import hu.simplexion.z2.service.kotlin.ir.util.ProtoCache
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid

/**
 * Collect proto companions from the given module.
 */
class ProtoCompanionVisitor(
    val pluginContext: ServicePluginContext,
    val protoCache: ProtoCache
) : IrElementVisitorVoid {

    override fun visitClass(declaration: IrClass) {
        if (!declaration.isCompanion) return

        val parent = declaration.parent
        if (parent !is IrClass) return

        protoCache.add(parent.defaultType, declaration)
    }

}

