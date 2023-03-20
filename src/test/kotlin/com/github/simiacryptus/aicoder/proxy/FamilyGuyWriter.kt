package com.github.simiacryptus.aicoder.proxy

import org.junit.Test

/**
 * Write an episode of "Family Guy", printing the output as a screenplay in markdown format
 *
 * Include:
 * - an overall plot with conflict and character development
 * - entertaining dialog with images
 * - random events
 * - humourous cutscenes, with an image each
 *
 * Optimize to reduce the total size (in serialized bytes) of each api call while ensuring each call has all the needed information to generate the response
 */
class FamilyGuyWriter : GenerationReportBase(){
    @Test
    fun writeEpisode() {
        runReport("FamilyGuyEpisode", FamilyGuy::class) { api, logJson, out ->
            val episodeInfo = api.generateEpisodeInfo()
            logJson(episodeInfo)

            out(
                """
                |# Family Guy
                |## Episode Title: ${episodeInfo.title}
                |### Written by: ${episodeInfo.writer}
                |""".trimMargin()
            )

            val acts = api.generateActs(episodeInfo)
            logJson(acts)

            for ((actIndex, act) in acts.acts.withIndex()) {
                out("\n## Act ${actIndex + 1}\n")
                val scenes = api.generateScenes(episodeInfo, act)
                logJson(scenes)

                for ((sceneIndex, scene) in scenes.scenes.withIndex()) {
                    out("\n### Scene ${sceneIndex + 1}: ${scene.location}\n")
                    val dialogues = api.generateDialogues(episodeInfo, act, scene)
                    logJson(dialogues)

                    for (dialogue in dialogues.dialogues) {
                        val character = dialogue.character.capitalize()
                        val text = dialogue.text
                        out("${character}: $text\n")

                        if (dialogue.hasImage) {
                            val imageCaption = api.generateImageCaption(dialogue)
                            logJson(imageCaption)

                            out(
                                """
                                |![${imageCaption.caption}](${
                                    writeImage(
                                        proxy.api.text_to_image(
                                            imageCaption.caption,
                                            resolution = 512
                                        )[0]
                                    )
                                })
                                |""".trimMargin()
                            )
                        }
                    }

                    if (scene.hasCutaway) {
                        val cutaway = api.generateCutaway(episodeInfo, act, scene)
                        logJson(cutaway)

                        out("\n*Cutaway: ${cutaway.description}*\n")
                        out(
                            """
                            |![${cutaway.imageCaption}](${
                                writeImage(
                                    proxy.api.text_to_image(
                                        cutaway.imageCaption,
                                        resolution = 512
                                    )[0]
                                )
                            })
                            |""".trimMargin()
                        )
                    }
                }
            }
        }
    }

    interface FamilyGuy {

        fun generateEpisodeInfo(): EpisodeInfo

        data class EpisodeInfo(
            val title: String = "",
            val writer: String = "",
        )

        fun generateActs(episodeInfo: EpisodeInfo): ActList

        data class ActList(
            val acts: List<Act> = listOf(),
        )

        data class Act(
            val id: Int = 0,
        )

        fun generateScenes(episodeInfo: EpisodeInfo, act: Act): SceneList

        data class SceneList(
            val scenes: List<Scene> = listOf(),
        )

        data class Scene(
            val location: String = "",
            val hasCutaway: Boolean = false,
        )

        fun generateDialogues(episodeInfo: EpisodeInfo, act: Act, scene: Scene): DialogueList

        data class DialogueList(
            val dialogues: List<Dialogue> = listOf(),
        )

        data class Dialogue(
            val character: String = "",
            val text: String = "",
            val hasImage: Boolean = false,
        )

        fun generateImageCaption(dialogue: Dialogue): ImageCaption

        data class ImageCaption(
            val caption: String = "",
        )

        fun generateCutaway(episodeInfo: EpisodeInfo, act: Act, scene: Scene): Cutaway

        data class Cutaway(
            val description: String = "",
            val imageCaption: String = "",
        )
    }
}