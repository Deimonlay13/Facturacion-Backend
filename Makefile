# Comandos rápidos del proyecto Facturación DTE
# Uso:  make dev   |   make build   |   make jar   |   make stop   |   make restart

.PHONY: dev run build jar stop restart

## dev / run: levanta la app en modo desarrollo -> http://localhost:8080
dev:
	./mvnw spring-boot:run

run: dev

## build: compila y empaqueta el .jar (sin tests)
build:
	./mvnw clean package -DskipTests

## jar: empaqueta y corre el .jar resultante
jar: build
	java -jar target/facturacion-backend-0.0.1-SNAPSHOT.jar

## stop: libera el puerto 8080 (mata la instancia en ejecución)
stop:
	-@lsof -ti:8080 | xargs kill -9 2>/dev/null || true
	@echo "puerto 8080 liberado"

## restart: para la instancia y la vuelve a levantar
restart: stop dev
