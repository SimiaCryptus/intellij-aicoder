package com.github.simiacryptus.aicoder.proxy

import org.junit.Test

/**
 * Define the objectives: Clearly outline the educational objectives, learning goals, and values you want students to achieve by the end of their K-12 education. Consider local, regional, or national education standards, and align your goals with these benchmarks.
 *
 * Identify the subjects and scope: Determine the subjects that will be covered at each grade level. Consider core subjects (math, language arts, science, and social studies), as well as electives and enrichment courses (arts, physical education, technology, etc.). Ensure the curriculum covers the necessary knowledge and skills for each subject.
 *
 * Sequence and progression: Organize the content and skills within each subject in a logical and sequential manner, allowing for scaffolding and building on previous knowledge. Create a scope and sequence document to outline the learning objectives and content for each grade level.
 *
 * Integration and interdisciplinary connections: Identify opportunities for integrating subjects and promoting interdisciplinary learning. This can help students make connections between different subjects and apply their knowledge in a more meaningful way.
 *
 * Teaching methodologies: Select diverse and evidence-based teaching strategies that cater to various learning styles and promote active, student-centered learning. Consider including project-based learning, inquiry-based learning, cooperative learning, and other approaches.
 *
 * Assessment and evaluation: Develop a variety of assessment methods to measure student progress and evaluate the effectiveness of the curriculum. Use both formative (ongoing) and summative (end of unit/term) assessments, including performance-based tasks, quizzes, tests, and projects.
 *
 * Inclusivity and accessibility: Ensure the curriculum is inclusive and accessible to all students, regardless of their abilities, backgrounds, and learning needs. Consider incorporating differentiated instruction, universal design for learning (UDL), and culturally responsive teaching practices.
 *
 * Social and emotional learning (SEL): Embed opportunities for developing social and emotional skills throughout the curriculum. This can include teaching empathy, self-awareness, self-regulation, problem-solving, and collaboration.
 *
 * Teacher support and professional development: Provide teachers with the necessary resources, training, and support to effectively implement the curriculum. This can include lesson plans, instructional materials, and ongoing professional development opportunities.
 *
 * Review and revision: Regularly evaluate and update the curriculum based on feedback from teachers, students, parents, and administrators, as well as current research and best practices in education.
 *
 * Outputs to produce for the curriculum:
 *
 * Curriculum guide: A comprehensive document outlining the learning objectives, content, and skills for each subject and grade level.
 *
 * Scope and sequence documents: Detailed roadmaps for each subject, outlining the progression of topics, skills, and learning objectives across grade levels.
 *
 * Lesson plans and instructional materials: Ready-to-use lesson plans, activities, and resources for teachers to implement the curriculum.
 *
 * Assessment tools: A variety of formative and summative assessments to measure student learning and evaluate the curriculum's effectiveness.
 *
 * Teacher support materials: Professional development resources, training materials, and ongoing support for teachers implementing the curriculum.
 *
 * Parent and community resources: Information and resources to help parents and community members understand and support the curriculum.
 */
class SchoolCurriculumCreatorTest : GenerationReportBase() {

    interface SchoolCurriculumCreator {

        fun defineObjectives(): List<String>

        fun identifySubjectsAndScope(): Map<String, List<String>>

        fun sequenceAndProgression(): Map<String, Map<String, List<String>>>

        fun integrationAndInterdisciplinaryConnections(): List<String>

        fun teachingMethodologies(): List<String>

        fun assessmentAndEvaluation(): List<String>

        fun inclusivityAndAccessibility(): List<String>

        fun socialAndEmotionalLearning(): List<String>

        fun teacherSupportAndProfessionalDevelopment(): List<String>

        fun reviewAndRevision(): List<String>

        data class CurriculumOutputs(
            val curriculumGuide: String,
            val scopeAndSequenceDocuments: Map<String, String>,
            val lessonPlansAndInstructionalMaterials: Map<String, List<String>>,
            val assessmentTools: List<String>,
            val teacherSupportMaterials: List<String>,
            val parentAndCommunityResources: List<String>
        )

