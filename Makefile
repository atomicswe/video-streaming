.PHONY: frontend backend all

# Run the frontend using Python's HTTP server
frontend:
	cd frontend && make run

# Run the backend using Maven
backend:
	cd backend && make build-and-test-and-run

# Run both services (frontend in background)
all:
	@echo "Starting both frontend and backend services..."
	@make frontend & make backend