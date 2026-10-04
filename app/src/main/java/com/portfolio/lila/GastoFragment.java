package com.portfolio.lila;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.Calendar;
import java.util.List;

public class GastoFragment extends Fragment {
    private View raiz;
    private EditText campoMonto;
    private EditText campoNota;
    private ChipGroup categorias;
    private TextView tvFecha;
    private Spinner spinnerPerfil;
    private SwitchMaterial switchFijo;
    private ChipGroup ritmoFijo;
    private TextView tvImpacto;
    private LinearLayout listaHoy;
    private String fecha = Util.hoy();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        raiz = inflater.inflate(R.layout.fragment_gasto, container, false);
        campoMonto = raiz.findViewById(R.id.campoMonto);
        campoNota = raiz.findViewById(R.id.campoNota);
        categorias = raiz.findViewById(R.id.categorias);
        tvFecha = raiz.findViewById(R.id.tvFecha);
        spinnerPerfil = raiz.findViewById(R.id.spinnerPerfil);
        switchFijo = raiz.findViewById(R.id.switchFijo);
        ritmoFijo = raiz.findViewById(R.id.ritmoFijo);
        tvImpacto = raiz.findViewById(R.id.tvImpacto);
        listaHoy = raiz.findViewById(R.id.listaHoy);

        tvFecha.setText(Util.fechaBonita(fecha));
        tvFecha.setOnClickListener(v -> elegirFecha());
        campoMonto.addTextChangedListener(vigilante());
        switchFijo.setOnCheckedChangeListener((button, checked) -> {
            ritmoFijo.setVisibility(checked ? View.VISIBLE : View.GONE);
            actualizarImpacto();
        });
        ritmoFijo.setOnCheckedStateChangeListener((grupo, ids) -> actualizarImpacto());
        spinnerPerfil.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                actualizarImpacto();
                pintarHoy();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
        raiz.findViewById(R.id.btnGuardarGasto).setOnClickListener(v -> guardar());
        return raiz;
    }

    @Override
    public void onResume() {
        super.onResume();
        cargarPerfiles();
        actualizarImpacto();
        pintarHoy();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && raiz != null) {
            cargarPerfiles();
            actualizarImpacto();
            pintarHoy();
        }
    }

    private void cargarPerfiles() {
        LilaDb db = LilaDb.get(requireContext());
        String[] nombres = {
                "Los dos",
                db.perfil(1).nombre,
                db.perfil(2).nombre
        };
        ArrayAdapter<String> adaptador = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, nombres);
        int seleccion = spinnerPerfil.getSelectedItemPosition();
        spinnerPerfil.setAdapter(adaptador);
        if (seleccion >= 0) {
            spinnerPerfil.setSelection(seleccion);
        } else {
            spinnerPerfil.setSelection(Math.max(0, Math.min(2, Sesion.perfilId)));
        }
    }

    private void elegirFecha() {
        Calendar calendario = Util.calendario();
        int anio = Integer.parseInt(fecha.substring(0, 4));
        int mes = Integer.parseInt(fecha.substring(5, 7)) - 1;
        int dia = Integer.parseInt(fecha.substring(8, 10));
        calendario.set(anio, mes, dia);
        new DatePickerDialog(requireContext(), (vista, y, m, d) -> {
            fecha = Util.iso(y, m + 1, d);
            tvFecha.setText(Util.fechaBonita(fecha));
        }, anio, mes, dia).show();
    }

    private void guardar() {
        long monto = Util.parse(campoMonto.getText());
        if (monto <= 0) {
            Toast.makeText(requireContext(), "Escribe el monto", Toast.LENGTH_SHORT).show();
            return;
        }
        int chipId = categorias.getCheckedChipId();
        Chip chip = raiz.findViewById(chipId);
        String categoria = chip == null ? "Otro" : chip.getText().toString();
        String nota = campoNota.getText().toString().trim();
        if (nota.isEmpty()) {
            nota = categoria;
        }
        int perfil = spinnerPerfil.getSelectedItemPosition();
        if (perfil < 0) {
            perfil = 0;
        }
        boolean fijo = switchFijo.isChecked();
        int periodo = fijo ? periodoFijo() : 0;
        LilaDb.get(requireContext()).insertarGasto(perfil, nota, monto, categoria, fecha, fijo, periodo);
        Toast.makeText(requireContext(), fijo ? "Quedó en Gastos fijos" : "Gasto guardado", Toast.LENGTH_SHORT).show();
        campoMonto.setText("");
        campoNota.setText("");
        ritmoFijo.check(R.id.fijoCadaQuincena);
        switchFijo.setChecked(false);
        fecha = Util.hoy();
        tvFecha.setText(Util.fechaBonita(fecha));
        pintarHoy();
        actualizarImpacto();
    }

    @SuppressLint("SetTextI18n")
    private void actualizarImpacto() {
        if (tvImpacto == null) {
            return;
        }
        long monto = Util.parse(campoMonto.getText());
        int perfil = Math.max(0, spinnerPerfil.getSelectedItemPosition());
        if (switchFijo.isChecked()) {
            tvImpacto.setText(textoFijo(monto));
            return;
        }
        long hoy = LilaDb.get(requireContext()).sumaHoy(perfil);
        if (monto <= 0) {
            tvImpacto.setText("Hoy llevas " + Util.cop(hoy) + ".");
        } else {
            tvImpacto.setText("Hoy llevas " + Util.cop(hoy) + ". Con este gasto irías en " + Util.cop(hoy + monto) + ".");
        }
    }

    private int periodoFijo() {
        int id = ritmoFijo.getCheckedChipId();
        if (id == R.id.fijoQuincena1) {
            return 1;
        }
        if (id == R.id.fijoQuincena2) {
            return 2;
        }
        if (id == R.id.fijoCadaMes) {
            return 0;
        }
        return 3;
    }

    private String textoFijo(long monto) {
        int periodo = periodoFijo();
        if (periodo == 3) {
            return monto <= 0
                    ? "Sale las dos quincenas. En el mes cuenta el doble."
                    : "Cada quincena " + Util.cop(monto) + ". En el mes, " + Util.cop(monto * 2) + ".";
        }
        if (periodo == 1) {
            return monto <= 0
                    ? "Solo sale en la primera quincena."
                    : "Se reservarán " + Util.cop(monto) + " en la primera quincena.";
        }
        if (periodo == 2) {
            return monto <= 0
                    ? "Solo sale en la segunda quincena."
                    : "Se reservarán " + Util.cop(monto) + " en la segunda quincena.";
        }
        return monto <= 0
                ? "Se reserva una sola vez en el mes."
                : "Se reservarán " + Util.cop(monto) + " una vez al mes.";
    }

    private void pintarHoy() {
        if (listaHoy == null) {
            return;
        }
        listaHoy.removeAllViews();
        int perfil = Math.max(0, spinnerPerfil.getSelectedItemPosition());
        List<Gasto> gastos = LilaDb.get(requireContext()).gastosDeHoy(perfil);
        if (gastos.isEmpty()) {
            TextView vacio = new TextView(requireContext());
            vacio.setText("Hoy todavía no hay gastos sueltos.");
            vacio.setTextColor(requireContext().getColor(R.color.texto_suave));
            listaHoy.addView(vacio);
            return;
        }
        for (Gasto gasto : gastos) {
            TextView linea = new TextView(requireContext());
            linea.setText(gasto.nombre + "  ·  " + Util.cop(gasto.monto));
            linea.setTextColor(requireContext().getColor(R.color.texto));
            linea.setPadding(0, 8, 0, 8);
            listaHoy.addView(linea);
        }
    }

    private TextWatcher vigilante() {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                actualizarImpacto();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };
    }
}
