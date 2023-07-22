package hu.simplexion.z2.service.kotlin.runners

import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.handlers.JvmBoxRunner
import org.jetbrains.kotlin.test.backend.ir.JvmIrBackendFacade
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.fir2IrStep
import org.jetbrains.kotlin.test.builders.irHandlersStep
import org.jetbrains.kotlin.test.builders.jvmArtifactsHandlersStep
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.runners.RunnerWithTargetBackendForTestGeneratorMarker
import org.jetbrains.kotlin.test.services.RuntimeClasspathProvider
import org.jetbrains.kotlin.test.services.TestServices
import java.io.File

/*
 * Containers of different directives, which can be used in tests:
 * - ModuleStructureDirectives
 * - LanguageSettingsDirectives
 * - DiagnosticsDirectives
 * - CodegenTestDirectives
 *
 * All of them are located in `org.jetbrains.kotlin.test.directives` package
 */
open class AbstractBoxTest : BaseTestRunner(), RunnerWithTargetBackendForTestGeneratorMarker {
    override val targetBackend: TargetBackend
        get() = TargetBackend.JVM_IR

    override fun TestConfigurationBuilder.configuration() {
        globalDefaults {
            targetBackend = TargetBackend.JVM_IR
            targetPlatform = JvmPlatforms.defaultJvmPlatform
            dependencyKind = DependencyKind.Binary
        }

        useCustomRuntimeClasspathProviders(AbstractBoxTest::ServiceRuntimeClassPathProvider)

        configureFirParser(FirParser.Psi)

//        defaultDirectives {
//            +DUMP_IR
//        }

        commonFirWithPluginFrontendConfiguration(dumpFir = false)
        fir2IrStep()
        irHandlersStep {
//            useHandlers(
//                ::IrTextDumpHandler,
//                ::IrTreeVerifierHandler,
//            )
        }
        facadeStep(::JvmIrBackendFacade)

        jvmArtifactsHandlersStep {
            useHandlers(::JvmBoxRunner)
        }

        useAfterAnalysisCheckers(::BlackBoxCodegenSuppressor)
    }

    class ServiceRuntimeClassPathProvider(testServices: TestServices) : RuntimeClasspathProvider(testServices) {
        override fun runtimeClassPaths(module: TestModule): List<File> = runtimeClassPath()
    }
}