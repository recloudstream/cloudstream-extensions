// use an integer for version numbers
version = 2


cloudstream {
    language = "en"
    // All of these properties are optional, you can safely remove them

    description = "Also includes Dopebox, Solarmovie, Zoro and 2embed"
    // authors = listOf("Cloudburst")

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
     * */
    status = 1 // will be 3 if unspecified
    tvTypes = listOf(
        "TvSeries",
        "Movie",
    )

    iconUrl = "https://www.google.com/s2/favicons?domain=www.2embed.to&sz=%size%"
}