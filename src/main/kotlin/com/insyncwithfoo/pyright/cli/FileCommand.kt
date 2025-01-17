package com.insyncwithfoo.pyright.cli

import com.insyncwithfoo.pyright.PyrightCommand
import com.insyncwithfoo.pyright.configuration.AllConfigurations
import com.insyncwithfoo.pyright.configuration.application.Locale
import com.insyncwithfoo.pyright.interpreterPath
import com.insyncwithfoo.pyright.onlyModuleOrNull
import com.insyncwithfoo.pyright.path
import com.insyncwithfoo.pyright.pyrightConfigurations
import com.insyncwithfoo.pyright.pyrightExecutable
import com.insyncwithfoo.pyright.toPathIfItExists
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.nio.file.Path


private object PathSerializer : KSerializer<Path> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Path", PrimitiveKind.STRING)
    
    override fun serialize(encoder: Encoder, value: Path) {
        encoder.encodeString(value.toString())
    }
    
    override fun deserialize(decoder: Decoder): Path {
        throw NotImplementedError("The deserializer should not be used")
    }
}


@Serializable
internal data class FileCommand(
    @Serializable(with = PathSerializer::class)
    val executable: Path,
    @Serializable(with = PathSerializer::class)
    val target: Path,
    val projectPath: String,
    val extraArguments: List<String>,
    override val environmentVariables: Map<String, String>
) : PyrightCommand() {
    
    override val workingDirectory by ::projectPath
    
    override val fragments: List<String>
        get() = listOf(
            executable.toString(),
            *extraArguments.toTypedArray(),
            target.toString()
        )
    
    companion object {
        
        internal fun create(
            configurations: AllConfigurations,
            executable: Path,
            target: Path,
            projectPath: Path,
            interpreterPath: Path
        ): FileCommand {
            val configurationFile = configurations.configurationFile
            
            val argumentForProject = configurationFile ?: projectPath
            val extraArguments: MutableList<String> = mutableListOf(
                "--outputjson",
                "--project", argumentForProject.toString(),
                "--pythonpath", interpreterPath.toString()
            )
            val environmentVariables = mutableMapOf<String, String>()
            
            if (configurations.minimumSeverityLevel != PyrightDiagnosticSeverity.INFORMATION) {
                extraArguments.add("--level")
                extraArguments.add(configurations.minimumSeverityLevel.name)
            }
            
            if (configurations.numberOfThreads != 0) {
                extraArguments.add("--threads")
                extraArguments.add(configurations.numberOfThreads.toString())
            }

            if (configurations.locale != Locale.DEFAULT) {
                environmentVariables.set("LC_ALL", configurations.locale.toString())
            }
            
            return FileCommand(executable, target, projectPath.toString(), extraArguments, environmentVariables)
        }
        
        private fun create(module: Module, file: VirtualFile): FileCommand? {
            val project = module.project
            val configurations = project.pyrightConfigurations
            
            val filePath = file.path.toPathIfItExists() ?: return null
            val projectPath = project.path ?: return null
            val executable = project.pyrightExecutable ?: return null
            val interpreterPath = module.interpreterPath ?: return null
            
            return create(configurations, executable, filePath, projectPath, interpreterPath)
        }
        
        fun create(project: Project, file: VirtualFile): FileCommand? {
            val module = project.onlyModuleOrNull ?: return null
            return create(module, file)
        }
        
        fun create(module: Module, file: PsiFile): FileCommand? {
            return create(module, file.virtualFile)
        }
        
    }
    
}
