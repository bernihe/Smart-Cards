package com.example.data

import kotlinx.coroutines.flow.Flow

class SmartCardRepository(private val dao: SmartCardDao) {

    val allFolders: Flow<List<Folder>> = dao.getAllFolders()

    fun getCardsForFolder(folderId: Int): Flow<List<SmartCard>> {
        return dao.getCardsForFolder(folderId)
    }

    suspend fun getFolderById(id: Int): Folder? {
        return dao.getFolderById(id)
    }

    suspend fun getCardById(id: Int): SmartCard? {
        return dao.getCardById(id)
    }

    suspend fun insertFolder(folder: Folder): Long {
        return dao.insertFolder(folder)
    }

    suspend fun updateFolder(folder: Folder) {
        dao.updateFolder(folder)
    }

    suspend fun deleteFolder(folder: Folder) {
        // First delete all cards in folder, then the folder itself
        dao.deleteCardsByFolderId(folder.id)
        dao.deleteFolder(folder)
    }

    suspend fun insertCard(card: SmartCard): Long {
        return dao.insertCard(card)
    }

    suspend fun updateCard(card: SmartCard) {
        dao.updateCard(card)
    }

    suspend fun deleteCard(card: SmartCard) {
        dao.deleteCard(card)
    }

    suspend fun ensureDefaultFoldersExist() {
        if (dao.getFolderCount() == 0) {
            val defaults = listOf(
                Folder(name = "✈️ Travel Plans", color = "#8B5CF6", iconName = "travel"),
                Folder(name = "💻 Project Ideas", color = "#3B82F6", iconName = "project"),
                Folder(name = "🍔 Food & Dining", color = "#EC4899", iconName = "food"),
                Folder(name = "💡 Creative Ideas", color = "#10B981", iconName = "idea")
            )
            for (folder in defaults) {
                dao.insertFolder(folder)
            }
        }
    }
}
