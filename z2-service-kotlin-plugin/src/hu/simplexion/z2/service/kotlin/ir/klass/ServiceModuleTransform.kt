/*
 * Copyright © 2022-2023, Simplexion, Hungary and contributors. Use of this source code is governed by the Apache 2.0 license.
 */
package hu.simplexion.z2.service.kotlin.ir.klass

import hu.simplexion.z2.service.kotlin.ir.ServicePluginContext
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass

class ServiceModuleTransform(
    private val pluginContext: ServicePluginContext
) : IrElementTransformerVoidWithContext() {

    override fun visitClassNew(declaration: IrClass): IrStatement {

        if (declaration.superTypes.contains(pluginContext.serviceConsumerType)) {
            return declaration.accept(ServiceConsumerClassTransform(pluginContext), null) as IrStatement
        }

//        if (declaration.superTypes.contains(pluginContext.serviceProviderType)) {
//            return declaration.accept(ServiceProviderClassTransform(pluginContext), null) as IrStatement
//        }

        return declaration
    }

}
