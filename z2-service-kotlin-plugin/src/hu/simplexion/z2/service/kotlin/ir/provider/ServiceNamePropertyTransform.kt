/*
 * Copyright © 2022-2023, Simplexion, Hungary and contributors. Use of this source code is governed by the Apache 2.0 license.
 */
package hu.simplexion.z2.service.kotlin.ir.provider

import hu.simplexion.z2.service.kotlin.ir.ServicePluginContext
import hu.simplexion.z2.service.kotlin.ir.util.IrBuilder
import hu.simplexion.z2.service.kotlin.ir.util.ServiceBuilder
import org.jetbrains.kotlin.backend.common.ir.addDispatchReceiver
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetFieldImpl
import org.jetbrains.kotlin.ir.util.defaultType

class ServiceNamePropertyTransform(
    override val pluginContext: ServicePluginContext,
    val serviceBuilder: ServiceBuilder,
    val transformedClass: IrClass,
    var property: IrProperty
) : IrBuilder {

    fun build() {
        if (! property.isFakeOverride) return

        require(serviceBuilder.serviceNames.isNotEmpty()) { "missing service interface (probably ': Service' is missing)" }
        require(serviceBuilder.serviceNames.size != 1) { "you have to set `serviceName` manually when more than one service is implemented" }

        property.isFakeOverride = false
        property.origin = IrDeclarationOrigin.DEFINED

        property.backingField = irFactory.buildField {
            name = property.name
            type = irBuiltIns.stringType
            origin = IrDeclarationOrigin.PROPERTY_BACKING_FIELD
            visibility = DescriptorVisibilities.PRIVATE
        }.apply {
            parent = property.parent
            initializer = irFactory.createExpressionBody(irConst(serviceBuilder.serviceNames.first()))
            correspondingPropertySymbol = property.symbol
        }

        transformGetter(property.getter!!, property.backingField!!)
        transformSetter(property.setter!!, property.backingField!!)
    }

    fun transformGetter(func: IrSimpleFunction, field: IrField) {
        func.origin = IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
        func.isFakeOverride = false

        func.addDispatchReceiver {
            type = transformedClass.defaultType
        }

        func.body = DeclarationIrBuilder(irContext, func.symbol).irBlockBody {
            + irReturn(
                IrGetFieldImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    field.symbol,
                    field.type,
                    IrGetValueImpl(
                        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                        func.dispatchReceiverParameter !!.type,
                        func.dispatchReceiverParameter !!.symbol
                    )
                )
            )
        }
    }

    fun transformSetter(func: IrSimpleFunction, field: IrField) {
        func.origin = IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
        func.isFakeOverride = false

        func.addDispatchReceiver {
            type = transformedClass.defaultType
        }

        func.body = DeclarationIrBuilder(irContext, func.symbol).irBlockBody {
            + IrSetFieldImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                field.symbol,
                irBuiltIns.unitType
            ).also {
                it.receiver = irGet(func.dispatchReceiverParameter!!)
                it.value = irGet(func.valueParameters.first())
            }
        }
    }
}