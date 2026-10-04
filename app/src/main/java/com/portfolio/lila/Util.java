package com.portfolio.lila;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

final class Util {
    static final int DIAS_QUINCENA = 15;
    private static final TimeZone ZONA = TimeZone.getTimeZone("America/Bogota");

    private Util() {
    }

    static String cop(long valor) {
        DecimalFormatSymbols simbolos = new DecimalFormatSymbols(new Locale("es", "CO"));
        DecimalFormat formato = new DecimalFormat("#,##0", simbolos);
        return "$" + formato.format(valor);
    }

    static long parse(CharSequence texto) {
        if (texto == null) {
            return 0;
        }
        String digitos = texto.toString().replaceAll("[^0-9]", "");
        if (digitos.isEmpty()) {
            return 0;
        }
        try {
            return Long.parseLong(digitos);
        } catch (NumberFormatException error) {
            return 0;
        }
    }

    static Calendar calendario() {
        return Calendar.getInstance(ZONA);
    }

    static String hoy() {
        Calendar calendario = calendario();
        return iso(calendario.get(Calendar.YEAR), calendario.get(Calendar.MONTH) + 1,
                calendario.get(Calendar.DAY_OF_MONTH));
    }

    static String iso(int anio, int mes, int dia) {
        return String.format(Locale.US, "%04d-%02d-%02d", anio, mes, dia);
    }

    static String inicioMes(int anio, int mes) {
        return iso(anio, mes, 1);
    }

    static String finMes(int anio, int mes) {
        if (mes == 12) {
            return iso(anio + 1, 1, 1);
        }
        return iso(anio, mes + 1, 1);
    }

    static int diasDelMes(int anio, int mes) {
        Calendar calendario = calendario();
        calendario.set(Calendar.YEAR, anio);
        calendario.set(Calendar.MONTH, mes - 1);
        calendario.set(Calendar.DAY_OF_MONTH, 1);
        return calendario.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    static int relacionMes(int anio, int mes) {
        Calendar calendario = calendario();
        int actual = calendario.get(Calendar.YEAR) * 12 + calendario.get(Calendar.MONTH) + 1;
        return Integer.compare(anio * 12 + mes, actual);
    }

    static String nombreMes(int anio, int mes) {
        String[] nombres = {
                "", "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        };
        return nombres[mes] + " " + anio;
    }

    static String mesCorto(int mes) {
        String[] nombres = {
                "", "enero", "febrero", "marzo", "abril", "mayo", "junio",
                "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
        };
        return nombres[mes];
    }

    static String fechaBonita(String iso) {
        if (iso == null || iso.length() < 10) {
            return "";
        }
        int dia = Integer.parseInt(iso.substring(8, 10));
        int mes = Integer.parseInt(iso.substring(5, 7));
        return dia + " de " + mesCorto(mes);
    }

    static long bonoPorDias(long bonoQuincena, int diasIncapacidad) {
        int incapacidad = Math.max(0, Math.min(DIAS_QUINCENA, diasIncapacidad));
        int trabajados = DIAS_QUINCENA - incapacidad;
        return Math.round(bonoQuincena * (trabajados / (double) DIAS_QUINCENA));
    }

    static long bonoDiario(long bonoQuincena) {
        return Math.round(bonoQuincena / (double) DIAS_QUINCENA);
    }

    static Calendar fecha(String iso) {
        Calendar calendario = calendario();
        calendario.set(Calendar.YEAR, Integer.parseInt(iso.substring(0, 4)));
        calendario.set(Calendar.MONTH, Integer.parseInt(iso.substring(5, 7)) - 1);
        calendario.set(Calendar.DAY_OF_MONTH, Integer.parseInt(iso.substring(8, 10)));
        calendario.set(Calendar.HOUR_OF_DAY, 0);
        calendario.set(Calendar.MINUTE, 0);
        calendario.set(Calendar.SECOND, 0);
        calendario.set(Calendar.MILLISECOND, 0);
        return calendario;
    }

    static String sumarMeses(String iso, int meses) {
        Calendar calendario = fecha(iso);
        calendario.add(Calendar.MONTH, meses);
        return iso(calendario.get(Calendar.YEAR), calendario.get(Calendar.MONTH) + 1,
                calendario.get(Calendar.DAY_OF_MONTH));
    }

    static int mesesEntre(String inicio, String fin) {
        Calendar desde = fecha(inicio);
        Calendar hasta = fecha(fin);
        int meses = (hasta.get(Calendar.YEAR) - desde.get(Calendar.YEAR)) * 12
                + (hasta.get(Calendar.MONTH) - desde.get(Calendar.MONTH));
        return Math.max(1, meses);
    }

    static long diasEntre(String inicio, String fin) {
        long diferencia = fecha(fin).getTimeInMillis() - fecha(inicio).getTimeInMillis();
        return Math.max(0, diferencia / 86400000L);
    }
}
