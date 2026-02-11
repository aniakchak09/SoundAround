package com.licenta.soundaround.data.model

class Artist {
    val name: String
    val listeners: String
    val imageUrl: String?

    constructor(name: String, listeners: String, imageUrl: String?) {
        this.name = name
        this.listeners = listeners
        this.imageUrl = imageUrl
    }
}