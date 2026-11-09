package com.frentefria.dados

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

// O banco guarda só o que aconteceu (menstruações, registros, eventos, kit).
// Previsão, fases e insights são sempre calculados pelo motor. Datas em ISO;
// enums e conjuntos de enums pelo nome (renomear exige migração).

@Entity(tableName = "menstruacao")
data class MenstruacaoEntity(
    @PrimaryKey val inicio: String,
    val fim: String? = null,
)

@Entity(tableName = "registro_dia")
data class RegistroEntity(
    @PrimaryKey val data: String,
    val humor: String? = null,
    /** Nomes de Sintoma separados por vírgula. */
    val sintomas: String = "",
    val dr: Int = 0,
    val motivos: String = "",
    /** "CHOCOLATE:1,MASSAGEM:0,ELOGIO:" (1 funcionou, 0 não, vazio sem avaliação). */
    val taticas: String = "",
    val nota: String = "",
)

@Entity(tableName = "evento")
data class EventoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val data: String,
    val titulo: String,
    val tipo: String,
)

@Entity(tableName = "item_kit")
data class ItemKitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val estoque: Boolean,
    val categoria: String,
    val tem: Boolean,
)

@Dao
interface FrenteFriaDao {
    @Query("SELECT * FROM menstruacao ORDER BY inicio")
    fun menstruacoes(): Flow<List<MenstruacaoEntity>>

    @Upsert
    suspend fun salvarMenstruacao(m: MenstruacaoEntity)

    @Query("DELETE FROM menstruacao WHERE inicio = :inicio")
    suspend fun apagarMenstruacao(inicio: String)

    @Query("SELECT * FROM registro_dia ORDER BY data")
    fun registros(): Flow<List<RegistroEntity>>

    @Query("SELECT * FROM registro_dia WHERE data = :data")
    suspend fun registro(data: String): RegistroEntity?

    @Upsert
    suspend fun salvarRegistro(r: RegistroEntity)

    @Query("DELETE FROM registro_dia WHERE data = :data")
    suspend fun apagarRegistro(data: String)

    @Query("SELECT * FROM evento ORDER BY data")
    fun eventos(): Flow<List<EventoEntity>>

    @Upsert
    suspend fun salvarEvento(e: EventoEntity)

    @Query("DELETE FROM evento WHERE id = :id")
    suspend fun apagarEvento(id: Long)

    @Query("SELECT * FROM item_kit ORDER BY estoque, nome")
    fun kit(): Flow<List<ItemKitEntity>>

    @Upsert
    suspend fun salvarItem(i: ItemKitEntity)

    @Query("DELETE FROM item_kit WHERE id = :id")
    suspend fun apagarItem(id: Long)

    @Query("SELECT COUNT(*) FROM item_kit")
    suspend fun contarKit(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirMenstruacoes(l: List<MenstruacaoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirRegistros(l: List<RegistroEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirEventos(l: List<EventoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirKit(l: List<ItemKitEntity>)

    @Query("DELETE FROM menstruacao") suspend fun limparMenstruacoes()
    @Query("DELETE FROM registro_dia") suspend fun limparRegistros()
    @Query("DELETE FROM evento") suspend fun limparEventos()
    @Query("DELETE FROM item_kit") suspend fun limparKit()

    @Transaction
    suspend fun limparTudo() {
        limparMenstruacoes(); limparRegistros(); limparEventos(); limparKit()
    }

    @Transaction
    suspend fun substituirTudo(
        ms: List<MenstruacaoEntity>,
        rs: List<RegistroEntity>,
        es: List<EventoEntity>,
        ks: List<ItemKitEntity>,
    ) {
        limparTudo()
        inserirMenstruacoes(ms); inserirRegistros(rs); inserirEventos(es); inserirKit(ks)
    }
}

@Database(
    entities = [MenstruacaoEntity::class, RegistroEntity::class, EventoEntity::class, ItemKitEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class FrenteFriaDatabase : RoomDatabase() {
    abstract fun dao(): FrenteFriaDao

    companion object {
        fun criar(context: Context): FrenteFriaDatabase =
            Room.databaseBuilder(context, FrenteFriaDatabase::class.java, "frente-fria.db").build()
    }
}
