package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.MainViewModel
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun StatisticsScreen(viewModel: MainViewModel) {
    val payments by viewModel.allPayments.collectAsState()
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(payments) {
        withContext(Dispatchers.Default) {
            // Aggregate payments by month (simple example: using monthYear field)
            val monthlyPayments = payments
                .groupBy { it.monthYear }
                .mapValues { entry -> entry.value.sumOf { it.paidAmount } }
                .toSortedMap()

            modelProducer.runTransaction {
                columnSeries {
                    series(monthlyPayments.values.map { it.toFloat() })
                }
            }
        }
    }

    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        Text("إحصائيات المدفوعات الشهرية", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        if (payments.isEmpty()) {
            Text("لا توجد بيانات متاحة.")
        } else {
            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberColumnCartesianLayer(),
                    startAxis = rememberStartAxis(),
                    bottomAxis = rememberBottomAxis()
                ),
                modelProducer = modelProducer,
                modifier = Modifier.fillMaxWidth().height(300.dp)
            )
        }
    }
}
