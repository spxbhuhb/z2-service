/*
 * Copyright (C) 2020 Brian Norman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hu.simplexion.z2.service.gradle

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory

/**
 * Settings for the Z2 Counter compiler plugin.
 */
open class ServiceGradleExtension(objects: ObjectFactory)

@Suppress("unused")
fun org.gradle.api.Project.z2service(configure: Action<ServiceGradleExtension>): Unit =
    (this as org.gradle.api.plugins.ExtensionAware).extensions.configure("z2service", configure)
