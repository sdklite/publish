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
        def isApp = project.plugins.hasPlugin(AppPlugin);
        def isLib = project.plugins.hasPlugin(LibraryPlugin);

        if ((!isApp) && (!isLib)) {
            throw new GradleException("Not an Android project");
        }

        if (!project.plugins.hasPlugin('maven')) {
            project.apply plugin: 'maven'
        }

        if (!project.plugins.hasPlugin('signing')) {
            project.apply plugin: 'signing'
        }

        project.afterEvaluate {
            final def username = 'git config --get user.name'.execute().text.trim() ?: System.getProperty('user.name')
            final def scm_url = 'git config --get remote.origin.url'.execute().text.trim()
            final def scm_connection = "scm:git:${scm_url}"
            final def scm_dev_connection = scm_connection

            project.uploadArchives {
                repositories {
                    mavenDeployer {
                        beforeDeployment { MavenDeployment deployment ->
                            project.signing.signPom(deployment)
                        }

                        pom.project {
                            groupId project.group
                            artifactId project.hasProperty("ARTIFACT_ID") ? project.ARTIFACT_ID : project.name
                            version project.hasProperty("VERSION") ? project.VERSION : project.version
                            packaging project.hasProperty("PACKAGING") ? project.PACKAGING : 'aar'
                            name project.name
                            description project.description ?: ''
                            url scm_url

                            scm {
                                url project.hasProperty('SCM_URL') ? project.SCM_URL : scm_url
                                connection project.hasProperty('SCM_CONNECTION') ? project.SCM_CONNECTION : scm_connection
                                developerConnection project.hasProperty('SCM_DEV_CONNECTION') ? project.SCM_DEV_CONNECTION : scm_dev_connection
                            }

                            if (project.hasProperty('LICENSE_NAME') && project.hasProperty('LICENSE_URL') && project.hasProperty('LICENSE_DIST')) {
                                licenses {
                                    license {
                                        name project.LICENSE_NAME
                                        url project.LICENSE_URL
                                        distribution project.LICENSE_DIST
                                    }
                                }
                            }

                            developers {
                                developer {
                                    id username
                                    name username
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

            project.android.libraryVariants.all { variant ->
                def bundle = project.tasks.findByName("bundle${variant.name.capitalize()}")
                def testUnitTest = project.tasks.findByName("test${variant.name.capitalize()}UnitTest")

                def androidJavadocs = project.tasks.create("android${variant.name.capitalize()}Javadocs", Javadoc) {
                    source = project.files(variant.javaCompiler.source) + project.fileTree("${project.buildDir}${File.separator}generated${File.separator}source")
                    classpath += project.files(project.android.bootClasspath)
                    classpath += project.files(variant.javaCompiler.classpath)
                    excludes += [
                        'android/**/*.java',
                        'android/databinding/**/*.java',
                        '**/android/databinding/*Binding.java',
                        '**/BR.java',
                        '**/*_MembersInjector.java',
                        '**/Dagger*Component.java',
                        '**/*Module_*Factory.java',
                    ]
    
                    if (JavaVersion.current().isJava8Compatible()) {
                        project.allprojects { Project p ->
                            p.tasks.withType(Javadoc) {
                                options {
                                    addStringOption 'Xdoclint:none', '-quiet'
                                    addStringOption 'charset', 'UTF-8'
                                    addStringOption 'encoding', 'UTF-8'
                                }
                            }
                        }
                    }
                }

                def androidJavadocsJar = project.tasks.create("android${variant.name.capitalize()}JavadocsJar", Jar) {
                    classifier = 'javadoc'
                    from androidJavadocs.destinationDir
                }

                def androidSourcesJar = project.tasks.create("android${variant.name.capitalize()}SourcesJar", Jar) {
                    classifier = 'sources'
                    from project.files(variant.javaCompiler.source) + project.fileTree("${project.buildDir}${File.separator}generated${File.separator}source")
                }

                
                bundle.dependsOn testUnitTest
                bundle.mustRunAfter testUnitTest
                androidJavadocsJar.dependsOn androidJavadocs
                androidJavadocs.dependsOn variant.javaCompiler
            }

            project.artifacts {
                archives project.tasks.findByName("android${project.android.defaultPublishConfig.capitalize()}SourcesJar")
                archives project.tasks.findByName("android${project.android.defaultPublishConfig.capitalize()}JavadocsJar")
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
