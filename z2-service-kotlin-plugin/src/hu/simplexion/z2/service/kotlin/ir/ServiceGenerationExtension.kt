/*
 * Copyright © 2022-2023, Simplexion, Hungary and contributors. Use of this source code is governed by the Apache 2.0 license.
 */
package hu.simplexion.z2.service.kotlin.ir

import hu.simplexion.z2.service.kotlin.ir.klass.ProtoCompanionVisitor
import hu.simplexion.z2.service.kotlin.ir.klass.ServiceModuleTransform
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.dump

internal class ServiceGenerationExtension : IrGenerationExtension {

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {

        ServicePluginContext(pluginContext).apply {

            debug("service") { "====  START  ==".padEnd(80, '=') }
            debug("service") { moduleFragment.dump() }

            moduleFragment.accept(ProtoCompanionVisitor(this, protoCache), null)
            moduleFragment.accept(ServiceModuleTransform(this), null)

            debug("service") { "====  END  ====".padEnd(80, '=') }
        }
    }
}

