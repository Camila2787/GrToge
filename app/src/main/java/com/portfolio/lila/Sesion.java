package com.portfolio.lila;

import java.util.Calendar;

final class Sesion {
    static int perfilId = 1;
    static int anio;
    static int mes;

    static {
        Calendar calendario = Util.calendario();
        anio = calendario.get(Calendar.YEAR);
        mes = calendario.get(Calendar.MONTH) + 1;
    }

    private Sesion() {
    }
}
