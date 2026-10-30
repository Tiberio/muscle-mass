# Meu Treino — Revisor de 130 exercícios

## Conteúdo

- `index.html`: página independente, com a lista e os dados embutidos; funciona aberta localmente ou no GitHub Pages.
- `dados_pesquisa_130.json`: registro auditável dos 44 candidatos anteriores + 86 pesquisas com 3 alternativas (302 sugestões, 294 vídeos distintos).

## Como usar

1. Abra a página `docs/revisor-videos-130/index.html` ou publique em `https://tiberio.github.io/muscle-mass/revisor-videos-130/` se a branch main e a pasta `/docs` estiverem configuradas no Pages.
2. Para cada exercício, confira o nome em português/inglês, a orientação do próprio catálogo do app, e reproduza o vídeo (ou escolha entre 3 alternativas).
3. Se o vídeo durar mais de 40 segundos, informe início/fim e clique em **Testar trecho**. Somente recortes válidos de até 40 segundos podem ser aprovados.
4. Marque a caixa que confirma a revisão visual e clique em **Aprovar vídeo selecionado**. Use **Rejeitar este vídeo** para passar à próxima alternativa; ao rejeitar a última, o exercício é marcado como sem vídeo adequado. Use **Rejeitar todas** se nenhuma proposta serve.
5. Clique em **Exportar JSON** periodicamente. Esse arquivo permite continuar noutro navegador por **Importar decisões** e serve para consolidar os aprovados na versão futura do aplicativo.

## Privacidade e limites

A página não tem backend, não possui API key e não envia aprovações a nenhum servidor. As decisões ficam no armazenamento local do navegador, mas podem ser perdidas ao limpar dados ou mudar de perfil. Portanto, exporte o JSON. Vídeos e miniaturas são carregados do YouTube, sujeitos aos termos de uso desse serviço e às regras do navegador. Disponibilidade técnica por metadados não é prova da correção biomecânica do movimento.

## Implantação

A pasta `docs/revisor-videos-130` não modifica a página anterior `docs/revisor-videos` nem o catálogo Android. Para adicioná-la no repositório use `unzip -o ... -d .`, `git add docs/revisor-videos-130`, `git commit ...`, `git push`.
