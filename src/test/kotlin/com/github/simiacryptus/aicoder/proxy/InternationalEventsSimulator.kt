package com.github.simiacryptus.aicoder.proxy

import org.junit.Test

/**
 * Simulate world nations interacting via news events over time and describe the changing relationships and motivations and causes of each event
 */
class InternationalEventsSimulator : GenerationReportBase() {
    interface InternationalEvents {
        fun generateInitialWorldState(
            nationNames: List<String> = listOf(),
            initialRelations: List<NationRelation> = listOf(),
        ): WorldState

        data class WorldState(
            val nations: List<Nation> = listOf(),
            val relations: List<NationRelation> = listOf(),
            val events: List<Event> = listOf(),
        )

        data class Nation(
            val name: String = "",
            val government: String = "",
            val economy: String = "",
            val military: String = "",
            val culture: String = "",
        )

        data class NationRelation(
            val nation1: String = "",
            val nation2: String = "",
            val relationStatus: String = "",
        )

        data class Event(
            val involvedNations: List<String> = listOf(),
            val eventType: String = "",
            val cause: String = "",
            val effect: String = "",
            val date: String = "",
        )

        fun generateNewEvent(worldState: WorldState): Event
        fun updateWorldState(worldState: WorldState, newEvent: Event): WorldState

        fun describeEvent(event: Event): String
    }

    @Test
    fun simulateInternationalEvents() {
        runReport("International Events", InternationalEvents::class) { api, logJson, out ->
            val initialWorldState = api.generateInitialWorldState(
                nationNames = listOf("USA", "China", "Russia", "UK", "France", "Germany", "Japan", "India", "Brazil", "Canada"),
            )
            logJson(initialWorldState)
            var worldState = initialWorldState

            for (i in 1..10) {
                val newEvent = api.generateNewEvent(worldState)
                logJson(newEvent)
                out(
                    """
                    |
                    |Event $i: ${api.describeEvent(newEvent)}
                    |
                    |""".trimMargin()
                )
                worldState = api.updateWorldState(worldState, newEvent)
                logJson(worldState)
            }
        }
    }
}