        fun generateCurriculumOutputs(): CurriculumOutputs
    }

    @Test
    fun testSchoolCurriculumCreator() {
        runReport("SchoolCurriculumCreator", SchoolCurriculumCreator::class) { api, logJson, out ->
            val objectives = api.defineObjectives()
            logJson(objectives)
            out("Objectives:\n${objectives.joinToString("\n")}\n")

            val subjectsAndScope = api.identifySubjectsAndScope()
            logJson(subjectsAndScope)
            out(
                "Subjects and Scope:\n${
                    subjectsAndScope.map { "${it.key}: ${it.value.joinToString()}" }.joinToString("\n")
                }\n")

            val sequenceAndProgression = api.sequenceAndProgression()
            logJson(sequenceAndProgression)
            out(
                "Sequence and Progression:\n${
                    sequenceAndProgression.map {
                        "${it.key}: ${
                            it.value.map { "${it.key}: ${it.value.joinToString()}" }.joinToString()
                        }"
                    }.joinToString("\n")
                }\n")

            val integrationAndInterdisciplinaryConnections = api.integrationAndInterdisciplinaryConnections()
            logJson(integrationAndInterdisciplinaryConnections)
            out(
                "Integration and Interdisciplinary Connections:\n${
                    integrationAndInterdisciplinaryConnections.joinToString(
                        "\n"
                    )
                }\n"
            )

            val teachingMethodologies = api.teachingMethodologies()
            logJson(teachingMethodologies)
            out("Teaching Methodologies:\n${teachingMethodologies.joinToString("\n")}\n")

            val assessmentAndEvaluation = api.assessmentAndEvaluation()
            logJson(assessmentAndEvaluation)
            out("Assessment and Evaluation:\n${assessmentAndEvaluation.joinToString("\n")}\n")

            val inclusivityAndAccessibility = api.inclusivityAndAccessibility()
            logJson(inclusivityAndAccessibility)
            out("Inclusivity and Accessibility:\n${inclusivityAndAccessibility.joinToString("\n")}\n")

            val socialAndEmotionalLearning = api.socialAndEmotionalLearning()
            logJson(socialAndEmotionalLearning)
            out("Social and Emotional Learning:\n${socialAndEmotionalLearning.joinToString("\n")}\n")

            val teacherSupportAndProfessionalDevelopment = api.teacherSupportAndProfessionalDevelopment()
            logJson(teacherSupportAndProfessionalDevelopment)
            out(
                "Teacher Support and Professional Development:\n${
                    teacherSupportAndProfessionalDevelopment.joinToString(
                        "\n"
                    )
                }\n"
            )

            val reviewAndRevision = api.reviewAndRevision()
            logJson(reviewAndRevision)
            out("Review and Revision:\n${reviewAndRevision.joinToString("\n")}\n")

            val curriculumOutputs = api.generateCurriculumOutputs()
            logJson(curriculumOutputs)
            out(
                """
                |Curriculum Outputs:
                |Curriculum Guide:
                |${curriculumOutputs.curriculumGuide}
                |
                |Scope and Sequence Documents:
                |${curriculumOutputs.scopeAndSequenceDocuments.map { "${it.key}:\n${it.value}" }.joinToString("\n\n")}
                |
                |Lesson Plans and Instructional Materials:
                |${
                    curriculumOutputs.lessonPlansAndInstructionalMaterials.map { "${it.key}: ${it.value.joinToString()}" }
                        .joinToString("\n")
                }
                |
                |Assessment Tools:
                |${curriculumOutputs.assessmentTools.joinToString("\n")}
                |
                |Teacher Support Materials:
                |${curriculumOutputs.teacherSupportMaterials.joinToString("\n")}
                |
                |Parent and Community Resources:
                |${curriculumOutputs.parentAndCommunityResources.joinToString("\n")}
                """.trimMargin()
            )
        }
    }
}