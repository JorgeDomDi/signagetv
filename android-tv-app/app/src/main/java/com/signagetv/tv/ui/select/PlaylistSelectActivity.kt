package com.signagetv.tv.ui.select

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.signagetv.tv.R
import com.signagetv.tv.SignageApp
import com.signagetv.tv.data.api.PlaylistDto
import com.signagetv.tv.databinding.ActivityPlaylistSelectBinding
import com.signagetv.tv.ui.login.LoginActivity
import com.signagetv.tv.ui.player.PlayerActivity
import com.signagetv.tv.util.Logger
import kotlinx.coroutines.launch

/**
 * Shows the available playlists for the current local plus an "Automatic" option.
 * D-pad navigable.
 */
class PlaylistSelectActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlaylistSelectBinding
    private val app by lazy { SignageApp.get(this) }
    private val adapter = PlaylistAdapter { entry -> onChoose(entry) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlaylistSelectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.localNameView.text = app.prefs.localName ?: ""
        binding.playlistList.layoutManager = LinearLayoutManager(this)
        binding.playlistList.adapter = adapter
        binding.playlistList.setHasFixedSize(true)

        loadPlaylists()
    }

    private fun loadPlaylists() {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val playlists = app.repository.listPlaylists()
                val entries = mutableListOf<PlaylistEntry>()
                entries += PlaylistEntry.Auto
                playlists.sortedBy { it.nombre.lowercase() }.forEach { p ->
                    entries += PlaylistEntry.Manual(p)
                }
                adapter.submit(entries, app.prefs.selectedPlaylistId)
                binding.playlistList.post {
                    binding.playlistList.requestFocus()
                    binding.playlistList.findViewHolderForAdapterPosition(0)
                        ?.itemView?.requestFocus()
                }
                binding.emptyView.visibility = if (playlists.isEmpty()) View.GONE else View.GONE
                // (Auto is always visible, so we don't show the "no playlists" empty state.)
            } catch (t: Throwable) {
                Logger.e("Load playlists failed", t)
                binding.emptyView.visibility = View.VISIBLE
                binding.emptyView.text = "Error: ${t.message}"
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun onChoose(entry: PlaylistEntry) {
        lifecycleScope.launch {
            try {
                val tvId = app.prefs.tvId
                val playlistId: Long? = when (entry) {
                    PlaylistEntry.Auto -> null
                    is PlaylistEntry.Manual -> entry.playlist.id
                }
                if (tvId > 0) {
                    runCatching { app.repository.assignPlaylist(tvId, playlistId) }
                        .onFailure { Logger.w("assignPlaylist failed: ${it.message}") }
                }
                app.prefs.selectedPlaylistId = playlistId ?: -1L
                startActivity(Intent(this@PlaylistSelectActivity, PlayerActivity::class.java))
                finish()
            } catch (t: Throwable) {
                Logger.e("Choose playlist failed", t)
            }
        }
    }

    /** Long-press MENU to log out. */
    override fun onKeyLongPress(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            app.repository.logout()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return true
        }
        return super.onKeyLongPress(keyCode, event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            event?.startTracking()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    // ===== Adapter =====

    sealed class PlaylistEntry {
        object Auto : PlaylistEntry()
        data class Manual(val playlist: PlaylistDto) : PlaylistEntry()
    }

    private class PlaylistAdapter(
        val onClick: (PlaylistEntry) -> Unit
    ) : RecyclerView.Adapter<PlaylistAdapter.VH>() {

        private val items = mutableListOf<PlaylistEntry>()
        private var selectedId: Long = -2L

        fun submit(list: List<PlaylistEntry>, selectedId: Long) {
            items.clear()
            items.addAll(list)
            this.selectedId = selectedId
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_playlist, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val entry = items[position]
            val name = holder.itemView.findViewById<TextView>(R.id.playlistName)
            val sub = holder.itemView.findViewById<TextView>(R.id.playlistSubtitle)
            when (entry) {
                PlaylistEntry.Auto -> {
                    name.text = holder.itemView.context.getString(R.string.auto_schedule)
                    sub.text = holder.itemView.context.getString(R.string.auto_schedule_subtitle)
                    holder.itemView.isSelected = selectedId == -1L
                }
                is PlaylistEntry.Manual -> {
                    name.text = entry.playlist.nombre
                    val n = entry.playlist.items.size
                    sub.text = "$n items · transición ${entry.playlist.transicion}"
                    holder.itemView.isSelected = selectedId == entry.playlist.id
                }
            }
            holder.itemView.setOnClickListener { onClick(entry) }
        }

        override fun getItemCount(): Int = items.size

        class VH(view: View) : RecyclerView.ViewHolder(view)
    }
}
