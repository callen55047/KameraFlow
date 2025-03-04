package cocoapods

import environment.IModuleArtifact
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class BuildPodSpec : DefaultTask() {
    @Input
    lateinit var url: String

    @Input
    lateinit var module: IModuleArtifact

    @Input
    lateinit var version: String

    @InputDirectory
    lateinit var outputDir: File

    @TaskAction
    fun generatePodspec() {
        val podspecContent =
            """
            Pod::Spec.new do |s|
              s.name = '${module.name}'
              s.version = '$version'
              s.swift_versions = '4.0'
              s.author = 'Callen Egan'
              s.license = { :text => 'MIT License' }
              s.homepage = 'https://www.linkedin.com/in/callen-egan-2983b218b/'
              s.source = { :http => '$url/$version/${module.name}.zip', :flatten => true }
              s.summary = 'KameraFlow ${module.name} iOS SDK'
              s.ios.vendored_frameworks = '${module.name}.xcframework'
              s.ios.deployment_target = '14.0'
              s.pod_target_xcconfig = { 'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'x86_64' }
              s.user_target_xcconfig = { 'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'x86_64' }
            end
            """.trimIndent()

        val podspecFile = File(outputDir, "${module.name}.podspec")
        podspecFile.writeText(podspecContent)

        println("${module.name}.podspec file generated: ${podspecFile.absolutePath}")
    }
}
