# Smart Tag BLE Test

Projeto Android desenvolvido em Kotlin para testar a comunicação com um chaveiro BLE.

## Objetivo

O objetivo principal do app é verificar se o chaveiro BLE envia corretamente o valor \`01\` ao apertar o botão, igual acontece no nRF Connect.

## Funcionalidades

- Escaneia o chaveiro BLE pelo MAC \`FF:FF:11:98:9C:E8\`
- Conecta via BLE GATT
- Procura o serviço \`FFE0\`
- Ativa notificações na característica \`FFE1\`
- Recebe o valor \`01\` quando o botão do chaveiro é apertado
- Conta cliques: 1, 2, 3, 4 e 5+
- Executa ações de teste usando Toast e som

## Como abrir o projeto

1. Extraia o arquivo ZIP, se o projeto estiver compactado.
2. Abra a pasta \`SmartTagBLETest\` no Android Studio.
3. Aguarde o Gradle sincronizar o projeto.
4. Rode o app em um celular físico.
5. Permita Bluetooth e localização caso o Android solicite.
6. Toque em **Conectar no chaveiro**.

## Dispositivo usado no teste

- MAC do chaveiro: \`FF:FF:11:98:9C:E8\`
- Serviço BLE esperado: \`FFE0\`
- Característica BLE esperada: \`FFE1\`
- Valor recebido ao apertar o botão: \`01\`

## Observações

A gravação de áudio e o uso da câmera ainda estão como marcadores de lugar.

O primeiro teste importante é confirmar se o app consegue receber o valor \`01\` do chaveiro, da mesma forma que aparece no nRF Connect.

## Status

Projeto em fase de teste e validação da comunicação BLE.
