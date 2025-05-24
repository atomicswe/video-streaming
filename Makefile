.PHONY: frontend backend all

# Run the frontend using Python's HTTP server
frontend:
	cd frontend && python3.13 -m http.server 80

# Run the backend using Maven
backend:
	cd backend && ./mvnw spring-boot:run

# Run both services (frontend in background)
all:
	@echo "Starting both frontend and backend services..."
	@make frontend & make backend