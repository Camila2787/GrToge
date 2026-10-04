package com.portfolio.lila;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class MetasActivity extends AppCompatActivity {
    private final List<Fila> filas = new ArrayList<>();
    private LilaDb db;
    private TextView vacio;
    private FilaAdapter adaptador;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista);
        db = LilaDb.get(this);
        TextView titulo = findViewById(R.id.tvTituloLista);
        titulo.setText("Metas");
        vacio = findViewById(R.id.tvVacio);
        vacio.setText("Una meta tiene plazo, fecha de inicio y fecha para cumplirla.");
        MaterialButton agregar = findViewById(R.id.btnAgregar);
        agregar.setText("Nueva meta");
        agregar.setOnClickListener(v -> abrirFormulario());
        findViewById(R.id.btnAtras).setOnClickListener(v -> finish());
        RecyclerView lista = findViewById(R.id.lista);
        adaptador = new FilaAdapter(filas, this::alTocar);
        lista.setLayoutManager(new LinearLayoutManager(this));
        lista.setAdapter(adaptador);
        cargar();
    }

    private void cargar() {
        filas.clear();
        for (MetaAhorro meta : db.metas(Sesion.perfilId)) {
            long falta = Math.max(0, meta.objetivo - meta.ahorrado);
            long primas = db.sumaPrimasEnPlazo((int) meta.perfilId, meta.inicio, meta.fin);
            long faltaSalario = Math.max(0, falta - primas);
            int meses = mesesRestantes(meta.fin);
            long mensual = meses <= 0 ? faltaSalario : faltaSalario / meses;
            long quincenal = meses <= 0 ? faltaSalario : faltaSalario / Math.max(1, meses * 2);
            String plan;
            if (falta <= 0) {
                plan = "\nMeta cumplida";
            } else if (primas > 0 && faltaSalario <= 0) {
                plan = lineaPrimas((int) meta.perfilId, meta.inicio, meta.fin)
                        + "\nLa prima cubre lo que falta.";
            } else if (primas > 0) {
                plan = lineaPrimas((int) meta.perfilId, meta.inicio, meta.fin)
                        + "\nAparta " + Util.cop(mensual) + " al mes · " + Util.cop(quincenal) + " por quincena";
            } else {
                plan = "\nAparta " + Util.cop(mensual) + " al mes · " + Util.cop(quincenal) + " por quincena";
            }
            Fila fila = new Fila();
            fila.tipo = "meta";
            fila.id = meta.id;
            fila.titulo = meta.nombre;
            fila.detalle = meta.plazoBonito() + " · " + db.nombrePerfil(meta.perfilId)
                    + "\n" + fechaConAnio(meta.inicio) + " → " + fechaConAnio(meta.fin)
                    + " · " + Util.mesesEntre(meta.inicio, meta.fin) + " meses"
                    + "\n" + Util.cop(meta.ahorrado) + " de " + Util.cop(meta.objetivo)
                    + plan;
            fila.monto = Util.cop(falta <= 0 ? meta.ahorrado : falta);
            fila.colorMonto = colorPlazo(meta.plazo);
            fila.progreso = meta.progreso();
            filas.add(fila);
        }
        vacio.setVisibility(filas.isEmpty() ? View.VISIBLE : View.GONE);
        adaptador.notifyDataSetChanged();
    }

    private void alTocar(Fila fila) {
        EditText monto = new EditText(this);
        monto.setHint("Cuánto apartas hoy");
        monto.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        monto.setTextColor(getColor(R.color.texto));
        new MaterialAlertDialogBuilder(this)
                .setTitle(fila.titulo)
                .setMessage(fila.detalle)
                .setView(monto)
                .setPositiveButton("Aportar", (d, w) -> {
                    long valor = Util.parse(monto.getText());
                    if (valor <= 0) {
                        Toast.makeText(this, "Escribe el monto", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    db.aportarMeta(fila.id, valor, Util.hoy());
                    cargar();
                })
                .setNeutralButton("Eliminar", (d, w) ->
                        new MaterialAlertDialogBuilder(this)
                                .setTitle("Quitar meta")
                                .setMessage("¿Quieres quitar " + fila.titulo + "?")
                                .setPositiveButton("Quitar", (a, b) -> {
                                    db.borrarMeta(fila.id);
                                    cargar();
                                })
                                .setNegativeButton("Cancelar", null)
                                .show())
                .setNegativeButton("Cerrar", null)
                .show();
    }

    private void abrirFormulario() {
        View formulario = getLayoutInflater().inflate(R.layout.dialog_meta, null);
        EditText nombre = formulario.findViewById(R.id.campoNombreMeta);
        EditText objetivo = formulario.findViewById(R.id.campoObjetivo);
        TextView corto = formulario.findViewById(R.id.plazoCorto);
        TextView mediano = formulario.findViewById(R.id.plazoMediano);
        TextView largo = formulario.findViewById(R.id.plazoLargo);
        TextView inicioVista = formulario.findViewById(R.id.btnInicioMeta);
        TextView finVista = formulario.findViewById(R.id.btnFinMeta);
        TextView lapso = formulario.findViewById(R.id.tvLapso);
        Spinner perfil = formulario.findViewById(R.id.spinnerMeta);
        perfil.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Los dos", db.perfil(1).nombre, db.perfil(2).nombre}));
        perfil.setSelection(Math.max(0, Math.min(2, Sesion.perfilId)));

        String[] plazo = {"CORTO"};
        String[] inicio = {Util.hoy()};
        String[] fin = {Util.sumarMeses(inicio[0], 6)};

        Runnable pintarPlazo = () -> {
            marcarPlazo(corto, "CORTO".equals(plazo[0]));
            marcarPlazo(mediano, "MEDIANO".equals(plazo[0]));
            marcarPlazo(largo, "LARGO".equals(plazo[0]));
        };
        Runnable pintarFechas = () -> {
            inicioVista.setText("Empieza: " + fechaConAnio(inicio[0]));
            finVista.setText("Termina: " + fechaConAnio(fin[0]));
            if (fin[0].compareTo(inicio[0]) <= 0) {
                lapso.setText("La fecha final tiene que ser después del inicio.");
                return;
            }
            long falta = Util.parse(objetivo.getText());
            int meses = Util.mesesEntre(inicio[0], fin[0]);
            int dueno = Math.max(0, perfil.getSelectedItemPosition());
            long primas = db.sumaPrimasEnPlazo(dueno, inicio[0], fin[0]);
            long faltaSalario = Math.max(0, falta - primas);
            long mensual = faltaSalario <= 0 ? 0 : faltaSalario / meses;
            long quincenal = faltaSalario <= 0 ? 0 : faltaSalario / Math.max(1, meses * 2);
            String texto = Util.diasEntre(inicio[0], fin[0]) + " días · " + meses + " meses.";
            if (primas > 0) {
                texto += " " + lineaPrimas(dueno, inicio[0], fin[0]).trim();
            }
            if (falta > 0 && faltaSalario <= 0) {
                texto += " La prima cubre esta meta.";
            } else if (falta > 0) {
                texto += " Apartarías " + Util.cop(mensual) + " al mes y "
                        + Util.cop(quincenal) + " por quincena.";
            }
            lapso.setText(texto);
        };
        perfil.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                pintarFechas.run();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
        pintarPlazo.run();
        pintarFechas.run();

        corto.setOnClickListener(v -> {
            plazo[0] = "CORTO";
            fin[0] = Util.sumarMeses(inicio[0], 6);
            pintarPlazo.run();
            pintarFechas.run();
        });
        mediano.setOnClickListener(v -> {
            plazo[0] = "MEDIANO";
            fin[0] = Util.sumarMeses(inicio[0], 24);
            pintarPlazo.run();
            pintarFechas.run();
        });
        largo.setOnClickListener(v -> {
            plazo[0] = "LARGO";
            fin[0] = Util.sumarMeses(inicio[0], 60);
            pintarPlazo.run();
            pintarFechas.run();
        });
        inicioVista.setOnClickListener(v -> elegirFecha(inicio[0], iso -> {
            inicio[0] = iso;
            if (fin[0].compareTo(inicio[0]) <= 0) {
                fin[0] = Util.sumarMeses(inicio[0], mesesDe(plazo[0]));
            }
            pintarFechas.run();
        }));
        finVista.setOnClickListener(v -> elegirFecha(fin[0], iso -> {
            fin[0] = iso;
            pintarFechas.run();
        }));
        objetivo.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                pintarFechas.run();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        new MaterialAlertDialogBuilder(this)
                .setTitle("Nueva meta")
                .setView(formulario)
                .setPositiveButton("Guardar", (d, w) -> {
                    String texto = nombre.getText().toString().trim();
                    long valor = Util.parse(objetivo.getText());
                    if (texto.isEmpty() || valor <= 0) {
                        Toast.makeText(this, "Escribe el nombre y el valor", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (fin[0].compareTo(inicio[0]) <= 0) {
                        Toast.makeText(this, "La fecha final tiene que ser después del inicio", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int dueno = Math.max(0, perfil.getSelectedItemPosition());
                    db.insertarMeta(dueno, texto, plazo[0], valor, inicio[0], fin[0]);
                    cargar();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void marcarPlazo(TextView chip, boolean activo) {
        chip.setBackgroundResource(activo ? R.drawable.bg_chip_on : R.drawable.bg_chip_off);
        chip.setTextColor(getColor(activo ? R.color.white : R.color.lila_oscuro));
    }

    private void elegirFecha(String actual, FechaElegida elegida) {
        int anio = Integer.parseInt(actual.substring(0, 4));
        int mes = Integer.parseInt(actual.substring(5, 7)) - 1;
        int dia = Integer.parseInt(actual.substring(8, 10));
        new DatePickerDialog(this, (vista, y, m, d) -> elegida.alElegir(Util.iso(y, m + 1, d)), anio, mes, dia).show();
    }

    private String lineaPrimas(int perfilId, String inicio, String fin) {
        List<Prima> primas = db.primasEnPlazo(perfilId, inicio, fin);
        if (primas.isEmpty()) {
            return "";
        }
        StringBuilder texto = new StringBuilder("\nPrima en este plazo: ");
        for (int i = 0; i < primas.size(); i++) {
            Prima prima = primas.get(i);
            if (i > 0) {
                texto.append(" · ");
            }
            texto.append(db.nombrePerfil(prima.perfilId))
                    .append(" ")
                    .append(Util.cop(prima.monto))
                    .append(" en ")
                    .append(Util.mesCorto(prima.mes));
        }
        return texto.toString();
    }

    private int mesesRestantes(String fin) {
        String hoy = Util.hoy();
        if (fin.compareTo(hoy) <= 0) {
            return 0;
        }
        return Util.mesesEntre(hoy, fin);
    }

    private int mesesDe(String plazo) {
        if ("LARGO".equals(plazo)) {
            return 60;
        }
        if ("MEDIANO".equals(plazo)) {
            return 24;
        }
        return 6;
    }

    private int colorPlazo(String plazo) {
        if ("LARGO".equals(plazo)) {
            return getColor(R.color.ahorro);
        }
        if ("MEDIANO".equals(plazo)) {
            return getColor(R.color.deuda);
        }
        return getColor(R.color.lila);
    }

    private String fechaConAnio(String iso) {
        return Util.fechaBonita(iso) + " de " + iso.substring(0, 4);
    }

    private interface FechaElegida {
        void alElegir(String iso);
    }
}
