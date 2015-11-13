/*
 * Copyright 2015 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.gradle.project

import com.jfrog.bintray.gradle.BintrayExtension
import com.jfrog.bintray.gradle.BintrayPlugin
import com.jfrog.bintray.gradle.BintrayUploadTask
import com.netflix.spinnaker.gradle.ospackage.OspackageBintrayExtension
import com.netflix.spinnaker.gradle.ospackage.OspackageBintrayPublishPlugin
import nebula.plugin.info.scm.ScmInfoExtension
import nebula.plugin.netflixossproject.NetflixOssProjectPlugin
import nebula.plugin.netflixossproject.publishing.PublishingPlugin
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention

class SpinnakerProjectConventionsPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.plugins.apply(NetflixOssProjectPlugin)

        //workaround nebulaoss doing a find instead of a withType:
        project.tasks.withType(BintrayUploadTask) { BintrayUploadTask bintrayUpload ->
            bintrayUpload.doFirst {
                ScmInfoExtension scmInfo = project.extensions.findByType(ScmInfoExtension)
                // We have to change the task directly, since they already copied from the extension in an afterEvaluate

                if (scmInfo) {
                    // Assuming scmInfo.origin is something like git@github.com:netflix/project.git
                    bintrayUpload.packageName = PublishingPlugin.calculateRepoFromOrigin(scmInfo.origin) ?: project.rootProject.name

                    def url = PublishingPlugin.calculateUrlFromOrigin(scmInfo.origin)
                    bintrayUpload.packageWebsiteUrl = url
                    bintrayUpload.packageIssueTrackerUrl = "${url}/issues"
                    bintrayUpload.packageVcsUrl = "${url}.git"
                }
            }
        }

        project.plugins.withType(JavaPlugin) {
            JavaPluginConvention convention = project.convention.getPlugin(JavaPluginConvention)
            convention.sourceCompatibility = JavaVersion.VERSION_1_8
            convention.targetCompatibility = JavaVersion.VERSION_1_8
        }

        project.plugins.withType(BintrayPlugin) {
            BintrayExtension bintray = (BintrayExtension) project.extensions.getByName('bintray')

            bintray.pkg.userOrg = 'spinnaker'
            bintray.pkg.repo = 'spinnaker'
            bintray.pkg.labels = ['Spinnaker', 'Netflix']
        }
/*
        project.plugins.withType(OspackageBintrayPublishPlugin) {
            OspackageBintrayExtension bintrayPackage = (OspackageBintrayExtension) project.extensions.getByName('bintrayPackage')
            bintrayPackage.packageRepo = 'ospackages'
        }
*/
        project.repositories.jcenter()
        project.repositories.maven { MavenArtifactRepository repo ->
            repo.name = 'Bintray Spinnaker repo'
            repo.url = 'https://dl.bintray.com/spinnaker/spinnaker'
        }
    }
}
