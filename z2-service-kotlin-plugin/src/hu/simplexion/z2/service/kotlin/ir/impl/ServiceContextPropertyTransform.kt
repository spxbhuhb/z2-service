/*
 * Copyright © 2022-2023, Simplexion, Hungary and contributors. Use of this source code is governed by the Apache 2.0 license.
 */
package hu.simplexion.z2.service.kotlin.ir.impl

import hu.simplexion.z2.service.kotlin.ir.ServicePluginContext
import hu.simplexion.z2.service.kotlin.ir.util.IrBuilder
import hu.simplexion.z2.service.kotlin.ir.util.ServiceBuilder
import org.jetbrains.kotlin.backend.common.ir.addDispatchReceiver
import org.jetbrains.kotlin.backend.common.lower.AnnotationImplementationTransformer
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.defaultType

class ServiceContextPropertyTransform(
    override val pluginContext: ServicePluginContext,
    val implClassTransform: ImplClassTransform,
    var property: IrProperty
) : IrBuilder {

    fun build() {
        if (! property.isFakeOverride) return

        property.isFakeOverride = false
        property.origin = IrDeclarationOrigin.DEFINED

        property.backingField = irFactory.buildField {
            name = property.name
            type = pluginContext.serviceContextType.makeNullable()
            origin = IrDeclarationOrigin.PROPERTY_BACKING_FIELD
            visibility = DescriptorVisibilities.PRIVATE
        }.apply {
            parent = property.parent
            initializer = buildInitializer()
            correspondingPropertySymbol = property.symbol
        }

        transformGetter(implClassTransform.transformedClass, property.getter!!, property.backingField!!)
    }

    fun buildInitializer() : IrExpressionBody =
        irFactory.createExpressionBody(irGet(implClassTransform.constructor.valueParameters.first()))

}