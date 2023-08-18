/*
 * Copyright © 2022-2023, Simplexion, Hungary and contributors. Use of this source code is governed by the Apache 2.0 license.
 */
package hu.simplexion.z2.service.kotlin.ir

import hu.simplexion.z2.service.kotlin.ir.consumer.GetConsumerTransform
import hu.simplexion.z2.service.kotlin.ir.proto.ProtoCompanionVisitor
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

internal class ServiceGenerationExtension : IrGenerationExtension {

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {

        ServicePluginContext(pluginContext).apply {

//            debug("service") { "====  START  ==".padEnd(80, '=') }

            // order is important here
            moduleFragment.accept(ProtoCompanionVisitor(this, protoCache), null)
            moduleFragment.accept(ServiceModuleTransform(this), null)
            moduleFragment.accept(GetConsumerTransform(this), null)

//            debug("service") { moduleFragment.dump() }
//            debug("service") { "====  END  ====".padEnd(80, '=') }
        }
    }
}

