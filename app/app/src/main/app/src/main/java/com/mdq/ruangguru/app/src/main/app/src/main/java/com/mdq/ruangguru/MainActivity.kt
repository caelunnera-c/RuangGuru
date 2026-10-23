package com.mdq.ruangguru

import android.app.Application
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ================= KELAS APLIKASI =================

class MDQApplication : Application() {
    val applicationScope = CoroutineScope(SupervisorJob())
    val database by lazy { AppDatabase.getDatabase(this, applicationScope) }
    val repository by lazy { AppRepository(database) }
    val userPreferences by lazy { UserPreferences(this) }
}

// ================= VIEWMODEL (LOGIKA DATA & DASHBOARD) =================

class MainViewModel(
    private val repository: AppRepository,
    private val preferences: UserPreferences
) : ViewModel() {

    val orientationMode = preferences.orientationFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "AUTO")
    val activeSemester = preferences.semesterFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Semester Genap")
    val activeTahunAjaran = preferences.tahunAjaranFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "2024/2025")
    val schoolName = preferences.schoolNameFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "MDQ Boarding School")

    val allKelas = repository.allKelas.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allSiswa = repository.allSiswa.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allJurnal = repository.allJurnal.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dashboardState: StateFlow<DashboardState> = combine(
        repository.jumlahSiswa,
        repository.jumlahKelas,
        repository.jumlahJurnal,
        repository.totalSesiAbsensi,
        repository.komposisiKehadiran
    ) { siswa, kelas, jurnal, sesi, komposisi ->
        var h = 0; var s = 0; var i = 0; var a = 0
        komposisi.forEach {
            when (it.status) {
                "HADIR" -> h += it.count
                "SAKIT" -> s += it.count
                "IZIN" -> i += it.count
                "ALFA" -> a += it.count
            }
        }
        val total = h + s + i + a
        val persen = if (total > 0) ((h.toDouble() / total) * 100).toInt() else 0

        DashboardState(
            totalSiswa = siswa,
            totalKelas = kelas,
            totalJurnal = jurnal,
            totalSesi = sesi,
            hadirCount = h,
            sakitCount = s,
            izinCount = i,
            alfaCount = a,
            persentaseHadir = persen
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardState())

    fun setOrientation(mode: String) = viewModelScope.launch { preferences.setOrientation(mode) }
    fun setSchoolInfo(name: String, sem: String, thn: String) = viewModelScope.launch { preferences.setSchoolInfo(name, sem, thn) }

    fun addKelas(nama: String, tingkat: String, wali: String, tahun: String, semester: String) = viewModelScope.launch {
        repository.insertKelas(KelasEntity(namaKelas = nama, tingkat = tingkat, waliKelas = wali, tahunAjaran = tahun, semester = semester))
    }
    fun deleteKelas(kelas: KelasEntity) = viewModelScope.launch { repository.deleteKelas(kelas) }

    fun addSiswa(nis: String, nama: String, jk: String, kelasId: Long, thn: String) = viewModelScope.launch {
        repository.insertSiswa(SiswaEntity(nis = nis, namaLengkap = nama, jenisKelamin = jk, kelasId = kelasId, tahunMasuk = thn))
    }
    fun deleteSiswa(siswa: SiswaEntity) = viewModelScope.launch { repository.deleteSiswa(siswa) }

    fun simpanAbsensi(list: List<AbsensiEntity>) = viewModelScope.launch { repository.simpanAbsensiBatch(list) }
    fun simpanJurnal(jurnal: JurnalEntity) = viewModelScope.launch { repository.insertJurnal(jurnal) }
    fun addKomponenNilai(kelasId: Long, mapel: String, nama: String, bobot: Double) = viewModelScope.launch {
        repository.insertKomponen(KomponenNilaiEntity(kelasId = kelasId, mataPelajaran = mapel, namaKomponen = nama, bobotPersen = bobot))
    }
    fun inputNilai(kompId: Long, siswaId: Long, nilai: Double) = viewModelScope.launch {
        repository.insertNilai(NilaiSiswaEntity(komponenId = kompId, siswaId = siswaId, nilai = nilai))
    }
}

