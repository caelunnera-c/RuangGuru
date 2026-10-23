package com.mdq.ruangguru

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

// ================= ENTITAS / TABEL DATABASE =================

@Entity(tableName = "kelas")
data class KelasEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val namaKelas: String,
    val tingkat: String,
    val waliKelas: String,
    val tahunAjaran: String,
    val semester: String,
    val keterangan: String = ""
)

@Entity(
    tableName = "siswa",
    foreignKeys = [
        ForeignKey(
            entity = KelasEntity::class,
            parentColumns = ["id"],
            childColumns = ["kelasId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("nis", unique = true), Index("kelasId")]
)
data class SiswaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nis: String,
    val nisn: String = "",
    val namaLengkap: String,
    val jenisKelamin: String,
    val kelasId: Long,
    val tahunMasuk: String,
    val isAktif: Boolean = true,
    val catatan: String = ""
)

@Entity(
    tableName = "absensi",
    foreignKeys = [
        ForeignKey(
            entity = SiswaEntity::class,
            parentColumns = ["id"],
            childColumns = ["siswaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["siswaId", "tanggal", "mataPelajaran"], unique = true),
        Index("kelasId"),
        Index("tanggal")
    ]
)
data class AbsensiEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val siswaId: Long,
    val kelasId: Long,
    val tanggal: String,
    val mataPelajaran: String,
    val status: String,
    val catatan: String = ""
)

@Entity(
    tableName = "komponen_nilai",
    foreignKeys = [
        ForeignKey(
            entity = KelasEntity::class,
            parentColumns = ["id"],
            childColumns = ["kelasId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("kelasId")]
)
data class KomponenNilaiEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kelasId: Long,
    val mataPelajaran: String,
    val namaKomponen: String,
    val bobotPersen: Double
)

@Entity(
    tableName = "nilai_siswa",
    foreignKeys = [
        ForeignKey(
            entity = KomponenNilaiEntity::class,
            parentColumns = ["id"],
            childColumns = ["komponenId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SiswaEntity::class,
            parentColumns = ["id"],
            childColumns = ["siswaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["komponenId", "siswaId"], unique = true),
        Index("siswaId")
    ]
)
data class NilaiSiswaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val komponenId: Long,
    val siswaId: Long,
    val nilai: Double
)

@Entity(tableName = "jurnal_mengajar")
data class JurnalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tanggal: String,
    val namaGuru: String,
    val kelas: String,
    val mataPelajaran: String,
    val jamPelajaran: String,
    val materi: String,
    val tujuan: String,
    val kegiatan: String,
    val refleksi: String,
    val kendala: String = "",
    val tindakLanjut: String = ""
)

@Entity(tableName = "guru")
data class GuruEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nama: String,
    val nip: String,
    val role: String,
    val pinHash: String = ""
)

data class StatusCount(val status: String, val count: Int)
data class TanggalStatusCount(val tanggal: String, val status: String, val count: Int)

// ================= DAO (OPERASI DATABASE) =================

@Dao
interface KelasDao {
    @Query("SELECT * FROM kelas ORDER BY namaKelas ASC")
    fun getAllKelas(): Flow<List<KelasEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKelas(kelas: KelasEntity): Long

    @Delete
    suspend fun deleteKelas(kelas: KelasEntity)

    @Query("SELECT COUNT(*) FROM kelas")
    fun getJumlahKelas(): Flow<Int>
}

@Dao
interface SiswaDao {
    @Query("SELECT * FROM siswa WHERE kelasId = :kelasId AND isAktif = 1 ORDER BY namaLengkap ASC")
    fun getSiswaByKelas(kelasId: Long): Flow<List<SiswaEntity>>

    @Query("SELECT * FROM siswa ORDER BY namaLengkap ASC")
    fun getAllSiswa(): Flow<List<SiswaEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSiswa(siswa: SiswaEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBatch(siswaList: List<SiswaEntity>)

    @Delete
    suspend fun deleteSiswa(siswa: SiswaEntity)

    @Query("SELECT COUNT(*) FROM siswa WHERE isAktif = 1")
    fun getJumlahSiswaAktif(): Flow<Int>
}

@Dao
interface AbsensiDao {
    @Query("SELECT * FROM absensi WHERE kelasId = :kelasId AND tanggal = :tanggal AND mataPelajaran = :mapel")
    fun getAbsensi(kelasId: Long, tanggal: String, mapel: String): Flow<List<AbsensiEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBatch(absensiList: List<AbsensiEntity>)

    @Query("SELECT status, COUNT(*) as count FROM absensi GROUP BY status")
    fun getKomposisiKehadiran(): Flow<List<StatusCount>>

    @Query("SELECT COUNT(DISTINCT tanggal || mataPelajaran || kelasId) FROM absensi")
    fun getTotalSesiAbsensi(): Flow<Int>
}

@Dao
interface JurnalDao {
    @Query("SELECT * FROM jurnal_mengajar ORDER BY id DESC")
    fun getAllJurnal(): Flow<List<JurnalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJurnal(jurnal: JurnalEntity): Long

    @Query("SELECT COUNT(*) FROM jurnal_mengajar")
    fun getJumlahJurnal(): Flow<Int>
}

@Dao
interface NilaiDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKomponen(komponen: KomponenNilaiEntity): Long

    @Query("SELECT * FROM komponen_nilai WHERE kelasId = :kelasId AND mataPelajaran = :mapel")
    fun getKomponenByKelasMapel(kelasId: Long, mapel: String): Flow<List<KomponenNilaiEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNilai(nilai: NilaiSiswaEntity)
}

@Dao
interface GuruDao {
    @Query("SELECT * FROM guru ORDER BY nama ASC")
    fun getAllGuru(): Flow<List<GuruEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGuru(guru: GuruEntity): Long
}

// ================= DATABASE LOKAL ROOM =================

@Database(
    entities = [
        KelasEntity::class,
        SiswaEntity::class,
        AbsensiEntity::class,
        KomponenNilaiEntity::class,
        NilaiSiswaEntity::class,
        JurnalEntity::class,
        GuruEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun kelasDao(): KelasDao
    abstract fun siswaDao(): SiswaDao
    abstract fun absensiDao(): AbsensiDao
    abstract fun nilaiDao(): NilaiDao
    abstract fun jurnalDao(): JurnalDao
    abstract fun guruDao(): GuruDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mdq_ruangguru_offline.db"
                )
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(private val scope: CoroutineScope) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        val k1 = KelasEntity(namaKelas = "X RPL 1", tingkat = "10", waliKelas = "Ahmad S.Kom", tahunAjaran = "2024/2025", semester = "Genap")
                        val idK1 = database.kelasDao().insertKelas(k1)
                        database.siswaDao().insertSiswa(SiswaEntity(nis = "1001", namaLengkap = "Budi Santoso", jenisKelamin = "L", kelasId = idK1, tahunMasuk = "2024"))
                        database.siswaDao().insertSiswa(SiswaEntity(nis = "1002", namaLengkap = "Anisa Putri", jenisKelamin = "P", kelasId = idK1, tahunMasuk = "2024"))
                        database.guruDao().insertGuru(GuruEntity(nama = "Admin Kepala Sekolah", nip = "19780101", role = "ADMIN"))
                    }
                }
            }
        }
    }
}

