package edu.ifsp.marketplace.ui.entregador

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import edu.ifsp.marketplace.data.Pedido
import edu.ifsp.marketplace.ui.CadastroViewModel
import edu.ifsp.marketplace.util.LocalizacaoService
import edu.ifsp.marketplace.util.criarArquivoParaCaptura
import edu.ifsp.marketplace.util.formatarDistancia
import kotlinx.coroutines.launch
import java.io.File

/**
 * Registro de entrega, disponível tanto na lista de entregas quanto na tela de rota.
 *
 * @param obtendoPosicao verdadeiro entre a foto tirada e o registro da entrega
 * @param iniciar pede as permissões, abre a câmera e, com a foto, registra a posição atual
 */
class RegistroEntrega(val obtendoPosicao: Boolean, val iniciar: (Pedido) -> Unit)

/**
 * Sequência da comprovação: localização -> conferência da distância -> câmera ->
 * foto -> posição atual.
 *
 * Longe do ponto marcado, o entregador confirma antes de abrir a câmera: um toque
 * em "Cheguei" ainda no caminho registraria a entrega no lugar errado. Por isso a
 * função também mostra esse diálogo, além de devolver o [RegistroEntrega].
 *
 * O endereço não é consultado aqui: o [CadastroViewModel] grava o registro e
 * completa o endereço em seguida, para não depender da conexão neste momento.
 */
@Composable
fun rememberRegistroEntrega(
    viewModel: CadastroViewModel,
    onRegistrada: () -> Unit = {}
): RegistroEntrega {
    val context = LocalContext.current
    val escopo = rememberCoroutineScope()

    // Sobrevivem à recriação da Activity, comum quando a câmera abre em aparelho com pouca memória
    var pedidoEmRegistro by rememberSaveable { mutableStateOf<Long?>(null) }
    var caminhoFoto by rememberSaveable { mutableStateOf<String?>(null) }
    var obtendoPosicao by remember { mutableStateOf(false) }
    // O pedido inteiro só é preciso antes da câmera, para medir a distância ao destino
    var pedidoAtual by remember { mutableStateOf<Pedido?>(null) }
    var distanciaParaConfirmar by remember { mutableStateOf<Double?>(null) }

    fun concluir(pedidoId: Long, foto: String) {
        obtendoPosicao = true
        escopo.launch {
            val posicao = LocalizacaoService.posicaoAtual(context)
            obtendoPosicao = false
            if (posicao == null) {
                File(foto).delete()
                Toast.makeText(
                    context,
                    "Não consegui obter sua posição. Verifique o GPS e tente de novo.",
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }
            viewModel.registrarEntrega(pedidoId, foto, posicao.latitude, posicao.longitude)
            Toast.makeText(context, "Entrega registrada", Toast.LENGTH_SHORT).show()
            onRegistrada()
        }
    }

    val tirarFoto = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { sucesso ->
        val pedidoId = pedidoEmRegistro
        val foto = caminhoFoto
        pedidoEmRegistro = null
        caminhoFoto = null
        if (sucesso && pedidoId != null && foto != null) concluir(pedidoId, foto)
    }

    fun abrirCamera() {
        val (caminho, uri) = criarArquivoParaCaptura(context)
        caminhoFoto = caminho
        tirarFoto.launch(uri)
    }

    val pedirCamera = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedida ->
        if (concedida) {
            abrirCamera()
        } else {
            pedidoEmRegistro = null
            Toast.makeText(context, "Permissão de câmera negada", Toast.LENGTH_SHORT).show()
        }
    }

    fun abrirCameraComPermissao() {
        val temCamera = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (temCamera) abrirCamera() else pedirCamera.launch(Manifest.permission.CAMERA)
    }

    /** Sem posição, segue para a câmera: o registro tenta de novo depois da foto e avisa se falhar. */
    fun conferirDistancia() {
        val pedido = pedidoAtual ?: return abrirCameraComPermissao()
        obtendoPosicao = true
        escopo.launch {
            val posicao = LocalizacaoService.posicaoAtual(context)
            obtendoPosicao = false
            if (posicao != null && pedido.longeDoDestino(posicao.latitude, posicao.longitude)) {
                distanciaParaConfirmar = pedido.distanciaAteDestinoMetros(posicao.latitude, posicao.longitude)
            } else {
                abrirCameraComPermissao()
            }
        }
    }

    val pedirLocalizacao = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissoes ->
        if (permissoes.values.any { it }) {
            conferirDistancia()
        } else {
            pedidoEmRegistro = null
            Toast.makeText(
                context,
                "A entrega precisa da sua localização para ser registrada",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    distanciaParaConfirmar?.let { metros ->
        AlertDialog(
            onDismissRequest = {
                distanciaParaConfirmar = null
                pedidoEmRegistro = null
            },
            title = { Text("Longe do destino") },
            text = {
                Text(
                    "Você está a ${formatarDistancia(metros)} do ponto de entrega marcado. " +
                        "Registrar a entrega aqui mesmo assim?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    distanciaParaConfirmar = null
                    abrirCameraComPermissao()
                }) {
                    Text("Registrar mesmo assim")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    distanciaParaConfirmar = null
                    pedidoEmRegistro = null
                }) {
                    Text("Voltar")
                }
            }
        )
    }

    return RegistroEntrega(obtendoPosicao) { pedido ->
        pedidoEmRegistro = pedido.id
        pedidoAtual = pedido
        if (LocalizacaoService.temPermissao(context)) conferirDistancia()
        else pedirLocalizacao.launch(LocalizacaoService.PERMISSOES)
    }
}
