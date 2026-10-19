# Compilação remota do aplicativo — sem alterar o Debian

**Objetivo:** gerar `app-debug.apk` no GitHub Actions usando Gradle 9.4.1, Build Tools 36 e Android 37.0 **somente no servidor do GitHub**. Nenhuma instalação adicional é feita no computador Debian.

## Preparação

1. Crie um repositório **privado** e vazio no GitHub, por exemplo `meu-treino-android` (sem inicializar README ou .gitignore no GitHub).
2. Descompacte esta distribuição no seu computador. Abra o terminal na pasta `MeuTreinoAndroid` que contém `settings.gradle.kts` **e** a pasta oculta `.github`. O arquivo `.github/workflows/android-debug.yml` precisa ser enviado junto.
3. Se não tiver `git`, instale-o pelo Synaptic usando o repositório oficial Debian. Não há necessidade de instalar Android Studio, SDK novo ou Gradle novo.
4. Configure nome/e-mail de commit caso ainda não tenha feito: `git config --global user.name "Seu nome"` e `git config --global user.email "seu-email@example.com"` (substitua por dados seus).
5. Execute:

```bash
git init -b main
git add .
git commit -m "Primeira versao Android com compilacao remota"
git remote add origin https://github.com/SEU_USUARIO/meu-treino-android.git
git push -u origin main
```

Substitua `SEU_USUARIO` pelo nome da sua conta e use o URL exato mostrado pelo GitHub. Caso o Git solicite credenciais por HTTPS, utilize o método de autenticação oferecido pelo GitHub (um token pessoal ou gerenciador de credenciais; a senha da conta não funciona para push HTTPS).

## Gerar e baixar o APK

1. Abra o repositório no navegador e entre em **Actions**.
2. Clique em **Compilar APK Android** (menu lateral).
3. Clique em **Run workflow** e selecione a branch `main` (a execução também pode ocorrer automaticamente após o `push`).
4. Abra a execução mais recente e acompanhe os passos; em caso de erro, copie a seção inicial da mensagem `FAILURE` ou `e: ...`, não apenas as últimas linhas do resumo.
5. **Somente se a compilação terminar com sucesso**, abra a página dessa execução e procure **Artifacts → MeuTreino-Android-debug**. Baixe o ZIP e extraia `app-debug.apk`.
6. Instale no telefone. Se o `adb` já estiver instalado pelo Synaptic e a depuração USB autorizada:

```bash
adb devices
adb install -r app-debug.apk
```

Também é possível transferir `app-debug.apk` para o telefone e abri-lo pelo gerenciador de arquivos, autorizando a instalação desse arquivo quando o Android solicitar.

## Limites e privacidade

- O **código-fonte será enviado ao GitHub**. Prefira repositório privado. Não coloque senhas, tokens, contas ou históricos pessoais no repositório.
- O runner GitHub Actions baixa SDK/Gradle e dependências para executar a compilação, mas **não instala nem modifica pacotes no Debian**.
- Este workflow está preparado, mas **a compilação Android ainda não foi executada**: podem aparecer erros Kotlin/Gradle no primeiro build, que precisaremos corrigir a partir do log.
- É uma versão de depuração para testes pessoais, não uma publicação na Play Store.
