package com.sdklite.publishing.maven

import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.maven.MavenDeployment
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin

/**
 * The gradle plugin for publishing Android library to maven repository
 */
public class MavenPublishingPlugin implements Plugin<Project> {

    @Override
    void apply(final Project project) {
        project.afterEvaluate {
            def isApp = project.plugins.hasPlugin(AppPlugin);
            def isLib = project.plugins.hasPlugin(LibraryPlugin);

            if ((!isApp) && (!isLib)) {
                throw new GradleException("Not an Android project");
            }

            project.apply plugin: 'maven'
            project.apply plugin: 'signing'

            project.uploadArchives {
                repositories {
                    mavenDeployer {
                        beforeDeployment { MavenDeployment deployment ->
                            project.signing.signPom(deployment)
                        }

                        pom.project {
                            groupId project.group
                            artifactId project.hasProperty("artifactId") ? project.artifactId : project.name
                            version project.version
                            packaging project.hasProperty("packaging") ? project.packaging : 'aar'
                            name project.name
                            description project.description ?: ''
                            url project.hasProperty('url') ? project.url : project.hasProperty('SCM_URL') ? project.SCM_URL : ''

                            scm {
                                url project.hasProperty('SCM_URL') ? project.SCM_URL : ''
                                connection project.hasProperty('SCM_CONNECTION') ? project.SCM_CONNECTION : ''
                                developerConnection project.hasProperty('SCM_DEV_CONNECTION') ? project.SCM_DEV_CONNECTION : ''
                            }

                            licenses {
                                license {
                                    name project.hasProperty('LICENSE_NAME') ? project.LICENSE_NAME : ''
                                    url project.hasProperty('LICENSE_URL') ? project.LICENSE_URL : ''
                                    distribution project.hasProperty('LICENSE_DIST') ? project.LICENSE_DIST : ''
                                }
                            }

                            developers {
                                developer {
                                    id project.hasProperty('DEVELOPER_ID') ? project.DEVELOPER_ID : System.getProperty('user.name')
                                    name project.hasProperty('DEVELOPER_NAME') ? project.DEVELOPER_NAME : System.getProperty('user.name')
                                }
                            }
                        }

                        repository(url: getReleaseRepositoryUrl(project)) {
                            authentication(userName: getRepositoryUsername(project), password: getRepositoryPassword(project))
                        }
                        snapshotRepository(url: getSnapshotRepositoryUrl(project)) {
                            authentication(userName: getRepositoryUsername(project), password: getRepositoryPassword(project))
                        }
                    }
                }
            }

            project.signing {
                required { project.version.contains('SNAPSHOT') && project.gradle.taskGraph.hasTask('uploadArchives') }
                sign project.configurations.archives
            }

            def androidJavadocs = project.tasks.create('androidJavadocs', Javadoc) {
                source = project.android.sourceSets.main.java.srcDirs
                classpath += project.files(project.android.bootClasspath.join(File.separator))

                if (JavaVersion.current().isJava8Compatible()) {
                    project.allprojects { Project p ->
                        p.tasks.withType(Javadoc) {
                            options.addStringOption('Xdoclint:none', '-quiet')
                        }
                    }
                }
            }

            def androidJavadocsJar = project.tasks.create('androidJavadocsJar', Jar) {
                classifier = 'javadoc'
                from androidJavadocs.destinationDir
            }

            def androidSourcesJar = project.tasks.create('androidSourcesJar', Jar) {
                classifier = 'sources'
                from project.android.sourceSets.main.java.sourceFiles
            }

            androidJavadocsJar.dependsOn(androidJavadocs)

            project.artifacts {
                archives androidSourcesJar
                archives androidJavadocsJar
            }
        }
    }

    def getReleaseRepositoryUrl(final Project project) {
        return project.hasProperty('RELEASE_REPOSITORY_URL')
                ? project.RELEASE_REPOSITORY_URL
                : "file://${System.getProperty('user.home')}${File.separator}.m2${File.separator}repository"
    }
    
    def getSnapshotRepositoryUrl(final Project project) {
        return project.hasProperty('SNAPSHOT_REPOSITORY_URL')
                ? project.SNAPSHOT_REPOSITORY_URL
                : "file://${System.getProperty('user.home')}${File.separator}.m2${File.separator}repository"
    }
    
    def getRepositoryUsername(final Project project) {
        return project.hasProperty('REPOSITORY_USERNAME') ? project.REPOSITORY_USERNAME : ""
    }
    
    def getRepositoryPassword(final Project project) {
        return project.hasProperty('REPOSITORY_PASSWORD') ? project.REPOSITORY_PASSWORD : ""
    }
    
}
