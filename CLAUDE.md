# Frente Fria

App Android **pessoal** (APK sem Play Store: o primeiro por USB, depois se atualiza pelas releases do GitHub) para o marido acompanhar o ciclo da esposa e sobreviver à TPM. Tom sarcástico. Ela não precisa saber que o app existe. Mesma base do `~/Projects/jardim` (estrutura, versões, padrão de avisos).

## Estrutura

- `motor/`: Kotlin puro (JVM), sem Android. Todas as regras, com teste:
  - `ciclo/Ciclo.kt`: previsão (média ponderada dos últimos 6 ciclos, o mais recente com peso 6; com menos de 3, o padrão informado entra com peso 1; ciclos fora de 18 a 50 dias ficam de fora), fases e dia (`diaEm`). Dia até hoje usa o registrado; passou do previsto sem registro é `ATRASADA` (no próprio dia previsto ainda é TPM). O futuro projeta ciclos com a média; atrasada projeta a partir de amanhã.
  - `ciclo/Aprendizado.kt`: TPM aprendida. Anda para trás a partir da véspera de cada início; dia com sinal (humor ≤ ruim, DR, 2 sintomas de TPM) estende, 3 dias registrados sem sinal param. Vale com 2 ciclos medidos (4 ou mais registros nos 14 dias antes); fica entre 3 e 12 dias. `tpmFixa` nas configurações vence.
  - `ciclo/Nivel.kt`: nível 0 a 4 (céu limpo a furacão). Base pela fase, misturada meio a meio com a intensidade observada no mesmo dia de outros ciclos (chave: "dia:N" até o dia 10, depois "antes:N"). Atraso nunca fica abaixo de 3.
  - `insights/`, `agenda/` (conflitos de evento), `kit/`, `avisos/PlanoDeAvisos.kt` (o que publicar de manhã e à noite), `conteudo/` (o JSON do manual), `texto/Vocabulario.kt` (todos os nomes nos dois modos), `backup/`.
  - `Situacao.kt` junta tudo: telas e avisos leem dela.
- `app/`: Compose + Room + DataStore + WorkManager, DI manual em `FrenteFriaApp`. Um ViewModel só (`PrincipalViewModel`), que recalcula a `Situacao` a cada mudança.
- Conteúdo (manchetes, faça/não faça, diga/nunca diga, agrados, fatos, "o que acontece") em `app/src/main/assets/conteudo/conteudo.json`. O `ConteudoTest` valida contra o JSON real.

## Modos (decisão do usuário, 2026-11-09)

- **CLIMA** (padrão): disfarçado de app de previsão do tempo. TPM = frente fria, menstruação = temporada de chuvas, folicular = primavera, ovulação = onda de calor, lútea = outono. **Nenhum texto visível pode ter as palavras de `PALAVRAS_PROIBIDAS_NO_CLIMA`** (tpm, menstrua, ciclo, cólica, ovula…). Texto do conteúdo que tem uma delas precisa do campo `clima`; os testes conferem conteúdo, vocabulário, dicas de evento e todos os avisos de 40 dias. Ao escrever texto novo para a UI, use `termos(modo)` ou um `if (clima)`.
- **DIRETO**: tudo com todas as letras.
- Escolhido no onboarding, trocável em Configurações. O ícone troca por dois `activity-alias` (`IconeClima`, `IconeDireto`; `Icone.kt`). Desligar o alias que abriu o app fecha a tarefa dele: por isso `aplicar` fecha a tarefa atual, abre o app pelo alias novo e desliga o antigo 1,5 s depois. Sem isso, o app fechava na troca.
- Canal de notificação único com nome neutro ("Previsão"); visibilidade privada na tela de bloqueio.

## Trava

PIN próprio do app (SHA-256 com sal no DataStore), não o do celular (ela pode saber). Digital opcional (`BIOMETRIC_STRONG`). Volta a travar depois de 1 min em segundo plano. Com trava, `FLAG_SECURE`: **o `screencap` sai vazio**, então para tirar print desligue a trava. Esqueceu o PIN: só apagando tudo.

## Privacidade

INTERNET só para o atualizador (`atualizacao/Atualizador.kt`, igual ao do Jardim): consulta `releases/latest` do repo, no máximo a cada 6 h, e baixa o APK para o `PackageInstaller`. Nenhum dado sai do celular; o repo é público (desde 2026-11-12) para o app consultar sem token. `allowBackup=false` e regras que excluem tudo do backup do Google. Backup só manual, em JSON, pelo seletor de arquivos (sem o PIN).

## Banco (Room)

`menstruacao` (início PK, fim), `registro_dia` (data PK; sintomas, motivos e táticas como texto de nomes de enum), `evento`, `item_kit`. Enums pelo nome: renomear exige migração. Esquemas em `app/schemas/`; ao mudar entidade, suba a versão e escreva migração (nada de `fallbackToDestructiveMigration`).

## Avisos

Corrente de OneTimeWork igual à do Jardim (`APPEND_OR_REPLACE`; abertura do app chama `garantirAgendado`). Manhã: resumo só com nível ≥ 2 ou mudança de fase (ou todo dia, opção), TPM em 2 dias, kit faltando 3 dias antes, prevista hoje / atrasada (com botão "Começou", `ComecouReceiver`), evento em conflito 3 dias antes. Noite: lembrete de registrar só na TPM e se o dia ainda não foi registrado.

## Comandos

- `./dev.sh test`: testes do motor (segundos).
- `./dev.sh run`: build release (R8), instala e abre no aparelho conectado (Fold7 via USB ou o emulador `trombadario_test`, que sobe com `./dev.sh emulator`).
- `./dev.sh print nome`: print em `prints/` (fora do git).
- `./dev.sh release`: do `main` limpo, envia e dispara `.github/workflows/release.yml`, que testa, builda, confere a assinatura e publica a `vX.Y.Z` (do `versionName`) com o APK. Suba `versionCode` e `versionName` antes. O app mostra o cartão "Versão X disponível" na tela inicial; o toque na versão, em Configurações, procura na hora.
- JDK: `/opt/homebrew/opt/openjdk@17`; SDK: `/opt/homebrew/share/android-commandlinetools`.
- O release é assinado com a chave de debug deste Mac (o CI usa a mesma, do secret `DEBUG_KEYSTORE`; o `release.yml` confere o SHA-256): instala por cima sem perder dados. De outra chave, exige desinstalar (apaga tudo; faça backup antes).

## Commits

Conventional commits em português com gitmoji (`feat: ✨ …`, `fix: 🐛 …`), igual aos outros repos.
