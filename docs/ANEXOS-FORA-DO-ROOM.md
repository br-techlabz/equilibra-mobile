# Anexos fora do Room — política verificável (Task 5.5)

**Decisão:** anexos binários **nunca** são armazenados no app. O backend é a
única fonte dos bytes; o Room guarda somente dados das transações/contas/
categorias/tags. Nenhuma coluna BLOB existe no banco local.

## O que o app guarda (e onde)

| Dado | Onde fica | Vida útil |
|---|---|---|
| Metadata do anexo (id, nome, tipo, tamanho, data) | **Memória** (`AttachmentsUiState`) | Enquanto a tela de detalhe está aberta |
| Bytes do anexo (upload) | Stream da URI do SAF → corpo multipart | Só durante o envio |
| Bytes do anexo (download) | Stream do backend → destino escolhido pelo usuário (SAF `CreateDocument`) | Arquivo do usuário, fora do app |
| PDF para preview | `cacheDir/preview*.pdf` (exigência do `PdfRenderer` nativo) | Apagado em `finally`; nome fixo `preview` para varredura |
| Imagem para preview | `Bitmap` em memória | Enquanto o diálogo de preview existe |
| **Binário em Room** | — | **Nunca** |

## Regras implementadas

1. **Sem tabela/coluna de anexo no Room.** O schema exportado tem 4 tabelas
   (`accounts`, `categories`, `tags`, `transactions`) e 0 colunas BLOB.
2. **Sem DAO ou entidade de anexo.** `data/local/` não referencia attachments.
3. **Metadata só em memória.** `TransactionAttachmentsViewModel` recarrega a
   lista da API a cada abertura (`GET /transactions/{id}/attachments`); não há
   cache de metadata entre sessões.
4. **Download não passa pelo app.** `saveDownload` copia o stream direto para
   o `OutputStream` do documento criado pelo usuário no SAF.
5. **Preview temporário é limpo.** O arquivo exigido pelo `PdfRenderer` fica no
   `cacheDir` do app e é apagado em `finally`, mesmo em erro.
6. **Falha de download não deixa lixo no destino.** O documento parcial criado
   pelo SAF é removido (`ContentResolver.delete`).

## Verificação automatizada

`app/src/test/java/br/com/equilibra/mobile/data/local/RoomAttachmentsPolicyTest.kt`
falha se alguém:

- adicionar tabela cujo nome contenha `attachment`;
- introduzir coluna com afinidade `BLOB`;
- referenciar anexos em código de `data/local/` (Kdoc/comentário é permitido).

Rode com:

```bash
./gradlew :app:testDebugUnitTest --tests "*RoomAttachmentsPolicyTest"
```

## Limites conhecidos

- **Preview de PDF** usa um arquivo temporário no `cacheDir` por exigência da
  API nativa (`PdfRenderer` recebe `ParcelFileDescriptor`). Ele existe apenas
  durante a renderização e é removido no mesmo escopo.
- **Thumbnails/cache de imagem** não são gerados: o `Bitmap` do preview vive
  apenas enquanto o diálogo está aberto.
- O `ContentResolver` do destino do download pertence ao usuário; o app não
  mantém permissão sobre ele após o fluxo.
