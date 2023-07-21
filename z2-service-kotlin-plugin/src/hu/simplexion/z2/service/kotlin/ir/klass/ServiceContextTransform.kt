/*
 * Copyright © 2022-2023, Simplexion, Hungary and contributors. Use of this source code is governed by the Apache 2.0 license.
 */
package hu.simplexion.z2.service.kotlin.ir.klass

import hu.simplexion.z2.service.kotlin.ir.ServicePluginContext
import hu.simplexion.z2.service.kotlin.ir.util.IrBuilder
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol

class ServiceContextTransform(
    override val pluginContext: ServicePluginContext,
    val function: IrSimpleFunction,
    val serviceContextGetter : IrSimpleFunctionSymbol
) : IrElementTransformerVoidWithContext(), IrBuilder {


    override fun visitCall(expression: IrCall): IrExpression {
        if (expression.symbol != serviceContextGetter) return expression
        return irGet(function.valueParameters.last())
    }

}
