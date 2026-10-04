package com.portfolio.lila;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class LilaDb extends SQLiteOpenHelper {
    private static LilaDb instancia;

    static LilaDb get(Context contexto) {
        if (instancia == null) {
            instancia = new LilaDb(contexto.getApplicationContext());
            SQLiteDatabase base = instancia.getWritableDatabase();
            base.execSQL("UPDATE perfil SET nombre='Jhonatan' WHERE id=2 AND nombre='Pareja'");
            base.execSQL("UPDATE perfil SET tipo='QUINCENAL', base=base/2, bono=0 WHERE id=2 AND tipo='MENSUAL'");
        }
        return instancia;
    }

    private LilaDb(Context contexto) {
        super(contexto, "lila.db", null, 4);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE perfil ("
                + "id INTEGER PRIMARY KEY,"
                + "nombre TEXT NOT NULL,"
                + "tipo TEXT NOT NULL,"
                + "base INTEGER NOT NULL,"
                + "bono INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE ingreso ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "perfil_id INTEGER NOT NULL,"
                + "anio INTEGER NOT NULL,"
                + "mes INTEGER NOT NULL,"
                + "periodo INTEGER NOT NULL,"
                + "dias_incapacidad INTEGER NOT NULL,"
                + "base INTEGER NOT NULL,"
                + "bono INTEGER NOT NULL,"
                + "total INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE gasto ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "perfil_id INTEGER NOT NULL,"
                + "nombre TEXT NOT NULL,"
                + "monto INTEGER NOT NULL,"
                + "categoria TEXT NOT NULL,"
                + "fecha TEXT NOT NULL,"
                + "es_fijo INTEGER NOT NULL,"
                + "periodo INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE TABLE deuda ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "perfil_id INTEGER NOT NULL,"
                + "nombre TEXT NOT NULL,"
                + "inicial INTEGER NOT NULL,"
                + "saldo INTEGER NOT NULL,"
                + "cuota INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE ahorro ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "perfil_id INTEGER NOT NULL,"
                + "nombre TEXT NOT NULL,"
                + "meta INTEGER NOT NULL,"
                + "ahorrado INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE aporte ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "ahorro_id INTEGER NOT NULL,"
                + "monto INTEGER NOT NULL,"
                + "fecha TEXT NOT NULL)");
        db.execSQL("INSERT INTO perfil (id, nombre, tipo, base, bono) VALUES (1, 'Camila', 'QUINCENAL', 1500000, 500000)");
        db.execSQL("INSERT INTO perfil (id, nombre, tipo, base, bono) VALUES (2, 'Jhonatan', 'QUINCENAL', 2000000, 0)");
        crearMetas(db);
        crearPrimas(db);
        sembrarPrimas(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int vieja, int nueva) {
        if (vieja < 2) {
            crearMetas(db);
        }
        if (vieja < 3) {
            db.execSQL("ALTER TABLE gasto ADD COLUMN periodo INTEGER NOT NULL DEFAULT 0");
        }
        if (vieja < 4) {
            crearPrimas(db);
            sembrarPrimas(db);
        }
    }

    private static void crearPrimas(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS prima ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "perfil_id INTEGER NOT NULL,"
                + "anio INTEGER NOT NULL,"
                + "mes INTEGER NOT NULL,"
                + "monto INTEGER NOT NULL,"
                + "nombre TEXT NOT NULL)");
    }

    private static void sembrarPrimas(SQLiteDatabase db) {
        sembrarPrima(db, 1, 2026, 12, 1500000, "Prima");
        sembrarPrima(db, 2, 2026, 12, 2000000, "Prima");
    }

    private static void sembrarPrima(SQLiteDatabase db, int perfilId, int anio, int mes, long monto, String nombre) {
        Cursor cursor = db.rawQuery(
                "SELECT id FROM prima WHERE perfil_id=? AND anio=? AND mes=? AND nombre=?",
                new String[]{String.valueOf(perfilId), String.valueOf(anio), String.valueOf(mes), nombre});
        try {
            if (cursor.moveToFirst()) {
                return;
            }
        } finally {
            cursor.close();
        }
        ContentValues valores = new ContentValues();
        valores.put("perfil_id", perfilId);
        valores.put("anio", anio);
        valores.put("mes", mes);
        valores.put("monto", monto);
        valores.put("nombre", nombre);
        db.insert("prima", null, valores);
    }

    private static void crearMetas(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS meta_ahorro ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "perfil_id INTEGER NOT NULL,"
                + "nombre TEXT NOT NULL,"
                + "plazo TEXT NOT NULL,"
                + "objetivo INTEGER NOT NULL,"
                + "ahorrado INTEGER NOT NULL,"
                + "inicio TEXT NOT NULL,"
                + "fin TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS aporte_meta ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "meta_id INTEGER NOT NULL,"
                + "monto INTEGER NOT NULL,"
                + "fecha TEXT NOT NULL)");
    }

    Perfil perfil(long id) {
        Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT id, nombre, tipo, base, bono FROM perfil WHERE id=?",
                new String[]{String.valueOf(id)});
        try {
            if (!cursor.moveToFirst()) {
                return null;
            }
            return leerPerfil(cursor);
        } finally {
            cursor.close();
        }
    }

    String nombrePerfil(long id) {
        if (id == 0) {
            return "Los dos";
        }
        Perfil perfil = perfil(id);
        return perfil == null ? "" : perfil.nombre;
    }

    void actualizarPerfil(long id, String nombre, long base, long bono) {
        ContentValues valores = new ContentValues();
        valores.put("nombre", nombre);
        valores.put("base", base);
        valores.put("bono", bono);
        getWritableDatabase().update("perfil", valores, "id=?", new String[]{String.valueOf(id)});
    }

    IngresoReg ingreso(long perfilId, int anio, int mes, int periodo) {
        Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT id, perfil_id, anio, mes, periodo, dias_incapacidad, base, bono, total "
                        + "FROM ingreso WHERE perfil_id=? AND anio=? AND mes=? AND periodo=?",
                new String[]{
                        String.valueOf(perfilId),
                        String.valueOf(anio),
                        String.valueOf(mes),
                        String.valueOf(periodo)
                });
        try {
            if (!cursor.moveToFirst()) {
                return null;
            }
            return leerIngreso(cursor);
        } finally {
            cursor.close();
        }
    }

    List<IngresoReg> ingresosDelMes(int perfilId, int anio, int mes) {
        Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT id, perfil_id, anio, mes, periodo, dias_incapacidad, base, bono, total "
                        + "FROM ingreso WHERE anio=? AND mes=? AND (?=0 OR perfil_id=?) "
                        + "ORDER BY periodo",
                new String[]{
                        String.valueOf(anio),
                        String.valueOf(mes),
                        String.valueOf(perfilId),
                        String.valueOf(perfilId)
                });
        ArrayList<IngresoReg> lista = new ArrayList<>();
        try {
            while (cursor.moveToNext()) {
                lista.add(leerIngreso(cursor));
            }
        } finally {
            cursor.close();
        }
        return lista;
    }

    void guardarIngreso(long perfilId, int anio, int mes, int periodo, int dias,
                        long base, long bono, long total) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("ingreso", "perfil_id=? AND anio=? AND mes=? AND periodo=?", new String[]{
                String.valueOf(perfilId),
                String.valueOf(anio),
                String.valueOf(mes),
                String.valueOf(periodo)
        });
        ContentValues valores = new ContentValues();
        valores.put("perfil_id", perfilId);
        valores.put("anio", anio);
        valores.put("mes", mes);
        valores.put("periodo", periodo);
        valores.put("dias_incapacidad", dias);
        valores.put("base", base);
        valores.put("bono", bono);
        valores.put("total", total);
        db.insert("ingreso", null, valores);
    }

    void borrarIngreso(long id) {
        getWritableDatabase().delete("ingreso", "id=?", new String[]{String.valueOf(id)});
    }

    void insertarGasto(long perfilId, String nombre, long monto, String categoria, String fecha, boolean fijo, int periodo) {
        ContentValues valores = new ContentValues();
        valores.put("perfil_id", perfilId);
        valores.put("nombre", nombre);
        valores.put("monto", monto);
        valores.put("categoria", categoria);
        valores.put("fecha", fecha);
        valores.put("es_fijo", fijo ? 1 : 0);
        valores.put("periodo", periodo);
        getWritableDatabase().insert("gasto", null, valores);
    }

    void borrarGasto(long id) {
        getWritableDatabase().delete("gasto", "id=?", new String[]{String.valueOf(id)});
    }

    List<Gasto> gastosFijos(int perfilId) {
        return listarGastos(perfilId, 1, null, null);
    }

    List<Gasto> gastosVariables(int perfilId, int anio, int mes) {
        return listarGastos(perfilId, 0, Util.inicioMes(anio, mes), Util.finMes(anio, mes));
    }

    List<Gasto> gastosDeHoy(int perfilId) {
        Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT id, perfil_id, nombre, monto, categoria, fecha, es_fijo, periodo FROM gasto "
                        + "WHERE es_fijo=0 AND fecha=? AND (?=0 OR perfil_id=? OR perfil_id=0) "
                        + "ORDER BY id DESC",
                new String[]{Util.hoy(), String.valueOf(perfilId), String.valueOf(perfilId)});
        return leerGastos(cursor);
    }

    long sumaHoy(int perfilId) {
        return unNumero(
                "SELECT COALESCE(SUM(monto),0) FROM gasto WHERE es_fijo=0 AND fecha=? "
                        + "AND (?=0 OR perfil_id=? OR perfil_id=0)",
                new String[]{Util.hoy(), String.valueOf(perfilId), String.valueOf(perfilId)});
    }

    void insertarDeuda(long perfilId, String nombre, long saldo, long cuota) {
        ContentValues valores = new ContentValues();
        valores.put("perfil_id", perfilId);
        valores.put("nombre", nombre);
        valores.put("inicial", saldo);
        valores.put("saldo", saldo);
        valores.put("cuota", cuota);
        getWritableDatabase().insert("deuda", null, valores);
    }

    List<Deuda> deudas(int perfilId) {
        Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT id, perfil_id, nombre, inicial, saldo, cuota FROM deuda "
                        + "WHERE (?=0 OR perfil_id=? OR perfil_id=0) ORDER BY nombre",
                new String[]{String.valueOf(perfilId), String.valueOf(perfilId)});
        ArrayList<Deuda> lista = new ArrayList<>();
        try {
            while (cursor.moveToNext()) {
                Deuda deuda = new Deuda();
                deuda.id = cursor.getLong(0);
                deuda.perfilId = cursor.getLong(1);
                deuda.nombre = cursor.getString(2);
                deuda.inicial = cursor.getLong(3);
                deuda.saldo = cursor.getLong(4);
                deuda.cuota = cursor.getLong(5);
                lista.add(deuda);
            }
        } finally {
            cursor.close();
        }
        return lista;
    }

    void pagarCuota(long id) {
        Deuda deuda = buscarDeuda(id);
        if (deuda == null || deuda.saldo <= 0) {
            return;
        }
        long nuevoSaldo = deuda.saldo - Math.min(deuda.cuota, deuda.saldo);
        ContentValues valores = new ContentValues();
        valores.put("saldo", nuevoSaldo);
        getWritableDatabase().update("deuda", valores, "id=?", new String[]{String.valueOf(id)});
    }

    void borrarDeuda(long id) {
        getWritableDatabase().delete("deuda", "id=?", new String[]{String.valueOf(id)});
    }

    void insertarAhorro(long perfilId, String nombre, long meta) {
        ContentValues valores = new ContentValues();
        valores.put("perfil_id", perfilId);
        valores.put("nombre", nombre);
        valores.put("meta", meta);
        valores.put("ahorrado", 0);
        getWritableDatabase().insert("ahorro", null, valores);
    }

    List<Ahorro> ahorros(int perfilId) {
        Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT id, perfil_id, nombre, meta, ahorrado FROM ahorro "
                        + "WHERE (?=0 OR perfil_id=? OR perfil_id=0) ORDER BY nombre",
                new String[]{String.valueOf(perfilId), String.valueOf(perfilId)});
        ArrayList<Ahorro> lista = new ArrayList<>();
        try {
            while (cursor.moveToNext()) {
                Ahorro ahorro = new Ahorro();
                ahorro.id = cursor.getLong(0);
                ahorro.perfilId = cursor.getLong(1);
                ahorro.nombre = cursor.getString(2);
                ahorro.meta = cursor.getLong(3);
                ahorro.ahorrado = cursor.getLong(4);
                lista.add(ahorro);
            }
        } finally {
            cursor.close();
        }
        return lista;
    }

    void aportar(long ahorroId, long monto, String fecha) {
        Ahorro ahorro = buscarAhorro(ahorroId);
        if (ahorro == null || monto <= 0) {
            return;
        }
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues valores = new ContentValues();
            valores.put("ahorrado", ahorro.ahorrado + monto);
            db.update("ahorro", valores, "id=?", new String[]{String.valueOf(ahorroId)});
            ContentValues aporte = new ContentValues();
            aporte.put("ahorro_id", ahorroId);
            aporte.put("monto", monto);
            aporte.put("fecha", fecha);
            db.insert("aporte", null, aporte);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    void borrarAhorro(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("aporte", "ahorro_id=?", new String[]{String.valueOf(id)});
        db.delete("ahorro", "id=?", new String[]{String.valueOf(id)});
    }

    void insertarMeta(long perfilId, String nombre, String plazo, long objetivo, String inicio, String fin) {
        ContentValues valores = new ContentValues();
        valores.put("perfil_id", perfilId);
        valores.put("nombre", nombre);
        valores.put("plazo", plazo);
        valores.put("objetivo", objetivo);
        valores.put("ahorrado", 0);
        valores.put("inicio", inicio);
        valores.put("fin", fin);
        getWritableDatabase().insert("meta_ahorro", null, valores);
    }

    List<MetaAhorro> metas(int perfilId) {
        Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT id, perfil_id, nombre, plazo, objetivo, ahorrado, inicio, fin FROM meta_ahorro "
                        + "WHERE (?=0 OR perfil_id=? OR perfil_id=0) ORDER BY fin, nombre",
                new String[]{String.valueOf(perfilId), String.valueOf(perfilId)});
        ArrayList<MetaAhorro> lista = new ArrayList<>();
        try {
            while (cursor.moveToNext()) {
                MetaAhorro meta = new MetaAhorro();
                meta.id = cursor.getLong(0);
                meta.perfilId = cursor.getLong(1);
                meta.nombre = cursor.getString(2);
                meta.plazo = cursor.getString(3);
                meta.objetivo = cursor.getLong(4);
                meta.ahorrado = cursor.getLong(5);
                meta.inicio = cursor.getString(6);
                meta.fin = cursor.getString(7);
                lista.add(meta);
            }
        } finally {
            cursor.close();
        }
        return lista;
    }

    void aportarMeta(long metaId, long monto, String fecha) {
        MetaAhorro meta = buscarMeta(metaId);
        if (meta == null || monto <= 0) {
            return;
        }
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues valores = new ContentValues();
            valores.put("ahorrado", meta.ahorrado + monto);
            db.update("meta_ahorro", valores, "id=?", new String[]{String.valueOf(metaId)});
            ContentValues aporte = new ContentValues();
            aporte.put("meta_id", metaId);
            aporte.put("monto", monto);
            aporte.put("fecha", fecha);
            db.insert("aporte_meta", null, aporte);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    void borrarMeta(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("aporte_meta", "meta_id=?", new String[]{String.valueOf(id)});
        db.delete("meta_ahorro", "id=?", new String[]{String.valueOf(id)});
    }

    private MetaAhorro buscarMeta(long id) {
        for (MetaAhorro meta : metas(0)) {
            if (meta.id == id) {
                return meta;
            }
        }
        return null;
    }

    Resumen resumen(int perfilId, int anio, int mes) {
        Resumen resumen = new Resumen();
        boolean[] estimado = {false};
        if (perfilId == 0) {
            resumen.ingresos = ingresoEstimado(1, anio, mes, estimado) + ingresoEstimado(2, anio, mes, estimado);
        } else {
            resumen.ingresos = ingresoEstimado(perfilId, anio, mes, estimado);
        }
        resumen.primas = sumaPrimasDelMes(perfilId, anio, mes);
        resumen.ingresos += resumen.primas;
        resumen.estimado = estimado[0];
        resumen.gastosFijos = sumarFijosDelMes(perfilId);
        resumen.gastosVariables = sumarGastos(perfilId, 0, Util.inicioMes(anio, mes), Util.finMes(anio, mes));
        resumen.gastadoHoy = sumaHoy(perfilId);
        for (Deuda deuda : deudas(perfilId)) {
            resumen.cuotas += Math.min(deuda.cuota, Math.max(0, deuda.saldo));
        }
        String[] rangoMes = {
                Util.inicioMes(anio, mes),
                Util.finMes(anio, mes),
                String.valueOf(perfilId),
                String.valueOf(perfilId)
        };
        resumen.ahorroMes = unNumero(
                "SELECT COALESCE(SUM(aporte.monto),0) FROM aporte "
                        + "JOIN ahorro ON ahorro.id = aporte.ahorro_id "
                        + "WHERE aporte.fecha>=? AND aporte.fecha<? "
                        + "AND (?=0 OR ahorro.perfil_id=? OR ahorro.perfil_id=0)",
                rangoMes);
        resumen.ahorroMes += unNumero(
                "SELECT COALESCE(SUM(aporte_meta.monto),0) FROM aporte_meta "
                        + "JOIN meta_ahorro ON meta_ahorro.id = aporte_meta.meta_id "
                        + "WHERE aporte_meta.fecha>=? AND aporte_meta.fecha<? "
                        + "AND (?=0 OR meta_ahorro.perfil_id=? OR meta_ahorro.perfil_id=0)",
                rangoMes);
        resumen.diasEnMes = Util.diasDelMes(anio, mes);
        resumen.relacionMes = Util.relacionMes(anio, mes);
        resumen.diaHoy = resumen.relacionMes == 0 ? Util.calendario().get(java.util.Calendar.DAY_OF_MONTH) : 0;
        return resumen;
    }

    void insertarPrima(long perfilId, String nombre, long monto, int anio, int mes) {
        ContentValues valores = new ContentValues();
        valores.put("perfil_id", perfilId);
        valores.put("nombre", nombre);
        valores.put("monto", monto);
        valores.put("anio", anio);
        valores.put("mes", mes);
        getWritableDatabase().insert("prima", null, valores);
    }

    void borrarPrima(long id) {
        getWritableDatabase().delete("prima", "id=?", new String[]{String.valueOf(id)});
    }

    List<Prima> primas(int perfilId) {
        Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT id, perfil_id, anio, mes, monto, nombre FROM prima "
                        + "WHERE (?=0 OR perfil_id=? OR perfil_id=0) "
                        + "ORDER BY anio, mes, id",
                new String[]{String.valueOf(perfilId), String.valueOf(perfilId)});
        return leerPrimas(cursor);
    }

    List<Prima> primasEnPlazo(int perfilId, String inicio, String fin) {
        ArrayList<Prima> dentro = new ArrayList<>();
        for (Prima prima : primas(perfilId)) {
            String desde = Util.inicioMes(prima.anio, prima.mes);
            String hasta = Util.finMes(prima.anio, prima.mes);
            if (inicio.compareTo(hasta) < 0 && fin.compareTo(desde) >= 0) {
                dentro.add(prima);
            }
        }
        return dentro;
    }

    long sumaPrimasEnPlazo(int perfilId, String inicio, String fin) {
        long total = 0;
        for (Prima prima : primasEnPlazo(perfilId, inicio, fin)) {
            total += prima.monto;
        }
        return total;
    }

    private long sumaPrimasDelMes(int perfilId, int anio, int mes) {
        long total = 0;
        for (Prima prima : primas(perfilId)) {
            if (prima.anio == anio && prima.mes == mes) {
                total += prima.monto;
            }
        }
        return total;
    }

    private List<Prima> leerPrimas(Cursor cursor) {
        ArrayList<Prima> lista = new ArrayList<>();
        try {
            while (cursor.moveToNext()) {
                Prima prima = new Prima();
                prima.id = cursor.getLong(0);
                prima.perfilId = cursor.getLong(1);
                prima.anio = cursor.getInt(2);
                prima.mes = cursor.getInt(3);
                prima.monto = cursor.getLong(4);
                prima.nombre = cursor.getString(5);
                lista.add(prima);
            }
        } finally {
            cursor.close();
        }
        return lista;
    }

    private long ingresoEstimado(int perfilId, int anio, int mes, boolean[] estimado) {
        Perfil perfil = perfil(perfilId);
        if (perfil == null) {
            return 0;
        }
        if (!perfil.quincenal()) {
            IngresoReg registro = ingreso(perfilId, anio, mes, 0);
            if (registro == null) {
                estimado[0] = true;
                return perfil.base;
            }
            return registro.total;
        }
        long total = 0;
        for (int periodo = 1; periodo <= 2; periodo++) {
            IngresoReg registro = ingreso(perfilId, anio, mes, periodo);
            if (registro == null) {
                estimado[0] = true;
                total += perfil.base + perfil.bono;
            } else {
                total += registro.total;
            }
        }
        return total;
    }

    private List<Gasto> listarGastos(int perfilId, int esFijo, String desde, String hasta) {
        ArrayList<String> args = new ArrayList<>();
        String sql = "SELECT id, perfil_id, nombre, monto, categoria, fecha, es_fijo, periodo FROM gasto "
                + "WHERE (?=0 OR perfil_id=? OR perfil_id=0) AND es_fijo=?";
        args.add(String.valueOf(perfilId));
        args.add(String.valueOf(perfilId));
        args.add(String.valueOf(esFijo));
        if (desde != null) {
            sql += " AND fecha>=? AND fecha<?";
            args.add(desde);
            args.add(hasta);
        }
        sql += " ORDER BY fecha DESC, id DESC";
        Cursor cursor = getReadableDatabase().rawQuery(sql, args.toArray(new String[0]));
        return leerGastos(cursor);
    }

    private long sumarFijosDelMes(int perfilId) {
        long total = 0;
        for (Gasto gasto : listarGastos(perfilId, 1, null, null)) {
            total += gasto.periodo == 3 ? gasto.monto * 2 : gasto.monto;
        }
        return total;
    }

    private long sumarGastos(int perfilId, int esFijo, String desde, String hasta) {
        ArrayList<String> args = new ArrayList<>();
        String sql = "SELECT COALESCE(SUM(monto),0) FROM gasto WHERE (?=0 OR perfil_id=? OR perfil_id=0) AND es_fijo=?";
        args.add(String.valueOf(perfilId));
        args.add(String.valueOf(perfilId));
        args.add(String.valueOf(esFijo));
        if (desde != null) {
            sql += " AND fecha>=? AND fecha<?";
            args.add(desde);
            args.add(hasta);
        }
        return unNumero(sql, args.toArray(new String[0]));
    }

    private long unNumero(String sql, String[] args) {
        Cursor cursor = getReadableDatabase().rawQuery(sql, args);
        try {
            if (cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
            return 0;
        } finally {
            cursor.close();
        }
    }

    private Deuda buscarDeuda(long id) {
        for (Deuda deuda : deudas(0)) {
            if (deuda.id == id) {
                return deuda;
            }
        }
        return null;
    }

    private Ahorro buscarAhorro(long id) {
        for (Ahorro ahorro : ahorros(0)) {
            if (ahorro.id == id) {
                return ahorro;
            }
        }
        return null;
    }

    private Perfil leerPerfil(Cursor cursor) {
        Perfil perfil = new Perfil();
        perfil.id = cursor.getLong(0);
        perfil.nombre = cursor.getString(1);
        perfil.tipo = cursor.getString(2);
        perfil.base = cursor.getLong(3);
        perfil.bono = cursor.getLong(4);
        return perfil;
    }

    private IngresoReg leerIngreso(Cursor cursor) {
        IngresoReg ingreso = new IngresoReg();
        ingreso.id = cursor.getLong(0);
        ingreso.perfilId = cursor.getLong(1);
        ingreso.anio = cursor.getInt(2);
        ingreso.mes = cursor.getInt(3);
        ingreso.periodo = cursor.getInt(4);
        ingreso.diasIncapacidad = cursor.getInt(5);
        ingreso.base = cursor.getLong(6);
        ingreso.bono = cursor.getLong(7);
        ingreso.total = cursor.getLong(8);
        return ingreso;
    }

    private List<Gasto> leerGastos(Cursor cursor) {
        ArrayList<Gasto> lista = new ArrayList<>();
        try {
            while (cursor.moveToNext()) {
                Gasto gasto = new Gasto();
                gasto.id = cursor.getLong(0);
                gasto.perfilId = cursor.getLong(1);
                gasto.nombre = cursor.getString(2);
                gasto.monto = cursor.getLong(3);
                gasto.categoria = cursor.getString(4);
                gasto.fecha = cursor.getString(5);
                gasto.fijo = cursor.getInt(6) == 1;
                int columnaPeriodo = cursor.getColumnIndex("periodo");
                gasto.periodo = columnaPeriodo >= 0 ? cursor.getInt(columnaPeriodo) : 0;
                lista.add(gasto);
            }
        } finally {
            cursor.close();
        }
        return lista;
    }
}
