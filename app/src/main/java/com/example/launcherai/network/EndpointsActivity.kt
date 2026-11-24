package com.example.launcherai.network

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.launcherai.R

class EndpointsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_endpoints)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        val endpoints = NovaNetworkRepository.discoveredEndpoints
        findViewById<TextView>(R.id.tvEndpointCount).text = "${endpoints.size} Decompiled Endpoints Traced via REA"

        val rv = findViewById<RecyclerView>(R.id.rvEndpoints)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = EndpointsAdapter(endpoints)
    }
}
