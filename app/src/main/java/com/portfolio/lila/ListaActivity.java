package com.portfolio.lila;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ListaActivity extends AppCompatActivity {
    private final List<Fila> filas = new ArrayList<>();
    private LilaDb db;
    private String modo = "fijos";
    private TextView vacio;
    private MaterialButton agregar;
    private FilaAdapter adaptador;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista);
        db = LilaDb.get(this);
        modo = getIntent().getStringExtra("modo");
        if (modo == null) {
            modo = "fijos";
        }
        TextView titulo = findViewById(R.id.tvTituloLista);
        vacio = findViewById(R.id.tvVacio);
        agregar = findViewById(R.id.btnAgregar);
        RecyclerView lista = findViewById(R.id.lista);
        findViewById(R.id.btnAtras).setOnClickListener(v -> finish());

        if ("deudas".equals(modo)) {
            titulo.setText("Deudas");
        } else if ("ahorros".equals(modo)) {
            titulo.setText("Ahorro");
        } else if ("historial".equals(modo)) {
            titulo.setText("Historial");
        } else if ("primas".equals(modo)) {
            titulo.setText("Primas");
        } else {
            titulo.setText("Gastos fijos");
        }

        adaptador = new FilaAdapter(filas, this::alTocar);
        lista.setLayoutManager(new LinearLayoutManager(this));
        lista.setAdapter(adaptador);
        agregar.setOnClickListener(v -> abrirFormulario());
        cargar();
    }

    private void cargar() {
        filas.clear();
        int perfil = Sesion.perfilId;
        if ("fijos".equals(modo)) {
            vacio.setText("Arriendo, servicios y lo que sale cada quincena o cada mes.");
            agregar.setText("Agregar gasto fijo");
            agregar.setVisibility(View.VISIBLE);
            for (Gasto gasto : db.gastosFijos(perfil)) {
                filas.add(filaGasto(gasto));
            }
        } else if ("deudas".equals(modo)) {
            vacio.setText("Anota el saldo y la cuota. GRToge aparta esa cuota del salario.");
            agregar.setText("Agregar deuda");
            agregar.setVisibility(View.VISIBLE);
            for (Deuda deuda : db.deudas(perfil)) {
                Fila fila = new Fila();
                fila.tipo = "deuda";
                fila.id = deuda.id;
                fila.titulo = deuda.nombre;
                fila.detalle = "Saldo " + Util.cop(deuda.saldo) + " · cuota " + Util.cop(deuda.cuota)
                        + " · " + db.nombrePerfil(deuda.perfilId);
                fila.monto = Util.cop(deuda.saldo);
                fila.colorMonto = getColor(R.color.deuda);
                fila.progreso = deuda.progreso();
                filas.add(fila);
            }
        } else if ("ahorros".equals(modo)) {
            vacio.setText("Crea una meta y ve sumando lo que apartas.");
            agregar.setText("Nueva meta");
            agregar.setVisibility(View.VISIBLE);
            for (Ahorro ahorro : db.ahorros(perfil)) {
                Fila fila = new Fila();
                fila.tipo = "ahorro";
                fila.id = ahorro.id;
                fila.titulo = ahorro.nombre;
                fila.detalle = "Llevas " + Util.cop(ahorro.ahorrado) + " de " + Util.cop(ahorro.meta)
                        + " · " + db.nombrePerfil(ahorro.perfilId);
                fila.monto = Util.cop(ahorro.ahorrado);
                fila.colorMonto = getColor(R.color.ahorro);
                fila.progreso = ahorro.progreso();
                filas.add(fila);
            }
        } else if ("primas".equals(modo)) {
            vacio.setText("La prima de diciembre ya está anotada. Si el plazo de una meta incluye ese mes, entra en el ahorro.");
            agregar.setText("Agregar prima");
            agregar.setVisibility(View.VISIBLE);
            for (Prima prima : db.primas(perfil)) {
                Fila fila = new Fila();
                fila.tipo = "prima";
                fila.id = prima.id;
                fila.titulo = prima.nombre;
                fila.detalle = Util.nombreMes(prima.anio, prima.mes) + " · " + db.nombrePerfil(prima.perfilId);
                fila.monto = Util.cop(prima.monto);
                fila.colorMonto = getColor(R.color.ahorro);
                filas.add(fila);
            }
        } else {
            vacio.setText("Cuando registres gastos o quincenas de " + Util.nombreMes(Sesion.anio, Sesion.mes) + ", aparecerán aquí.");
            agregar.setVisibility(View.GONE);
            for (Gasto gasto : db.gastosVariables(perfil, Sesion.anio, Sesion.mes)) {
                Fila fila = filaGasto(gasto);
                fila.orden = gasto.fecha + "-" + gasto.id;
                filas.add(fila);
            }
            for (Prima prima : db.primas(perfil)) {
                if (prima.anio != Sesion.anio || prima.mes != Sesion.mes) {
                    continue;
                }
                Fila fila = new Fila();
                fila.tipo = "prima";
                fila.id = prima.id;
                fila.titulo = prima.nombre;
                fila.detalle = db.nombrePerfil(prima.perfilId);
                fila.monto = Util.cop(prima.monto);
                fila.colorMonto = getColor(R.color.ahorro);
                fila.orden = Util.iso(prima.anio, prima.mes, 15) + "-" + prima.id;
                filas.add(fila);
            }
            for (IngresoReg ingreso : db.ingresosDelMes(perfil, Sesion.anio, Sesion.mes)) {
                Fila fila = new Fila();
                fila.tipo = "ingreso";
                fila.id = ingreso.id;
                fila.titulo = ingreso.periodo == 0
                        ? "Salario del mes"
                        : "Quincena " + ingreso.periodo;
                String extra = ingreso.diasIncapacidad > 0
                        ? ingreso.diasIncapacidad + " días de incapacidad · "
                        : "";
                fila.detalle = extra + db.nombrePerfil(ingreso.perfilId);
                fila.monto = Util.cop(ingreso.total);
                fila.colorMonto = getColor(R.color.ahorro);
                int dia = ingreso.periodo == 2 ? 16 : 1;
                fila.orden = Util.iso(ingreso.anio, ingreso.mes, dia) + "-" + ingreso.id;
                filas.add(fila);
            }
            Collections.sort(filas, (a, b) -> b.orden.compareTo(a.orden));
        }
        vacio.setVisibility(filas.isEmpty() ? View.VISIBLE : View.GONE);
        adaptador.notifyDataSetChanged();
    }

    private Fila filaGasto(Gasto gasto) {
        Fila fila = new Fila();
        fila.tipo = "gasto";
        fila.id = gasto.id;
        fila.titulo = gasto.nombre;
        if (gasto.fijo) {
            fila.detalle = ritmoFijo(gasto) + " · " + db.nombrePerfil(gasto.perfilId);
        } else {
            fila.detalle = gasto.categoria + " · " + db.nombrePerfil(gasto.perfilId);
            if (gasto.fecha != null) {
                fila.detalle = Util.fechaBonita(gasto.fecha) + " · " + fila.detalle;
            }
        }
        fila.monto = Util.cop(gasto.monto);
        fila.colorMonto = getColor(R.color.gasto);
        return fila;
    }

    private void alTocar(Fila fila) {
        if ("deuda".equals(fila.tipo)) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(fila.titulo)
                    .setMessage("Registrar el pago de la cuota descuenta ese valor del saldo.")
                    .setPositiveButton("Pagué la cuota", (d, w) -> {
                        db.pagarCuota(fila.id);
                        Toast.makeText(this, "Cuota registrada", Toast.LENGTH_SHORT).show();
                        cargar();
                    })
                    .setNeutralButton("Eliminar", (d, w) -> confirmarBorrado("esta deuda", () -> db.borrarDeuda(fila.id)))
                    .setNegativeButton("Cerrar", null)
                    .show();
        } else if ("ahorro".equals(fila.tipo)) {
            EditText monto = campo("Cuánto apartas", true);
            new MaterialAlertDialogBuilder(this)
                    .setTitle(fila.titulo)
                    .setView(monto)
                    .setPositiveButton("Aportar", (d, w) -> {
                        long valor = Util.parse(monto.getText());
                        if (valor <= 0) {
                            Toast.makeText(this, "Escribe el monto", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        db.aportar(fila.id, valor, Util.hoy());
                        cargar();
                    })
                    .setNeutralButton("Eliminar meta", (d, w) -> confirmarBorrado("esta meta", () -> db.borrarAhorro(fila.id)))
                    .setNegativeButton("Cerrar", null)
                    .show();
        } else if ("ingreso".equals(fila.tipo)) {
            confirmarBorrado("este ingreso", () -> db.borrarIngreso(fila.id));
        } else if ("prima".equals(fila.tipo)) {
            confirmarBorrado("esta prima", () -> db.borrarPrima(fila.id));
        } else {
            confirmarBorrado("este gasto", () -> db.borrarGasto(fila.id));
        }
    }

    private void confirmarBorrado(String cosa, Runnable accion) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Quitar")
                .setMessage("¿Quieres quitar " + cosa + "?")
                .setPositiveButton("Quitar", (d, w) -> {
                    accion.run();
                    cargar();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void abrirFormulario() {
        if ("deudas".equals(modo)) {
            formularioDeuda();
        } else if ("ahorros".equals(modo)) {
            formularioAhorro();
        } else if ("primas".equals(modo)) {
            formularioPrima();
        } else {
            formularioFijo();
        }
    }

    private void formularioFijo() {
        EditText nombre = campo("Nombre, por ejemplo arriendo", false);
        EditText monto = campo("Valor de la quincena", true);
        TextView ayuda = new TextView(this);
        ayuda.setTextColor(getColor(R.color.texto_suave));
        ayuda.setTextSize(13);
        int aire = (int) (8 * getResources().getDisplayMetrics().density);
        ayuda.setPadding(0, aire, 0, aire);
        final int[] periodo = {3};
        ChipGroup ritmo = ritmoFijo(periodo, monto, ayuda);
        Spinner perfil = spinnerPerfil();
        LinearLayout caja = caja(nombre, ritmo, monto, ayuda, perfil);
        new MaterialAlertDialogBuilder(this)
                .setTitle("Gasto fijo")
                .setView(caja)
                .setPositiveButton("Guardar", (d, w) -> {
                    long valor = Util.parse(monto.getText());
                    String texto = nombre.getText().toString().trim();
                    if (texto.isEmpty() || valor <= 0) {
                        Toast.makeText(this, "Escribe nombre y valor", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    db.insertarGasto(perfil.getSelectedItemPosition(), texto, valor, "Fijo", Util.hoy(), true, periodo[0]);
                    cargar();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private ChipGroup ritmoFijo(int[] periodo, EditText monto, TextView ayuda) {
        ChipGroup grupo = new ChipGroup(this);
        grupo.setSingleSelection(true);
        grupo.setSelectionRequired(true);
        grupo.addView(chipPeriodo("Cada quincena", 3, true));
        grupo.addView(chipPeriodo("Quincena 1", 1, false));
        grupo.addView(chipPeriodo("Quincena 2", 2, false));
        grupo.addView(chipPeriodo("Cada mes", 0, false));
        explicarPeriodo(3, monto, ayuda);
        grupo.setOnCheckedStateChangeListener((g, ids) -> {
            int elegido = 3;
            int id = grupo.getCheckedChipId();
            if (id != View.NO_ID) {
                Chip chip = grupo.findViewById(id);
                if (chip != null && chip.getTag() instanceof Integer) {
                    elegido = (Integer) chip.getTag();
                }
            }
            periodo[0] = elegido;
            explicarPeriodo(elegido, monto, ayuda);
        });
        return grupo;
    }

    private Chip chipPeriodo(String texto, int valor, boolean marcado) {
        Chip chip = new Chip(this);
        chip.setText(texto);
        chip.setCheckable(true);
        chip.setChecked(marcado);
        chip.setTag(valor);
        chip.setId(View.generateViewId());
        return chip;
    }

    private void explicarPeriodo(int periodo, EditText monto, TextView ayuda) {
        if (periodo == 0) {
            monto.setHint("Valor del mes");
            ayuda.setText("Se cuenta una sola vez en el mes.");
        } else if (periodo == 1) {
            monto.setHint("Valor de la quincena");
            ayuda.setText("Solo sale en la primera quincena.");
        } else if (periodo == 2) {
            monto.setHint("Valor de la quincena");
            ayuda.setText("Solo sale en la segunda quincena.");
        } else {
            monto.setHint("Valor de la quincena");
            ayuda.setText("Sale las dos quincenas. En el mes cuenta el doble.");
        }
    }

    private String ritmoFijo(Gasto gasto) {
        if (gasto.periodo == 3) {
            return "Cada quincena · en el mes " + Util.cop(gasto.monto * 2);
        }
        if (gasto.periodo == 1) {
            return "Solo quincena 1";
        }
        if (gasto.periodo == 2) {
            return "Solo quincena 2";
        }
        return "Cada mes";
    }

    private void formularioPrima() {
        EditText nombre = campo("Prima", false);
        nombre.setText("Prima");
        EditText monto = campo("Valor de la prima", true);
        Spinner mes = new Spinner(this);
        String[] meses = new String[12];
        for (int i = 1; i <= 12; i++) {
            String corto = Util.mesCorto(i);
            meses[i - 1] = Character.toUpperCase(corto.charAt(0)) + corto.substring(1);
        }
        mes.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, meses));
        mes.setSelection(11);
        EditText anio = campo("Año", true);
        anio.setText(String.valueOf(Util.calendario().get(java.util.Calendar.YEAR)));
        Spinner perfil = spinnerPerfil();
        LinearLayout caja = caja(nombre, monto, mes, anio, perfil);
        new MaterialAlertDialogBuilder(this)
                .setTitle("Prima")
                .setView(caja)
                .setPositiveButton("Guardar", (d, w) -> {
                    String texto = nombre.getText().toString().trim();
                    long valor = Util.parse(monto.getText());
                    int ano = (int) Util.parse(anio.getText());
                    if (texto.isEmpty() || valor <= 0 || ano < 2000) {
                        Toast.makeText(this, "Escribe nombre, valor y año", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    db.insertarPrima(perfil.getSelectedItemPosition(), texto, valor, ano, mes.getSelectedItemPosition() + 1);
                    cargar();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void formularioDeuda() {
        EditText nombre = campo("A quién o por qué", false);
        EditText saldo = campo("Saldo que debes", true);
        EditText cuota = campo("Cuota del mes", true);
        Spinner perfil = spinnerPerfil();
        LinearLayout caja = caja(nombre, saldo, cuota, perfil);
        new MaterialAlertDialogBuilder(this)
                .setTitle("Deuda")
                .setView(caja)
                .setPositiveButton("Guardar", (d, w) -> {
                    String texto = nombre.getText().toString().trim();
                    long valorSaldo = Util.parse(saldo.getText());
                    long valorCuota = Util.parse(cuota.getText());
                    if (texto.isEmpty() || valorSaldo <= 0 || valorCuota <= 0) {
                        Toast.makeText(this, "Completa nombre, saldo y cuota", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    db.insertarDeuda(perfil.getSelectedItemPosition(), texto, valorSaldo, valorCuota);
                    cargar();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void formularioAhorro() {
        EditText nombre = campo("Nombre de la meta", false);
        EditText meta = campo("Cuánto quieres juntar", true);
        Spinner perfil = spinnerPerfil();
        LinearLayout caja = caja(nombre, meta, perfil);
        new MaterialAlertDialogBuilder(this)
                .setTitle("Meta de ahorro")
                .setView(caja)
                .setPositiveButton("Crear", (d, w) -> {
                    String texto = nombre.getText().toString().trim();
                    long valor = Util.parse(meta.getText());
                    if (texto.isEmpty() || valor <= 0) {
                        Toast.makeText(this, "Escribe nombre y meta", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    db.insertarAhorro(perfil.getSelectedItemPosition(), texto, valor);
                    cargar();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private LinearLayout caja(View... vistas) {
        LinearLayout caja = new LinearLayout(this);
        caja.setOrientation(LinearLayout.VERTICAL);
        int espacio = (int) (12 * getResources().getDisplayMetrics().density);
        caja.setPadding(espacio * 2, espacio, espacio * 2, 0);
        for (View vista : vistas) {
            caja.addView(vista);
        }
        return caja;
    }

    private EditText campo(String hint, boolean numero) {
        EditText campo = new EditText(this);
        campo.setHint(hint);
        campo.setInputType(numero
                ? InputType.TYPE_CLASS_NUMBER
                : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        campo.setTextColor(getColor(R.color.texto));
        return campo;
    }

    private Spinner spinnerPerfil() {
        Spinner spinner = new Spinner(this);
        String[] nombres = {"Los dos", db.perfil(1).nombre, db.perfil(2).nombre};
        spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, nombres));
        spinner.setSelection(Math.max(0, Math.min(2, Sesion.perfilId)));
        return spinner;
    }
}
