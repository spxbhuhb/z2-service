/*
 * Copyright © 2022-2023, Simplexion, Hungary and contributors. Use of this source code is governed by the Apache 2.0 license.
 */
package hu.simplexion.z2.service.kotlin.ir.consumer

import hu.simplexion.z2.service.kotlin.ir.ServicePluginContext
import hu.simplexion.z2.service.kotlin.ir.util.IrBuilder
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.constructors

class GetConsumerTransform(
    override val pluginContext: ServicePluginContext
) : IrElementTransformerVoidWithContext(), IrBuilder {

    override fun visitCall(expression: IrCall): IrExpression {
        if (expression.symbol == pluginContext.getService) {
            expression.putValueArgument(0, newInstance(checkNotNull(expression.getTypeArgument(0))))
        }

        return super.visitCall(expression)
    }

    fun newInstance(type : IrType) : IrExpression {

        val consumerClass = pluginContext.consumerCache[type]

        return IrConstructorCallImpl(
            SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
            type,
            consumerClass.constructors.first { it.isPrimary }.symbol,
            0, 0, 0
        )
    }

}