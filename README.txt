SMART TAG BLE TEST

Este é um projeto Android completo em Kotlin para testar o chaveiro BLE.

O que ele faz:
- escaneia o chaveiro pelo MAC FF:FF:11:98:9C:E8
- conecta via BLE GATT
- procura o serviço FFE0
- ativa notification na característica FFE1
- recebe o valor 01 quando o botão é apertado
- conta 1, 2, 3, 4, 5+ cliques
- executa ações de teste com Toast e som

Como abrir:
1. Extraia o ZIP.
2. Abra a pasta SmartTagBLETest no Android Studio.
3. Espere o Gradle sincronizar.
4. Rode no celular físico.
5. Permita Bluetooth/localização se pedir.
6. Clique em "Conectar no chaveiro".

Observação:
A gravação de áudio e câmera ainda estão como marcador de lugar.
Primeiro teste é confirmar que o app recebe o 01 igual o nRF Connect.
