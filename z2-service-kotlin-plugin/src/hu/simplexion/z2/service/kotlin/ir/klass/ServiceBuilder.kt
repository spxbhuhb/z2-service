package hu.simplexion.z2.service.kotlin.ir.klass

import hu.simplexion.z2.service.kotlin.ir.util.IrBuilder
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isSubtypeOfClass

interface ServiceBuilder : IrBuilder {

    var transformedClass: IrClass

    var serviceNameGetter: IrSimpleFunctionSymbol

    val serviceFunctions: MutableList<IrSimpleFunctionSymbol>

    fun collectServiceFunctions() {
        for (superType in transformedClass.superTypes) {
            if (superType.isSubtypeOfClass(pluginContext.serviceClass)) {
                serviceFunctions += pluginContext.serviceFunctionCache[superType]
            }
        }
    }

    fun IrFunction.asServiceFun(): IrSimpleFunction? {
        if (this !is IrSimpleFunction) return null
        for (overriddenSymbol in this.overriddenSymbols) {
            if (overriddenSymbol in serviceFunctions) return this
        }
        return null
    }

    fun getServiceName(function: IrSimpleFunction): IrCallImpl =
        irCall(
            serviceNameGetter,
            IrStatementOrigin.GET_PROPERTY,
            dispatchReceiver = irGet(checkNotNull(function.dispatchReceiverParameter))
        )

    fun IrType.protoBuilderFun() = when (this) {
        irBuiltIns.booleanType -> pluginContext.protoBuilderBoolean
        irBuiltIns.intType -> pluginContext.protoBuilderInt
        irBuiltIns.longType -> pluginContext.protoBuilderLong
        irBuiltIns.stringType -> pluginContext.protoBuilderString
        irBuiltIns.byteArray -> pluginContext.protoBuilderByteArray
        pluginContext.uuidType -> pluginContext.protoBuilderUuid
        else -> null
    }

    fun IrType.protoMessageFun() = when (this) {
        irBuiltIns.booleanType -> pluginContext.protoMessageBoolean
        irBuiltIns.intType -> pluginContext.protoMessageInt
        irBuiltIns.longType -> pluginContext.protoMessageLong
        irBuiltIns.stringType -> pluginContext.protoMessageString
        irBuiltIns.byteArray -> pluginContext.protoMessageByteArray
        pluginContext.uuidType -> pluginContext.protoMessageUuid
        else -> null
    }

}