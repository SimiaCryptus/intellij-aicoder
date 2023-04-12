package com.github.simiacryptus.aicoder.proxy

import org.junit.Test

/**
 * RecipeBook builds a recipe book centered around a theme.
 * Recipes are organized by main ingredient.
 * Each recipe has a title, a list of ingredients, and a list of steps.
 * Also included are substitutions for ingredients, a list of related recipes, and cooking tips.
 */
class RecipeBook : GenerationReportBase<RecipeBook.Recipes>(Recipes::class) {
    interface Recipes {

        fun getRecipes(
            theme: String,
            ingredient: String,
            recipeCount: Int = 10
        ): RecipeList

        data class RecipeList(
            val recipes: List<Recipe> = listOf(),
            val theme: String = "",
            val ingredient: String = "",
        )

        data class Recipe(
            val title: String = "",
            val ingredients: List<String> = listOf(),
            val steps: List<String> = listOf(),
            val substitutions: List<String> = listOf(),
            val relatedRecipes: List<String> = listOf(),
            val cookingTips: List<String> = listOf(),
            val image: ImageDescription? = null,
        )

        data class ImageDescription(
            val style: String = "",
            val subject: String = "",
            val background: String = "",
            val detailedCaption: String = "",
        )

    }

    @Test
    fun recipeBook() {
        runReport("Recipes") { api, logJson, out ->
            val theme = "Italian"
            val ingredients = listOf(
                "beef",
                "chicken",
                "pasta",
                "potatoes",
            )
            out(
                """
                |
                |# ${theme.capitalize()} Recipes
                |
                |""".trimMargin()
            )
            for (ingredient in ingredients) {
                out(
                    """
                    |
                    |## Recipes with $ingredient
                    |
                    |""".trimMargin()
                )
                try {
                    val recipes = api.getRecipes(theme, ingredient)
                    logJson(recipes)
                    for (recipe in recipes.recipes) {
                        out(
                            """
                            |
                            |### ${recipe.title}
                            |
                            |![${recipe.image!!.detailedCaption}](${
                                writeImage(
                                    proxy.api.render(
                                        recipe.image.detailedCaption,
                                        resolution = 512
                                    )[0]
                                )
                            })
                            |Ingredients:
                            |
                            |${recipe.ingredients.joinToString("\n")}
                            |
                            |Steps:
                            |
                            |${recipe.steps.joinToString("\n")}
                            |
                            |Substitutions:
                            |
                            |${recipe.substitutions.joinToString("\n")}
                            |
                            |Related Recipes:
                            |
                            |${recipe.relatedRecipes.joinToString("\n")}
                            |
                            |Cooking Tips:
                            |
                            |${recipe.cookingTips.joinToString("\n")}
                            |
                            |""".trimMargin()
                        )
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }
    }
}