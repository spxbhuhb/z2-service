package hu.simplexion.z2.service.kotlin.ir.proto

import hu.simplexion.z2.service.kotlin.ir.PROTO_PACKAGE
import hu.simplexion.z2.service.kotlin.ir.ServicePluginContext
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class ProtoEnum(
    val pluginContext: ServicePluginContext
) {
    val irBuiltins = pluginContext.irContext.irBuiltIns

    val enumClass = pluginContext.irContext.irBuiltIns.enumClass

    val enumOrNullToOrdinal = with(pluginContext) {
        checkNotNull(
            irContext.referenceFunctions(CallableId(FqName(PROTO_PACKAGE),Name.identifier("enumOrNullToOrdinal"))).first().owner.symbol
        ) { "Missing enumOrNullToOrdinal, is the plugin added to gradle?" }
    }

    val ordinalToEnum = with(pluginContext) {
        checkNotNull(
            irContext.referenceFunctions(CallableId(FqName(PROTO_PACKAGE),Name.identifier("ordinalToEnum"))).first().owner.symbol
        ) { "Missing ordinalToEnum, is the plugin added to gradle?" }
    }

    val ordinalOrNullToEnum = with(pluginContext) {
        checkNotNull(
            irContext.referenceFunctions(CallableId(FqName(PROTO_PACKAGE),Name.identifier("ordinalOrNullToEnum"))).first().owner.symbol
        ) { "Missing ordinalOrNullToEnum, is the plugin added to gradle?" }
    }

    val ordinalListToEnum = with(pluginContext) {
        checkNotNull(
            irContext.referenceFunctions(CallableId(FqName(PROTO_PACKAGE),Name.identifier("ordinalListToEnum"))).first().owner.symbol
        ) { "Missing ordinalListToEnum, is the plugin added to gradle?" }
    }

    val ordinalListOrNullToEnum = with(pluginContext) {
        checkNotNull(
            irContext.referenceFunctions(CallableId(FqName(PROTO_PACKAGE),Name.identifier("ordinalListOrNullToEnum"))).first().owner.symbol
        ) { "Missing ordinalListOrNullToEnum, is the plugin added to gradle?" }
    }

    val enumListToOrdinals = with(pluginContext) {
        checkNotNull(
            irContext.referenceFunctions(CallableId(FqName(PROTO_PACKAGE),Name.identifier("enumListToOrdinals"))).first().owner.symbol
        ) { "Missing enumListToOrdinals, is the plugin added to gradle?" }
    }

    val enumListOrNullToOrdinals = with(pluginContext) {
        checkNotNull(
            irContext.referenceFunctions(CallableId(FqName(PROTO_PACKAGE),Name.identifier("enumListOrNullToOrdinals"))).first().owner.symbol
        ) { "Missing enumListOrNullToOrdinals, is the plugin added to gradle?" }
    }

    val enumGet =  with(pluginContext) {
        "EnumEntries".runtimeClass("kotlin.enums")
            .owner.functions.single { it.name.identifier == "get" && it.valueParameters.size == 1 && it.valueParameters[0].type == irBuiltins.intType }
    }

    val enumOne = with(pluginContext) { "ProtoOneEnum".runtimeClass(PROTO_PACKAGE) }
    val enumOneOrNull = with(pluginContext) { "ProtoOneEnumOrNull".runtimeClass(PROTO_PACKAGE) }
    val enumOneList = with(pluginContext) { "ProtoOneEnumList".runtimeClass(PROTO_PACKAGE) }
    val enumOneListOrNull = with(pluginContext) { "ProtoOneEnumListOrNull".runtimeClass(PROTO_PACKAGE) }

    val enumOneConstructor = enumOne.constructors.first()
    val enumOneOrNullConstructor = enumOneOrNull.constructors.first()
    val enumOneListConstructor = enumOneList.constructors.first()
    val enumOneListOrNullConstructor = enumOneListOrNull.constructors.first()

}