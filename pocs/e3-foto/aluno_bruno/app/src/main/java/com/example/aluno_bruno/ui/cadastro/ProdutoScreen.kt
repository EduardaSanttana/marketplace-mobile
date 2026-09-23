package com.example.aluno_bruno.ui.cadastro

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.aluno_bruno.ui.CadastroViewModel
import com.example.aluno_bruno.util.criarArquivoParaCaptura
import com.example.aluno_bruno.util.salvarFotoLocal
import java.io.File

@Composable
fun ProdutoScreen(viewModel: CadastroViewModel, modifier: Modifier = Modifier) {
    var nome by remember { mutableStateOf("") }
    var descricao by remember { mutableStateOf("") }
    var preco by remember { mutableStateOf("") }
    var quantidade by remember { mutableStateOf("") }
    var negocianteId by remember { mutableStateOf("") }
    var fotoPath by remember { mutableStateOf<String?>(null) }

    val contexto = LocalContext.current
    val seletorDeImagem = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            fotoPath = salvarFotoLocal(contexto, uri)
        }
    }

    var capturaPendente by remember { mutableStateOf<Pair<String, Uri>?>(null) }
    val capturaDeFoto = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { sucesso ->
        if (sucesso) {
            fotoPath = capturaPendente?.first
        }
        capturaPendente = null
    }
    val solicitarPermissaoCamera = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { concedida ->
        if (concedida) {
            val (caminho, uri) = criarArquivoParaCaptura(contexto)
            capturaPendente = caminho to uri
            capturaDeFoto.launch(uri)
        }
    }
    val abrirCamera = {
        val temPermissao = ContextCompat.checkSelfPermission(
            contexto, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (temPermissao) {
            val (caminho, uri) = criarArquivoParaCaptura(contexto)
            capturaPendente = caminho to uri
            capturaDeFoto.launch(uri)
        } else {
            solicitarPermissaoCamera.launch(Manifest.permission.CAMERA)
        }
    }

    val produtos by viewModel.produtos.collectAsState()

    LazyColumn(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Cadastro de Produto")
            OutlinedTextField(
                value = nome,
                onValueChange = { nome = it },
                label = { Text("Nome") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = descricao,
                onValueChange = { descricao = it },
                label = { Text("Descrição") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = preco,
                onValueChange = { preco = it },
                label = { Text("Preço") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = quantidade,
                onValueChange = { quantidade = it },
                label = { Text("Quantidade") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = negocianteId,
                onValueChange = { negocianteId = it },
                label = { Text("ID do negociante") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            SeletorDeFoto(
                fotoPath = fotoPath,
                onEscolherDaGaleria = { seletorDeImagem.launch("image/*") },
                onTirarFoto = abrirCamera
            )
            Button(
                enabled = nome.isNotBlank(),
                onClick = {
                    viewModel.cadastrarProduto(
                        nome = nome,
                        descricao = descricao,
                        preco = preco.toDoubleOrNull() ?: 0.0,
                        quantidade = quantidade.toIntOrNull() ?: 0,
                        negocianteId = negocianteId.toLongOrNull() ?: 0L,
                        fotoPath = fotoPath
                    )
                    nome = ""
                    descricao = ""
                    preco = ""
                    quantidade = ""
                    negocianteId = ""
                    fotoPath = null
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Salvar")
            }
            Text("Produtos cadastrados")
        }
        items(produtos, key = { it.id }) { produto ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (produto.fotoPath != null) {
                        AsyncImage(
                            model = File(produto.fotoPath),
                            contentDescription = produto.nome,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                        )
                    }
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(produto.nome)
                        Text(produto.descricao)
                        Text("Preço: ${produto.preco} | Estoque: ${produto.quantidade}")
                    }
                }
            }
        }
    }
}

@Composable
internal fun SeletorDeFoto(
    fotoPath: String?,
    onEscolherDaGaleria: () -> Unit,
    onTirarFoto: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (fotoPath != null) {
            AsyncImage(
                model = File(fotoPath),
                contentDescription = "Foto do produto",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onTirarFoto) {
                Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                Text(" Tirar foto")
            }
            OutlinedButton(onClick = onEscolherDaGaleria) {
                Icon(Icons.Filled.AddAPhoto, contentDescription = null)
                Text(" Galeria")
            }
        }
    }
}
