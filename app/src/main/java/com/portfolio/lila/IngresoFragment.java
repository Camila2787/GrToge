package com.portfolio.lila;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

public class IngresoFragment extends Fragment {
    private View raiz;
    private LinearLayout contenedor;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        raiz = inflater.inflate(R.layout.fragment_ingreso, container, false);
        contenedor = raiz.findViewById(R.id.contenedorIngresos);
        Ui.prepararMes(raiz, this::pintar);
        Ui.prepararChips(raiz, this::pintar);
        return raiz;
    }

    @Override
    public void onResume() {
        super.onResume();
        pintar();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && raiz != null) {
            pintar();
        }
    }

    private void pintar() {
        if (raiz == null) {
            return;
        }
        Ui.pintarMes(raiz);
        Ui.pintarChips(raiz);
        contenedor.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        LilaDb db = LilaDb.get(requireContext());
        if (Sesion.perfilId == 0 || Sesion.perfilId == 1) {
            agregarPerfil(inflater, db.perfil(1));
        }
        if (Sesion.perfilId == 0 || Sesion.perfilId == 2) {
            agregarPerfil(inflater, db.perfil(2));
        }
    }

    private void agregarPerfil(LayoutInflater inflater, Perfil perfil) {
        if (perfil.quincenal()) {
            contenedor.addView(tarjetaQuincena(inflater, perfil, 1));
            contenedor.addView(tarjetaQuincena(inflater, perfil, 2));
        } else {
            contenedor.addView(tarjetaMensual(inflater, perfil));
        }
    }

    private View tarjetaQuincena(LayoutInflater inflater, Perfil perfil, int periodo) {
        View tarjeta = inflater.inflate(R.layout.card_quincena, contenedor, false);
        TextView titulo = tarjeta.findViewById(R.id.tvTitulo);
        TextView estado = tarjeta.findViewById(R.id.tvEstado);
        TextView regla = tarjeta.findViewById(R.id.tvRegla);
        TextView diasVista = tarjeta.findViewById(R.id.tvDias);
        TextView trabajados = tarjeta.findViewById(R.id.tvTrabajados);
        TextView formula = tarjeta.findViewById(R.id.tvFormula);
        TextView base = tarjeta.findViewById(R.id.tvBase);
        TextView sugerido = tarjeta.findViewById(R.id.tvSugerido);
        EditText campoTotal = tarjeta.findViewById(R.id.campoTotal);
        MaterialButton menos = tarjeta.findViewById(R.id.btnMenos);
        MaterialButton mas = tarjeta.findViewById(R.id.btnMas);
        MaterialButton guardar = tarjeta.findViewById(R.id.btnGuardarQuincena);

        int ultimoDia = Util.diasDelMes(Sesion.anio, Sesion.mes);
        String rango = periodo == 1
                ? "1 al 15 de " + Util.mesCorto(Sesion.mes)
                : "16 al " + ultimoDia + " de " + Util.mesCorto(Sesion.mes);
        titulo.setText(perfil.nombre + " · Quincena " + periodo);
        if (perfil.bono > 0) {
            regla.setText("El bono se paga por día trabajado. La quincena tiene 15 días, así que cada incapacidad descuenta una quinceava parte del bono. El salario base no se descuenta aquí; si el depósito fue otro, corrige el total. Periodo: " + rango + ".");
        } else {
            regla.setText("Cada día de incapacidad descuenta una quinceava parte de esta quincena. Si el depósito fue otro, corrige el total. Periodo: " + rango + ".");
        }

        IngresoReg registro = LilaDb.get(requireContext()).ingreso(perfil.id, Sesion.anio, Sesion.mes, periodo);
        int[] dias = {registro == null ? 0 : registro.diasIncapacidad};

        Runnable aplicar = () -> {
            int trabajadosDias = Util.DIAS_QUINCENA - dias[0];
            long calculado = totalQuincena(perfil, dias[0]);
            diasVista.setText(String.valueOf(dias[0]));
            menos.setEnabled(dias[0] > 0);
            mas.setEnabled(dias[0] < Util.DIAS_QUINCENA);
            trabajados.setText(dias[0] == 0
                    ? "Quincena completa, sin incapacidad"
                    : "Días pagados: " + trabajadosDias + " de 15");
            if (perfil.bono > 0) {
                long bono = Util.bonoPorDias(perfil.bono, dias[0]);
                formula.setText("Bono: " + Util.cop(Util.bonoDiario(perfil.bono)) + " × " + trabajadosDias
                        + " = " + Util.cop(bono));
                base.setText("Salario base: " + Util.cop(perfil.base));
            } else {
                formula.setText("Por día: " + Util.cop(Util.bonoDiario(perfil.base)) + " × " + trabajadosDias
                        + " = " + Util.cop(calculado));
                base.setText("Quincena completa: " + Util.cop(perfil.base));
            }
            sugerido.setText("Sugerido: " + Util.cop(calculado));
            campoTotal.setText(String.valueOf(calculado));
        };

        if (registro == null) {
            estado.setText("Sin registrar");
            aplicar.run();
        } else {
            estado.setText("Registrada");
            aplicar.run();
            sugerido.setText("Total registrado: " + Util.cop(registro.total));
            campoTotal.setText(String.valueOf(registro.total));
        }

        menos.setOnClickListener(v -> {
            if (dias[0] == 0) {
                return;
            }
            dias[0]--;
            estado.setText("Sin guardar los cambios");
            aplicar.run();
        });
        mas.setOnClickListener(v -> {
            if (dias[0] >= Util.DIAS_QUINCENA) {
                return;
            }
            dias[0]++;
            estado.setText("Sin guardar los cambios");
            aplicar.run();
        });
        guardar.setOnClickListener(v -> {
            long bono = perfil.bono > 0 ? Util.bonoPorDias(perfil.bono, dias[0]) : 0;
            long baseGuardada = perfil.bono > 0 ? perfil.base : Util.bonoPorDias(perfil.base, dias[0]);
            long total = Util.parse(campoTotal.getText());
            if (total <= 0) {
                total = baseGuardada + bono;
            }
            LilaDb.get(requireContext()).guardarIngreso(
                    perfil.id, Sesion.anio, Sesion.mes, periodo, dias[0], baseGuardada, bono, total);
            Toast.makeText(requireContext(), "Quincena guardada", Toast.LENGTH_SHORT).show();
            pintar();
        });
        return tarjeta;
    }

    private long totalQuincena(Perfil perfil, int diasIncapacidad) {
        if (perfil.bono > 0) {
            return perfil.base + Util.bonoPorDias(perfil.bono, diasIncapacidad);
        }
        return Util.bonoPorDias(perfil.base, diasIncapacidad);
    }

    private View tarjetaMensual(LayoutInflater inflater, Perfil perfil) {
        View tarjeta = inflater.inflate(R.layout.card_mensual, contenedor, false);
        TextView titulo = tarjeta.findViewById(R.id.tvTitulo);
        TextView estado = tarjeta.findViewById(R.id.tvEstado);
        TextView regla = tarjeta.findViewById(R.id.tvRegla);
        TextView sugerido = tarjeta.findViewById(R.id.tvSugerido);
        EditText campoTotal = tarjeta.findViewById(R.id.campoTotal);
        titulo.setText(perfil.nombre + " · salario del mes");
        regla.setText("Salario base fijo de un mes. No depende de un bono por días trabajados.");
        sugerido.setText("Salario: " + Util.cop(perfil.base));
        IngresoReg registro = LilaDb.get(requireContext()).ingreso(perfil.id, Sesion.anio, Sesion.mes, 0);
        if (registro == null) {
            estado.setText("Sin registrar");
            campoTotal.setText(String.valueOf(perfil.base));
        } else {
            estado.setText("Registrado");
            campoTotal.setText(String.valueOf(registro.total));
        }
        tarjeta.findViewById(R.id.btnGuardarMes).setOnClickListener(v -> {
            long total = Util.parse(campoTotal.getText());
            if (total <= 0) {
                total = perfil.base;
            }
            LilaDb.get(requireContext()).guardarIngreso(
                    perfil.id, Sesion.anio, Sesion.mes, 0, 0, perfil.base, 0, total);
            Toast.makeText(requireContext(), "Salario del mes guardado", Toast.LENGTH_SHORT).show();
            pintar();
        });
        return tarjeta;
    }
}
