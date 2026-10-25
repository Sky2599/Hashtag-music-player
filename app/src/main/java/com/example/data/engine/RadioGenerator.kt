package com.example.data.engine

import com.example.data.model.RadioMix
import com.example.data.model.Song

object RadioGenerator {

    /**
     * Generates YouTube Music-style smart mixes, dynamic personalized radios,
     * and mood-curated playlists from the user's local music library.
     */
    fun generateRadioMixes(
        allSongs: List<Song>,
        mostPlayed: List<Song>,
        recentlyAdded: List<Song>,
        favorites: List<Song> = emptyList()
    ): List<RadioMix> {
        if (allSongs.isEmpty()) return emptyList()

        val mixes = mutableListOf<RadioMix>()

        // 1. YouTube Music Flagship: "My Supermix"
        // Blends favorites, top-played tracks, and recent discoveries
        val supermixPool = mutableListOf<Song>()
        val supermixSeen = mutableSetOf<Long>()
        
        fun addToSupermix(song: Song) {
            if (!supermixSeen.contains(song.id)) {
                supermixPool.add(song)
                supermixSeen.add(song.id)
            }
        }
        
        // Interleave top played, favorites, and recent
        val maxLen = maxOf(mostPlayed.size, favorites.size, recentlyAdded.size, allSongs.size)
        for (i in 0 until maxLen) {
            favorites.getOrNull(i)?.let { addToSupermix(it) }
            mostPlayed.getOrNull(i)?.let { addToSupermix(it) }
            recentlyAdded.getOrNull(i)?.let { addToSupermix(it) }
            if (supermixPool.size >= 25) break
        }
        if (supermixPool.isEmpty()) {
            supermixPool.addAll(allSongs.take(20))
        }

        mixes.add(
            RadioMix(
                id = "mix_supermix",
                title = "My Supermix",
                subtitle = "Tailored endless mix • ${supermixPool.size} tracks",
                seedSong = supermixPool.firstOrNull(),
                tracks = supermixPool,
                coverArtUri = supermixPool.firstOrNull()?.let { it.customArtworkUri ?: it.albumArtUri?.toString() },
                moodCategory = "All",
                badgeLabel = "SUPERMIX"
            )
        )

        // 2. YouTube Music "Quick Picks"
        // Songs you frequently jump into or play repeatedly
        val quickPicks = (mostPlayed.take(12) + favorites.take(12))
            .distinctBy { it.id }
            .take(16)
            .ifEmpty { allSongs.take(12) }

        mixes.add(
            RadioMix(
                id = "mix_quick_picks",
                title = "Quick Picks",
                subtitle = "Start listening right away • ${quickPicks.size} tracks",
                seedSong = quickPicks.firstOrNull(),
                tracks = quickPicks,
                coverArtUri = quickPicks.firstOrNull()?.let { it.customArtworkUri ?: it.albumArtUri?.toString() },
                moodCategory = "All",
                badgeLabel = "QUICK PICKS"
            )
        )

        // 3. YouTube Music "Energy / Workout Mix"
        // Upbeat, punchy tracks (e.g. tracks with duration between 2:00 and 4:30, or higher bitrates)
        val energyTracks = allSongs
            .filter { it.durationMs in 120_000L..270_000L || it.bitrateKbps >= 256 }
            .shuffled()
            .take(18)
            .ifEmpty { allSongs.shuffled().take(12) }

        mixes.add(
            RadioMix(
                id = "mix_energy",
                title = "Energy Booster",
                subtitle = "High tempo, pump-up motivation • ${energyTracks.size} tracks",
                seedSong = energyTracks.firstOrNull(),
                tracks = energyTracks,
                coverArtUri = energyTracks.firstOrNull()?.let { it.customArtworkUri ?: it.albumArtUri?.toString() },
                moodCategory = "Workout",
                badgeLabel = "WORKOUT"
            )
        )

        // 4. YouTube Music "Chill & Focus Mix"
        // Mellow, longer, acoustic or ambient tracks (duration > 3:30)
        val chillTracks = allSongs
            .filter { it.durationMs >= 210_000L }
            .shuffled()
            .take(18)
            .ifEmpty { allSongs.takeLast(12) }

        mixes.add(
            RadioMix(
                id = "mix_chill",
                title = "Chill & Focus",
                subtitle = "Deep flow & calm vibes • ${chillTracks.size} tracks",
                seedSong = chillTracks.firstOrNull(),
                tracks = chillTracks,
                coverArtUri = chillTracks.firstOrNull()?.let { it.customArtworkUri ?: it.albumArtUri?.toString() },
                moodCategory = "Chill",
                badgeLabel = "CHILL"
            )
        )

        // 5. YouTube Music "Deep Cuts"
        // Hidden gems in library: tracks with low play count or by favorite artists that you haven't played much
        val topArtistNames = mostPlayed.map { it.artist.trim().lowercase() }.toSet()
        val deepCuts = allSongs
            .filter { song -> song.playCount <= 1 || topArtistNames.contains(song.artist.trim().lowercase()) }
            .shuffled()
            .take(16)
            .ifEmpty { allSongs.shuffled().take(10) }

        mixes.add(
            RadioMix(
                id = "mix_deep_cuts",
                title = "Deep Cuts",
                subtitle = "Unsung library gems • ${deepCuts.size} tracks",
                seedSong = deepCuts.firstOrNull(),
                tracks = deepCuts,
                coverArtUri = deepCuts.firstOrNull()?.let { it.customArtworkUri ?: it.albumArtUri?.toString() },
                moodCategory = "Focus",
                badgeLabel = "DEEP CUTS"
            )
        )

        // 6. YouTube Music "Forgotten Favorites"
        // Favorites or top played tracks not in recently added
        val recentIds = recentlyAdded.map { it.id }.toSet()
        val forgotten = (favorites + mostPlayed)
            .filter { !recentIds.contains(it.id) }
            .distinctBy { it.id }
            .take(16)

        if (forgotten.isNotEmpty()) {
            mixes.add(
                RadioMix(
                    id = "mix_forgotten",
                    title = "Forgotten Favorites",
                    subtitle = "Rediscover past loves • ${forgotten.size} tracks",
                    seedSong = forgotten.firstOrNull(),
                    tracks = forgotten,
                    coverArtUri = forgotten.firstOrNull()?.let { it.customArtworkUri ?: it.albumArtUri?.toString() },
                    moodCategory = "Chill",
                    badgeLabel = "REDISCOVER"
                )
            )
        }

        // 7. Seed-based Artist Radios (YouTube Music style "[Artist] Radio")
        val candidateSeeds = (mostPlayed + favorites + recentlyAdded + allSongs).distinctBy { it.artist }
        for (seed in candidateSeeds.take(4)) {
            val clusterTracks = clusterTracksForSeed(seed, allSongs)
            if (clusterTracks.size >= 3) {
                mixes.add(
                    RadioMix(
                        id = "radio_${seed.id}",
                        title = "${seed.artist} Radio",
                        subtitle = "Continuous artist mix • ${clusterTracks.size} tracks",
                        seedSong = seed,
                        tracks = clusterTracks,
                        coverArtUri = seed.customArtworkUri ?: seed.albumArtUri?.toString(),
                        moodCategory = "All",
                        badgeLabel = "ARTIST RADIO"
                    )
                )
            }
        }

        return mixes
    }

