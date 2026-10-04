package com.portfolio.lila;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class MasFragment extends Fragment {
    private View raiz;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        raiz = inflater.inflate(R.layout.fragment_mas, container, false);
        Ui.prepararChips(raiz, () -> Ui.pintarChips(raiz));
        raiz.findViewById(R.id.btnFijos).setOnClickListener(v -> abrir("fijos"));
        raiz.findViewById(R.id.btnDeudas).setOnClickListener(v -> abrir("deudas"));
        raiz.findViewById(R.id.btnAhorro).setOnClickListener(v -> abrir("ahorros"));
        raiz.findViewById(R.id.btnMetas).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), MetasActivity.class)));
        raiz.findViewById(R.id.btnPrimas).setOnClickListener(v -> abrir("primas"));
        raiz.findViewById(R.id.btnHistorial).setOnClickListener(v -> abrir("historial"));
        raiz.findViewById(R.id.btnAjustes).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AjustesActivity.class)));
        return raiz;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (raiz != null) {
            Ui.pintarChips(raiz);
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && raiz != null) {
            Ui.pintarChips(raiz);
        }
    }

    private void abrir(String modo) {
        startActivity(new Intent(requireContext(), ListaActivity.class).putExtra("modo", modo));
    }
}
