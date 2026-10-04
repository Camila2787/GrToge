package com.portfolio.lila;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class MainActivity extends AppCompatActivity {

    private InicioFragment inicio;
    private GastoFragment gasto;
    private IngresoFragment ingreso;
    private MasFragment mas;
    private LinearLayout navInicio;
    private LinearLayout navGasto;
    private LinearLayout navIngreso;
    private LinearLayout navMas;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        LilaDb.get(this);

        navInicio = findViewById(R.id.navInicio);
        navGasto = findViewById(R.id.navGasto);
        navIngreso = findViewById(R.id.navIngreso);
        navMas = findViewById(R.id.navMas);

        FragmentManager manager = getSupportFragmentManager();
        if (savedInstanceState == null) {
            inicio = new InicioFragment();
            gasto = new GastoFragment();
            ingreso = new IngresoFragment();
            mas = new MasFragment();
            manager.beginTransaction()
                    .add(R.id.contenedor, mas, "mas").hide(mas)
                    .add(R.id.contenedor, ingreso, "ingreso").hide(ingreso)
                    .add(R.id.contenedor, gasto, "gasto").hide(gasto)
                    .add(R.id.contenedor, inicio, "inicio")
                    .commit();
            seleccionar(navInicio);
        } else {
            inicio = (InicioFragment) manager.findFragmentByTag("inicio");
            gasto = (GastoFragment) manager.findFragmentByTag("gasto");
            ingreso = (IngresoFragment) manager.findFragmentByTag("ingreso");
            mas = (MasFragment) manager.findFragmentByTag("mas");
            if (gasto != null && !gasto.isHidden()) {
                seleccionar(navGasto);
            } else if (ingreso != null && !ingreso.isHidden()) {
                seleccionar(navIngreso);
            } else if (mas != null && !mas.isHidden()) {
                seleccionar(navMas);
            } else {
                seleccionar(navInicio);
            }
        }

        navInicio.setOnClickListener(v -> mostrar(inicio, navInicio));
        navGasto.setOnClickListener(v -> mostrar(gasto, navGasto));
        navIngreso.setOnClickListener(v -> mostrar(ingreso, navIngreso));
        navMas.setOnClickListener(v -> mostrar(mas, navMas));
    }

    private void mostrar(Fragment destino, LinearLayout navegacion) {
        FragmentTransaction transaccion = getSupportFragmentManager().beginTransaction();
        transaccion.hide(inicio).hide(gasto).hide(ingreso).hide(mas).show(destino).commit();
        seleccionar(navegacion);
    }

    private void seleccionar(LinearLayout activo) {
        LinearLayout[] todos = {navInicio, navGasto, navIngreso, navMas};
        for (LinearLayout item : todos) {
            boolean encendido = item == activo;
            item.setBackgroundResource(encendido ? R.drawable.bg_chip_on : android.R.color.transparent);
            for (int i = 0; i < item.getChildCount(); i++) {
                View hijo = item.getChildAt(i);
                if (hijo instanceof TextView) {
                    ((TextView) hijo).setTextColor(getColor(encendido ? R.color.white : R.color.texto_suave));
                }
            }
        }
    }
}
