/*
 * Copyright © 2022-2023, Simplexion, Hungary and contributors. Use of this source code is governed by the Apache 2.0 license.
 */
package hu.simplexion.z2.service.kotlin.ir

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.types.IrTypeSystemContextImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class ServicePluginContext(
    val irContext: IrPluginContext,
) {

    val serviceClass = SERVICE_CLASS.runtimeClass()

    val typeSystem = IrTypeSystemContextImpl(irContext.irBuiltIns)

    fun String.runtimeClass(pkg: String? = null) =
        checkNotNull(irContext.referenceClass(ClassId(FqName(pkg ?: RUNTIME_PACKAGE), Name.identifier(this)))) {
            "Missing runtime class. Maybe the gradle dependency on \"hu.simplexion.z2:z2-service-runtime\" is missing."
        }

}