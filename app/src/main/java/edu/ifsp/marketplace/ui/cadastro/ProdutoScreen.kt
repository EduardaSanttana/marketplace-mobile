package edu.ifsp.marketplace.ui.cadastro

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import edu.ifsp.marketplace.data.Negociante
import edu.ifsp.marketplace.data.Produto
import edu.ifsp.marketplace.ui.CadastroViewModel
import edu.ifsp.marketplace.util.criarArquivoParaCaptura
import edu.ifsp.marketplace.util.salvarFotoLocal
import java.io.File
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.launch

/** Preço como o usuário digita, "15,90" ou "15.90"; null quando inválido ou não positivo. */
internal fun lerPreco(texto: String): Double? =
    texto.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 }

/** Estoque como o usuário digita: inteiro e não negativo; null quando inválido. */
internal fun lerQuantidade(texto: String): Int? = texto.trim().toIntOrNull()?.takeIf { it >= 0 }

/**
 * Produtos à venda do negociante ligado à conta: cadastra, edita (inclusive a
 * foto) e exclui. O vendedor é sempre ele, então não há escolha de dono.
 */
@Composable
fun ProdutoScreen(viewModel: CadastroViewModel, eu: Negociante, modifier: Modifier = Modifier) {
    var nome by remember { mutableStateOf("") }
    var descricao by remember { mutableStateOf("") }
    var preco by remember { mutableStateOf("") }
    var quantidade by remember { mutableStateOf("") }
    var fotoPath by remember { mutableStateOf<String?>(null) }
    // Produto aberto para edição; null quando o formulário cadastra um novo
    var editandoId by remember { mutableStateOf<Long?>(null) }
    val lista = rememberLazyListState()
    val escopo = rememberCoroutineScope()
    var produtoParaExcluir by remember { mutableStateOf<Produto?>(null) }

    val precoLido = lerPreco(preco)
    val quantidadeLida = lerQuantidade(quantidade)
    // O erro só aparece depois que algo foi digitado no campo
    val precoComErro = preco.isNotBlank() && precoLido == null
    val quantidadeComErro = quantidade.isNotBlank() && quantidadeLida == null

    fun limparFormulario() {
        nome = ""
        descricao = ""
        preco = ""
        quantidade = ""
        fotoPath = null
        editandoId = null
    }

    fun editar(produto: Produto) {
        editandoId = produto.id
        nome = produto.nome
        descricao = produto.descricao
        preco = "%.2f".format(Locale("pt", "BR"), produto.preco)
        quantidade = produto.quantidade.toString()
        fotoPath = produto.fotoPath
        // O formulário fica no topo da lista
        escopo.launch { lista.animateScrollToItem(0) }
    }

    val contexto = LocalContext.current
    // O seletor de documentos lê as pastas do aparelho, como Downloads. O seletor
    // de fotos esconde arquivos ainda não indexados, como os arrastados para o emulador
    val seletorDeImagem = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
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
    val meusProdutos = produtos.filter { it.negocianteId == eu.id }
    val emEdicao = meusProdutos.firstOrNull { it.id == editandoId }
    val moeda = remember { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }

    LazyColumn(
        state = lista,
        modifier = modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SecaoFormulario(
                titulo = if (emEdicao != null) "Editar ${emEdicao.nome}" else "Novo produto",
                icone = if (emEdicao != null) Icons.Filled.Edit else Icons.Filled.ShoppingBag
            ) {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = descricao,
                    onValueChange = { descricao = it },
                    label = { Text("Descrição") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = preco,
                        onValueChange = { preco = it },
                        label = { Text("Preço") },
                        isError = precoComErro,
                        supportingText = if (precoComErro) {
                            { Text("Maior que zero") }
                        } else null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = quantidade,
                        onValueChange = { quantidade = it },
                        label = { Text("Qtd.") },
                        isError = quantidadeComErro,
                        supportingText = if (quantidadeComErro) {
                            { Text("Inteiro, 0 ou mais") }
                        } else null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    "Vendido por ${eu.nome}",
                    style = MaterialTheme.typography.labelLarge
                )
                SeletorDeFoto(
                    fotoPath = fotoPath,
                    onEscolherDaGaleria = { seletorDeImagem.launch(arrayOf("image/*")) },
                    onTirarFoto = abrirCamera
                )
                BotaoSalvar(
                    habilitado = nome.isNotBlank() && precoLido != null && quantidadeLida != null,
                    onClick = {
                        val valor = precoLido ?: return@BotaoSalvar
                        val estoque = quantidadeLida ?: return@BotaoSalvar
                        if (emEdicao != null) {
                            viewModel.atualizarProduto(
                                original = emEdicao,
                                nome = nome.trim(),
                                descricao = descricao.trim(),
                                preco = valor,
                                quantidade = estoque,
                                vendedor = eu,
                                fotoPath = fotoPath
                            )
                        } else {
                            viewModel.cadastrarProduto(
                                nome = nome.trim(),
                                descricao = descricao.trim(),
                                preco = valor,
                                quantidade = estoque,
                                vendedor = eu,
                                fotoPath = fotoPath
                            )
                        }
                        limparFormulario()
                    }
                )
                if (emEdicao != null) {
                    OutlinedButton(
                        onClick = { limparFormulario() },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancelar edição")
                    }
                }
            }

            Spacer12()
            TituloLista("Meus produtos", meusProdutos.size)
        }

        if (meusProdutos.isEmpty()) {
            item {
                EstadoVazio(
                    icone = Icons.Filled.Inventory2,
                    mensagem = "Você ainda não tem produtos à venda"
                )
            }
        }

        items(meusProdutos, key = { it.id }) { produto ->
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    // A foto fica só no aparelho que a tirou; nos outros, o ícone ocupa o lugar
                    val foto = produto.fotoPath?.let(::File)?.takeIf { it.exists() }
                    if (foto != null) {
                        AsyncImage(
                            model = foto,
                            contentDescription = produto.nome,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                        )
                    } else {
                        IconeCircular(icone = Icons.Filled.ShoppingBag)
                    }
                    Spacer12Horizontal()
                    Column(modifier = Modifier.weight(1f)) {
                        Text(produto.nome, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        if (produto.descricao.isNotBlank()) {
                            Text(
                                produto.descricao,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                moeda.format(produto.preco),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Estoque: ${produto.quantidade}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = { editar(produto) }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Editar")
                    }
                    IconButton(onClick = { produtoParaExcluir = produto }) {
                        Icon(
                            Icons.Filled.DeleteOutline,
                            contentDescription = "Excluir",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }

    produtoParaExcluir?.let { produto ->
        AlertDialog(
            onDismissRequest = { produtoParaExcluir = null },
            title = { Text("Excluir ${produto.nome}") },
            text = {
                Text(
                    "O produto sai da vitrine deste e dos outros aparelhos. " +
                        "Os pedidos já feitos continuam com o nome dele."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (editandoId == produto.id) limparFormulario()
                    viewModel.excluirProduto(produto)
                    produtoParaExcluir = null
                }) {
                    Text("Excluir", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { produtoParaExcluir = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
internal fun SecaoFormulario(
    titulo: String,
    icone: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer12Horizontal()
                Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            content()
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
            OutlinedButton(onClick = onTirarFoto, shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                Spacer12Horizontal()
                Text("Tirar foto")
            }
            OutlinedButton(onClick = onEscolherDaGaleria, shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Filled.AddAPhoto, contentDescription = null)
                Spacer12Horizontal()
                Text("Galeria")
            }
        }
    }
}

@Composable
internal fun BotaoSalvar(habilitado: Boolean, onClick: () -> Unit) {
    androidx.compose.material3.Button(
        onClick = onClick,
        enabled = habilitado,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Salvar")
    }
}

@Composable
internal fun TituloLista(titulo: String, quantidade: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            "$quantidade",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
internal fun EstadoVazio(icone: androidx.compose.ui.graphics.vector.ImageVector, mensagem: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            icone,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(40.dp)
        )
        Text(
            mensagem,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun IconeCircular(icone: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icone,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
internal fun Spacer12() {
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
internal fun Spacer12Horizontal() {
    Spacer(modifier = Modifier.width(10.dp))
}
