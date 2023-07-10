package com.github.simiacryptus.aicoder.util

object MarkdownProcessor {

    data class MarkdownData(val name: String, val sections: List<Section>)

    data class Section(val title: String, val codeType: String, val code: String)

    @JvmStatic
    fun parse(input: String): List<MarkdownData> {
        val lines = input.split("\n")
        val markdownDataList = mutableListOf<MarkdownData>()

        var name = ""
        var title = ""
        var codeType: String? = null
        var code = ""
        var sections = mutableListOf<Section>()

        for (line in lines) {
            when {
                line.startsWith("# ") -> {
                    if (name.isNotEmpty()) {
                        markdownDataList.add(MarkdownData(name, sections))
                        sections = mutableListOf()
                    }
                    name = line.removePrefix("# ").trim()
                }

                line.startsWith("## ") -> {
                    title = line.removePrefix("## ").trim()
                }

                line.startsWith("```") -> {
                    if (codeType == null) {
                        codeType = line.removePrefix("```").trim()
                        code = ""
                    } else {
                        if (title.isNotEmpty()) {
                            sections.add(Section(title, codeType ?: "", code.trim()))
                        }
                        codeType = null
                    }
                }

                else -> code += "$line\n"
            }
        }

        // Add the last section
        if (title.isNotEmpty()) {
            sections.add(Section(title, codeType ?: "", code.trim()))
        }

        // Add the last markdown data
        if (name.isNotEmpty()) {
            markdownDataList.add(MarkdownData(name, sections))
        }

        return markdownDataList
    }

    fun toString(markdownDataList: List<MarkdownData>): String {
        var output = ""

        for (markdownData in markdownDataList) {
            output += "# ${markdownData.name}\n"

            for (section in markdownData.sections) {
                output += "\n## ${section.title}\n\n```${section.codeType}\n${section.code}\n```\n"
            }
        }

        return output
    }
}

