package com.example.LoginCliente.Models;


import java.util.HashMap;
import java.util.Map;

public enum Permiso {
    Administrador("Administrador",0),
    Contador("Contador", 1),
    Auditor("Auditor", 2);

    private static final Map<String, Permiso> POR_TEXTOS = new HashMap<>();
    private static final Map<Integer, Permiso> POR_VALORES = new HashMap<>();

    static {
        for (Permiso permiso : Permiso.values()) {
            POR_TEXTOS.put(permiso.texto,  permiso);
            POR_VALORES.put(permiso.valor, permiso);
        }
    }

    public final String texto;
    public final int valor;

    Permiso(String texto, int valor){
        this.texto = texto;
        this.valor = valor;
    }

    public static Permiso valueOfValor(int valor){
        return POR_VALORES.get(valor);
    }

    public static Permiso valueOfTexto(String texto){
        return POR_TEXTOS.get(texto);
    }
    public static Integer valueOf(Permiso permiso){ return permiso.valor; }
}
