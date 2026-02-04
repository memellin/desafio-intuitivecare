<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { Bar } from 'vue-chartjs'
import { Chart as ChartJS, Title, Tooltip, Legend, BarElement, CategoryScale, LinearScale } from 'chart.js'

// --- CONFIGURAÇÃO DOS GRÁFICOS ---
ChartJS.register(Title, Tooltip, Legend, BarElement, CategoryScale, LinearScale)

// --- VARIÁVEIS DE ESTADO (Onde guardamos os dados) ---
const operadoras = ref([])
const estatisticas = ref(null)
const page = ref(1)
const totalPages = ref(1)
const search = ref("")
const loading = ref(false)
const errorMessage = ref("")

// Configuração do Gráfico (Vazio inicialmente)
const chartData = ref({ labels: [], datasets: [] })
const chartOptions = { responsive: true, maintainAspectRatio: false }

// --- FUNÇÃO 1: Buscar Operadoras (Tabela) ---
const fetchOperadoras = async () => {
  loading.value = true
  errorMessage.value = ""
  
  try {
    // ATENÇÃO: Conectando na porta 8001 onde o Python está rodando
    const response = await axios.get(`http://127.0.0.1:8001/api/operadoras`, {
      params: { 
        page: page.value, 
        limit: 10, 
        search: search.value 
      }
    })
    
    operadoras.value = response.data.data
    // Cálculo simples de páginas: Total de registros / 10 por página
    totalPages.value = Math.ceil(response.data.meta.total / 10)
    
  } catch (error) {
    console.error("Erro API:", error)
    errorMessage.value = "Erro ao conectar com o servidor Python (Porta 8001). Verifique se ele está rodando!"
  } finally {
    loading.value = false
  }
}

// --- FUNÇÃO 2: Buscar Estatísticas (Gráfico) ---
const fetchEstatisticas = async () => {
  try {
    const response = await axios.get(`http://127.0.0.1:8001/api/estatisticas`)
    estatisticas.value = response.data
    
    // Pegamos apenas os Top 10 estados para o gráfico não ficar gigante
    const topUfs = response.data.distribuicao_uf.slice(0, 10)
    
    chartData.value = {
      labels: topUfs.map(item => item.uf), // Eixo X: Estados
      datasets: [{
        label: 'Total de Despesas (R$)',
        backgroundColor: '#42b983',
        data: topUfs.map(item => item.valor) // Eixo Y: Valores
      }]
    }
  } catch (error) {
    console.error("Erro Stats:", error)
  }
}

// --- CONTROLES DA TELA ---
const nextPage = () => {
  if (page.value < totalPages.value) {
    page.value++
    fetchOperadoras()
  }
}

const prevPage = () => {
  if (page.value > 1) {
    page.value--
    fetchOperadoras()
  }
}

const buscar = () => {
  page.value = 1 // Volta para a primeira página ao buscar
  fetchOperadoras()
}

// --- QUANDO A TELA CARREGAR ---
onMounted(() => {
  fetchOperadoras()
  fetchEstatisticas()
})
</script>

<template>
  <div class="app-container">
    <header>
      <h1>🏥 Intuitive Care - Dashboard</h1>
      <div v-if="estatisticas" class="stats-box">
        <p>Total Geral de Despesas: <strong>R$ {{ estatisticas.total_geral.toLocaleString('pt-BR', { minimumFractionDigits: 2 }) }}</strong></p>
      </div>
    </header>

    <div v-if="errorMessage" class="error-alert">
      {{ errorMessage }}
    </div>

    <div class="chart-section" v-if="chartData.labels.length > 0">
      <h3>Distribuição por Estado (Top 10)</h3>
      <div class="chart-wrapper">
        <Bar :data="chartData" :options="chartOptions" />
      </div>
    </div>

    <div class="search-bar">
      <input 
        v-model="search" 
        placeholder="Digite o CNPJ para buscar..." 
        @keyup.enter="buscar"
      />
      <button @click="buscar" :disabled="loading">
        {{ loading ? 'Carregando...' : '🔍 Buscar' }}
      </button>
    </div>

    <div class="table-wrapper">
      <table>
        <thead>
          <tr>
            <th>Registro ANS</th>
            <th>CNPJ</th>
            <th>Razão Social</th>
            <th>UF</th>
            <th>Modalidade</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="op in operadoras" :key="op.registro_ans">
            <td>{{ op.registro_ans }}</td>
            <td>{{ op.cnpj }}</td>
            <td>{{ op.razao_social }}</td>
            <td>{{ op.uf }}</td>
            <td>{{ op.modalidade }}</td>
          </tr>
          <tr v-if="operadoras.length === 0 && !loading">
            <td colspan="5" style="text-align: center; padding: 20px;">
              Nenhuma operadora encontrada.
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="pagination">
      <button @click="prevPage" :disabled="page === 1 || loading">◀ Anterior</button>
      <span>Página {{ page }} de {{ totalPages }}</span>
      <button @click="nextPage" :disabled="page >= totalPages || loading">Próxima ▶</button>
    </div>
  </div>
</template>

<style scoped>
/* ESTILOS SIMPLES E LIMPOS (KISS) */
.app-container {
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px;
  color: #2c3e50;
}

header {
  text-align: center;
  margin-bottom: 30px;
  border-bottom: 2px solid #eee;
  padding-bottom: 20px;
}

h1 { color: #42b983; margin: 0; }
.stats-box { font-size: 1.2rem; margin-top: 10px; }

.error-alert {
  background-color: #ffdddd;
  color: #d8000c;
  padding: 15px;
  border-radius: 5px;
  margin-bottom: 20px;
  text-align: center;
  border: 1px solid #d8000c;
}

.chart-section {
  background: #f9f9f9;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 30px;
  box-shadow: 0 2px 5px rgba(0,0,0,0.05);
}
.chart-wrapper { height: 300px; }

.search-bar { display: flex; gap: 10px; margin-bottom: 15px; }
input {
  flex: 1;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 1rem;
}
button {
  padding: 10px 20px;
  background-color: #35495e;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.3s;
}
button:hover:not(:disabled) { background-color: #42b983; }
button:disabled { background-color: #ccc; cursor: not-allowed; }

.table-wrapper { overflow-x: auto; margin-bottom: 20px; }
table {
  width: 100%;
  border-collapse: collapse;
  background: white;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}
th, td { padding: 12px 15px; text-align: left; border-bottom: 1px solid #eee; }
th { background-color: #35495e; color: white; font-weight: 600; }
tr:hover { background-color: #f1f1f1; }

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 15px;
  margin-top: 20px;
}
</style>