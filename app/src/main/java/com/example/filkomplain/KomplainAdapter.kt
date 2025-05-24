package com.example.filkomplain.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.filkomplain.R
import com.example.filkomplain.model.KomplainModel
import com.google.android.material.imageview.ShapeableImageView

class KomplainAdapter(private val listKomplain: List<KomplainModel>) :
    RecyclerView.Adapter<KomplainAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val phItemKomplain: ShapeableImageView = itemView.findViewById(R.id.phItemKomplain)
        val titleItemKomplain: TextView = itemView.findViewById(R.id.titleItemKomplain)
        val textLokasiKomplain: TextView = itemView.findViewById(R.id.textLokasiKomplain)
        val textTanggalKomplain: TextView = itemView.findViewById(R.id.textTanggalKomplain)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_komplain, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val komplain = listKomplain[position]
        holder.titleItemKomplain.text = komplain.judul
        holder.textLokasiKomplain.text = komplain.lokasi
        holder.textTanggalKomplain.text = komplain.tanggal
        Glide.with(holder.itemView.context)
            .load(komplain.imageUrl)
            .placeholder(R.drawable.ph_img_komplain)
            .into(holder.phItemKomplain)
    }

    override fun getItemCount(): Int = listKomplain.size
}