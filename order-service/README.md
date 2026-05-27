

com.rpe.orderservice
├── core                        # O Hexágono (Regras de Negócio)
│   ├── domain                  # Entidades, Enums e Exceções de Domínio
│   └── ports                   # Contratos/Interfaces
│       ├── inbound             # Interfaces que os Casos de Uso implementam (ex: CreateOrderUseCase)
│       └── outbound            # Interfaces que a infraestrutura implementa (ex: OrderRepositoryPort)
├── adapters                    # A Infraestrutura (Frameworks, BD, APIs)
│   ├── inbound                 # Quem chama a nossa aplicação (REST Controllers, SQS Listeners)
│   └── outbound                # Quem a nossa aplicação chama (Spring Data Repositories, Feign Clients)
└── config                      # Configurações globais (Beans, Security, Error Handler, Redis)