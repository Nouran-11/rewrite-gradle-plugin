/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.gradle

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledIf
import org.junit.jupiter.api.io.TempDir
import org.openrewrite.Issue
import java.io.File

class ConfigurationCacheReproducerTest : GradleRunnerTest {

    @Test
    fun `check with rewriteDryRun has no configuration cache problems and reuses cache on second run`(@TempDir projectDir: File) {
        gradleProject(projectDir) {
            buildGradle(
                """
                plugins {
                    id("org.openrewrite.rewrite")
                }

                group = "org.example"
                version = "1.0-SNAPSHOT"

                repositories {
                    mavenLocal()
                    mavenCentral()
                    maven {
                       url = uri("https://central.sonatype.com/repository/maven-snapshots")
                    }
                }

                tasks.register("test") {
                    doLast {
                        println("Running unit tests...")
                        Thread.sleep(1_000L)
                        println("Unit tests succeeded")
                    }
                }

                tasks.register("integrationTest") {
                    doLast {
                        println("Running integration tests...")
                        Thread.sleep(1_000L)
                        println("Integration tests succeeded")
                    }
                }

                tasks.register("check") {
                    dependsOn("test", "integrationTest", "rewriteDryRun")
                }
                """
            )

            propertiesFile(
                "gradle.properties",
                """
                org.gradle.configuration-cache=true
                """.trimIndent()
            )

        }

        val first = runGradle(projectDir, "check", "--configuration-cache")
        assertThat(first.output)
            .contains("Running unit tests...")
            .contains("Integration tests succeeded")
        assertThat(first.output)
            .doesNotContain("Configuration cache problems found")
            .doesNotContain("problems were found storing the configuration cache")

        val second = runGradle(projectDir, "check", "--configuration-cache")
        assertThat(second.output)
            .contains("Reusing configuration cache").doesNotContain("problems were found storing the configuration cache")
    }
}


