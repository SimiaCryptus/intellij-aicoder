package com.github.simiacryptus.openai

class Engine {
    @Suppress("unused")
    var id: String? = null

    @Suppress("unused")
    var ready = false

    @Suppress("unused")
    var owner: String? = null

    @Suppress("unused")
    var `object`: String? = null

    @Suppress("unused")
    var created: Int? = null

    @Suppress("unused")
    var permissions: String? = null

    @Suppress("unused")
    var replicas: Int? = null

    @Suppress("unused")
    var max_replicas: Int? = null

    @Suppress("unused")
    constructor()

    @Suppress("unused")
    constructor(
        id: String?,
        ready: Boolean,
        owner: String?,
        `object`: String?,
        created: Int?,
        permissions: String?,
        replicas: Int?,
        max_replicas: Int?
    ) {
        this.id = id
        this.ready = ready
        this.owner = owner
        this.`object` = `object`
        this.created = created
        this.permissions = permissions
        this.replicas = replicas
        this.max_replicas = max_replicas
    }
}