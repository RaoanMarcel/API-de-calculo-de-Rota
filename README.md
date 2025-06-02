Otimizador de Rotas com Ktor e Algoritmo Genético

Este projeto é uma API REST desenvolvida com Ktor (Kotlin) que permite:

    Receber coordenadas geográficas de usuários.

    Calcular a rota mais eficiente entre os pontos usando um algoritmo genético.

    Obter rotas otimizadas via API do Google Maps.

Funcionalidades

    POST /coordenadas: Recebe coordenadas geográficas no formato JSON.

    GET /coordenadas: Retorna todas as coordenadas armazenadas.

    POST /rota-genetica: Calcula a melhor rota entre os pontos usando algoritmo genético.

    POST /rota-google: Retorna a rota otimizada utilizando a API do Google Maps.

Tecnologias Utilizadas

    Ktor: Framework assíncrono para criação de servidores em Kotlin.

    Kotlinx Serialization: Para serialização e desserialização de objetos JSON.

    Coroutines: Para operações assíncronas não bloqueantes.

    Google Maps API: Para cálculo de rotas otimizadas.

