package com.example.player

import com.example.data.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class QueueSlot(
    val id: Int,
    val name: String,
    val items: List<Song> = emptyList(),
    val activeIndex: Int = 0,
    val lastUpdatedMs: Long = 0L
) {
    val totalDurationMs: Long
        get() = items.sumOf { it.durationMs }

    val currentSong: Song?
        get() = items.getOrNull(activeIndex)
}

/**
 * Multi-Queue Engine:
 * Supports up to 4 independent, persistent playback queues.
 * When a new song or playlist is started that is not part of the current queue,
 * the current queue remains saved in its slot and playback automatically rotates
 * to an empty slot—or overwrites the oldest slot if all 4 slots are occupied.
 */
class MultiQueueManager {
    private val _queues = MutableStateFlow(
        listOf(
            QueueSlot(id = 1, name = "Queue 1 (Main)", lastUpdatedMs = 4L),
            QueueSlot(id = 2, name = "Queue 2 (Lossless)", lastUpdatedMs = 0L),
            QueueSlot(id = 3, name = "Queue 3 (Focus/Acoustic)", lastUpdatedMs = 0L),
            QueueSlot(id = 4, name = "Queue 4 (Party/Energy)", lastUpdatedMs = 0L)
        )
    )
    val queues: StateFlow<List<QueueSlot>> = _queues.asStateFlow()

    private val _activeQueueSlotIndex = MutableStateFlow(0)
    val activeQueueSlotIndex: StateFlow<Int> = _activeQueueSlotIndex.asStateFlow()

    fun restoreQueues(restoredSlots: List<QueueSlot>, activeIndex: Int) {
        if (restoredSlots.size == 4) {
            val now = System.currentTimeMillis()
            _queues.value = restoredSlots.mapIndexed { idx, slot ->
                if (slot.lastUpdatedMs == 0L && slot.items.isNotEmpty()) {
                    slot.copy(lastUpdatedMs = now - (4 - idx) * 1000L)
                } else {
                    slot
                }
            }
            _activeQueueSlotIndex.value = activeIndex.coerceIn(0, 3)
        }
    }

    /**
     * Checks whether two song lists represent the same queue session.
     */
    fun isSameQueueList(a: List<Song>, b: List<Song>): Boolean {
        if (a.size != b.size || a.isEmpty()) return false
        for (i in a.indices) {
            if (a[i].id != b[i].id) return false
        }
        return true
    }

