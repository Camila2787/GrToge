package com.portfolio.lila;

import android.view.View;
import android.widget.TextView;

final class Ui {
    private Ui() {
    }

    static void prepararMes(View raiz, Runnable cambio) {
        raiz.findViewById(R.id.btnMesAnt).setOnClickListener(v -> {
            if (Sesion.mes == 1) {
                Sesion.mes = 12;
                Sesion.anio--;
            } else {
                Sesion.mes--;
            }
            cambio.run();
        });
        raiz.findViewById(R.id.btnMesSig).setOnClickListener(v -> {
            if (Sesion.mes == 12) {
                Sesion.mes = 1;
                Sesion.anio++;
            } else {
                Sesion.mes++;
            }
            cambio.run();
        });
    }

    static void pintarMes(View raiz) {
        TextView mes = raiz.findViewById(R.id.tvMes);
        mes.setText(Util.nombreMes(Sesion.anio, Sesion.mes));
    }

    static void prepararChips(View raiz, Runnable cambio) {
        raiz.findViewById(R.id.chipCamila).setOnClickListener(v -> {
            Sesion.perfilId = 1;
            cambio.run();
        });
        raiz.findViewById(R.id.chipPareja).setOnClickListener(v -> {
            Sesion.perfilId = 2;
            cambio.run();
        });
        raiz.findViewById(R.id.chipDos).setOnClickListener(v -> {
            Sesion.perfilId = 0;
            cambio.run();
        });
    }

    static void pintarChips(View raiz) {
        LilaDb db = LilaDb.get(raiz.getContext());
        TextView camila = raiz.findViewById(R.id.chipCamila);
        TextView pareja = raiz.findViewById(R.id.chipPareja);
        TextView dos = raiz.findViewById(R.id.chipDos);
        camila.setText(db.perfil(1).nombre);
        pareja.setText(db.perfil(2).nombre);
        marcar(camila, Sesion.perfilId == 1);
        marcar(pareja, Sesion.perfilId == 2);
        marcar(dos, Sesion.perfilId == 0);
    }

    private static void marcar(TextView chip, boolean activo) {
        chip.setBackgroundResource(activo ? R.drawable.bg_chip_on : R.drawable.bg_chip_off);
        chip.setTextColor(chip.getContext().getColor(activo ? R.color.white : R.color.lila_oscuro));
    }
}
