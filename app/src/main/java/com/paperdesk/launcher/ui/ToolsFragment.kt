package com.paperdesk.launcher.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.paperdesk.launcher.R
import com.paperdesk.launcher.util.UnitConverter

class ToolsFragment : Fragment() {

    private var appsMode = false
    private var justEvaluated = false
    private val appRows = mutableListOf<LaunchApp>()
    private lateinit var appAdapter: AppAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.page_tools, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<TextView>(R.id.cMul)?.text = "x"
        bindKeys(view)
        appAdapter = AppAdapter(appRows) { openApp(it) }
        view.findViewById<RecyclerView>(R.id.appsList).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = appAdapter
        }
        val modeCalc = view.findViewById<TextView>(R.id.toolsModeCalc)
        val modeApps = view.findViewById<TextView>(R.id.toolsModeApps)
        fun showMode(apps: Boolean) {
            appsMode = apps
            view.findViewById<View>(R.id.calcWorkspace).visibility =
                if (apps) View.GONE else View.VISIBLE
            view.findViewById<View>(R.id.appsPane).visibility =
                if (apps) View.VISIBLE else View.GONE
            val on = requireContext().getColor(R.color.accent)
            val off = requireContext().getColor(R.color.white)
            modeCalc.setTextColor(if (apps) off else on)
            modeApps.setTextColor(if (apps) on else off)
            if (apps) reloadApps(view) else paint(view)
        }
        modeCalc.setOnClickListener { showMode(false) }
        modeApps.setOnClickListener { showMode(true) }
        showMode(false)
    }

    override fun onResume() {
        super.onResume()
        if (appsMode) view?.let { reloadApps(it) }
    }

    private fun reloadApps(view: View) {
        val pm = requireContext().packageManager
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = if (Build.VERSION.SDK_INT >= 33) {
            pm.queryIntentActivities(query, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(query, PackageManager.MATCH_ALL)
        }
        val found = resolved
            .mapNotNull { info ->
                val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                val activity = info.activityInfo.name
                if (activity.endsWith(".ui.LauncherActivity")) return@mapNotNull null
                val label = info.loadLabel(pm).toString().trim().ifBlank { pkg }
                LaunchApp(label, pkg, activity)
            }
            .distinctBy { it.pkg + "/" + it.activity }
            .sortedBy { it.label.lowercase() }
        appRows.clear()
        appRows.addAll(found)
        appAdapter.notifyDataSetChanged()
        val empty = appRows.isEmpty()
        view.findViewById<View>(R.id.appsEmptyBox).visibility = if (empty) View.VISIBLE else View.GONE
        view.findViewById<View>(R.id.appsList).visibility = if (empty) View.GONE else View.VISIBLE
    }

    private fun openApp(app: LaunchApp) {
        val launch = requireContext().packageManager.getLaunchIntentForPackage(app.pkg)
            ?: Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setClassName(app.pkg, app.activity)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { startActivity(launch) }
    }

    private fun bindKeys(view: View) {
        val map = mapOf(
            R.id.c0 to "0", R.id.c1 to "1", R.id.c2 to "2", R.id.c3 to "3", R.id.c4 to "4",
            R.id.c5 to "5", R.id.c6 to "6", R.id.c7 to "7", R.id.c8 to "8", R.id.c9 to "9",
            R.id.cDot to ".", R.id.cAdd to "+", R.id.cSub to "-", R.id.cMul to "*", R.id.cDiv to "/",
            R.id.cClear to "C", R.id.cBack to "DEL", R.id.cEq to "=",
            R.id.cFt to "ft", R.id.cIn to "in",
            R.id.cLParen to "(", R.id.cRParen to ")"
        )
        map.forEach { (id, t) ->
            view.findViewById<TextView>(id)?.setOnClickListener { press(view, t) }
        }
    }

    private fun press(view: View, t: String) {
        when (t) {
            "C" -> {
                UnitConverter.clear()
                justEvaluated = false
            }
            "DEL" -> {
                UnitConverter.backspace()
                justEvaluated = false
            }
            "ft" -> UnitConverter.appendFt()
            "in" -> UnitConverter.appendIn()
            "+", "-", "*", "/" -> {
                justEvaluated = false
                UnitConverter.appendOp(t)
            }
            "=" -> {
                UnitConverter.evaluateInPlace()
                justEvaluated = true
            }
            "." -> UnitConverter.appendDot()
            "(", ")" -> {
                if (justEvaluated) {
                    UnitConverter.clear()
                    justEvaluated = false
                }
                UnitConverter.appendParen(t)
            }
            else -> if (t.length == 1 && t[0].isDigit()) {
                if (justEvaluated) {
                    UnitConverter.clear()
                    justEvaluated = false
                }
                UnitConverter.appendDigit(t)
            }
        }
        paint(view)
    }

    private fun paint(view: View) {
        view.findViewById<TextView>(R.id.calcDisplay)?.let {
            it.text = UnitConverter.display()
            DeskType.primary(it)
        }
        view.findViewById<TextView>(R.id.calcKind)?.text = UnitConverter.kindLabel()
        view.findViewById<TextView>(R.id.calcSummary)?.text =
            if (UnitConverter.raw.isEmpty() && UnitConverter.lastExpr.isEmpty()) "" else UnitConverter.summary()
        val g = UnitConverter.grid()
        view.findViewById<TextView>(R.id.unitM)?.text = g.m
        view.findViewById<TextView>(R.id.unitM2)?.text = g.m2
        view.findViewById<TextView>(R.id.unitM3)?.text = g.m3
        view.findViewById<TextView>(R.id.unitF)?.text = g.f
        view.findViewById<TextView>(R.id.unitF2)?.text = g.f2
        view.findViewById<TextView>(R.id.unitF3)?.text = g.f3
    }

    private data class LaunchApp(val label: String, val pkg: String, val activity: String)

    private class AppAdapter(
        private val rows: List<LaunchApp>,
        private val onOpen: (LaunchApp) -> Unit
    ) : RecyclerView.Adapter<AppAdapter.VH>() {
        class VH(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.appName)
            val pkg: TextView = view.findViewById(R.id.appPkg)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            return VH(LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false))
        }

        override fun getItemCount(): Int = rows.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = rows[position]
            holder.name.text = item.label
            DeskType.primary(holder.name)
            holder.pkg.text = item.pkg
            DeskType.meta(holder.pkg)
            holder.itemView.setOnClickListener { onOpen(item) }
        }
    }
}