// ================= DATASTORE PREFERENCES & REPOSITORY =================

val Context.dataStore by preferencesDataStore(name = "user_preferences")

class UserPreferences(private val context: Context) {
    companion object {
        val ORIENTATION_KEY = stringPreferencesKey("screen_orientation")
        val ACTIVE_SEMESTER = stringPreferencesKey("active_semester")
        val ACTIVE_TAHUN_AJARAN = stringPreferencesKey("active_tahun_ajaran")
        val SCHOOL_NAME = stringPreferencesKey("school_name")
    }

    val orientationFlow: Flow<String> = context.dataStore.data.map { it[ORIENTATION_KEY] ?: "AUTO" }
    val semesterFlow: Flow<String> = context.dataStore.data.map { it[ACTIVE_SEMESTER] ?: "Semester Genap" }
    val tahunAjaranFlow: Flow<String> = context.dataStore.data.map { it[ACTIVE_TAHUN_AJARAN] ?: "2024/2025" }
    val schoolNameFlow: Flow<String> = context.dataStore.data.map { it[SCHOOL_NAME] ?: "MDQ Boarding School" }

    suspend fun setOrientation(mode: String) { context.dataStore.edit { it[ORIENTATION_KEY] = mode } }
    suspend fun setSchoolInfo(name: String, semester: String, tahun: String) {
        context.dataStore.edit {
            it[SCHOOL_NAME] = name
            it[ACTIVE_SEMESTER] = semester
            it[ACTIVE_TAHUN_AJARAN] = tahun
        }
    }
}

class AppRepository(private val db: AppDatabase) {
    val allKelas = db.kelasDao().getAllKelas()
    val jumlahKelas = db.kelasDao().getJumlahKelas()
    suspend fun insertKelas(kelas: KelasEntity) = db.kelasDao().insertKelas(kelas)
    suspend fun deleteKelas(kelas: KelasEntity) = db.kelasDao().deleteKelas(kelas)

    val jumlahSiswa = db.siswaDao().getJumlahSiswaAktif()
    val allSiswa = db.siswaDao().getAllSiswa()
    suspend fun insertSiswa(siswa: SiswaEntity) = db.siswaDao().insertSiswa(siswa)
    suspend fun insertSiswaBatch(list: List<SiswaEntity>) = db.siswaDao().insertBatch(list)
    suspend fun deleteSiswa(siswa: SiswaEntity) = db.siswaDao().deleteSiswa(siswa)

    val komposisiKehadiran = db.absensiDao().getKomposisiKehadiran()
    val totalSesiAbsensi = db.absensiDao().getTotalSesiAbsensi()
    suspend fun simpanAbsensiBatch(list: List<AbsensiEntity>) = db.absensiDao().insertOrUpdateBatch(list)

    val allJurnal = db.jurnalDao().getAllJurnal()
    val jumlahJurnal = db.jurnalDao().getJumlahJurnal()
    suspend fun insertJurnal(jurnal: JurnalEntity) = db.jurnalDao().insertJurnal(jurnal)

    suspend fun insertKomponen(komponen: KomponenNilaiEntity) = db.nilaiDao().insertKomponen(komponen)
    suspend fun insertNilai(nilai: NilaiSiswaEntity) = db.nilaiDao().insertNilai(nilai)
}
