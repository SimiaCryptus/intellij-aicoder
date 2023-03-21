package com.github.simiacryptus.aicoder.proxy

import org.junit.Test

/**
 * Simulate world nations interacting via events over time,
 * and describe the changing world over time as a series of world news articles.
 * (e.g. an alternate history of Europe from 1900 to 1950)
 *
 * Include:
 * - events resulting from interactions with each other
 * - random events
 * - events caused by the passage of time
 * - a description of the effects of each event
 * - model political factions within each country, including the policies of each and how much control each has
 *
 * Optimize to reduce the total size (in serialized bytes) of each api call while ensuring each call has all the needed information to generate the response
 */
class AlternateHistorySimulator : GenerationReportBase(){
    @Test
    fun generateAlternateHistory() {
        runReport("AlternateHistory", AlternateHistory::class) { api, logJson, out ->
            val initialWorldState = api.setupWorld(
                startYear = 1900,
                endYear = 1950,
                nations = listOf("United Kingdom", "France", "Germany", "Russia", "Austria-Hungary", "Italy", "Ottoman Empire")
            )
            logJson(initialWorldState)
            var currentWorldState = initialWorldState

            for (year in initialWorldState.startYear..initialWorldState.endYear) {
                val events = api.generateYearlyEvents(currentWorldState, year)
                logJson(events)
                val updatedWorldState = api.updateWorldState(currentWorldState, events.events)
                logJson(updatedWorldState)

                out("Year: $year")
                events.events.forEach { event ->
                    val eventDescription = api.describeEvent(event, updatedWorldState)
                    out("- $eventDescription")
                }
                currentWorldState = updatedWorldState
            }
        }
    }

    interface AlternateHistory {
        fun setupWorld(
            startYear: Int,
            endYear: Int,
            nations: List<String>
        ): WorldState

        data class WorldState(
            val startYear: Int = 0,
            val endYear: Int = 0,
            val nations: List<Nation> = listOf(),
        )

        data class Nation(
            val name: String = "",
            val politicalFactions: List<PoliticalFaction> = listOf(),
            val territory: List<String> = listOf(),
            val economy: String = "",
            val military: String = "",
            val diplomacy: Map<String, String> = mapOf(),
            val technology: String = "",
        )

        data class PoliticalFaction(
            val name: String = "",
            val policies: List<String> = listOf(),
            val influence: Int = 0,
        )

        fun generateYearlyEvents(worldState: WorldState, year: Int): Events

        data class Events(
            val events: List<Event> = listOf(),
        )
        fun updateWorldState(worldState: WorldState, events: List<Event>): WorldState
        fun describeEvent(event: Event, worldState: WorldState): String

        data class Event(
            val type: String = "",
            val nation: String = "",
            val details: Map<String, String> = mapOf(),
        )
    }

}