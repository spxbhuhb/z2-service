package hu.simplexion.z2.service.kotlin.ir.util

import hu.simplexion.z2.service.kotlin.ir.SERVICE_IMPL_FQ_NAME
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.isSubtypeOfClass

interface ServiceBuilder : IrBuilder {

    var serviceNameGetter: IrSimpleFunctionSymbol

    val overiddenServiceFunctions: MutableList<IrSimpleFunctionSymbol>

    val serviceNames : MutableList<String>

    fun collectServiceFunctions(klass : IrClass) {
        for (superType in klass.superTypes) {
            if (superType.isSubtypeOfClass(pluginContext.serviceClass) && superType.classFqName != SERVICE_IMPL_FQ_NAME) {
                serviceNames += superType.classFqName!!.asString()
                overiddenServiceFunctions += pluginContext.serviceFunctionCache[superType]
            }
        }
    }

    fun IrFunction.asServiceFun(): IrSimpleFunction? {
        if (this !is IrSimpleFunction) return null
        for (overriddenSymbol in this.overriddenSymbols) {
            if (overriddenSymbol in overiddenServiceFunctions) return this
        }
        return null
    }

    fun getServiceName(function: IrSimpleFunction): IrCallImpl =
        irCall(
            serviceNameGetter,
            IrStatementOrigin.GET_PROPERTY,
            dispatchReceiver = irGet(checkNotNull(function.dispatchReceiverParameter))
        )


}