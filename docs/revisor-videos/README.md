# Meu Treino — site de revisão dos 60 vídeos candidatos

Este site é uma ferramenta **independente do APK** do Meu Treino. Ele foi preparado a partir do arquivo `Auditoria-60-videos-MeuTreino.zip` e do cadastro da versão 0.4.5.

## O que contém

- 60 vídeos candidatos do YouTube: 45 com duração total de até 40 segundos e 15 com duração maior.
- Nome do exercício em português e em inglês, com uma orientação curta extraída do catálogo do aplicativo.
- Player incorporado ao YouTube, com alternativa para abrir o original.
- Para vídeos acima de 40 segundos: campos para escolher início e fim, e para visualizar a seleção; só é possível aprovar após definir e reproduzir um intervalo de até 40 segundos.
- Botões **✓ Aprovar vídeo** e **✕ Rejeitar vídeo**, motivo opcional, anotações, indicadores de progresso e filtros.
- Gravação local das decisões no navegador e exportação dos resultados para JSON ou CSV.
- Importação de decisões previamente exportadas em JSON.

As explicações dos exercícios vieram do catálogo do Meu Treino. A disponibilidade de cada vídeo, sua duração e permissão de incorporação vieram da auditoria do YouTube. **A correspondência do movimento e sua execução técnica ainda precisam da sua avaliação visual.**

## Hospedagem recomendada: GitHub Pages

O pacote foi organizado para descompactação na raiz do repositório `muscule-mass`; os arquivos ficam em `docs/revisor-videos/`.

```bash
cd ~/muscule-mass
unzip -o ~/Downloads/MeuTreino_SITE_Revisor_60_Videos.zip -d .
git add docs/revisor-videos
git commit -m "Adiciona revisor web dos 60 videos"
git push
```

No GitHub, abra **Settings → Pages → Build and deployment** e configure:

- **Source:** `Deploy from a branch`
- **Branch:** `main`
- **Folder:** `/docs`
- Clique em **Save**.

A página da configuração mostrará o endereço publicado. Em um repositório com este nome, espera-se um endereço semelhante a `https://tiberio.github.io/muscule-mass/revisor-videos/`, mas confirme o link real no GitHub após a publicação.

Se já houver uma configuração do GitHub Pages, confira antes de alterá-la. O conteúdo publicado pelo GitHub Pages é acessível publicamente; **não inclua dados pessoais no site**.

### Alternativa sem hospedar

O arquivo `index.html` é independente de bibliotecas JavaScript e do backend e pode ser aberto no navegador do computador. Alguns navegadores/players do YouTube podem não permitir a incorporação quando aberto diretamente por `file://`; nesse caso, use o GitHub Pages. Para testar localmente com HTTP:

```bash
cd ~/muscule-mass/docs/revisor-videos
python3 -m http.server 8000
```

Então abra `http://localhost:8000/` no computador.

## Como revisar

1. Escolha um exercício na lista da esquerda (no celular, a lista fica acima do player).
2. Leia a orientação curta e assista ao vídeo.
3. **Vídeo de até 40 segundos:** pode ser aprovado integralmente.
4. **Vídeo longo:** assista ao original, informe o primeiro e o último segundo do trecho demonstrativo, incluindo 2–4 segundos de contexto antes e depois quando possível, e toque em **Reproduzir trecho selecionado**. O trecho aprovado deve ter no máximo 40 segundos.
5. Clique em **✓ Aprovar vídeo** ou **✕ Rejeitar vídeo**. Inclua motivo e observação quando necessário.
6. Ao terminar (ou antes de trocar de dispositivo), toque em **Exportar JSON** e envie o arquivo `revisao_60_videos_meutreino.json` na conversa do ChatGPT.

As decisões são guardadas em `localStorage` **apenas no navegador e dispositivo usados**. Elas não são enviadas ao GitHub, não alteram automaticamente o catálogo e podem desaparecer se você limpar os dados do navegador. Use **Exportar JSON** para preservar seu trabalho; **Importar decisões** permite retomá-lo em outro aparelho. O CSV é útil para consulta em planilhas.

## Observações técnicas

- Os IDs do YouTube não são inventados: vêm do relatório da auditoria.
- O trecho é reproduzido pelo player oficial com parâmetros `start` e `end`, medidos em segundos a partir do início do vídeo.
- Este site não precisa da chave `YOUTUBE_API_KEY` e não armazena nem exibe nenhum segredo.
- O YouTube pode recusar a reprodução incorporada de algum vídeo mesmo quando os metadados o indicam como incorporável. Use **Abrir no YouTube** caso isso ocorra.
- O site não determina se a técnica é correta; essa decisão é humana.
