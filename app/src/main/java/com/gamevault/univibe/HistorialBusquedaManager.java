package com.gamevault.univibe;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

public class HistorialBusquedaManager {

    private static final String PREF_NAME = "historial_busqueda_prefs";
    private static final String KEY_HISTORIAL = "historial_lista";
    private static final int MAX_HISTORIAL = 10;

    public static List<String> obtenerHistorial(Context context) {
        List<String> historial = new ArrayList<>();
        if (context == null) return historial;

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String jsonString = prefs.getString(KEY_HISTORIAL, "[]");

        try {
            JSONArray array = new JSONArray(jsonString);
            for (int i = 0; i < array.length(); i++) {
                String item = array.getString(i);
                if (item != null && !item.trim().isEmpty()) {
                    historial.add(item.trim());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return historial;
    }

    public static void guardarTermino(Context context, String termino) {
        if (context == null || termino == null) return;
        String limpio = termino.trim();
        if (limpio.length() < 2) return;

        List<String> historialActual = obtenerHistorial(context);

        // Remove if existing (case-insensitive)
        for (int i = historialActual.size() - 1; i >= 0; i--) {
            if (historialActual.get(i).equalsIgnoreCase(limpio)) {
                historialActual.remove(i);
            }
        }

        // Insert at index 0 (top/most recent)
        historialActual.add(0, limpio);

        // Limit size
        if (historialActual.size() > MAX_HISTORIAL) {
            historialActual = historialActual.subList(0, MAX_HISTORIAL);
        }

        guardarListaEnPrefs(context, historialActual);
    }

    public static void eliminarTermino(Context context, String termino) {
        if (context == null || termino == null) return;

        List<String> historialActual = obtenerHistorial(context);
        for (int i = historialActual.size() - 1; i >= 0; i--) {
            if (historialActual.get(i).equalsIgnoreCase(termino.trim())) {
                historialActual.remove(i);
            }
        }

        guardarListaEnPrefs(context, historialActual);
    }

    public static void limpiarHistorial(Context context) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_HISTORIAL).apply();
    }

    private static void guardarListaEnPrefs(Context context, List<String> lista) {
        JSONArray array = new JSONArray();
        for (String item : lista) {
            array.put(item);
        }

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_HISTORIAL, array.toString()).apply();
    }
}
