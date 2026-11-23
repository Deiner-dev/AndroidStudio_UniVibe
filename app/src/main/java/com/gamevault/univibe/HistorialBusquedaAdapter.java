package com.gamevault.univibe;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class HistorialBusquedaAdapter extends RecyclerView.Adapter<HistorialBusquedaAdapter.HistorialViewHolder> {

    public interface OnHistorialItemClickListener {
        void onTermClick(String term);
        void onTermDelete(String term);
    }

    private final List<String> historialList;
    private final OnHistorialItemClickListener listener;

    public HistorialBusquedaAdapter(List<String> historialList, OnHistorialItemClickListener listener) {
        this.historialList = historialList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public HistorialViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_busqueda_reciente, parent, false);
        return new HistorialViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistorialViewHolder holder, int position) {
        String term = historialList.get(position);
        holder.tvRecentTerm.setText(term);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTermClick(term);
            }
        });

        holder.ivDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTermDelete(term);
            }
        });
    }

    @Override
    public int getItemCount() {
        return historialList != null ? historialList.size() : 0;
    }

    public static class HistorialViewHolder extends RecyclerView.ViewHolder {
        TextView tvRecentTerm;
        ImageView ivDelete;

        public HistorialViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRecentTerm = itemView.findViewById(R.id.tv_recent_term);
            ivDelete = itemView.findViewById(R.id.iv_delete_recent);
        }
    }
}
