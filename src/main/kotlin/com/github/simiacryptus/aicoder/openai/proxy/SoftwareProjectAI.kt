package com.github.simiacryptus.aicoder.openai.proxy

interface SoftwareProjectAI {
    fun newProject(description: String): Project

    data class Project(
        val name: String = "",
        val description: String = "",
        val language: String = "",
        val libraries: List<String> = listOf(),
        val buildTools: List<String> = listOf(),
    )

    fun getProjectStatements(project: Project): ProjectStatements

    data class ProjectStatements(
        val assumptions: List<String> = listOf(),
        val requirements: List<String> = listOf(),
        val risks: List<String> = listOf(),
    )

    fun buildProjectDesign(project: Project, requirements: ProjectStatements): ProjectDesign

    data class ProjectDesign(
        val designDetails: List<String> = listOf(),
        val tests: List<String> = listOf(),
    )

    fun buildProjectFileSpecifications(project: Project, requirements: ProjectStatements, design: ProjectDesign, recursive: Boolean = true): FileList

    data class FileList(
        val files: List<FileSpecification> = listOf(),
    )

    data class FileSpecification(
        val location: FilePath = FilePath(),
        val description: String = "",
        val requires: List<FilePath> = listOf(),
        val publicProperties: List<String> = listOf(),
        val publicMethodSignatures: List<String> = listOf(),
    )

    data class FilePath(
        val path: String = "",
        val name: String = "",
        val extension: String = "",
    ) {
        override fun toString(): String {
            return "${path.trimEnd('/')}/$name.$extension"
        }
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as FilePath
            if (path != other.path) return false
            if (name != other.name) return false
            if (extension != other.extension) return false
            return true
        }
        override fun hashCode(): Int {
            var result = path.hashCode()
            result = 31 * result + name.hashCode()
            result = 31 * result + extension.hashCode()
            return result
        }

    }

    fun implement(project: Project, imports: List<FilePath>, specification: FileSpecification): SourceCode

    data class SourceCode(
        val language: String = "",
        val code: String = "",
    )

}