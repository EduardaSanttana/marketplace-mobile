package edu.ifsp.marketplace.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

private fun pastaFotos(context: Context): File =
    File(context.filesDir, "fotos_produtos").apply { mkdirs() }

/**
 * Copia a imagem selecionada (ex: da galeria) para a pasta interna do app
 * (sistema de arquivos do dispositivo), evitando depender de permissões
 * persistentes sobre a URI original. Não há envio para nenhum serviço de nuvem.
 */
fun salvarFotoLocal(context: Context, uriOrigem: Uri): String? {
    return try {
        val arquivoDestino = File(pastaFotos(context), "${UUID.randomUUID()}.jpg")
        context.contentResolver.openInputStream(uriOrigem)?.use { input ->
            arquivoDestino.outputStream().use { output -> input.copyTo(output) }
        }
        arquivoDestino.absolutePath
    } catch (e: Exception) {
        null
    }
}

/**
 * Cria um arquivo vazio na pasta local de fotos e retorna, junto com o
 * caminho absoluto, a URI de conteúdo (via FileProvider) que a câmera do
 * sistema usa para gravar a foto capturada diretamente ali.
 */
fun criarArquivoParaCaptura(context: Context): Pair<String, Uri> {
    val arquivo = File(pastaFotos(context), "${UUID.randomUUID()}.jpg")
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", arquivo)
    return arquivo.absolutePath to uri
}
