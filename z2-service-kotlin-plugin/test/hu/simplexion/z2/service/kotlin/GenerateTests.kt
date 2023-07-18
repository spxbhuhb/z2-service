package hu.simplexion.z2.service.kotlin

import hu.simplexion.z2.service.kotlin.runners.AbstractAdhocTest
import hu.simplexion.z2.service.kotlin.runners.AbstractBoxTest
import hu.simplexion.z2.service.kotlin.runners.AbstractDiagnosticTest
import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5

fun main() {
    generateTestGroupSuiteWithJUnit5 {
        testGroup(testDataRoot = "testData", testsRoot = "test-gen") {

            testClass<AbstractDiagnosticTest> {
                model("diagnostics")
            }

            testClass<AbstractBoxTest> {
                model("box")
            }

            testClass<AbstractAdhocTest> {
                model("adhoc")
            }

        }
    }
}