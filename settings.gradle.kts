rootProject.name = "CloudstreamPlugins"

// This file sets what projects are included. Every time you add a new project, you must add it
// to the includes below.

// Plugins are included like this
val disabled = listOf(
    "SflixProProvider",
    "BflixProvider",
    "FmoviesToProvider"
)

File(rootDir, ".").eachDir { dir ->
    if (!disabled.contains(dir.name) && File(dir, "build.gradle.kts").exists()) {
        include(dir.name)
    }
}

fun File.eachDir(block: (File) -> Unit) {
    listFiles()?.filter { it.isDirectory }?.forEach { block(it) }
}


// To only include a single project, comment out the previous lines (except the first one), and include your plugin like so:
// include("PluginName")
