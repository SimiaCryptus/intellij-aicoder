package com.github.simiacryptus.aicoder.proxy

import org.junit.Test

/**
 * TravelGuide builds a travel guide for a given destination.
 * The guide includes information about the destination, attractions, restaurants, and activities.
 * Also included are tips for getting around, a list of related destinations, and a list of recommended hotels.
 */
class TravelGuide : GenerationReportBase() {
    interface Travel {

        fun getDestination(destinationName: String): Destination

        data class Destination(
            val name: String = "",
            val description: String = "",
            val attractions: List<Attraction> = listOf(),
            val restaurants: List<Restaurant> = listOf(),
            val activities: List<Activity> = listOf(),
            val tips: List<String> = listOf(),
            val relatedDestinations: List<String> = listOf(),
            val recommendedHotels: List<String> = listOf(),
        )

        data class Attraction(
            val name: String = "",
            val description: String = "",
            val image: ImageDescription? = null,
        )

        data class Restaurant(
            val name: String = "",
            val description: String = "",
            val image: ImageDescription? = null,
        )

        data class Activity(
            val name: String = "",
            val description: String = "",
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
    fun travelGuide() {
        runReport("Travel", Travel::class) { api, logJson, out ->

        }
    }
}

