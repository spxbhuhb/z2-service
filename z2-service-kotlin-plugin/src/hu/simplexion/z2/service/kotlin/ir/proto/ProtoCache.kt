package hu.simplexion.z2.service.kotlin.ir.proto

import hu.simplexion.z2.service.kotlin.ir.*
import org.jetbrains.kotlin.backend.jvm.functionByName
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.util.companionObject
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.isSubclassOf
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class ProtoCache(
    val pluginContext: ServicePluginContext
) {

    val protoMessageBuilderClass = with(pluginContext) { PROTO_MESSAGE_BUILDER_CLASS.runtimeClass(PROTO_PACKAGE) }
    val protoMessageClass = with(pluginContext) { PROTO_MESSAGE_CLASS.runtimeClass(PROTO_PACKAGE) }
    val protoEncoderClass = with(pluginContext) { PROTO_ENCODER_CLASS.runtimeClass(PROTO_PACKAGE).owner }
    val protoDecoderClass = with(pluginContext) { PROTO_DECODER_CLASS.runtimeClass(PROTO_PACKAGE).owner }

    val protoMessageBuilderConstructor = protoMessageBuilderClass.constructors.first()
    val protoMessageBuilderPack = protoMessageBuilderClass.functionByName(PROTO_MESSAGE_BUILDER_PACK)

    fun type(packageName: String, className: String) =
        klass(packageName, className).defaultType

    fun klass(packageName: String, className: String) =
        checkNotNull(pluginContext.irContext.referenceClass(ClassId(FqName(packageName), Name.identifier(className))))
        { "Missing class: $packageName.$className" }.owner

    class BuiltinEntry(
        val decode: IrSimpleFunctionSymbol,
        val decodeOrNull: IrSimpleFunctionSymbol,
        val encode: IrSimpleFunctionSymbol,
        val encodeOrNull: IrSimpleFunctionSymbol,
        val one: IrClassSymbol,
        val oneOrNull: IrClassSymbol,
        val decodeList: IrSimpleFunctionSymbol,
        val decodeListOrNull: IrSimpleFunctionSymbol,
        val encodeList: IrSimpleFunctionSymbol,
        val encodeListOrNull: IrSimpleFunctionSymbol,
        val oneList: IrClassSymbol,
        val oneListOrNull: IrClassSymbol
    )

    fun builtIn(type: String): BuiltinEntry {
        return with(pluginContext) {
            val lcType = type.take(1).lowercase() + type.drop(1)
            BuiltinEntry(
                decode = protoMessageClass.functionByName(lcType),
                decodeOrNull = protoMessageClass.functionByName("${lcType}OrNull"),
                encode = protoMessageBuilderClass.functionByName(lcType),
                encodeOrNull = protoMessageBuilderClass.functionByName("${lcType}OrNull"),
                one = "ProtoOne$type".runtimeClass(PROTO_PACKAGE),
                oneOrNull = "ProtoOne${type}OrNull".runtimeClass(PROTO_PACKAGE),
                decodeList = protoMessageClass.functionByName("${lcType}List"),
                decodeListOrNull = protoMessageClass.functionByName("${lcType}ListOrNull"),
                encodeList = protoMessageBuilderClass.functionByName("${lcType}List"),
                encodeListOrNull = protoMessageBuilderClass.functionByName("${lcType}ListOrNull"),
                oneList = "ProtoOne${type}List".runtimeClass(PROTO_PACKAGE),
                oneListOrNull = "ProtoOne${type}ListOrNull".runtimeClass(PROTO_PACKAGE)
            )
        }
    }

    val protoOneUnit = with(pluginContext) { PROTO_ONE_UNIT.runtimeClass(PROTO_PACKAGE) }
    val protoInt = builtIn("Int")

    val protoPrimitives = mapOf(
        IdSignatureValues._boolean to builtIn("Boolean"),
        IdSignatureValues._int to protoInt,
        IdSignatureValues._long to builtIn("Long"),
        getPublicSignature(StandardNames.BUILT_INS_PACKAGE_FQ_NAME, "String") to builtIn("String"),
        getPublicSignature(StandardNames.BUILT_INS_PACKAGE_FQ_NAME, "ByteArray") to builtIn("ByteArray"),
    )

    val protoUuid = builtIn("Uuid")
    val protoInstance = builtIn("Instance")

    val protoOneInstanceConstructor = protoInstance.one.constructors.first()
    val protoOneInstanceOrNullConstructor = protoInstance.oneOrNull.constructors.first()
    val protoOneInstanceListConstructor = protoInstance.oneList.constructors.first()
    val protoOneInstanceListOrNullConstructor = protoInstance.oneListOrNull.constructors.first()

    val protoCoders = mutableMapOf<IrType, IrClass?>(
        type(TIME_PACKAGE, TIME_DURATION) to klass(PROTO_PACKAGE, PROTO_DURATION),
        type(DATETIME_PACKAGE, DATETIME_INSTANT) to klass(PROTO_PACKAGE, PROTO_INSTANT),
        type(DATETIME_PACKAGE, DATETIME_LOCAL_DATE) to klass(PROTO_PACKAGE, PROTO_LOCAL_DATE),
        type(DATETIME_PACKAGE, DATETIME_LOCAL_DATE_TIME) to klass(PROTO_PACKAGE, PROTO_LOCAL_DATE_TIME)
    )

    fun primitive(type: IrType): BuiltinEntry? {
        if (type.isSubtypeOfClass(pluginContext.uuidType)) return protoUuid
        val classifier = type.classifierOrNull ?: return null
        return protoPrimitives[classifier.signature]
    }

    fun list(type: IrType): BuiltinEntry? {
        if (! type.isSubtypeOfClass(pluginContext.listClass)) return null

        // FIXME hackish list item type retrieval
        val itemType = (type as IrSimpleTypeImpl).arguments.first() as IrType
        check(! itemType.isNullable()) { "nullable items in lists are not supported" }

        return primitive(itemType)
    }

    operator fun get(type: IrType) =
        protoCoders.getOrPut(type) { add(type) }

    fun add(type: IrType): IrClass? {

        val companion = type.getClass()?.companionObject() ?: tryLoadCompanion(type) ?: return null

        if (! companion.isSubclassOf(protoEncoderClass)) return null
        if (! companion.isSubclassOf(protoDecoderClass)) return null

        pluginContext.debug("service") { "protoCache add $type $companion" }
        return companion
    }

    fun add(type: IrType, companion: IrClass) {
        pluginContext.debug("service") { "protoCache add $type $companion" }
        protoCoders[type] = companion
    }

    private fun tryLoadCompanion(type: IrType): IrClass? {
        val typeFqName = type.classFqName ?: return null
        val classId = ClassId(typeFqName.parent(), typeFqName.shortName()).createNestedClassId(Name.identifier(COMPANION_OBJECT_NAME))
        return pluginContext.irContext.referenceClass(classId)?.owner
    }

}