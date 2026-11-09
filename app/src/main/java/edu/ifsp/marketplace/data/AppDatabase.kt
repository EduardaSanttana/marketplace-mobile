package edu.ifsp.marketplace.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Produto::class, Negociante::class, Avaliacao::class, Pedido::class],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun produtoDao(): ProdutoDao
    abstract fun negocianteDao(): NegocianteDao
    abstract fun avaliacaoDao(): AvaliacaoDao
    abstract fun pedidoDao(): PedidoDao
    abstract fun sincronizacaoDao(): SincronizacaoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // A foto da entrega só existe neste aparelho: recriar o banco perderia o vínculo com ela
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pedidos ADD COLUMN vendedorId INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE pedidos ADD COLUMN vendedorNome TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE pedidos ADD COLUMN enderecoRetirada TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE pedidos ADD COLUMN latitudeRetirada REAL")
                db.execSQL("ALTER TABLE pedidos ADD COLUMN longitudeRetirada REAL")
            }
        }

        // Vínculos pelo id do Firestore e a conta ligada a cada negociante
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE produtos ADD COLUMN negocianteFirestoreId TEXT")
                db.execSQL("ALTER TABLE pedidos ADD COLUMN produtoFirestoreId TEXT")
                db.execSQL("ALTER TABLE pedidos ADD COLUMN compradorFirestoreId TEXT")
                db.execSQL("ALTER TABLE pedidos ADD COLUMN vendedorFirestoreId TEXT")
                db.execSQL("ALTER TABLE avaliacoes ADD COLUMN produtoFirestoreId TEXT")
                db.execSQL("ALTER TABLE negociantes ADD COLUMN usuarioUid TEXT")
            }
        }

        // Horários da compra e da retirada, para a linha do tempo do pedido
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pedidos ADD COLUMN criadoEm INTEGER")
                db.execSQL("ALTER TABLE pedidos ADD COLUMN retiradoEm INTEGER")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "marketplace.db"
                ).addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
