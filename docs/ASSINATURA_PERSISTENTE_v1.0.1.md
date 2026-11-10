# Meu Treino v1.0.1 — assinatura persistente para builds debug

## Objetivo

O Android só aceita atualização sobre o mesmo pacote (`br.com.meutreino`) se a assinatura do novo APK for compatível. Como os runners do GitHub Actions são descartáveis, o APK debug precisa receber **a mesma chave em todas as execuções**. A v1.0.1 usa `versionCode=21`; o banco permanece Room v5.

O workflow recebe a cópia da chave exclusivamente via secret do GitHub e falha se não encontrar esse secret. **Correção adicional:** a partir do hotfix, o Gradle também recebe explicitamente `MEUTREINO_DEBUG_KEYSTORE_PATH` e vincula esse arquivo ao `signingConfig` de `debug`. Sem essa vinculação, o APK anterior foi assinado com uma chave diferente, mesmo após o secret ter sido carregado. Depois de compilar, verifica automaticamente se o certificado SHA-256 do APK **coincide** com o da chave fornecida.

## 1. Criar a chave uma única vez (Debian 13)

```bash
cd ~/muscle-mass
bash scripts/criar_chave_debug_persistente.sh
```

O script usa `keytool`, normalmente disponível em `openjdk-17-jdk` dos repositórios Debian. Se necessário: `sudo apt install openjdk-17-jdk`.

A chave é criada em `~/.local/share/meutreino-assinatura/debug.keystore` e **não** será substituída caso já exista. O arquivo usa o alias padrão `androiddebugkey` e credenciais de debug `android`; por isso, **a privacidade da cópia do keystore é essencial**. Esse esquema atende a builds de desenvolvimento pessoal, mas não à distribuição pública.

Faça uma cópia offline protegida da chave e não crie outra para versões futuras. Se perder a chave e o secret, novas atualizações poderão voltar a exigir reinstalação.

## 2. Gravar a chave no GitHub Actions

Abra o repositório `Tiberio/muscle-mass` no navegador e acesse:

**Settings → Secrets and variables → Actions → New repository secret**

- Nome: `MEUTREINO_DEBUG_KEYSTORE_B64`
- Valor: a **saída** produzida por `base64 -w0 "$HOME/.local/share/meutreino-assinatura/debug.keystore"; echo` no seu Debian

Cole somente o texto base64 no campo de valor do secret, sem aspas, sem quebras de linha e sem comandos. **Nunca** coloque essa sequência em arquivos versionados, `git commit`, mensagens, issues ou prints públicos. Não envie a chave neste chat.

Se o secret já existir, preserve a chave antiga **se ela foi utilizada para assinar algum APK que você quer continuar atualizando**. Não substitua o secret a cada versão.

## 3. Compilar v1.0.1

Atualize seu repositório com os arquivos fornecidos e execute os testes. Faça `git commit` e `git push origin main`. No GitHub Actions, confira:

- Etapa **Carregar assinatura de depuração persistente**: passou
- Etapa **Exibir configuracao de assinatura escolhida pelo Gradle**: o `signingReport` deve mostrar o caminho da chave carregada
- Etapa **Verificar assinatura do APK contra o certificado persistente**: mostra `ASSINATURA OK`
- Artefato: `MeuTreino-Android-v1.0.1-debug` (contém `app-debug.apk`)

O hotfix pode ser aplicado **sem alterar o secret nem criar outra chave**, desde que a chave configurada no GitHub seja a mesma salva no Debian.

Um workflow executado antes da criação do secret pode falhar como previsto: **não use o APK gerado por um workflow antigo**.

## 4. Instalar no celular — primeira vez com chave fixa

O usuário optou por **não preservar o histórico local da v0.6.0**. Para não perder acesso ao app em caso de erro de compilação, primeiro baixe o APK v1.0.1 com `ASSINATURA OK` e **depois**:

1. Desinstale o Meu Treino antigo pelo Android. Isso apagará as fichas, sessões e medidas armazenadas localmente.
2. Instale `app-debug.apk` gerado pela execução v1.0.1 corrigida.
3. Conclua a configuração inicial e confira a identificação da versão.
4. Faça um teste em **Perfil → Cópia de segurança**: exporte um JSON para local privado.

Para a v1.0.2 e posteriores, mantenha o **mesmo secret**, use `versionCode` crescente e instale o APK novo **sem desinstalar**. A verificação SHA-256 no workflow ajudará a evitar mudanças involuntárias na assinatura.

## Limitações

- Uma chave fixa criada agora não recupera os dados de uma instalação assinada pela chave antiga.
- O backup JSON é legível: não o publique em repositórios ou chats.
- Apagar o app antigo remove seu banco local, independentemente de a nova versão ter backup embutido.
- Esse certificado é de **debug**. Uma futura publicação pública exigirá um planejamento de chave de **release**, sem presumir atualização direta de uma versão debug.
