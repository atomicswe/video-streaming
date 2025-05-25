.PHONY: build run test clean

# Build the project
build:
	./mvnw clean package -DskipTests

# Run the application
run:
	./mvnw spring-boot:run

# Run the tests
test:
	./mvnw test

# Clean the project
clean:
	./mvnw clean

# Build and run the application
build-and-run: build run

# Build and test the project
build-and-test: build test

# Build, run the tests and run the application
build-and-test-and-run: build test run

# Show help
help:
	@echo "Available commands:"
	@echo "  make build        - Build the project"
	@echo "  make run         - Run the application"
	@echo "  make clean       - Clean the project"
	@echo "  make build-and-run - Build and run the application"
	@echo "  make help        - Show this help message"
