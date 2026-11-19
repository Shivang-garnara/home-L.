package com.example.launcherai

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

class EndpointsAdapter(
    private val endpoints: List<EndpointInfo>
) : RecyclerView.Adapter<EndpointsAdapter.EndpointViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EndpointViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_endpoint, parent, false)
        return EndpointViewHolder(view)
    }

    override fun onBindViewHolder(holder: EndpointViewHolder, position: Int) {
        holder.bind(endpoints[position])
    }

    override fun getItemCount(): Int = endpoints.size

    inner class EndpointViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMethod: TextView = itemView.findViewById(R.id.tvMethod)
        private val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val tvUrl: TextView = itemView.findViewById(R.id.tvUrl)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        private val tvDetails: TextView = itemView.findViewById(R.id.tvDetails)

        fun bind(endpoint: EndpointInfo) {
            tvMethod.text = endpoint.method
            tvCategory.text = endpoint.category
            tvStatus.text = endpoint.status
            tvUrl.text = endpoint.url
            tvDescription.text = endpoint.description
            tvDetails.text = endpoint.details

            itemView.setOnClickListener {
                val clipboard = itemView.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Endpoint URL", endpoint.url)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(itemView.context, "Copied endpoint URL to clipboard", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
