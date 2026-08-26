package com.paperdesk.launcher.bridge

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.paperdesk.launcher.R

class SyncTileService : TileService() {
    override fun onStartListening() {
        val tile = qsTile ?: return
        tile.label = getString(R.string.sync)
        tile.state = Tile.STATE_INACTIVE
        tile.updateTile()
    }

    override fun onClick() {
        unlockAndRun {
            Thread {
                OsDataService.requestCompanionSync(applicationContext)
                qsTile?.let {
                    it.label = getString(R.string.synced)
                    it.state = Tile.STATE_ACTIVE
                    it.updateTile()
                }
            }.start()
        }
    }
}
