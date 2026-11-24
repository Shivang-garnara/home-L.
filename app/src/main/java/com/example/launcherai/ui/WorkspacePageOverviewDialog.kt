package com.example.launcherai.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.launcherai.R
import com.example.launcherai.data.db.WorkspacePageEntity
import com.google.android.material.bottomsheet.BottomSheetDialog

class WorkspacePageOverviewDialog(
    context: Context,
    private val viewModel: WorkspaceViewModel,
    private val onPageSelected: (screenId: Int) -> Unit
) : BottomSheetDialog(context, R.style.Theme_LauncherAI) {

    private lateinit var adapter: PageOverviewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_page_overview)

        behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
        behavior.skipCollapsed = true

        val rv = findViewById<RecyclerView>(R.id.rvPageOverviewList)
        val btnAddPage = findViewById<View>(R.id.btnAddPage)

        adapter = PageOverviewAdapter(
            pages = viewModel.pages.value,
            defaultScreenId = viewModel.getValidDefaultPage(),
            pageItemsMap = viewModel.resolvedDisplayPageItemsMap.value,
            onSelectPage = { screenId ->
                dismiss()
                onPageSelected(screenId)
            },
            onSetDefault = { screenId ->
                viewModel.setDefaultPage(screenId)
                adapter.updateDefaultScreenId(screenId)
                Toast.makeText(context, "Default home page updated", Toast.LENGTH_SHORT).show()
            },
            onDeletePage = { screenId ->
                val currentPages = viewModel.pages.value
                if (currentPages.size <= 1) {
                    Toast.makeText(context, "Cannot delete the only page", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.deletePage(screenId) { success ->
                        if (success) {
                            Toast.makeText(context, "Page removed", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        )

        rv?.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        rv?.adapter = adapter

        val touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPos = viewHolder.bindingAdapterPosition
                val toPos = target.bindingAdapterPosition
                if (fromPos != RecyclerView.NO_POSITION && toPos != RecyclerView.NO_POSITION) {
                    adapter.movePageItem(fromPos, toPos)
                    viewModel.reorderPages(adapter.getCurrentScreenOrder())
                    return true
                }
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
        })
        touchHelper.attachToRecyclerView(rv)

        btnAddPage?.setOnClickListener {
            viewModel.addNewPage()
            Toast.makeText(context, "New page added", Toast.LENGTH_SHORT).show()
        }
    }

    private inner class PageOverviewAdapter(
        pages: List<WorkspacePageEntity>,
        private var defaultScreenId: Int,
        private val pageItemsMap: Map<Int, List<WorkspaceItemDisplay>>,
        private val onSelectPage: (screenId: Int) -> Unit,
        private val onSetDefault: (screenId: Int) -> Unit,
        private val onDeletePage: (screenId: Int) -> Unit
    ) : RecyclerView.Adapter<PageOverviewAdapter.PageViewHolder>() {

        private val pagesList = pages.toMutableList()

        fun updateDefaultScreenId(screenId: Int) {
            defaultScreenId = screenId
            notifyDataSetChanged()
        }

        fun movePageItem(fromPos: Int, toPos: Int) {
            val item = pagesList.removeAt(fromPos)
            pagesList.add(toPos, item)
            notifyItemMoved(fromPos, toPos)
        }

        fun getCurrentScreenOrder(): List<Int> {
            return pagesList.map { it.screenId }
        }

        override fun getItemCount(): Int = pagesList.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_page_overview_card, parent, false)
            return PageViewHolder(v)
        }

        override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
            val page = pagesList[position]
            holder.bind(page, position)
        }

        inner class PageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val ivSetDefault: ImageView = view.findViewById(R.id.ivSetDefaultPage)
            private val ivDelete: ImageView = view.findViewById(R.id.ivDeletePage)
            private val containerCard: View = view.findViewById(R.id.cardPageSnapshotContainer)
            private val tvTitle: TextView = view.findViewById(R.id.tvPageTitleBadge)

            fun bind(page: WorkspacePageEntity, pos: Int) {
                val isDefault = page.screenId == defaultScreenId
                tvTitle.text = "Page ${pos + 1}${if (isDefault) " (Default)" else ""}"

                if (isDefault) {
                    ivSetDefault.setColorFilter(ContextCompat.getColor(context, R.color.nova_accent_orange))
                } else {
                    ivSetDefault.setColorFilter(ContextCompat.getColor(context, R.color.text_secondary))
                }

                ivSetDefault.setOnClickListener {
                    onSetDefault(page.screenId)
                }

                ivDelete.visibility = if (pagesList.size > 1) View.VISIBLE else View.GONE
                ivDelete.setOnClickListener {
                    onDeletePage(page.screenId)
                }

                containerCard.setOnClickListener {
                    onSelectPage(page.screenId)
                }
            }
        }
    }
}