    /**
     * Finds or rotates to the appropriate slot when a user starts a new song/queue:
     * 1. If the current active slot is empty, use the current active slot.
     * 2. If any of the 4 slots already contains this exact queue (or single song), switch to that slot.
     * 3. Otherwise, preserve the current queue in its slot and pick the first empty slot (0..3).
     * 4. If all 4 slots are non-empty, overwrite the OLDEST non-active slot (lowest lastUpdatedMs).
     */
    fun allocateSlotForNewQueue(
        newQueue: List<Song>,
        activeIndex: Int,
        suggestedLabel: String? = null
    ): Int {
        val currentList = _queues.value.toMutableList()
        val currentSlotIdx = _activeQueueSlotIndex.value.coerceIn(0, 3)
        val now = System.currentTimeMillis()

        // 1. Check if current active slot is empty or already has this exact queue
        val activeSlot = currentList[currentSlotIdx]
        if (activeSlot.items.isEmpty() || isSameQueueList(activeSlot.items, newQueue)) {
            val updatedName = if (activeSlot.items.isEmpty() && !suggestedLabel.isNullOrBlank()) {
                "Q${currentSlotIdx + 1}: $suggestedLabel"
            } else {
                activeSlot.name
            }
            currentList[currentSlotIdx] = activeSlot.copy(
                name = updatedName,
                items = newQueue,
                activeIndex = activeIndex.coerceIn(0, (newQueue.size - 1).coerceAtLeast(0)),
                lastUpdatedMs = now
            )
            _queues.value = currentList
            return currentSlotIdx
        }

        // 2. Check if another slot already holds this exact queue
        val matchingSlotIdx = currentList.indexOfFirst { isSameQueueList(it.items, newQueue) }
        if (matchingSlotIdx >= 0) {
            val matched = currentList[matchingSlotIdx]
            currentList[matchingSlotIdx] = matched.copy(
                activeIndex = activeIndex.coerceIn(0, (newQueue.size - 1).coerceAtLeast(0)),
                lastUpdatedMs = now
            )
            _queues.value = currentList
            _activeQueueSlotIndex.value = matchingSlotIdx
            return matchingSlotIdx
        }

        // 3. Find first empty slot so we preserve the current queue untouched
        val emptySlotIdx = currentList.indexOfFirst { it.items.isEmpty() }
        val targetSlotIdx = if (emptySlotIdx >= 0) {
            emptySlotIdx
        } else {
            // 4. All 4 queues are occupied: find the oldest slot (excluding the one we just saved)
            val candidates = currentList.indices.filter { it != currentSlotIdx }
            candidates.minByOrNull { idx -> currentList[idx].lastUpdatedMs } ?: ((currentSlotIdx + 1) % 4)
        }

        val targetSlot = currentList[targetSlotIdx]
        val defaultSlotTitle = "Queue ${targetSlotIdx + 1}"
        val slotTitle = if (!suggestedLabel.isNullOrBlank()) {
            "Q${targetSlotIdx + 1}: $suggestedLabel"
        } else {
            targetSlot.name.ifBlank { defaultSlotTitle }
        }

        currentList[targetSlotIdx] = targetSlot.copy(
            name = slotTitle,
            items = newQueue,
            activeIndex = activeIndex.coerceIn(0, (newQueue.size - 1).coerceAtLeast(0)),
            lastUpdatedMs = now
        )
        _queues.value = currentList
        _activeQueueSlotIndex.value = targetSlotIdx
        return targetSlotIdx
    }

    fun updateCurrentQueue(songs: List<Song>, activeIndex: Int) {
        updateQueue(_activeQueueSlotIndex.value, songs, activeIndex)
    }

    fun updateQueue(slotIndex: Int, songs: List<Song>, activeIndex: Int) {
        if (slotIndex !in 0..3) return
        val currentList = _queues.value.toMutableList()
        val oldSlot = currentList[slotIndex]
        currentList[slotIndex] = oldSlot.copy(
            items = songs,
            activeIndex = activeIndex.coerceIn(0, (songs.size - 1).coerceAtLeast(0)),
            lastUpdatedMs = System.currentTimeMillis()
        )
        _queues.value = currentList
    }

    fun addSongsToSlot(slotIndex: Int, songs: List<Song>) {
        if (slotIndex !in 0..3 || songs.isEmpty()) return
        val currentList = _queues.value.toMutableList()
        val oldSlot = currentList[slotIndex]
        val updatedItems = oldSlot.items + songs
        currentList[slotIndex] = oldSlot.copy(
            items = updatedItems,
            activeIndex = oldSlot.activeIndex.coerceIn(0, (updatedItems.size - 1).coerceAtLeast(0)),
            lastUpdatedMs = System.currentTimeMillis()
        )
        _queues.value = currentList
    }

    fun removeSongAtFromSlot(slotIndex: Int, itemIndex: Int) {
        if (slotIndex !in 0..3) return
        val currentList = _queues.value.toMutableList()
        val oldSlot = currentList[slotIndex]
        if (itemIndex !in oldSlot.items.indices) return
        val updatedItems = oldSlot.items.toMutableList().apply { removeAt(itemIndex) }
        val newActiveIdx = when {
            updatedItems.isEmpty() -> 0
            itemIndex < oldSlot.activeIndex -> (oldSlot.activeIndex - 1).coerceAtLeast(0)
            else -> oldSlot.activeIndex.coerceIn(0, updatedItems.size - 1)
        }
        currentList[slotIndex] = oldSlot.copy(
            items = updatedItems,
            activeIndex = newActiveIdx,
            lastUpdatedMs = System.currentTimeMillis()
        )
        _queues.value = currentList
    }

