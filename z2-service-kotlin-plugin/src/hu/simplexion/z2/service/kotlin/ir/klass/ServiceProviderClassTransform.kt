/*
 * Copyright © 2022-2023, Simplexion, Hungary and contributors. Use of this source code is governed by the Apache 2.0 license.
 */
package hu.simplexion.z2.service.kotlin.ir.klass

import hu.simplexion.z2.service.kotlin.ir.ServicePluginContext
import hu.simplexion.z2.service.kotlin.ir.util.IrBuilder
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass

class ServiceProviderClassTransform(
    override val pluginContext: ServicePluginContext
) : IrElementTransformerVoidWithContext(), IrBuilder {

    override fun visitClassNew(declaration: IrClass): IrStatement {

        return declaration
    }

}
