package com.portfolio.lila;

class Perfil {
    long id;
    String nombre;
    String tipo;
    long base;
    long bono;

    boolean quincenal() {
        return "QUINCENAL".equals(tipo);
    }
}

class IngresoReg {
    long id;
    long perfilId;
    int anio;
    int mes;
    int periodo;
    int diasIncapacidad;
    long base;
    long bono;
    long total;
}

class Gasto {
    long id;
    long perfilId;
    String nombre;
    long monto;
    String categoria;
    String fecha;
    boolean fijo;
    int periodo;
}

class Deuda {
    long id;
    long perfilId;
    String nombre;
    long inicial;
    long saldo;
    long cuota;

    int progreso() {
        if (inicial <= 0) {
            return 0;
        }
        long pagado = Math.max(0, inicial - saldo);
        return (int) Math.min(100, Math.round(pagado * 100.0 / inicial));
    }
}

class Ahorro {
    long id;
    long perfilId;
    String nombre;
    long meta;
    long ahorrado;

    int progreso() {
        if (meta <= 0) {
            return 0;
        }
        return (int) Math.min(100, Math.round(ahorrado * 100.0 / meta));
    }
}

class Resumen {
    long ingresos;
    long primas;
    boolean estimado;
    long gastosFijos;
    long gastosVariables;
    long cuotas;
    long ahorroMes;
    long gastadoHoy;
    int diasEnMes;
    int diaHoy;
    int relacionMes;

    long gastos() {
        return gastosFijos + gastosVariables;
    }

    long libre() {
        return ingresos - gastos() - cuotas - ahorroMes;
    }

    long cupoDiario() {
        long libre = libre();
        if (libre <= 0) {
            return 0;
        }
        if (relacionMes > 0) {
            return libre / Math.max(1, diasEnMes);
        }
        if (relacionMes < 0) {
            return 0;
        }
        int quedan = Math.max(1, diasEnMes - diaHoy + 1);
        return libre / quedan;
    }
}

class Prima {
    long id;
    long perfilId;
    int anio;
    int mes;
    long monto;
    String nombre;
}

class MetaAhorro {
    long id;
    long perfilId;
    String nombre;
    String plazo;
    long objetivo;
    long ahorrado;
    String inicio;
    String fin;

    int progreso() {
        if (objetivo <= 0) {
            return 0;
        }
        return (int) Math.min(100, Math.round(ahorrado * 100.0 / objetivo));
    }

    String plazoBonito() {
        if ("MEDIANO".equals(plazo)) {
            return "Mediano plazo";
        }
        if ("LARGO".equals(plazo)) {
            return "Largo plazo";
        }
        return "Corto plazo";
    }
}

class Fila {
    String tipo;
    long id;
    String titulo;
    String detalle;
    String monto;
    String orden = "";
    int colorMonto;
    int progreso = -1;
}