    fun moveSongInSlot(slotIndex: Int, fromIndex: Int, toIndex: Int) {
        if (slotIndex !in 0..3) return
        val currentList = _queues.value.toMutableList()
        val oldSlot = currentList[slotIndex]
        if (fromIndex !in oldSlot.items.indices || toIndex !in oldSlot.items.indices || fromIndex == toIndex) return
        val updatedItems = oldSlot.items.toMutableList()
        val moved = updatedItems.removeAt(fromIndex)
        updatedItems.add(toIndex, moved)

        val newActiveIdx = when (oldSlot.activeIndex) {
            fromIndex -> toIndex
            in (minOf(fromIndex, toIndex)..maxOf(fromIndex, toIndex)) -> {
                if (fromIndex < toIndex) oldSlot.activeIndex - 1 else oldSlot.activeIndex + 1
            }
            else -> oldSlot.activeIndex
        }.coerceIn(0, (updatedItems.size - 1).coerceAtLeast(0))

        currentList[slotIndex] = oldSlot.copy(
            items = updatedItems,
            activeIndex = newActiveIdx,
            lastUpdatedMs = System.currentTimeMillis()
        )
        _queues.value = currentList
    }

    fun shuffleSlot(slotIndex: Int) {
        if (slotIndex !in 0..3) return
        val currentList = _queues.value.toMutableList()
        val oldSlot = currentList[slotIndex]
        if (oldSlot.items.size <= 1) return
        val activeSong = oldSlot.currentSong
        val shuffled = oldSlot.items.shuffled()
        val newActiveIdx = if (activeSong != null) {
            shuffled.indexOfFirst { it.id == activeSong.id }.coerceAtLeast(0)
        } else 0
        currentList[slotIndex] = oldSlot.copy(
            items = shuffled,
            activeIndex = newActiveIdx,
            lastUpdatedMs = System.currentTimeMillis()
        )
        _queues.value = currentList
    }

    fun duplicateSlot(fromSlotIndex: Int, toSlotIndex: Int) {
        if (fromSlotIndex !in 0..3 || toSlotIndex !in 0..3 || fromSlotIndex == toSlotIndex) return
        val currentList = _queues.value.toMutableList()
        val source = currentList[fromSlotIndex]
        val target = currentList[toSlotIndex]
        currentList[toSlotIndex] = target.copy(
            items = source.items.toList(),
            activeIndex = source.activeIndex,
            lastUpdatedMs = System.currentTimeMillis()
        )
        _queues.value = currentList
    }

    fun switchToSlot(slotIndex: Int): QueueSlot? {
        if (slotIndex !in 0..3) return null
        _activeQueueSlotIndex.value = slotIndex
        val currentList = _queues.value.toMutableList()
        val slot = currentList[slotIndex]
        currentList[slotIndex] = slot.copy(lastUpdatedMs = System.currentTimeMillis())
        _queues.value = currentList
        return currentList[slotIndex]
    }

    fun renameQueue(slotIndex: Int, newName: String) {
        if (slotIndex !in 0..3 || newName.isBlank()) return
        val currentList = _queues.value.toMutableList()
        currentList[slotIndex] = currentList[slotIndex].copy(name = newName.trim())
        _queues.value = currentList
    }

    fun clearQueue(slotIndex: Int) {
        if (slotIndex !in 0..3) return
        val currentList = _queues.value.toMutableList()
        currentList[slotIndex] = currentList[slotIndex].copy(items = emptyList(), activeIndex = 0, lastUpdatedMs = 0L)
        _queues.value = currentList
    }

    fun getCurrentQueue(): List<Song> {
        return _queues.value.getOrNull(_activeQueueSlotIndex.value)?.items ?: emptyList()
    }

    fun getCurrentIndex(): Int {
        return _queues.value.getOrNull(_activeQueueSlotIndex.value)?.activeIndex ?: 0
    }
}
