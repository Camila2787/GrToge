package com.portfolio.lila;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.List;

public class InicioFragment extends Fragment {
    private View raiz;
    private TextView tvSaludo;
    private TextView tvEtiquetaQueda;
    private TextView tvTeQueda;
    private TextView tvDetalleIngreso;
    private TextView tvEstimado;
    private TextView pctGastos;
    private TextView pctAhorro;
    private TextView pctDeudas;
    private TextView pctLibre;
    private TextView montoGastos;
    private TextView montoAhorro;
    private TextView montoDeudas;
    private TextView montoLibre;
    private ProgressBar barGastos;
    private ProgressBar barAhorro;
    private ProgressBar barDeudas;
    private ProgressBar barLibre;
    private TextView tvEtiquetaCupo;
    private TextView tvCupo;
    private TextView tvHoyDetalle;
    private TextView aviso;
    private LinearLayout listaReciente;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        raiz = inflater.inflate(R.layout.fragment_inicio, container, false);
        tvSaludo = raiz.findViewById(R.id.tvSaludo);
        tvEtiquetaQueda = raiz.findViewById(R.id.tvEtiquetaQueda);
        tvTeQueda = raiz.findViewById(R.id.tvTeQueda);
        tvDetalleIngreso = raiz.findViewById(R.id.tvDetalleIngreso);
        tvEstimado = raiz.findViewById(R.id.tvEstimado);
        pctGastos = raiz.findViewById(R.id.pctGastos);
        pctAhorro = raiz.findViewById(R.id.pctAhorro);
        pctDeudas = raiz.findViewById(R.id.pctDeudas);
        pctLibre = raiz.findViewById(R.id.pctLibre);
        montoGastos = raiz.findViewById(R.id.montoGastos);
        montoAhorro = raiz.findViewById(R.id.montoAhorro);
        montoDeudas = raiz.findViewById(R.id.montoDeudas);
        montoLibre = raiz.findViewById(R.id.montoLibre);
        barGastos = raiz.findViewById(R.id.barGastos);
        barAhorro = raiz.findViewById(R.id.barAhorro);
        barDeudas = raiz.findViewById(R.id.barDeudas);
        barLibre = raiz.findViewById(R.id.barLibre);
        tvEtiquetaCupo = raiz.findViewById(R.id.tvEtiquetaCupo);
        tvCupo = raiz.findViewById(R.id.tvCupo);
        tvHoyDetalle = raiz.findViewById(R.id.tvHoyDetalle);
        aviso = raiz.findViewById(R.id.aviso);
        listaReciente = raiz.findViewById(R.id.listaReciente);
        Ui.prepararMes(raiz, this::pintar);
        Ui.prepararChips(raiz, this::pintar);
        aviso.setOnClickListener(v -> startActivity(
                new Intent(requireContext(), ListaActivity.class).putExtra("modo", "fijos")));
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
        LilaDb db = LilaDb.get(requireContext());
        Ui.pintarMes(raiz);
        Ui.pintarChips(raiz);
        if (Sesion.perfilId == 0) {
            tvSaludo.setText("El mes de los dos");
        } else {
            tvSaludo.setText("Hola, " + db.perfil(Sesion.perfilId).nombre);
        }

        Resumen resumen = db.resumen(Sesion.perfilId, Sesion.anio, Sesion.mes);
        long libre = resumen.libre();
        if (libre < 0) {
            tvEtiquetaQueda.setText("Te pasaste");
            tvTeQueda.setText(Util.cop(Math.abs(libre)));
        } else {
            tvEtiquetaQueda.setText("Te queda");
            tvTeQueda.setText(Util.cop(libre));
        }
        String ingresoTexto = "de " + Util.cop(resumen.ingresos) + " de ingresos";
        if (resumen.primas > 0) {
            ingresoTexto += ", incluida la prima de " + Util.cop(resumen.primas);
        }
        tvDetalleIngreso.setText(ingresoTexto);
        if (resumen.estimado) {
            tvEstimado.setVisibility(View.VISIBLE);
            tvEstimado.setText("Parte del ingreso está estimada, sin incapacidades");
        } else {
            tvEstimado.setVisibility(View.GONE);
        }

