package com.frentefria.motor.kit

import com.frentefria.motor.texto.Modo
import kotlinx.serialization.Serializable

enum class CategoriaFavorito { CHOCOLATE, DOCE, COMIDA, BEBIDA, FILME_SERIE, FLOR, REMEDIO, OUTRO }

fun nomeDaCategoria(c: CategoriaFavorito): String = when (c) {
    CategoriaFavorito.CHOCOLATE -> "Chocolate"
    CategoriaFavorito.DOCE -> "Doce"
    CategoriaFavorito.COMIDA -> "Comida"
    CategoriaFavorito.BEBIDA -> "Bebida"
    CategoriaFavorito.FILME_SERIE -> "Filme / série"
    CategoriaFavorito.FLOR -> "Flor"
    CategoriaFavorito.REMEDIO -> "Remédio"
    CategoriaFavorito.OUTRO -> "Outro"
}

fun emojiDaCategoria(c: CategoriaFavorito): String = when (c) {
    CategoriaFavorito.CHOCOLATE -> "🍫"
    CategoriaFavorito.DOCE -> "🍰"
    CategoriaFavorito.COMIDA -> "🍕"
    CategoriaFavorito.BEBIDA -> "🧋"
    CategoriaFavorito.FILME_SERIE -> "🎬"
    CategoriaFavorito.FLOR -> "🌷"
    CategoriaFavorito.REMEDIO -> "💊"
    CategoriaFavorito.OUTRO -> "⭐"
}

/** Algo que ela gosta (favorito) ou que precisa ter em casa (estoque: tem ou não tem). */
@Serializable
data class ItemKit(
    val id: Long = 0,
    val nome: String,
    val estoque: Boolean,
    val categoria: CategoriaFavorito = CategoriaFavorito.OUTRO,
    val tem: Boolean = true,
)

/** O estoque que o app sugere na primeira abertura. */
fun estoqueInicial(modo: Modo): List<ItemKit> = listOf(
    ItemKit(nome = "Chocolate", estoque = true, categoria = CategoriaFavorito.CHOCOLATE, tem = false),
    ItemKit(nome = if (modo == Modo.CLIMA) "Remédio para dor" else "Remédio para cólica", estoque = true, categoria = CategoriaFavorito.REMEDIO, tem = false),
    ItemKit(nome = "Bolsa térmica", estoque = true, categoria = CategoriaFavorito.OUTRO, tem = false),
    ItemKit(nome = "Chá", estoque = true, categoria = CategoriaFavorito.BEBIDA, tem = false),
    ItemKit(nome = "Sorvete", estoque = true, categoria = CategoriaFavorito.DOCE, tem = false),
    ItemKit(nome = "Lenço de papel", estoque = true, categoria = CategoriaFavorito.OUTRO, tem = false),
)
