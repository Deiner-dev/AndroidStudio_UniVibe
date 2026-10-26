
package com.gamevault.univibe;

public class Universidad {

    private final String nombre;
    private final String pais;
    private final String dominio;
    private final String paginaWeb;

    public Universidad(
            String nombre,
            String pais,
            String dominio,
            String paginaWeb) {

        this.nombre = nombre;
        this.pais = pais;
        this.dominio = dominio;
        this.paginaWeb = paginaWeb;
    }

    public String getNombre() {
        return nombre;
    }

    public String getPais() {
        return pais;
    }

    public String getDominio() {
        return dominio;
    }

    public String getPaginaWeb() {
        return paginaWeb;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