        ponerPorcentaje(pctGastos, barGastos, montoGastos, resumen.gastos(), resumen.ingresos);
        ponerPorcentaje(pctAhorro, barAhorro, montoAhorro, resumen.ahorroMes, resumen.ingresos);
        ponerPorcentaje(pctDeudas, barDeudas, montoDeudas, resumen.cuotas, resumen.ingresos);
        long libreVisible = Math.max(0, libre);
        ponerPorcentaje(pctLibre, barLibre, montoLibre, libreVisible, resumen.ingresos);
        if (libre < 0) {
            montoLibre.setText(Util.cop(libre));
        }

        if (resumen.relacionMes > 0) {
            tvEtiquetaCupo.setText("Cupo diario");
            tvCupo.setText(Util.cop(resumen.cupoDiario()));
            tvHoyDetalle.setText("Este mes todavía no empieza. El cupo usa el ingreso estimado y los gastos fijos.");
        } else if (resumen.relacionMes < 0) {
            tvEtiquetaCupo.setText("Gastos sueltos");
            tvCupo.setText(Util.cop(resumen.gastosVariables));
            tvHoyDetalle.setText("Mes cerrado. Esto salió en gastos que no se repiten.");
        } else {
            tvEtiquetaCupo.setText("Cupo diario");
            tvCupo.setText(Util.cop(resumen.cupoDiario()));
            long promedio = resumen.diaHoy == 0 ? 0 : resumen.gastosVariables / resumen.diaHoy;
            tvHoyDetalle.setText("Lo que queda, repartido en los días que faltan. Hoy llevas "
                    + Util.cop(resumen.gastadoHoy) + ". Promedio diario: " + Util.cop(promedio) + ".");
        }

        StringBuilder mensaje = new StringBuilder();
        if (resumen.estimado) {
            mensaje.append("Registra las quincenas en Ingreso para usar el valor real. ");
        }
        if (resumen.gastosFijos == 0) {
            mensaje.append("Agrega arriendo, servicios y lo demás que pagas cada quincena o cada mes.");
        }
        if (mensaje.length() == 0) {
            aviso.setVisibility(View.GONE);
        } else {
            aviso.setVisibility(View.VISIBLE);
            aviso.setText(mensaje.toString().trim());
        }

        listaReciente.removeAllViews();
        List<Gasto> gastos = db.gastosVariables(Sesion.perfilId, Sesion.anio, Sesion.mes);
        if (gastos.isEmpty()) {
            listaReciente.addView(linea("Todavía no hay gastos sueltos en este mes.", true));
        } else {
            int limite = Math.min(5, gastos.size());
            for (int i = 0; i < limite; i++) {
                Gasto gasto = gastos.get(i);
                listaReciente.addView(linea(gasto.nombre + "  ·  " + Util.cop(gasto.monto), false));
            }
        }
    }

    private void ponerPorcentaje(TextView porcentaje, ProgressBar barra, TextView monto, long parte, long ingresos) {
        int valor = ingresos <= 0 ? 0 : (int) Math.round(parte * 100.0 / ingresos);
        porcentaje.setText(valor + "%");
        barra.setProgress(Math.max(0, Math.min(valor, 100)));
        monto.setText(Util.cop(parte));
    }

    private TextView linea(String texto, boolean suave) {
        TextView vista = new TextView(requireContext());
        vista.setText(texto);
        vista.setTextSize(15);
        vista.setTextColor(requireContext().getColor(suave ? R.color.texto_suave : R.color.texto));
        vista.setPadding(0, 10, 0, 10);
        return vista;
    }
}
