package com.portfolio.lila;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FilaAdapter extends RecyclerView.Adapter<FilaAdapter.Holder> {
    interface Toque {
        void alTocar(Fila fila);
    }

    private final List<Fila> filas;
    private final Toque toque;

    FilaAdapter(List<Fila> filas, Toque toque) {
        this.filas = filas;
        this.toque = toque;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_fila, parent, false);
        return new Holder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Fila fila = filas.get(position);
        holder.titulo.setText(fila.titulo);
        holder.detalle.setText(fila.detalle);
        holder.monto.setText(fila.monto);
        holder.monto.setTextColor(fila.colorMonto);
        if (fila.progreso >= 0) {
            holder.barra.setVisibility(View.VISIBLE);
            holder.barra.setProgress(fila.progreso);
        } else {
            holder.barra.setVisibility(View.GONE);
        }
        holder.itemView.setOnClickListener(v -> toque.alTocar(fila));
    }

    @Override
    public int getItemCount() {
        return filas.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView titulo;
        final TextView detalle;
        final TextView monto;
        final ProgressBar barra;

        Holder(View vista) {
            super(vista);
            titulo = vista.findViewById(R.id.titulo);
            detalle = vista.findViewById(R.id.detalle);
            monto = vista.findViewById(R.id.monto);
            barra = vista.findViewById(R.id.barra);
        }
    }
}