    private fun clusterTracksForSeed(seed: Song, allSongs: List<Song>): List<Song> {
        val result = mutableListOf<Song>()
        val addedIds = HashSet<Long>()

        // 1. Seed song plays first
        result.add(seed)
        addedIds.add(seed.id)

        // 2. Tracks by the same artist
        for (song in allSongs) {
            if (!addedIds.contains(song.id) && song.artist.equals(seed.artist, ignoreCase = true)) {
                result.add(song)
                addedIds.add(song.id)
                if (result.size >= 8) break
            }
        }

        // 3. Tracks from the same album
        for (song in allSongs) {
            if (!addedIds.contains(song.id) && song.album.equals(seed.album, ignoreCase = true)) {
                result.add(song)
                addedIds.add(song.id)
                if (result.size >= 12) break
            }
        }

        // 4. Tracks sharing similar duration (+/- 60s)
        for (song in allSongs) {
            if (!addedIds.contains(song.id) && Math.abs(song.durationMs - seed.durationMs) <= 60000L) {
                result.add(song)
                addedIds.add(song.id)
                if (result.size >= 16) break
            }
        }

        // 5. Fill to at least 8 tracks
        if (result.size < 8) {
            for (song in allSongs.shuffled()) {
                if (!addedIds.contains(song.id)) {
                    result.add(song)
                    addedIds.add(song.id)
                    if (result.size >= 8) break
                }
            }
        }

        return result
    }
}