class ViewModelFactory(
    private val repository: AppRepository,
    private val preferences: UserPreferences
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(repository, preferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// ================= ACTIVITY UTAMA =================

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as MDQApplication
        val factory = ViewModelFactory(app.repository, app.userPreferences)
        viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

        setContent {
            MDQTheme {
                val orientationMode by viewModel.orientationMode.collectAsState()

                LaunchedEffect(orientationMode) {
                    requestedOrientation = when (orientationMode) {
                        "PORTRAIT" -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        "LANDSCAPE" -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    }
                }

                val config = LocalConfiguration.current
                val isTablet = config.screenWidthDp >= 600

                MainContainer(viewModel = viewModel, isTablet = isTablet)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContainer(viewModel: MainViewModel, isTablet: Boolean) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "dashboard"

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val dashboardState by viewModel.dashboardState.collectAsState()
    val semester by viewModel.activeSemester.collectAsState()
    val tahunAjaran by viewModel.activeTahunAjaran.collectAsState()
    val schoolName by viewModel.schoolName.collectAsState()
    val orientationMode by viewModel.orientationMode.collectAsState()

    val kelasList by viewModel.allKelas.collectAsState()
    val siswaList by viewModel.allSiswa.collectAsState()
    val jurnalList by viewModel.allJurnal.collectAsState()

    if (isTablet) {
        Row(modifier = Modifier.fillMaxSize()) {
            AppSidebar(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onQuickJournalClick = { navController.navigate("jurnal") }
            )

            Box(modifier = Modifier.weight(1f)) {
                NavContent(navController, viewModel, dashboardState, semester, tahunAjaran, schoolName, orientationMode, kelasList, siswaList, jurnalList, true)
            }
        }
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(modifier = Modifier.width(260.dp)) {
                    AppSidebar(
                        currentRoute = currentRoute,
                        onNavigate = { route ->
                            scope.launch { drawerState.close() }
                            navController.navigate(route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onQuickJournalClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate("jurnal")
                        }
                    )
                }
            }
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("MDQ Ruang Guru", style = MaterialTheme.typography.titleMedium) },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        }
                    )
                }
            ) { padding ->
                Box(modifier = Modifier.padding(padding)) {
                    NavContent(navController, viewModel, dashboardState, semester, tahunAjaran, schoolName, orientationMode, kelasList, siswaList, jurnalList, false)
                }
            }
        }
    }
}

@Composable
fun NavContent(
    navController: androidx.navigation.NavHostController,
    viewModel: MainViewModel,
    dashboardState: DashboardState,
    semester: String,
    tahunAjaran: String,
    schoolName: String,
    orientationMode: String,
    kelasList: List<KelasEntity>,
    siswaList: List<SiswaEntity>,
    jurnalList: List<JurnalEntity>,
    isTablet: Boolean
) {
    NavHost(navController = navController, startDestination = "dashboard") {
        composable("dashboard") {
            DashboardScreen(
                state = dashboardState,
                semester = semester,
                tahunAjaran = tahunAjaran,
                onMulaiAbsensi = { navController.navigate("absensi") },
                onInputNilai = { navController.navigate("nilai") },
                isTablet = isTablet
            )
        }
        composable("kelas") {
            KelasScreen(kelasList, { n, t, w, th, s -> viewModel.addKelas(n, t, w, th, s) }, { viewModel.deleteKelas(it) })
        }
        composable("siswa") {
            SiswaScreen(siswaList, kelasList, { nis, nama, jk, kid, th -> viewModel.addSiswa(nis, nama, jk, kid, th) }, { viewModel.deleteSiswa(it) })
        }
        composable("absensi") {
            AbsensiScreen(kelasList, siswaList) { viewModel.simpanAbsensi(it) }
        }
        composable("nilai") {
            NilaiScreen(kelasList, siswaList, { kid, m, n, b -> viewModel.addKomponenNilai(kid, m, n, b) }, { kid, sid, n -> viewModel.inputNilai(kid, sid, n) })
        }
        composable("jurnal") {
            JurnalScreen(jurnalList) { viewModel.simpanJurnal(it) }
        }
        composable("pengaturan") {
            PengaturanScreen(orientationMode, schoolName, semester, tahunAjaran, { viewModel.setOrientation(it) }, { n, s, t -> viewModel.setSchoolInfo(n, s, t) })
        }
    }
}
