/*
 * Copyright © 2022-2023, Simplexion, Hungary and contributors. Use of this source code is governed by the Apache 2.0 license.
 */
package hu.simplexion.z2.service.kotlin.ir.consumer

import hu.simplexion.z2.service.kotlin.ir.*
import hu.simplexion.z2.service.kotlin.ir.proto.ProtoDecoderIrBuilder
import hu.simplexion.z2.service.kotlin.ir.proto.ProtoMessageBuilderIrBuilder
import hu.simplexion.z2.service.kotlin.ir.util.FunctionSignature
import hu.simplexion.z2.service.kotlin.ir.util.IrClassBaseBuilder
import hu.simplexion.z2.service.kotlin.ir.util.ServiceBuilder
import org.jetbrains.kotlin.backend.common.ir.addDispatchReceiver
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.builders.declarations.addTypeParameter
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.name.Name

/**
 * Builds the consumer class.
 */
class ConsumerClassBuilder(
    override val pluginContext: ServicePluginContext,
    val interfaceClass : IrClass
) : IrElementVisitorVoid, ServiceBuilder, IrClassBaseBuilder {

    val consumerClass = buildClassBase {
        name = Name.identifier(interfaceClass.name.identifier + CONSUMER_POSTFIX)
        visibility = interfaceClass.visibility
    }.also {
        it.parent = interfaceClass.file
        it.superTypes = listOf(interfaceClass.defaultType)

        interfaceClass.file.addChild(it)
    }

    override val serviceFunctions = mutableListOf<IrSimpleFunctionSymbol>()

    override val serviceNames = mutableListOf<String>()

    override lateinit var serviceNameGetter: IrSimpleFunctionSymbol

    fun build() {
        collectServiceFunctions(consumerClass)

        addServiceNameProperty()

        for (serviceFunction in serviceFunctions) {
            addServiceFunction(serviceFunction.owner)
        }

        pluginContext.consumerCache.add(interfaceClass.defaultType, consumerClass)
    }

    private fun addServiceNameProperty() {
        val ancestor = pluginContext.serviceName
        consumerClass.addIrProperty(
            Name.identifier(SERVICE_NAME_PROPERTY),
            irBuiltIns.stringType,
            inIsVar = true,
            irConst(serviceNames.first()),
            overridden = listOf(ancestor.symbol)
        ).also {
            serviceNameGetter = it.getter!!.symbol
            it.getter!!.overriddenSymbols = listOf(ancestor.getter!!.symbol)
            it.setter!!.overriddenSymbols = listOf(ancestor.setter!!.symbol)
        }
    }

    private fun addServiceFunction(original: IrSimpleFunction) {
        irFactory.buildFun {
            name = original.name
            returnType = original.returnType
            modality = Modality.FINAL
        }.also { function ->

            function.isSuspend = true
            function.overriddenSymbols = listOf(original.symbol)
            function.parent = consumerClass

            function.addDispatchReceiver {
                type = consumerClass.defaultType
            }

            for (typeParameter in original.typeParameters) {
                function.addTypeParameter {
                    updateFrom(typeParameter)
                    superTypes += typeParameter.superTypes // FIXME use type parameter mapper somehow... check addTypeParameter source
                }
            }

            for (valueParameter in original.valueParameters) {
                function.addValueParameter {
                    name = valueParameter.name
                    updateFrom(valueParameter)
                }
            }

            function.body = DeclarationIrBuilder(irContext, function.symbol).irBlockBody {
                +irReturn(
                    irCall(
                        pluginContext.callFunction,
                        dispatchReceiver = getServiceTransport()
                    ).also {
                        it.type = function.returnType
                        it.putTypeArgument(CALL_TYPE_INDEX, function.returnType)
                        it.putValueArgument(CALL_SERVICE_NAME_INDEX, getServiceName(function))
                        it.putValueArgument(CALL_FUN_NAME_INDEX, irConst(FunctionSignature(pluginContext, function).signature()))
                        it.putValueArgument(CALL_PAYLOAD_INDEX, buildPayload(function))
                        it.putValueArgument(CALL_DECODER_INDEX, ProtoDecoderIrBuilder(pluginContext).getDecoder(function.returnType))
                    }
                )
            }

            consumerClass.declarations += function
        }
    }

    fun getServiceTransport(): IrCallImpl =
        irCall(
            pluginContext.defaultServiceCallTransport,
            IrStatementOrigin.GET_PROPERTY
        )

    fun buildPayload(function: IrSimpleFunction): IrExpression {
        val protoBuilder = ProtoMessageBuilderIrBuilder(pluginContext)

        for (valueParameter in function.valueParameters) {
            protoBuilder.next(valueParameter)
            check(protoBuilder.valid) {
                "unsupported type: ${valueParameter.symbol} function: ${function.symbol}\n" +
                    valueParameter.type.getClass()?.companionObject()?.dump() + "\n" +
                    valueParameter.type.getClass()?.dump()
            }
        }

        return irCall(
            pluginContext.protoBuilderPack,
            dispatchReceiver = protoBuilder.current
        )
    }

}
